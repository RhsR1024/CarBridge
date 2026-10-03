package com.geely.auto.music;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.IBinder;

import com.mediabridge.app.R;
import com.mediabridge.app.BuildConfig;

/** Android lifecycle adapter around the single BridgeCoordinator. */
public class UniversalBridgeService extends Service {
    public static final String ACTION_SNAPSHOT = BridgeEvents.ACTION_SNAPSHOT;
    private static final String LEGACY_ACTION_SNAPSHOT = "com.geely.auto.music.UPDATE_METADATA";
    private static final String LEGACY_ACTION_CLEAR = "com.geely.auto.music.CLEAR_METADATA";
    private static final String LEGACY_ACTION_REQUEST_FOCUS = "com.geely.auto.music.ACTION_REQUEST_FOCUS";
    private static final String CHANNEL_ID = "mediabridge_status";
    private static final int NOTIFICATION_ID = 1001;
    private static final String VENDOR_SUPPORT_ACTION = "ecarx.xsf.mediacenter.intent.action.SUPPORT_SERVICE";
    static UniversalBridgeService live;
    static boolean companionReady() { return live != null && live.coordinator != null && live.coordinator.isReady(); }
    static void quiesceManaged(String pkg, java.util.function.Consumer<Boolean> result) {
        if (live == null || live.coordinator == null) { result.accept(!CarBridgeCompanionService.uncertainOutput); return; }
        live.coordinator.quiesceManaged(pkg, result);
    }
    static void prepareManaged(PlayerSnapshot snapshot) { if (companionReady()) live.coordinator.prepareManaged(snapshot); }
    static void pauseManaged(String pkg) { if (live != null && live.coordinator != null) live.coordinator.pauseManaged(pkg); }
    static void playManaged(PlayerSnapshot snapshot, java.util.function.Consumer<Boolean> result) {
        if (!companionReady()) { result.accept(false); return; }
        live.coordinator.requestManagedPlay(snapshot, result);
    }
    private SettingsRepository settings;
    private BridgeCoordinator coordinator;
    private boolean coordinatorStarted;

