package cn.manstep.phonemirrorBox.bridge;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.MediaMetadata;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.*;
import android.util.Log;
import android.view.KeyEvent;
import com.zqsdk.callBack.IInputCallback;
import org.json.JSONObject;

/** Read-only box metadata observer plus explicitly requested controls on the original F25 chain. */
public final class UsbMediaBridge {
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static volatile Runtime live;
    private UsbMediaBridge() {}
    public static void start(Context context, IInputCallback input) {
        if (context == null || input == null || Build.VERSION.SDK_INT < 28) return;
        MAIN.post(() -> {
            if (live != null) return;
            try {
                Runtime r = new Runtime(context.getApplicationContext(), input);
                live = r; r.observe(null); r.route.start(); MAIN.post(r.poll);
            } catch (RuntimeException | LinkageError error) { Log.e("USBMediaBridge", "Start failed", error); }
        });
    }
    public static void metadata(String json) {
        // The original parser/distribution has already run unchanged before this hook.
        Runtime r = live;
        if (r != null && json != null && json.length() <= 65536) r.observe(json);
    }
    public static void metadataObject(JSONObject value) {
        if (value != null && live != null) metadata(value.toString());
    }
    /** Already-decoded dashboard cover event; never reads or changes the USB stream. */
    public static void artwork(byte[] value) {
        Runtime r = live;
        if (r == null || value == null || value.length == 0 || value.length > 4 * 1024 * 1024) return;
        byte[] copy = value.clone();
        MAIN.post(() -> { if (live == r) r.artwork(copy); });
    }
    public static void close() {
        MAIN.post(() -> { Runtime r = live; live = null; if (r != null) r.close(); });
    }
    public static void refresh() { MAIN.post(() -> { if (live != null) live.route.refresh(); }); }
    public static String status() {
        Runtime r = live; return r == null ? "等待 F25 媒体服务初始化" : r.route.status;
    }
    private static String text(JSONObject value, String key) {
        Object field = value.opt(key); return field instanceof String ? (String) field : null;
    }
    private static Long number(JSONObject value, String key) {
        Object field = value.opt(key); return field instanceof Number ? ((Number) field).longValue() : null;
    }
    private static final class Runtime implements BridgeRoute.Listener {
        final TrackState track = new TrackState();
        final BoxClock clock = new BoxClock();
        final java.util.concurrent.ThreadPoolExecutor pictures = new java.util.concurrent.ThreadPoolExecutor(1, 1, 30,
                java.util.concurrent.TimeUnit.SECONDS, new java.util.concurrent.ArrayBlockingQueue<>(1),
                task -> new Thread(task, "USBBox-Cover"), new java.util.concurrent.ThreadPoolExecutor.DiscardOldestPolicy());
        Bitmap cover;
        long coverRevision, publishedPosition = -2;
        final MediaSession session;
        final BridgeRoute route;
        final com.zqsdk.OooOo0 original;
        private final Object wire = new Object();
        private boolean observed, connected, active;
        private long stamp, claimRevision;
        private String metadataKey = "";
        private int publishedState = Integer.MIN_VALUE;
        private final Runnable poll = new Runnable() {
            @Override public void run() {
                if (live != Runtime.this) return;
                observe(null); MAIN.postDelayed(this, 1000);
            }
        };
        Runtime(Context c, IInputCallback input) {
            original = new com.zqsdk.OooOo0(input);
            route = new BridgeRoute(c, input, this);
            session = new MediaSession(c, "USBBox@MediaBridge");
            session.setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS | MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS);
            session.setCallback(new Controls(this), MAIN);
            Intent launch = c.getPackageManager().getLaunchIntentForPackage(c.getPackageName());
            if (launch != null) session.setSessionActivity(PendingIntent.getActivity(c, 28103, launch,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
        }
        void observe(String json) {
            synchronized (wire) {
                boolean next = cn.manstep.phonemirrorBox.BoxInterface.f.P; // READ ONLY
                if (!observed || next != connected) {
                    observed = true; connected = next; final long expected = ++stamp;
                    MAIN.post(() -> {
                        if (!current(expected)) return;
                        track.reset(); clock.reset(); cover = null; coverRevision++; claimRevision++;
                        route.update(track, isConnected() ? Long.toString(expected) : ""); publish();
                    });
                }
                if (!connected || json == null) return;
                final long expected = stamp;
                MAIN.post(() -> {
                    if (!current(expected) || !isConnected()) return;
                    try {
                        JSONObject value = new JSONObject(json);
                        Integer state = value.has("MediaPlayStatus") && !value.isNull("MediaPlayStatus")
                                ? value.optInt("MediaPlayStatus", -1) : null;
                        boolean before = track.playing(), hadTrack = track.hasTrack();
                        long previousRevision = track.revision;
                        track.update(text(value, "MediaSongName"), text(value, "MediaArtistName"),
                                text(value, "MediaAlbumName"), state);
                        if (previousRevision != track.revision) { cover = null; coverRevision++; clock.newTrack(); }
                        track.updateLyrics(text(value, "MediaLyrics"));
                        clock.update(number(value, "MediaSongDuration"), number(value, "MediaSongPlayTime"), track.playing(), SystemClock.elapsedRealtime());
                        route.update(track, Long.toString(expected)); publish();
                        if (before && !track.playing()) { claimRevision++; route.suspend(); }
                        if (track.playing() && (!before || !hadTrack && track.hasTrack())) claim();
                    } catch (Exception error) { Log.w("USBMediaBridge", "Metadata ignored", error); }
                });
            }
        }
        private boolean current(long expected) { synchronized (wire) { return live == this && expected == stamp; } }
        private boolean isConnected() {
            synchronized (wire) { return connected && cn.manstep.phonemirrorBox.BoxInterface.f.P; }
        }
        private void artwork(byte[] bytes) {
            if (!isConnected() || !track.complete()) return;
            final long connection = stamp, song = track.revision;
            final String identity = track.mediaId();
            pictures.execute(() -> {
                Bitmap decoded = null;
                try {
                    BitmapFactory.Options options = new BitmapFactory.Options(); options.inJustDecodeBounds = true;
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);
                    if (options.outWidth <= 0 || options.outHeight <= 0 || (long)options.outWidth * options.outHeight > 16000000) return;
                    options.inJustDecodeBounds = false; options.inSampleSize = 1;
                    while (Math.max(options.outWidth, options.outHeight) / options.inSampleSize > 256) options.inSampleSize *= 2;
                    decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);
                } catch (RuntimeException error) { Log.w("USBMediaBridge", "Cover ignored", error); }
                final Bitmap picture = decoded;
                MAIN.post(() -> {
                    if (!current(connection) || !isConnected() || song != track.revision || !identity.equals(track.mediaId())) return;
                    if (picture != null) { cover = picture; coverRevision++; publish(); }
                });
            });
        }
        @Override public void changed() { claimRevision++; publish(); }
        @Override public void ready() { publish(); if (track.playing()) claim(); }
        @Override public void yielded() { claimRevision++; }
        private void claim() {
            final long revision = ++claimRevision;
            requestClaim(revision, 0);
        }
        private void requestClaim(long revision, int attempt) {
            MAIN.postDelayed(() -> {
                if (live != this || revision != claimRevision || !isConnected() || !route.ready() || !track.hasTrack()) return;
                route.requestPlay(ok -> {
                    // Registration/grant can race the Android session notification.
                    // Retry only car-side selection; NEVER synthesize a USB PLAY.
                    if (!ok && revision == claimRevision && attempt < 2) requestClaim(revision, attempt + 1);
                });
            }, attempt == 0 ? 150 : 500);
        }
        private void publish() {
            boolean visible = isConnected() && track.hasTrack() && "BRIDGE".equals(route.route());
            String key = visible ? track.mediaId() + "|" + track.lyrics + "|" + clock.duration() + "|" + coverRevision : "";
            if (!key.equals(metadataKey) || publishedState == Integer.MIN_VALUE) {
                MediaMetadata.Builder m = new MediaMetadata.Builder();
                if (visible) {
                    m.putString(MediaMetadata.METADATA_KEY_TITLE, track.title);
                    m.putString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE, track.title);
                    m.putString(MediaMetadata.METADATA_KEY_ARTIST, track.artist);
                    m.putString(MediaMetadata.METADATA_KEY_ALBUM, track.album);
                    m.putString(MediaMetadata.METADATA_KEY_MEDIA_ID, track.mediaId());
                    m.putString("android.media.metadata.LYRICS", track.lyrics);
                    if (clock.duration() > 0) m.putLong(MediaMetadata.METADATA_KEY_DURATION, clock.duration());
                    if (cover != null) m.putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, cover);
                }
                // Time is published only after measured unit confirmation. No seek capability.
                session.setMetadata(m.build()); metadataKey = key;
            }
            int state = !visible || track.status == -1 ? PlaybackState.STATE_NONE
                    : track.playing() ? PlaybackState.STATE_PLAYING : PlaybackState.STATE_PAUSED;
            long position = visible ? clock.position() : -1;
            if (state != publishedState || position != publishedPosition) {
                long actions = PlaybackState.ACTION_PLAY | PlaybackState.ACTION_PAUSE | PlaybackState.ACTION_PLAY_PAUSE
                        | PlaybackState.ACTION_STOP | PlaybackState.ACTION_SKIP_TO_NEXT | PlaybackState.ACTION_SKIP_TO_PREVIOUS;
                session.setPlaybackState(new PlaybackState.Builder().setActions(actions)
                        .setState(state, position,
                                state == PlaybackState.STATE_PLAYING && position >= 0 ? 1f : 0f,
                                position >= 0 ? clock.observedAt() : SystemClock.elapsedRealtime()).build());
                publishedState = state; publishedPosition = position;
            }
            // Paused tracks remain reachable, so a subsequent PLAY can reach the phone.
            if (active != visible) { session.setActive(visible); active = visible; }
        }
        @Override public void command(String action) {
            if (live != this || !isConnected() || !"BRIDGE".equals(route.route())) return;
            if ("TOGGLE".equals(action)) action = track.playing() ? "PAUSE" : "PLAY";
            try {
                switch (action) {
                    // Dispatch immediately and exactly once. A late car-side grant can
                    // never produce a delayed phone PLAY after the user has paused.
                    case "PLAY": original.onPlay(); claim(); break;
                    case "PAUSE": case "STOP": claimRevision++; route.suspend(); original.onPause(); break;
                    case "NEXT": original.onNext(); break;
                    case "PREVIOUS": original.onPrevious(); break;
                    case "FAST_FORWARD": original.onForward(); break;
                    case "REWIND": original.onRewind(); break;
                    default: break;
                }
            } catch (RuntimeException error) { Log.e("USBMediaBridge", "Original control failed", error); }
        }
        void close() {
            claimRevision++; MAIN.removeCallbacks(poll); route.close(); pictures.shutdownNow();
            session.setActive(false); session.release();
        }
    }
    private static final class Controls extends MediaSession.Callback {
        private final Runtime owner;
        Controls(Runtime owner) { this.owner = owner; }
        @Override public void onPlay() { owner.command("PLAY"); }
        @Override public void onPause() { owner.command("PAUSE"); }
        @Override public void onStop() { owner.command("STOP"); }
        @Override public void onSkipToNext() { owner.command("NEXT"); }
        @Override public void onSkipToPrevious() { owner.command("PREVIOUS"); }
        @Override public void onFastForward() { owner.command("FAST_FORWARD"); }
        @Override public void onRewind() { owner.command("REWIND"); }
        @Override public boolean onMediaButtonEvent(Intent intent) {
            KeyEvent event = intent == null ? null : intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
            if (event == null) return false;
            String action;
            switch (event.getKeyCode()) {
                case KeyEvent.KEYCODE_MEDIA_PLAY: action = "PLAY"; break;
                case KeyEvent.KEYCODE_MEDIA_PAUSE: case KeyEvent.KEYCODE_MEDIA_STOP: action = "PAUSE"; break;
                case KeyEvent.KEYCODE_MEDIA_NEXT: action = "NEXT"; break;
                case KeyEvent.KEYCODE_MEDIA_PREVIOUS: action = "PREVIOUS"; break;
                case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE: case KeyEvent.KEYCODE_HEADSETHOOK: action = "TOGGLE"; break;
                default: return false;
            }
            if (event.getAction() == KeyEvent.ACTION_DOWN && event.getRepeatCount() == 0) owner.command(action);
            return true;
        }
    }
}
