package cn.manstep.phonemirrorBox.bridge;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import com.ecarx.eas.sdk.ECarXApiClient;
import com.ecarx.eas.sdk.mediacenter.*;
import com.zqsdk.callBack.IInputCallback;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/** Only the car-side music registration. All explicit controls use the existing F25 client. */
final class F25Direct {
    interface Result { void done(boolean ok); }
    private static final AtomicBoolean LEASE = new AtomicBoolean();
    private final Handler main = new Handler(Looper.getMainLooper());
    private final Context context;
    private final Result ready;
    private final ThreadPoolExecutor io = new ThreadPoolExecutor(1, 1, 0, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(16), r -> new Thread(r, "USBBox-F25"));
    private final AtomicBoolean updateQueued = new AtomicBoolean();
    private final MusicClient client;
    private volatile boolean closed, registered, failed;
    private boolean ownsLease;
    private volatile MusicPlaybackInfo info = new MusicPlaybackInfo();
    private MediaCenterAPI api;
    private Object token;

    F25Direct(Context context, IInputCallback input, Result ready) {
        this.context = context; this.ready = ready;
        final com.zqsdk.OooOo0 original = new com.zqsdk.OooOo0(input);
        client = new MusicClient() {
            private boolean active() { return registered && !closed && !failed; }
            @Override public boolean onPlay() { return active() && original.onPlay(); }
            @Override public boolean onPause() { return active() && original.onPause(); }
            @Override public boolean onNext() { return active() && original.onNext(); }
            @Override public boolean onPrevious() { return active() && original.onPrevious(); }
            @Override public boolean onForward() { return active() && original.onForward(); }
            @Override public boolean onRewind() { return active() && original.onRewind(); }
            @Override public MusicPlaybackInfo getMusicPlaybackInfo() { return info; }
            @Override public int getCurrentSourceType() { return 6; }
            @Override public int[] getMediaSourceTypeList() { return new int[]{6}; }
        };
    }
    void start() {
        ownsLease = LEASE.compareAndSet(false, true);
        if (!ownsLease) { fail(); return; }
        work(() -> {
            api = MediaCenterAPI.get(context);
            api.init(context, new ECarXApiClient.Callback() {
                @Override public void onAPIReady(boolean value) {
                    if (closed || failed) return;
                    if (!value) { fail(); return; }
                    work(() -> {
                        if (registered) return;
                        token = ((IMediaCenterAPI) api).registerMusic(context.getPackageName(), client);
                        if (token == null) { fail(); return; }
                        if (closed) return; // close's serial fence will unregister this token
                        registered = true; publish();
                        main.post(() -> { if (!closed && !failed) ready.done(true); });
                    });
                }
            });
        });
        main.postDelayed(() -> { if (!closed && !registered) fail(); }, 10000);
    }
    private void work(Runnable action) {
        if (closed || failed) return;
        try {
            io.execute(() -> {
                if (closed || failed) return;
                try { action.run(); } catch (RuntimeException | LinkageError error) { fail(); }
            });
        } catch (RejectedExecutionException error) { fail(); }
    }
    private void fail() {
        if (closed || failed) return;
        failed = true;
        main.post(() -> { if (!closed) ready.done(false); });
    }
    void update(TrackState track) {
        final String title = track.title, artist = track.artist, album = track.album;
        final int playing = track.playing() ? 1 : 0;
        Intent launch = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
        final PendingIntent pending = launch == null ? null : PendingIntent.getActivity(context, 28104, launch,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        info = new MusicPlaybackInfo() {
            @Override public String getTitle() { return title; }
            @Override public String getArtist() { return artist; }
            @Override public String getAlbum() { return album; }
            @Override public int getPlaybackStatus() { return playing; }
            @Override public int getSourceType() { return 6; }
            @Override public String getPackageName() { return context.getPackageName(); }
            @Override public String getAppName() { return "CarPlay"; }
            @Override public PendingIntent getLaunchIntent() { return pending; }
            @Override public PendingIntent getPlayerIntent() { return pending; }
        };
        if (registered && updateQueued.compareAndSet(false, true)) work(() -> {
            updateQueued.set(false); publish();
        });
    }
    private void publish() {
        if (closed || !registered) return;
        ((IMediaCenterAPI) api).updateCurrentSourceType(token, 6);
        ((IMediaCenterAPI) api).updateMusicPlaybackState(token, info);
    }
    void requestPlay(Result done) {
        if (closed || failed || !registered) { done.done(false); return; }
        work(() -> {
            boolean ok = ((IMediaCenterAPI) api).requestPlay(token);
            main.post(() -> { if (!closed) done.done(ok && !failed); });
        });
    }
    void close(Result done) {
        if (closed) { done.done(false); return; }
        closed = true; registered = false;
        // Only our own pending car SDK work is discarded. An in-progress operation
        // completes before this unregister fence; USB work never enters this queue.
        io.getQueue().clear();
        AtomicBoolean replied = new AtomicBoolean();
        Result finish = ok -> {
            if (replied.compareAndSet(false, true)) done.done(ok);
        };
        main.postDelayed(() -> finish.done(false), 5000);
        try {
            io.execute(() -> {
                boolean ok = false;
                try {
                    ok = !ownsLease && LEASE.get() ? false : token == null || ((IMediaCenterAPI) api).unregister(token);
                    if (ok && api != null) api.release();
                } catch (RuntimeException | LinkageError ignored) { ok = false; }
                if (ok && ownsLease) LEASE.set(false);
                final boolean confirmed = ok;
                main.post(() -> finish.done(confirmed));
                io.shutdown();
            });
        } catch (RejectedExecutionException error) { finish.done(false); }
    }
}