    @Override public void onCreate() {
        super.onCreate();
        live = this;
        settings = new SettingsRepository(this);
        BridgeStateStore.setOutputRunning(settings.isBridgeEnabled());
        createChannel();
        startForeground(NOTIFICATION_ID, buildNotification("正在初始化桥接"));
        coordinator = new BridgeCoordinator(
                mode -> mode == BackendMode.PHONE_DEBUG
                        ? new PhoneDebugBackend(this) : new LegacyBackend(this),
                new HandlerBridgeScheduler(), new BackoffPolicy(), new CoordinatorObserver());
        coordinator.updateCollectionIgnoredPackages(settings.getFavoriteIgnoredPackages());
        BackendMode mode = configuredMode();
        coordinator.updateSettings(mode, settings.getTuneMs(mode), settings.isLyricsEnabled(),
                settings.isShowCollectionEnabled());
        if (!settings.isBridgeEnabled()) {
            BridgeStateStore.setOutputState("已停止（用户未启用）");
            stopSelf();
        }
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (!settings.isBridgeEnabled()) {
            stopSelf();
            return START_NOT_STICKY;
        }
        String action = intent == null ? null : intent.getAction();
        boolean legacyAction = LEGACY_ACTION_SNAPSHOT.equals(action)
                || LEGACY_ACTION_CLEAR.equals(action)
                || LEGACY_ACTION_REQUEST_FOCUS.equals(action);
        if ((BridgeEvents.isInternalServiceAction(action) || legacyAction)
                && !BridgeEvents.isTrustedServiceIntent(this, intent)) {
            DiagnosticsLog.w("Rejected unauthenticated internal bridge action=" + action);
            if (!coordinatorStarted) stopSelf(startId);
            return coordinatorStarted ? START_STICKY : START_NOT_STICKY;
        }
        boolean legacySnapshot = LEGACY_ACTION_SNAPSHOT.equals(action);
        boolean legacyClear = LEGACY_ACTION_CLEAR.equals(action);
        boolean legacyFocus = LEGACY_ACTION_REQUEST_FOCUS.equals(action);
        if (intent == null || VENDOR_SUPPORT_ACTION.equals(action)
                || legacySnapshot || legacyClear || legacyFocus
                || BridgeEvents.isInternalServiceAction(action)) {
            ensureCoordinatorStarted();
        } else {
            DiagnosticsLog.w("Ignored unknown external bridge action=" + action);
            if (!coordinatorStarted) stopSelf(startId);
            return coordinatorStarted ? START_STICKY : START_NOT_STICKY;
        }
        if (intent != null) {
            if ((ACTION_SNAPSHOT.equals(action) || BridgeEvents.ACTION_INPUT_INVALIDATED.equals(action))
                    && intent.hasExtra("inputEpoch")
                    && intent.getLongExtra("inputEpoch", -1) != BridgeStateStore.inputEpoch()) {
                DiagnosticsLog.i("SOURCE_PRIORITY dropped stale input event");
                return START_STICKY;
            }
            if (ACTION_SNAPSHOT.equals(action) || legacySnapshot) {
                String sourcePackage = intent.getStringExtra("package");
                if (io.github.rhsr1024.interop.BridgeProtocol.managed(sourcePackage)
                        && !CarBridgeCompanionService.allows(sourcePackage)) return START_STICKY;
                coordinator.updateSnapshot(snapshotFromIntent(intent));
            } else if (BridgeEvents.ACTION_INPUT_INVALIDATED.equals(action) || legacyClear) {
                String nativeOwner = intent.getStringExtra("nativeOwner");
                if (nativeOwner != null && !nativeOwner.isEmpty()) coordinator.yieldToNative(nativeOwner);
                coordinator.updateSnapshot(null);
            } else if (legacyFocus) {
                coordinator.requestPlayFocus();
            } else if (BridgeEvents.ACTION_SETTINGS_CHANGED.equals(action)) {
                coordinator.updateCollectionIgnoredPackages(settings.getFavoriteIgnoredPackages());
                BackendMode mode = configuredMode();
                coordinator.updateSettings(mode, settings.getTuneMs(mode), settings.isLyricsEnabled(),
                        settings.isShowCollectionEnabled());
                if (coordinatorStarted && coordinator.targetMode() != mode) {
                    // This is an explicit debug/vehicle selection, so do not use hot-switch rollback:
                    // a missing vehicle service on a phone must not silently re-enable debug mode.
                    DiagnosticsLog.i("Restarting bridge for explicit mode selection=" + mode.value());
                    coordinator.stop();
                    coordinatorStarted = false;
                    ensureCoordinatorStarted();
                }
            } else if (BridgeEvents.ACTION_RECONNECT.equals(action)) {
                DiagnosticsLog.i("Manual bridge reconnect requested");
                coordinator.reconnect();
            } else if (BridgeEvents.ACTION_RETRY_LYRICS.equals(action)) {
                coordinator.retryLyrics();
            } else if (BridgeEvents.ACTION_PROBE_IOU.equals(action)) {
                coordinator.probeNativeIou();
            }
        }
        return START_STICKY;
    }

    private void ensureCoordinatorStarted() {
        if (coordinatorStarted || coordinator == null) return;
        coordinatorStarted = true;
        if (!BridgeStateStore.getNativeOwner().isEmpty()) coordinator.yieldToNative(BridgeStateStore.getNativeOwner());
        coordinator.start(configuredMode());
        coordinator.updateSnapshot(BridgeStateStore.getSnapshot());
    }

    private BackendMode configuredMode() {
        return BuildConfig.DEBUG && settings.isPhoneDebugEnabled()
                ? BackendMode.PHONE_DEBUG : BackendMode.LEGACY;
    }

