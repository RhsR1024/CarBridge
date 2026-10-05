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
    public static void resourcesChanged() { MAIN.post(() -> { if (live != null) live.refreshDisplay(); }); }
    public static void titleFormatChanged() { MAIN.post(() -> {
        if (live != null) {
            live.track.titleFormat = BridgeSettings.titleFormat(live.context);
            live.refreshDisplay();
        }
    }); }
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
        final Context context;
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
        private String lastCommand = "-", lastCommandResult = "-";
        private String formatDiagnostic = "";
        private int publishedState = Integer.MIN_VALUE;
        private final Runnable poll = new Runnable() {
            @Override public void run() {
                if (live != Runtime.this) return;
                observe(null); MAIN.postDelayed(this, 1000);
            }
        };
        Runtime(Context c, IInputCallback input) {
            context = c;
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
                        track.titleFormat = BridgeSettings.titleFormat(context);
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
                        if (previousRevision != track.revision) { cover = null; coverRevision++; track.artworkUri = ""; clock.newTrack(); }
                        track.updateLyrics(text(value, "MediaLyrics"));
                        clock.update(number(value, "MediaSongDuration"), number(value, "MediaSongPlayTime"), track.playing(), SystemClock.elapsedRealtime());
                        syncTime();
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
        /**
         * Controls stay reachable while CarPlay is connected and the car's own direct route is
         * not in charge, so a temporarily missing MediaBridge companion cannot dead-key the
         * vehicle mini window or the steering wheel.
         */
        private boolean controllable() {
            synchronized (wire) {
                return connected && cn.manstep.phonemirrorBox.BoxInterface.f.P && !"DIRECT".equals(route.route());
            }
        }
        private void syncTime() {
            track.durationMs = clock.duration(); track.positionMs = clock.position(); track.positionAtMs = clock.observedAt();
        }
        private void refreshDisplay() {
            if (!isConnected()) return;
            syncTime(); route.update(track, Long.toString(stamp)); publish();
        }
        private void artwork(byte[] bytes) {
            if (!isConnected() || track.title.isEmpty()) return;
            final long connection = stamp, song = track.revision;
            final String identity = track.mediaId();
            pictures.execute(() -> {
                Bitmap decoded = null;
                String stored = "";
                try {
                    BitmapFactory.Options options = new BitmapFactory.Options(); options.inJustDecodeBounds = true;
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);
                    if (options.outWidth <= 0 || options.outHeight <= 0 || (long)options.outWidth * options.outHeight > 16000000) return;
                    options.inJustDecodeBounds = false; options.inSampleSize = 1;
                    while (Math.max(options.outWidth, options.outHeight) / options.inSampleSize > 256) options.inSampleSize *= 2;
                    decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);
                    if (decoded != null) try { stored = DirectArtworkCache.save(context, decoded); }
                    catch (Exception error) { Log.w("USBMediaBridge", "Direct cover cache unavailable", error); }
                } catch (RuntimeException error) { Log.w("USBMediaBridge", "Cover ignored", error); }
                final Bitmap picture = decoded;
                final String uri = stored;
                MAIN.post(() -> {
                    if (!current(connection) || !isConnected() || song != track.revision || !identity.equals(track.mediaId())) return;
                    if (picture != null) { cover = picture; coverRevision++; track.artworkUri = uri; refreshDisplay(); }
                });
            });
        }
        @Override public void changed() { claimRevision++; publish(); }
        @Override public void ready() { publish(); if (track.playing()) claim(); }
        @Override public void yielded() { claimRevision++; }
        private String pendingSkip;
        private long pendingSkipAt;
        private final Runnable pendingSkipPoll = new Runnable() {
            @Override public void run() {
                if (live != Runtime.this || pendingSkip == null) return;
                if (!controllable()) { pendingSkip = null; return; }
                boolean playing = track.playing();
                // The reported flag trails the phone by up to one metadata callback (about
                // 460 ms here), so cap the wait just past the measured report latency.
                if (!playing && SystemClock.elapsedRealtime() - pendingSkipAt < 900) {
                    MAIN.postDelayed(this, 100); return;
                }
                dispatchPendingSkip(playing);
            }
        };
        /**
         * The phone ignores a skip while it is paused, so resume first and dispatch the skip as
         * soon as the box reports playback again. A newer skip flushes the pending one instead
         * of losing it.
         */
        private void skipAfterResume(String action) {
            flushPendingSkip();
            pendingSkip = action; pendingSkipAt = SystemClock.elapsedRealtime();
            original.onPlay(); claim();
            lastCommandResult = action + " resumeFirst";
            publish();
            MAIN.postDelayed(pendingSkipPoll, 100);
        }
        private void flushPendingSkip() {
            if (pendingSkip != null) dispatchPendingSkip(track.playing());
        }
        private void dispatchPendingSkip(boolean playing) {
            String action = pendingSkip; pendingSkip = null;
            if (action == null) return;
            boolean ok = "PREVIOUS".equals(action) ? original.onPrevious() : original.onNext();
            lastCommandResult = action + " skipped playing=" + playing + " ok=" + ok;
            publish();
        }
        private void claim() {
            final long revision = ++claimRevision;
            requestClaim(revision, 0);
        }
        private void requestClaim(long revision, int attempt) {
            MAIN.postDelayed(() -> {
                if (live != this || revision != claimRevision || !isConnected() || !route.ready()) return;
                route.requestPlay(ok -> {
                    // Registration/grant can race the Android session notification.
                    // Retry only car-side selection; NEVER synthesize a USB PLAY.
                    if (!ok && revision == claimRevision && attempt < 2) requestClaim(revision, attempt + 1);
                });
            }, attempt == 0 ? 150 : 500);
        }
        // MediaMetadata permits custom keys; these carry diagnostics and the existing lyrics contract.
        @android.annotation.SuppressLint("WrongConstant")
        private void publish() {
            CombinedTitleMetadata display = track.display();
            String diagnostic = track.mediaId() + "|" + track.titleFormat + "|" + display.reason;
            if (!diagnostic.equals(formatDiagnostic)) {
                formatDiagnostic = diagnostic;
                Log.i("USBMediaBridge", "TitleFormat format=" + track.titleFormat + " reason=" + display.reason);
            }
            boolean visible = isConnected() && "BRIDGE".equals(route.route());
            // Bridge state and the last control result travel through the session so the
            // companion's exported log shows them even while this outlet is not active.
            String diag = "route=" + route.route() + " conn=" + (isConnected() ? "1" : "0")
                    + " ctrl=" + (controllable() ? "1" : "0")
                    + " cmd=" + lastCommand + " res=" + lastCommandResult
                    + " clock=" + clock.diagnostic();
            String key = (visible ? stamp + "|" + track.mediaId() + "|" + display.title + "|" + display.artist
                    + "|" + track.lyrics + "|" + clock.duration() + "|" + coverRevision : "") + "|" + diag;
            if (!key.equals(metadataKey) || publishedState == Integer.MIN_VALUE) {
                MediaMetadata.Builder m = new MediaMetadata.Builder();
                m.putString("usb.media.build", BridgeSettings.BUILD);
                m.putString("usb.media.diag", diag);
                if (isConnected()) m.putString("usb.media.connection", Long.toString(stamp));
                if (visible) {
                    m.putString(MediaMetadata.METADATA_KEY_TITLE, display.title);
                    m.putString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE, display.title);
                    m.putString(MediaMetadata.METADATA_KEY_ARTIST, display.artist);
                    m.putString("usb.media.titleFormat", track.titleFormat);
                    m.putString("usb.media.titleFormatReason", display.reason);
                    m.putString(MediaMetadata.METADATA_KEY_ALBUM, track.album);
                    m.putString(MediaMetadata.METADATA_KEY_MEDIA_ID, track.mediaId());
                    m.putString("android.media.metadata.LYRICS", track.lyrics);
                    if (clock.duration() > 0) m.putLong(MediaMetadata.METADATA_KEY_DURATION, clock.duration());
                    if (cover != null) m.putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, cover);
                }
                // Time is published only after measured unit confirmation. No seek capability.
                session.setMetadata(m.build()); metadataKey = key;
            }
            int state = !visible ? PlaybackState.STATE_NONE
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
            // Paused tracks remain reachable, and the session stays visible to the car while the
            // MediaBridge companion is briefly away, so a later PLAY/NEXT can still reach the phone.
            boolean reachable = controllable();
            if (active != reachable) { session.setActive(reachable); active = reachable; }
        }
        @Override public void command(String action) {
            lastCommand = action;
            lastCommandResult = "accepted";
            if (live != this || !controllable()) { lastCommandResult = "rejected"; publish(); return; }
            if ("TOGGLE".equals(action)) action = track.playing() ? "PAUSE" : "PLAY";
            try {
                switch (action) {
                    // Dispatch immediately and exactly once. A late car-side grant can
                    // never produce a delayed phone PLAY after the user has paused.
                    case "PLAY": original.onPlay(); lastCommandResult = "onPlay"; claim(); break;
                    case "PAUSE": case "STOP":
                        claimRevision++; route.suspend(); original.onPause(); lastCommandResult = "onPause"; break;
                    case "NEXT": case "PREVIOUS": {
                        // This box only gets its skip honoured while the phone reports playing
                        // (measured), so a paused skip resumes first and skips once it is back.
                        // A track without identity metadata still needs that path: gating on it
                        // is what made the very first paused skip do nothing.
                        if (track.playing()) {
                            boolean ok = "PREVIOUS".equals(action) ? original.onPrevious() : original.onNext();
                            lastCommandResult = ("PREVIOUS".equals(action) ? "onPrevious=" : "onNext=")
                                    + ok + " playing=true";
                        } else {
                            skipAfterResume(action);
                        }
                        break;
                    }
                    case "FAST_FORWARD": original.onForward(); lastCommandResult = "onForward"; break;
                    case "REWIND": original.onRewind(); lastCommandResult = "onRewind"; break;
                    default: lastCommandResult = "unknown"; break;
                }
            } catch (RuntimeException error) {
                lastCommandResult = "error:" + error.getClass().getSimpleName();
                Log.e("USBMediaBridge", "Original control failed", error);
            }
            publish();
        }
        void close() {
            pendingSkip = null; claimRevision++;
            MAIN.removeCallbacks(poll); MAIN.removeCallbacks(pendingSkipPoll);
            route.close(); pictures.shutdownNow();
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