    private PlayerSnapshot snapshotFromIntent(Intent intent) {
        String packageName = intent.getStringExtra("package");
        if (packageName == null) return null;
        FavoriteState favorite = FavoriteState.UNKNOWN;
        try { favorite = FavoriteState.valueOf(intent.getStringExtra("favoriteState")); }
        catch (Exception ignored) {}
        PlaybackStatus status = PlaybackStatus.NONE;
        try { status = PlaybackStatus.valueOf(intent.getStringExtra("playbackStatus")); }
        catch (Exception ignored) {}
        if (LEGACY_ACTION_SNAPSHOT.equals(intent.getAction())) {
            int legacyState = intent.getIntExtra("state", 0);
            status = legacyState == 3 ? PlaybackStatus.PLAYING
                    : (legacyState == 1 || legacyState == 2 ? PlaybackStatus.PAUSED : PlaybackStatus.NONE);
        }
        String artwork = intent.getStringExtra("artworkUri");
        if (artwork == null) artwork = intent.getStringExtra("artwork");
        return new PlayerSnapshot(intent.getIntExtra("userId", -1), packageName,
                valueOrPackage(intent.getStringExtra("appName"), packageName),
                valueOrEmpty(intent.getStringExtra("title")),
                valueOrEmpty(intent.getStringExtra("artist")),
                valueOrEmpty(intent.getStringExtra("album")),
                valueOrEmpty(artwork),
                intent.getLongExtra("duration", 0L), intent.getLongExtra("position", 0L),
                status, favorite, intent.getBooleanExtra("favoriteSupported", false),
                intent.getParcelableExtra("sessionActivity"), System.currentTimeMillis(),
                intent.getStringExtra("sessionId"), intent.getStringExtra("trackId"),
                intent.getFloatExtra("speed", 1f), valueOrEmpty(intent.getStringExtra("playerLyrics")),
                valueOrEmpty(intent.getStringExtra("labelSource")), intent.getBooleanExtra("favoritePending", false),
                valueOrEmpty(intent.getStringExtra("favoriteMessage")));
    }

    private static String valueOrEmpty(String value) { return value == null ? "" : value; }
    private static String valueOrPackage(String value, String packageName) {
        return value == null || value.trim().isEmpty() ? packageName : value;
    }

    private void createChannel() {
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                getString(R.string.notification_channel_name), NotificationManager.IMPORTANCE_LOW);
        channel.setDescription(getString(R.string.notification_channel_description));
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) manager.createNotificationChannel(channel);
    }

    private Notification buildNotification(String text) {
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, SettingsActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        return new Notification.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(text).setOngoing(true).setContentIntent(contentIntent).build();
    }

    private void updateNotification(String text) {
        NotificationManager manager = getSystemService(NotificationManager.class);
        boolean allowed = Build.VERSION.SDK_INT < 33
                || checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
        if (manager != null && allowed) manager.notify(NOTIFICATION_ID, buildNotification(text));
        sendBroadcast(BridgeEvents.stateIntent());
    }

    @Override public void onDestroy() {
        CarBridgeCompanionService.backendStopping();
        BridgeStateStore.setOutputRunning(false);
        if (coordinator != null && coordinatorStarted) coordinator.stop();
        final BridgeCoordinator retiring = coordinator;
        if (retiring != null) retiring.quiesceManaged(CarBridgeCompanionService.managedPackage(),
                ok -> { if (ok) CarBridgeCompanionService.uncertainOutput = false; });
        if (live == this) live = null;
        coordinator = null;
        coordinatorStarted = false;
        BridgeStateStore.setOutputState("已停止");
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) { return null; }

    private final class CoordinatorObserver implements BridgeCoordinator.Observer {
        @Override public void onNativeSourceSelected(String ownerPackage) {
            MediaListenerService.yieldToVehicleSource(ownerPackage);
        }
        @Override public void onStateChanged(BackendConnectionState state, String detail, long generation,
                                             BackendMode targetMode, BackendMode activeMode) {
            BridgeStateStore.setOutputState(detail);
            BridgeStateStore.setTargetMode(targetMode.label());
            BridgeStateStore.setActiveMode(activeMode == null ? null : activeMode.label());
            DiagnosticsLog.i("bridge state=" + state + " generation=" + generation
                    + " target=" + targetMode.value() + " active="
                    + (activeMode == null ? "none" : activeMode.value()) + " detail=" + detail);
            updateNotification(detail);
        }

        @Override public void onModeApplied(BackendMode mode) {
            settings.setLastSuccessfulMode(mode.value());
        }

        @Override public void onModeRolledBack(BackendMode failedMode, BackendMode restoredMode, String reason) {
            settings.setBackendMode(restoredMode.value());
            DiagnosticsLog.w("Backend switch rolled back failed=" + failedMode.value()
                    + " restored=" + restoredMode.value() + " reason=" + reason);
        }
    }
}
