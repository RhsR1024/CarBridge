package com.geely.auto.music;

import android.app.PendingIntent;
import android.app.Notification;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.media.MediaMetadata;
import android.media.Rating;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.Settings;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;
import android.widget.Toast;

import java.util.List;
import java.util.Locale;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Compatibility component whose fully-qualified name is kept for the existing notification
 * listener grant. All player policy lives in this class rather than in the UI activity.
 */
public class MediaListenerService extends NotificationListenerService {
    public static final String ACTION_MEDIA_COMMAND = BridgeEvents.ACTION_MEDIA_COMMAND;
    private static final String IOU_PACKAGE = "com.netease.cloudmusic.iou";
    private static final String IOV_PACKAGE = "com.netease.cloudmusic.iov";
    private static final String CARPLAY_PACKAGE = "com.flyme.auto.energy";
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ThreadPoolExecutor probeWorker = new ThreadPoolExecutor(1, 1, 30, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(1), task -> {
                Thread thread = new Thread(task, "MediaBridge-IouProbe");
                thread.setDaemon(true);
                return thread;
            });
    private MediaSessionManager sessionManager;
    private MediaController currentController;
    private final Map<MediaSession.Token, SessionRecord> sessionRecords = new HashMap<>();
    private final Map<String, NotificationRecord> notificationRecords = new HashMap<>();
    private MediaSessionManager.OnActiveSessionsChangedListener sessionsListener;
    private SettingsRepository settings;
    private PlayerNameResolver nameResolver;
    private ArtworkRepository artworkRepository;
    private boolean connected;
    private long playbackOrder, nativeBarrier = -1;
    private String yieldedOwner = "";
    private boolean initialSessionScan = true;
    private static volatile MediaListenerService liveInstance;
    private final Set<MediaSession.Token> destroyedTokens = new HashSet<>();
    private long inputRevision, artworkRevision, pendingFavoriteRevision, uncertainRevision, nextAccessCheck, nextRebind;
    private String lastCarBridgeMetadataProbe = "", lastArtworkProbe = "";
    private int rebindAttempts;
    private String lastPlayingSession = "", lastResumeKey = "";
    private long lastResumeAt = Long.MIN_VALUE, resumeSequence;
    private String pendingFavoriteTrack, pendingFavoriteTitle, pendingFavoriteArtist;
    private String uncertainTrack, favoriteMessage = "", favoriteMessageTrack;
    private Boolean pendingFavorite;
    private String pendingFavoritePackage;
    private String pendingFavoriteSession;
    private long pendingFavoriteDeadline;
    private boolean pendingFavoriteTimeoutLogged;

    private static final class SessionRecord {
        final MediaController controller;
        final MediaController.Callback callback;
        long lastPlayingAt;
        long playingOrder;
        long lastActiveAt;
        boolean wasPlaying;
        boolean wasControlOnly;
        int lastState = PlaybackState.STATE_NONE;
        final PositionTracker positionTracker = new PositionTracker();

        SessionRecord(MediaController controller, MediaController.Callback callback, long now) {
            this.controller = controller;
            this.callback = callback;
            this.lastActiveAt = now;
        }
    }

    private static final class NotificationRecord {
        final String packageName;
        final MediaSession.Token token;
        final MediaController controller;
        final String title;
        final String artist;
        final String artworkUri;
        final android.graphics.drawable.Icon artworkIcon;
        final Boolean liked;
        final PendingIntent likeIntent;
        final PendingIntent unlikeIntent;
        final long postedAt;

        NotificationRecord(String packageName, MediaSession.Token token, MediaController controller,
                           String title, String artist, String artworkUri, android.graphics.drawable.Icon artworkIcon, Boolean liked,
                           PendingIntent likeIntent, PendingIntent unlikeIntent, long postedAt) {
            this.packageName = packageName;
            this.token = token;
            this.controller = controller;
            this.title = title;
            this.artist = artist;
            this.artworkUri = artworkUri;
            this.artworkIcon = artworkIcon;
            this.liked = liked;
            this.likeIntent = likeIntent;
            this.unlikeIntent = unlikeIntent;
            this.postedAt = postedAt;
        }

        boolean supportsFavorite() {
            return likeIntent != null || unlikeIntent != null;
        }
    }
    private final Runnable progressTicker = new Runnable() {
        @Override public void run() {
            checkAccessAndRecover();
            if (connected) refreshFromSystem();
            mainHandler.postDelayed(this, 1000L);
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        settings = new SettingsRepository(this);
        liveInstance = this;
        yieldedOwner = BridgeStateStore.getNativeOwner();
        if (!yieldedOwner.isEmpty()) nativeBarrier = playbackOrder;
        nameResolver = new PlayerNameResolver(this);
        artworkRepository = new ArtworkRepository(this);
        sessionManager = (MediaSessionManager) getSystemService(MEDIA_SESSION_SERVICE);
        BridgeStateStore.setInputState("监听服务已启动，等待授权连接");
        DiagnosticsLog.i("MediaListenerService created");
        mainHandler.postDelayed(progressTicker, 1000L);
    }

    @Override
    public void onListenerConnected() {
        super.onListenerConnected();
        // The framework callback is authoritative. Some vehicle builds update
        // enabled_notification_listeners after delivering this callback; rechecking here
        // used to turn a successful bind into a false denial and permanently skip session setup.
        connected = true; rebindAttempts = 0; nextRebind = 0L;
        settings.markNotificationAccessGranted();
        BridgeStateStore.setInputState("通知访问已连接，等待播放器");
        DiagnosticsLog.i("Notification listener connected");
        notificationRecords.clear();
        try {
            StatusBarNotification[] active = getActiveNotifications();
            if (active != null) for (StatusBarNotification item : active) cacheNotification(item);
        } catch (RuntimeException error) {
            DiagnosticsLog.e("Unable to read active media notifications", error);
        }
        if (sessionManager == null) {
            refreshControllers(notificationControllers(null));
            return;
        }
        removeSessionsListener();
        sessionsListener = controllers -> refreshControllers(controllers);
        try {
            sessionManager.addOnActiveSessionsChangedListener(
                    sessionsListener,
                    new ComponentName(this, MediaListenerService.class),
                    mainHandler);
        } catch (SecurityException error) {
            DiagnosticsLog.e("Unable to register active-session listener", error);
        }
        try {
            refreshControllers(notificationControllers(sessionManager.getActiveSessions(
                    new ComponentName(this, MediaListenerService.class))));
        } catch (SecurityException error) {
            // A notification listener can be connected while the platform still refuses
            // MediaSession enumeration (for example during a permission revocation race).
            // Keep this state visible instead of letting the service die silently.
            DiagnosticsLog.e("Active-session enumeration denied", error);
            refreshControllers(notificationControllers(null));
            if (currentController == null) {
                BridgeStateStore.setInputState("通知访问已连接，但系统拒绝读取媒体会话");
                sendStateChanged();
            }
        }
    }

    @Override public void onNotificationPosted(StatusBarNotification sbn) {
        if (sbn == null) return;
        inputRevision++;
        artworkRevision++;
        cacheNotification(sbn);
        refreshFromSystem();
    }

    @Override public void onNotificationRemoved(StatusBarNotification sbn) {
        if (sbn == null) return;
        if (notificationRecords.remove(sbn.getKey()) != null) artworkRevision++;
        refreshFromSystem();
    }

    @Override
    public void onListenerDisconnected() {
        connected = false;
        initialSessionScan = true;
        removeSessionsListener();
        unregisterController();
        unregisterAllSessionRecords();
        notificationRecords.clear();
        cancelPendingFavorite("播放器连接已断开");
        BridgeStateStore.clearSnapshot();
        invalidateVehicleSnapshot();
        NotificationAccess.State access = NotificationAccess.check(this);
        boolean stillGranted = access == NotificationAccess.State.GRANTED;
        BridgeStateStore.setInputState(stillGranted
                ? "通知访问已授权，正在恢复监听" : access == NotificationAccess.State.UNKNOWN
                ? "授权状态暂时无法确认，监听已断开" : "通知访问已关闭，无法读取播放器");
        DiagnosticsLog.w("Notification listener disconnected");
        if (stillGranted) {
            rebindAttempts = 1; nextRebind = SystemClock.elapsedRealtime() + 2000L;
            try {
                requestRebind(new ComponentName(this, MediaListenerService.class));
            } catch (Throwable error) {
                DiagnosticsLog.e("Notification listener rebind request failed", error);
            }
        }
        super.onListenerDisconnected();
    }

    @Override
    public void onDestroy() {
        liveInstance = null;
        probeWorker.shutdownNow();
        if (artworkRepository != null) artworkRepository.close();
        mainHandler.removeCallbacksAndMessages(null);
        unregisterController();
        unregisterAllSessionRecords();
        notificationRecords.clear();
        removeSessionsListener();
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            if ((ACTION_MEDIA_COMMAND.equals(intent.getAction())
                    || BridgeEvents.ACTION_REFRESH.equals(intent.getAction())
                    || BridgeEvents.ACTION_PROBE_IOU.equals(intent.getAction()))
                    && !BridgeEvents.isTrustedServiceIntent(this, intent)) {
                DiagnosticsLog.w("Rejected unauthenticated player command=" + intent.getAction());
                return START_NOT_STICKY;
            }
            if (ACTION_MEDIA_COMMAND.equals(intent.getAction())) {
                sendCommand(intent.getStringExtra("command"), intent);
            } else if (BridgeEvents.ACTION_REFRESH.equals(intent.getAction())) {
                refreshFromSystem();
            } else if (BridgeEvents.ACTION_PROBE_IOU.equals(intent.getAction())) {
                try { probeWorker.execute(this::probeIouMedia); }
                catch (RuntimeException error) { DiagnosticsLog.e("IOU_PROBE side=android result=busy", error); }
            }
        }
        return START_NOT_STICKY;
    }

    /** Read-only snapshot: never alters source selection, callbacks or the vehicle output. */
    private void probeIouMedia() {
        DiagnosticsLog.i("IOU_PROBE side=android event=begin listenerConnected=" + connected);
        for (String target : new String[]{IOU_PACKAGE, IOV_PACKAGE}) {
            try {
                android.content.pm.PackageInfo info = getPackageManager().getPackageInfo(target, 0);
                DiagnosticsLog.i("IOU_PROBE side=android target=" + target
                        + " package=installed versionCode=" + info.getLongVersionCode());
            } catch (PackageManager.NameNotFoundException error) {
                DiagnosticsLog.w("IOU_PROBE side=android target=" + target + " package=not-visible-or-not-installed");
            } catch (RuntimeException error) {
                DiagnosticsLog.e("IOU_PROBE side=android target=" + target + " package=query-failed", error);
            }
        }
        try {
            List<MediaController> sessions = sessionManager == null ? null : sessionManager.getActiveSessions(
                    new ComponentName(this, MediaListenerService.class));
            int count = sessions == null ? 0 : sessions.size();
            DiagnosticsLog.i("IOU_PROBE side=android sessions=count:" + count);
            if (sessions != null) for (MediaController controller : sessions) {
                String pkg = controller.getPackageName();
                DiagnosticsLog.i("IOU_PROBE side=android sessionPackage=" + pkg);
                if (!IOU_PACKAGE.equals(pkg) && !IOV_PACKAGE.equals(pkg)) continue;
                PlaybackState state = controller.getPlaybackState();
                MediaMetadata metadata = controller.getMetadata();
                String queueSize;
                try {
                    List<MediaSession.QueueItem> queue = controller.getQueue();
                    queueSize = queue == null ? "null" : String.valueOf(queue.size());
                } catch (RuntimeException error) {
                    queueSize = "query-failed";
                    DiagnosticsLog.e("IOU_PROBE side=android target=" + pkg + " queue=query-failed", error);
                }
                DiagnosticsLog.i("IOU_PROBE side=android target=" + pkg + " targetSession state="
                        + (state == null ? "null" : state.getState()) + " actions="
                        + (state == null ? 0 : state.getActions()) + " metadata=" + (metadata != null)
                        + " queueSize=" + queueSize
                        + " browserRoot=not-probed");
                try {
                    Rating userRating = metadata == null ? null : metadata.getRating(MediaMetadata.METADATA_KEY_USER_RATING);
                    Rating generalRating = metadata == null ? null : metadata.getRating(MediaMetadata.METADATA_KEY_RATING);
                    DiagnosticsLog.i("FAVORITE_PROBE target=" + pkg + " ratingType=" + controller.getRatingType()
                            + " setRatingAction=" + (state != null && (state.getActions() & PlaybackState.ACTION_SET_RATING) != 0)
                            + " userRating=" + ratingSummary(userRating) + " generalRating=" + ratingSummary(generalRating));
                    List<PlaybackState.CustomAction> customActions = state == null ? null : state.getCustomActions();
                    DiagnosticsLog.i("FAVORITE_PROBE target=" + pkg + " customActionCount="
                            + (customActions == null ? 0 : customActions.size()));
                    if (customActions != null) for (int i = 0; i < Math.min(customActions.size(), 20); i++) {
                        PlaybackState.CustomAction action = customActions.get(i);
                        DiagnosticsLog.i("FAVORITE_PROBE target=" + pkg + " customAction[" + i + "]="
                                + action.getAction() + " label=" + action.getName());
                    }
                } catch (RuntimeException error) {
                    DiagnosticsLog.e("FAVORITE_PROBE target=" + pkg + " session=query-failed", error);
                }
            }
        } catch (SecurityException error) {
            DiagnosticsLog.e("IOU_PROBE side=android sessions=access-denied", error);
        } catch (RuntimeException error) {
            DiagnosticsLog.e("IOU_PROBE side=android sessions=query-failed", error);
        }
        try {
            StatusBarNotification[] notifications = connected ? getActiveNotifications() : null;
            int iouCount = 0, iovCount = 0;
            if (notifications != null) for (StatusBarNotification item : notifications) {
                String pkg = item.getPackageName();
                if (IOU_PACKAGE.equals(pkg)) iouCount++;
                else if (IOV_PACKAGE.equals(pkg)) iovCount++;
                else continue;
                Notification notification = item.getNotification();
                Bundle extras = notification == null ? null : notification.extras;
                Object token = extras == null ? null : extras.getParcelable(Notification.EXTRA_MEDIA_SESSION);
                DiagnosticsLog.i("IOU_PROBE side=android target=" + pkg + " targetNotification category="
                        + (notification == null ? "null" : notification.category)
                        + " mediaSessionPresent=" + (token instanceof MediaSession.Token)
                        + " titlePresent=" + (extras != null && extras.getCharSequence(Notification.EXTRA_TITLE) != null));
                Notification.Action[] actions = notification == null ? null : notification.actions;
                DiagnosticsLog.i("FAVORITE_PROBE target=" + pkg + " notificationActionCount="
                        + (actions == null ? 0 : actions.length));
                if (actions != null) for (int i = 0; i < Math.min(actions.length, 20); i++) {
                    DiagnosticsLog.i("FAVORITE_PROBE target=" + pkg + " notificationAction[" + i + "]="
                            + actions[i].title + " executable=" + (actions[i].actionIntent != null));
                }
            }
            DiagnosticsLog.i("IOU_PROBE side=android target=" + IOU_PACKAGE + " targetNotifications=count:" + iouCount);
            DiagnosticsLog.i("IOU_PROBE side=android target=" + IOV_PACKAGE + " targetNotifications=count:" + iovCount
                    + " listenerConnected=" + connected);
        } catch (RuntimeException error) {
            DiagnosticsLog.e("IOU_PROBE side=android notifications=query-failed", error);
        }
        PlayerSnapshot snapshot = BridgeStateStore.getSnapshot();
        DiagnosticsLog.i("FAVORITE_PROBE selectedPackage=" + (snapshot == null ? "null" : snapshot.packageName)
                + " state=" + (snapshot == null ? "null" : snapshot.favoriteState)
                + " writeSupported=" + (snapshot != null && snapshot.favoriteWriteSupported)
                + " pending=" + (snapshot != null && snapshot.favoritePending)
                + " showCollection=" + (settings != null && settings.isShowCollectionEnabled()));
        DiagnosticsLog.i("IOU_PROBE side=android event=end");
    }

    private static String ratingSummary(Rating rating) {
        if (rating == null) return "null";
        int style = rating.getRatingStyle();
        if (!rating.isRated()) return "style:" + style + ",unrated";
        if (style == Rating.RATING_HEART) return "style:heart,value:" + rating.hasHeart();
        if (style == Rating.RATING_THUMB_UP_DOWN) return "style:thumb,value:" + rating.isThumbUp();
        return "style:" + style + ",rated";
    }

    private void refreshControllers(List<MediaController> controllers) {
        if (!connected) return;
        syncSessionRecords(controllers);
        initialSessionScan = false;
        MediaController selected = selectController(controllers);
        if (selected == null) {
            unregisterController();
            cancelPendingFavorite("播放器会话已结束");
            BridgeStateStore.clearSnapshot();
            invalidateVehicleSnapshot();
            String fixed = settings.getSelectedPackage();
            if (nativeBarrier >= 0) {
                BridgeStateStore.setInputState("已让位：" + resolveLabel(yieldedOwner) + "；等待第三方播放器开始播放");
            } else if (!TextUtils.isEmpty(fixed) && settings.getBlacklist().contains(fixed)) {
                BridgeStateStore.setInputState("固定播放器已被忽略：" + fixed);
            } else if (!TextUtils.isEmpty(fixed)) {
                BridgeStateStore.setInputState("等待固定播放器建立媒体会话：" + fixed);
            } else {
                BridgeStateStore.setInputState(connected ? "已连接，等待播放器开始播放" : "监听未连接");
            }
            sendStateChanged();
            return;
        }
        if (currentController != null && currentController.getSessionToken().equals(selected.getSessionToken())) {
            publish(selected);
            return;
        }
        unregisterController();
        cancelPendingFavorite("已切换播放器");
        currentController = selected;
        settings.addSeenPackage(selected.getPackageName());
        DiagnosticsLog.i("Selected player: " + selected.getPackageName());
        publish(selected);
    }

    private void refreshFromSystem() {
        if (!connected) return;
        if (sessionManager == null) {
            refreshControllers(notificationControllers(null));
            return;
        }
        try {
            refreshControllers(notificationControllers(sessionManager.getActiveSessions(
                    new ComponentName(this, MediaListenerService.class))));
        } catch (SecurityException error) {
            DiagnosticsLog.e("Active-session refresh denied", error);
            refreshControllers(notificationControllers(null));
        }
    }

    private void cacheNotification(StatusBarNotification sbn) {
        Notification notification = sbn.getNotification();
        if (notification == null || notification.extras == null) return;
        Bundle extras = notification.extras;
        Object value = extras.getParcelable(Notification.EXTRA_MEDIA_SESSION);
        MediaSession.Token token = value instanceof MediaSession.Token ? (MediaSession.Token) value : null;
        if (token == null && !Notification.CATEGORY_TRANSPORT.equals(notification.category)) {
            notificationRecords.remove(sbn.getKey());
            return;
        }
        MediaController controller = null;
        if (token != null) {
            try {
                controller = new MediaController(this, token);
                if (!sbn.getPackageName().equals(controller.getPackageName())) {
                    DiagnosticsLog.w("Ignored media notification token/package mismatch=" + sbn.getPackageName());
                    controller = null;
                    token = null;
                }
            } catch (RuntimeException error) {
                DiagnosticsLog.e("Unable to open notification media token", error);
                token = null;
            }
        }
        String artworkUri = "";
        CharSequence title = extras.getCharSequence(Notification.EXTRA_TITLE);
        CharSequence artist = extras.getCharSequence(Notification.EXTRA_TEXT);
        Boolean liked = null;
        PendingIntent likeIntent = null;
        PendingIntent unlikeIntent = null;
        Notification.Action[] actions = notification.actions;
        if (actions != null) {
            for (Notification.Action action : actions) {
                if (action == null || action.title == null || action.actionIntent == null) continue;
                String actionName = action.title.toString();
                if ("RemoveLike".equals(actionName)) {
                    liked = Boolean.TRUE;
                    unlikeIntent = action.actionIntent;
                } else if ("AddLike".equals(actionName)) {
                    liked = Boolean.FALSE;
                    likeIntent = action.actionIntent;
                }
            }
        }
        notificationRecords.put(sbn.getKey(), new NotificationRecord(sbn.getPackageName(), token,
                controller, title == null ? "" : title.toString(),
                artist == null ? "" : artist.toString(), artworkUri, notification.getLargeIcon(), liked,
                likeIntent, unlikeIntent, sbn.getPostTime()));
    }

    private List<MediaController> notificationControllers(List<MediaController> active) {
        ArrayList<MediaController> combined = new ArrayList<>();
        Set<MediaSession.Token> tokens = new HashSet<>();
        if (active != null) for (MediaController controller : active) {
            if (destroyedTokens.contains(controller.getSessionToken())) continue;
            combined.add(controller);
            tokens.add(controller.getSessionToken());
        }
        for (NotificationRecord record : notificationRecords.values()) {
            if (record.controller != null && record.token != null && !destroyedTokens.contains(record.token) && tokens.add(record.token)) {
                combined.add(record.controller);
            }
        }
        return combined;
    }

    private NotificationRecord notificationFor(MediaController controller) {
        NotificationRecord best = null;
        int samePackageSessions = 0;
        boolean controllerIsActive = false;
        for (SessionRecord record : sessionRecords.values()) {
            if (controller.getPackageName().equals(record.controller.getPackageName())) {
                samePackageSessions++;
                if (controller.getSessionToken().equals(record.controller.getSessionToken())) {
                    controllerIsActive = true;
                }
            }
        }
        for (NotificationRecord record : notificationRecords.values()) {
            if (!controller.getPackageName().equals(record.packageName)) continue;
            if (record.token != null && !record.token.equals(controller.getSessionToken())) continue;
            if (record.token == null && (!controllerIsActive || samePackageSessions != 1)) continue;
            if (best == null || record.postedAt > best.postedAt) best = record;
        }
        return best;
    }

    private MediaController selectController(List<MediaController> controllers) {
        if (controllers == null || controllers.isEmpty()) return null;
        String fixedPackage = settings.getSelectedPackage();
        // Scan all sessions before selection. The known CarPlay key-only session is not playback evidence.
        String activeNative = null;
        long activeNativeOrder = -1;
        for (MediaController controller : controllers) {
            String pkg = controller.getPackageName();
            if (isControlOnlySession(controller)) continue;
            PlaybackState state = controller.getPlaybackState();
            if (!getPackageName().equals(pkg) && (settings.getBlacklist().contains(pkg) || isNativePackage(pkg)
                    || (io.github.rhsr1024.interop.BridgeProtocol.managed(pkg) && !CarBridgeCompanionService.allows(pkg)))
                    && state != null && (state.getState() == PlaybackState.STATE_PLAYING
                    || state.getState() == PlaybackState.STATE_BUFFERING)) {
                SessionRecord record = sessionRecords.get(controller.getSessionToken());
                long order = record == null ? playbackOrder : record.playingOrder;
                if (activeNative == null || order > activeNativeOrder
                        || (order == activeNativeOrder && pkg.compareTo(activeNative) < 0)) {
                    activeNative = pkg; activeNativeOrder = order;
                }
            }
        }
        if (activeNative != null) {
            nativeBarrier = Math.max(nativeBarrier, activeNativeOrder);
            if (!activeNative.equals(yieldedOwner)) {
                nativeBarrier = Math.max(nativeBarrier, playbackOrder);
                BridgeStateStore.invalidatePendingInput();
            }
            yieldedOwner = activeNative;
            BridgeStateStore.setNativeOwner(activeNative);
            return null;
        }
        ArrayList<MediaController> allowed = new ArrayList<>();
        for (MediaController controller : controllers) {
            // Also exclude it when not ignored, including fixed selection and paused fallback.
            if (isControlOnlySession(controller)) continue;
            String packageName = controller.getPackageName();
            String reason = filterReason(packageName);
            if (reason == null) allowed.add(controller);
            else DiagnosticsLog.i("Session filtered package=" + packageName + " reason=" + reason);
        }
        if (fixedPackage != null && !fixedPackage.isEmpty()) {
            allowed.removeIf(controller -> !fixedPackage.equals(controller.getPackageName()));
            if (allowed.isEmpty()) return null;
        }
        MediaController bestPlaying = null;
        long bestPlayingAt = Long.MIN_VALUE;
        for (MediaController controller : allowed) {
            SessionRecord record = sessionRecords.get(controller.getSessionToken());
            PlaybackState state = controller.getPlaybackState();
            if (state != null && state.getState() == PlaybackState.STATE_PLAYING) {
                if (nativeBarrier >= 0 && (record == null || record.playingOrder <= nativeBarrier)) continue;
                long at = record == null ? 0L : record.lastPlayingAt;
                if (bestPlaying == null || at > bestPlayingAt
                        || (at == bestPlayingAt && stableKey(controller).compareTo(stableKey(bestPlaying)) < 0)) {
                    bestPlaying = controller;
                    bestPlayingAt = at;
                }
            }
        }
        if (bestPlaying != null) {
            if (nativeBarrier >= 0) BridgeStateStore.invalidatePendingInput();
            nativeBarrier = -1; yieldedOwner = "";
            BridgeStateStore.setNativeOwner("");
            return bestPlaying;
        }
        if (nativeBarrier >= 0) return null;
        if (currentController != null) {
            for (MediaController controller : allowed)
                if (currentController.getSessionToken().equals(controller.getSessionToken())) return controller;
        }
        MediaController fallback = null;
        long fallbackAt = Long.MIN_VALUE;
        for (MediaController controller : allowed) {
            SessionRecord record = sessionRecords.get(controller.getSessionToken());
            long at = record == null ? 0L : record.lastActiveAt;
            if (fallback == null || at > fallbackAt
                    || (at == fallbackAt && stableKey(controller).compareTo(stableKey(fallback)) < 0)) {
                fallback = controller;
                fallbackAt = at;
            }
        }
        return fallback;
    }

    private boolean isControlOnlySession(MediaController controller) {
        // This CarPlay build publishes PLAYING to receive keys before supplying any song metadata.
        // Do not generalize this rule to other players: many publish metadata after playback starts.
        if (!CARPLAY_PACKAGE.equals(controller.getPackageName())) return false;
        MediaMetadata metadata = controller.getMetadata();
        if (metadata == null) return true;
        return TextUtils.isEmpty(metadata.getString(MediaMetadata.METADATA_KEY_TITLE))
                && TextUtils.isEmpty(metadata.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE))
                && TextUtils.isEmpty(metadata.getString(MediaMetadata.METADATA_KEY_MEDIA_ID))
                && TextUtils.isEmpty(metadata.getString(MediaMetadata.METADATA_KEY_ARTIST))
                && TextUtils.isEmpty(metadata.getString(MediaMetadata.METADATA_KEY_ALBUM))
                && metadata.getLong(MediaMetadata.METADATA_KEY_DURATION) <= 0;
    }

    private String filterReason(String packageName) {
        if (TextUtils.isEmpty(packageName)) return "empty_package";
        if (getPackageName().equals(packageName)) return "self";
        if (io.github.rhsr1024.interop.BridgeProtocol.managed(packageName)
                && !CarBridgeCompanionService.allows(packageName)) return "CarBridge 等待协作或处于直连模式";
        if (settings.getBlacklist().contains(packageName)) return "ignored";
        if (isNativePackage(packageName)) return "vehicle_bridge_loop";
        return null;
    }

    private String stableKey(MediaController controller) {
        return controller.getPackageName() + "|" + controller.getSessionToken();
    }

    private boolean isNativePackage(String packageName) {
        return packageName.startsWith("com.android.bluetooth")
                || packageName.equals("com.ecarx.mediacenter")
                || packageName.equals("ecarx.xsf.mediacenter")
                || packageName.equals("com.flyme");
    }

    private void unregisterController() {
        currentController = null;
    }

    private void syncSessionRecords(List<MediaController> controllers) {
        Set<MediaSession.Token> live = new HashSet<>();
        Set<String> activePackages = new HashSet<>();
        if (controllers != null) for (MediaController controller : controllers) {
            if (!TextUtils.isEmpty(controller.getPackageName())) activePackages.add(controller.getPackageName());
            MediaSession.Token token = controller.getSessionToken();
            live.add(token);
            SessionRecord existing = sessionRecords.get(token);
            if (existing == null) {
                final MediaSession.Token callbackToken = token;
                MediaController.Callback callback = new MediaController.Callback() {
                    @Override public void onMetadataChanged(MediaMetadata metadata) {
                        inputRevision++;
                        artworkRevision++;
                        SessionRecord record = sessionRecords.get(callbackToken);
                        if (record != null) {
                            record.lastActiveAt = SystemClock.elapsedRealtime();
                            refreshFromSystem();
                        }
                    }
                    @Override public void onPlaybackStateChanged(PlaybackState state) {
                        inputRevision++;
                        SessionRecord record = sessionRecords.get(callbackToken);
                        if (record != null) updateRecord(record, state);
                        refreshFromSystem();
                    }
                    @Override public void onQueueChanged(List<MediaSession.QueueItem> queue) {
                        SessionRecord record = sessionRecords.get(callbackToken);
                        if (record != null && currentController != null
                                && callbackToken.equals(currentController.getSessionToken())) publish(currentController);
                    }
                    @Override public void onQueueTitleChanged(CharSequence title) {
                        SessionRecord record = sessionRecords.get(callbackToken);
                        if (record != null && currentController != null
                                && callbackToken.equals(currentController.getSessionToken())) publish(currentController);
                    }
                    @Override public void onSessionDestroyed() {
                        if (destroyedTokens.size() >= 256) destroyedTokens.clear();
                        destroyedTokens.add(callbackToken);
                        notificationRecords.values().removeIf(record -> callbackToken.equals(record.token));
                        removeSessionRecord(callbackToken);
                        refreshFromSystem();
                    }
                };
                existing = new SessionRecord(controller, callback, SystemClock.elapsedRealtime());
                // Rebinding the listener must not treat a cached PLAYING session as a new user action.
                if (initialSessionScan && nativeBarrier >= 0) {
                    PlaybackState cached = controller.getPlaybackState();
                    existing.wasPlaying = cached != null && (cached.getState() == PlaybackState.STATE_PLAYING
                            || cached.getState() == PlaybackState.STATE_BUFFERING);
                }
                sessionRecords.put(token, existing);
                try { controller.registerCallback(callback, mainHandler); }
                catch (RuntimeException error) { DiagnosticsLog.e("Unable to register session callback", error); }
            }
            updateRecord(existing, controller.getPlaybackState());
        }
        for (MediaSession.Token token : new ArrayList<>(sessionRecords.keySet()))
            if (!live.contains(token)) removeSessionRecord(token);
        settings.setActivePackages(activePackages);
    }

    private void updateRecord(SessionRecord record, PlaybackState state) {
        int stateValue = state == null ? PlaybackState.STATE_NONE : state.getState();
        boolean controlOnly = isControlOnlySession(record.controller);
        if (controlOnly != record.wasControlOnly) {
            record.wasControlOnly = controlOnly;
            DiagnosticsLog.i("SESSION_GUARD package=" + record.controller.getPackageName()
                    + " controlOnly=" + controlOnly + " state=" + stateValue
                    + " reason=" + (controlOnly ? "carplay_without_track_metadata" : "track_metadata_available"));
        }
        boolean playing = stateValue == PlaybackState.STATE_PLAYING || stateValue == PlaybackState.STATE_BUFFERING;
        long now = SystemClock.elapsedRealtime();
        if (playing && !record.wasPlaying && !initialSessionScan && !controlOnly
                && !io.github.rhsr1024.interop.BridgeProtocol.managed(record.controller.getPackageName())) {
            CarBridgeCompanionService.yieldPlayback("本地播放器开始播放：" + record.controller.getPackageName());
        }
        if (playing && !record.wasPlaying) {
            record.lastPlayingAt = now;
            record.playingOrder = ++playbackOrder;
        }
        if (stateValue != record.lastState && stateValue != PlaybackState.STATE_NONE) record.lastActiveAt = now;
        record.wasPlaying = playing;
        record.lastState = stateValue;
    }

    private void removeSessionRecord(MediaSession.Token token) {
        SessionRecord record = sessionRecords.remove(token);
        if (record != null) try { record.controller.unregisterCallback(record.callback); }
        catch (RuntimeException ignored) {}
    }

    private void unregisterAllSessionRecords() {
        for (MediaSession.Token token : new ArrayList<>(sessionRecords.keySet())) removeSessionRecord(token);
    }

    private void removeSessionsListener() {
        if (sessionManager != null && sessionsListener != null) {
            try { sessionManager.removeOnActiveSessionsChangedListener(sessionsListener); }
            catch (RuntimeException ignored) {}
        }
        sessionsListener = null;
    }

    private void publish(MediaController controller) {
        if (controller == null || !connected) return;
        String packageName = controller.getPackageName();
        MediaMetadata metadata = controller.getMetadata();
        PlaybackState playbackState = controller.getPlaybackState();
        String title = metadata == null ? "" : metadata.getString(MediaMetadata.METADATA_KEY_TITLE);
        String artist = metadata == null ? "" : metadata.getString(MediaMetadata.METADATA_KEY_ARTIST);
        String album = metadata == null ? "" : metadata.getString(MediaMetadata.METADATA_KEY_ALBUM);
        ArtworkSelection artwork = preferredArtwork(metadata, packageName);
        String artworkUri = artwork.uri;
        Bitmap artworkBitmap = artwork.bitmap;
        logArtworkCandidates(packageName, metadata, artwork);
        // CarBridge's notification can say "CarPlay 已连接" when artist is unknown.
        // Only its canonical MediaSession fields may become song/search metadata.
        NotificationRecord notification = io.github.rhsr1024.interop.BridgeProtocol.managed(packageName)
                ? null : notificationFor(controller);
        if (notification != null && metadata != null
                && ((!TextUtils.isEmpty(title) && !TextUtils.isEmpty(notification.title) && !title.equals(notification.title))
                || (!TextUtils.isEmpty(artist) && !TextUtils.isEmpty(notification.artist) && !artist.equals(notification.artist)))) {
            notification = null; // A notification from the previous track cannot confirm this track's state.
        }
        if (notification != null) {
            if (TextUtils.isEmpty(title)) title = notification.title;
            if (TextUtils.isEmpty(artist)) artist = notification.artist;
            if (TextUtils.isEmpty(artworkUri)) artworkUri = notification.artworkUri;
        }
        long duration = metadata == null ? 0L : metadata.getLong(MediaMetadata.METADATA_KEY_DURATION);
        if (io.github.rhsr1024.interop.BridgeProtocol.managed(packageName)) {
            String probe = TrackIdentity.of(controller.getSessionToken().toString(),
                    metadata == null ? "" : metadata.getString(MediaMetadata.METADATA_KEY_MEDIA_ID), title, artist, album, duration);
            if (!probe.equals(lastCarBridgeMetadataProbe)) {
                lastCarBridgeMetadataProbe = probe;
                DiagnosticsLog.i("CARBRIDGE_METADATA source=MediaSession notificationFallback=false duration=" + duration
                        + " lookupAllowed=" + CarBridgeMetadataPolicy.allowsLookup(packageName, title, artist, duration)
                        + "\ntitle=" + probeText(title) + "\nartist=" + probeText(artist) + "\nalbum=" + probeText(album));
            }
        }
        long position = currentPosition(playbackState, duration);
        PlaybackStatus status = mapStatus(playbackState);
        String label = resolveLabel(packageName);
        Rating rating = favoriteRating(metadata);
        Boolean ratedFavorite = favoriteValue(rating);
        FavoriteState favoriteState = FavoriteState.UNKNOWN;
        if (ratedFavorite != null) {
            favoriteState = ratedFavorite ? FavoriteState.FAVORITED : FavoriteState.NOT_FAVORITED;
        }
        if (favoriteState == FavoriteState.UNKNOWN && notification != null && notification.liked != null) {
            favoriteState = notification.liked ? FavoriteState.FAVORITED : FavoriteState.NOT_FAVORITED;
        }
        Boolean adapterFavorite = PlayerFeatures.favoriteFromState(packageName, playbackState);
        if (adapterFavorite != null) favoriteState = adapterFavorite ? FavoriteState.FAVORITED : FavoriteState.NOT_FAVORITED;
        String session = controller.getSessionToken().toString();
        String track = TrackIdentity.of(session, metadata == null ? "" : metadata.getString(MediaMetadata.METADATA_KEY_MEDIA_ID),
                title, artist, album, duration);
        artworkUri = artworkRepository.resolve(packageName, track, artworkUri, artworkBitmap,
                notification == null ? null : notification.artworkIcon, title, artist, duration, artworkRevision, this::refreshFromSystem);
        SessionRecord sourceRecord = sessionRecords.get(controller.getSessionToken());
        if (PlayerFeatures.NAVI.equals(packageName) && sourceRecord != null)
            position = sourceRecord.positionTracker.update(track, status == PlaybackStatus.PLAYING, duration, SystemClock.elapsedRealtime());
        if (pendingFavorite != null && (!packageName.equals(pendingFavoritePackage)
                || !session.equals(pendingFavoriteSession))) {
            cancelPendingFavorite("已切换播放器");
            favoriteMessage = "";
        } else if (pendingFavorite != null && !FavoriteTrackIdentity.sameSong(
                pendingFavoriteTrack, pendingFavoriteTitle, pendingFavoriteArtist, track, title, artist)) {
            cancelPendingFavorite("歌曲已切换，请核对原曲的收藏状态");
            favoriteMessage = "";
        } else if (pendingFavorite != null && !track.equals(pendingFavoriteTrack)) {
            DiagnosticsLog.i("Favorite metadata identity refreshed without visible song change package=" + packageName);
        }
        if (favoriteMessageTrack != null && !favoriteMessageTrack.equals(track)) {
            favoriteMessageTrack = null; favoriteMessage = "";
        }
        if (uncertainTrack != null && (!uncertainTrack.equals(track) || inputRevision > uncertainRevision)) {
            uncertainTrack = null; favoriteMessage = ""; favoriteMessageTrack = null;
        }
        if (pendingFavorite != null) {
            boolean confirmed = inputRevision > pendingFavoriteRevision && favoriteState != FavoriteState.UNKNOWN
                    && (favoriteState == FavoriteState.FAVORITED) == pendingFavorite;
            if (confirmed) {
                favoriteMessage = "播放器已确认收藏状态";
                favoriteMessageTrack = track;
                DiagnosticsLog.i("Favorite confirmed package=" + packageName + " value=" + pendingFavorite);
                clearPendingFavorite();
            } else if (SystemClock.elapsedRealtime() >= pendingFavoriteDeadline) {
                favoriteMessage = "收藏结果无法确认：播放器在 3 秒内没有反馈，请到播放器核对";
                favoriteMessageTrack = track;
                uncertainTrack = track; uncertainRevision = inputRevision;
                DiagnosticsLog.w("Favorite confirmation timed out package=" + packageName);
                notifyFavoriteFailure(favoriteMessage);
                clearPendingFavorite();
            } else { favoriteMessage = "等待播放器确认收藏状态"; favoriteMessageTrack = track; }
        }
        if (track.equals(uncertainTrack)) favoriteState = FavoriteState.UNKNOWN;
        int ratingType = controller.getRatingType();
        if (ratingType == Rating.RATING_NONE && rating != null) ratingType = rating.getRatingStyle();
        boolean desired = favoriteState != FavoriteState.FAVORITED;
        boolean supportsFavorite = favoriteState != FavoriteState.UNKNOWN && pendingFavorite == null
                && (PlayerFeatures.ratingSupported(packageName, playbackState, ratingType)
                    || PlayerFeatures.favoriteAction(packageName, playbackState, desired, favoriteState) != null
                    || notification != null && (desired ? notification.likeIntent : notification.unlikeIntent) != null
                    || IOU_PACKAGE.equals(packageName));
        PendingIntent activity = controller.getSessionActivity();
        PlayerSnapshot snapshot = new PlayerSnapshot(nameResolver.userId(), packageName, label, nullToEmpty(title),
                nullToEmpty(artist), nullToEmpty(album), nullToEmpty(artworkUri), duration, position, status,
                favoriteState, supportsFavorite, activity, System.currentTimeMillis(),
                controller.getSessionToken().toString(), track,
                playbackState == null ? 1f : playbackState.getPlaybackSpeed(), playerLyrics(metadata),
                nameResolver.resolve(packageName).source, pendingFavorite != null, favoriteMessage);
        BridgeStateStore.setSnapshot(snapshot);
        if (settings.isBridgeEnabled() && BridgeStateStore.isOutputRunning()
                && status == PlaybackStatus.PLAYING && !snapshot.title.isEmpty()
                && LastPlayerStore.allowed(this, packageName)) {
            LastPlayerStore.remember(this, packageName);
            lastPlayingSession = snapshot.sessionId;
        }
        publishQueue(controller, snapshot, playbackState);
        BridgeStateStore.setInputState("已发现：" + label + "（" + packageName + "）");
        sendSnapshot(snapshot);
    }

    private void publishQueue(MediaController controller, PlayerSnapshot snapshot, PlaybackState state) {
        if (controller == null || snapshot == null) { BridgeStateStore.clearQueueSnapshot(); return; }
        MediaSession.Token token = controller.getSessionToken();
        MediaController selected = currentController;
        if (selected != null && !token.equals(selected.getSessionToken())) return;
        List<MediaSession.QueueItem> queue = null;
        CharSequence queueTitle = null;
        try {
            queue = controller.getQueue();
            queueTitle = controller.getQueueTitle();
        } catch (RuntimeException error) {
            DiagnosticsLog.e("Unable to read MediaSession queue", error);
        }
        selected = currentController;
        if (selected != null && !token.equals(selected.getSessionToken())) return;
        long activeQueueId = state == null ? MediaSession.QueueItem.UNKNOWN_ID : state.getActiveQueueItemId();
        QueueSnapshot previous = BridgeStateStore.getQueueSnapshot();
        if (previous != null && previous.matches(snapshot)
                && previous.sameContent(queueTitle, queue, activeQueueId)) return;
        BridgeStateStore.setQueueSnapshot(QueueSnapshot.from(snapshot.packageName, snapshot.sessionId,
                BridgeStateStore.getGeneration(), queueTitle, queue, activeQueueId));
    }

    static String preferredArtworkUri(MediaMetadata metadata) {
        if (metadata == null) return "";
        String value = metadata.getString(MediaMetadata.METADATA_KEY_ART_URI);
        return TextUtils.isEmpty(value) ? metadata.getString(MediaMetadata.METADATA_KEY_ALBUM_ART_URI) : value;
    }

    static Bitmap preferredArtworkBitmap(MediaMetadata metadata) {
        if (metadata == null) return null;
        Bitmap value = metadata.getBitmap(MediaMetadata.METADATA_KEY_ART);
        return value == null ? metadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART) : value;
    }

    static final class ArtworkSelection {
        final String uri;
        final Bitmap bitmap;
        final String source;
        ArtworkSelection(String uri, Bitmap bitmap, String source) {
            this.uri = nullToEmpty(uri);
            this.bitmap = bitmap;
            this.source = source;
        }
    }

    /** Keep URI and Bitmap from the same metadata family so two different covers are not mixed. */
    static ArtworkSelection preferredArtwork(MediaMetadata metadata, String packageName) {
        if (metadata == null) return new ArtworkSelection("", null, "none");
        ArtworkSelection art = new ArtworkSelection(
                metadata.getString(MediaMetadata.METADATA_KEY_ART_URI),
                metadata.getBitmap(MediaMetadata.METADATA_KEY_ART), "art");
        ArtworkSelection album = new ArtworkSelection(
                metadata.getString(MediaMetadata.METADATA_KEY_ALBUM_ART_URI),
                metadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART), "album");
        boolean artAvailable = !TextUtils.isEmpty(art.uri) || art.bitmap != null;
        boolean albumAvailable = !TextUtils.isEmpty(album.uri) || album.bitmap != null;
        // These players expose the complete cover through the album family. Their ART candidate
        // may be a display crop, which cannot be repaired by scaling it after the fact.
        boolean preferCompleteAlbum = "com.kugou.android.lite".equals(packageName)
                || "cn.kuwo.kwmusiccar".equals(packageName)
                // LX's TrackPlayer publishes the source URL as ART_URI, then puts the
                // asynchronously decoded 512 px bitmap in ALBUM_ART. Prefer that usable
                // bitmap once it arrives instead of remaining pinned to the URI-only family.
                || PlayerFeatures.LX_MUSIC.equals(packageName);
        if (preferCompleteAlbum && albumAvailable) return album;
        if (artAvailable) return art;
        return albumAvailable ? album : new ArtworkSelection("", null, "none");
    }

    private void logArtworkCandidates(String packageName, MediaMetadata metadata,
                                             ArtworkSelection selected) {
        if (metadata == null) return;
        String artUri = metadata.getString(MediaMetadata.METADATA_KEY_ART_URI);
        String albumUri = metadata.getString(MediaMetadata.METADATA_KEY_ALBUM_ART_URI);
        Bitmap art = metadata.getBitmap(MediaMetadata.METADATA_KEY_ART);
        Bitmap album = metadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART);
        String probe = "ARTWORK_PROBE event=metadata_candidates package=" + packageName
                + " artUri=" + uriScheme(artUri) + " albumUri=" + uriScheme(albumUri)
                + " artBitmap=" + dimensions(art) + " albumBitmap=" + dimensions(album)
                + " selectedSource=" + selected.source
                + " selectedUri=" + uriScheme(selected.uri)
                + " selectedBitmap=" + dimensions(selected.bitmap);
        if (!probe.equals(lastArtworkProbe)) { lastArtworkProbe = probe; DiagnosticsLog.i(probe); }
    }

    private static String probeText(String value) {
        if (value == null) return "";
        return value.replace('\n', ' ').replace('\r', ' ').substring(0, Math.min(value.length(), 256));
    }

    private static String uriScheme(String value) {
        if (TextUtils.isEmpty(value)) return "none";
        try { return String.valueOf(Uri.parse(value).getScheme()); }
        catch (RuntimeException ignored) { return "invalid"; }
    }

    private static String dimensions(Bitmap bitmap) {
        return bitmap == null || bitmap.isRecycled() ? "none" : bitmap.getWidth() + "x" + bitmap.getHeight();
    }

    private long currentPosition(PlaybackState state, long duration) {
        if (state == null) return 0L;
        long position = Math.max(0L, state.getPosition());
        if (state.getState() == PlaybackState.STATE_PLAYING && state.getLastPositionUpdateTime() > 0L) {
            long elapsed = Math.max(0L, SystemClock.elapsedRealtime() - state.getLastPositionUpdateTime());
            position += (long) (elapsed * state.getPlaybackSpeed());
        }
        return duration > 0L ? Math.min(duration, Math.max(0L, position)) : Math.max(0L, position);
    }

    private PlaybackStatus mapStatus(PlaybackState state) {
        if (state == null) return PlaybackStatus.NONE;
        switch (state.getState()) {
            case PlaybackState.STATE_PLAYING:
                return PlaybackStatus.PLAYING;
            case PlaybackState.STATE_BUFFERING:
                return PlaybackStatus.BUFFERING;
            case PlaybackState.STATE_ERROR:
                return PlaybackStatus.ERROR;
            default:
                return PlaybackStatus.PAUSED;
        }
    }

    private String resolveLabel(String packageName) {
        return nameResolver.resolve(packageName).label;
    }

    private void sendSnapshot(PlayerSnapshot snapshot) {
        if (!settings.isBridgeEnabled() || (!settings.isBootRestoreEnabled() && !BridgeStateStore.isOutputRunning())) {
            sendStateChanged(); return;
        }
        Intent intent = BridgeEvents.serviceIntent(this, BridgeEvents.ACTION_SNAPSHOT)
                .putExtra("inputEpoch", BridgeStateStore.inputEpoch())
                .putExtra("userId", snapshot.userId)
                .putExtra("package", snapshot.packageName)
                .putExtra("sessionId", snapshot.sessionId)
                .putExtra("trackId", snapshot.trackId)
                .putExtra("speed", snapshot.playbackSpeed)
                .putExtra("playerLyrics", snapshot.playerLyrics)
                .putExtra("labelSource", snapshot.labelSource)
                .putExtra("favoritePending", snapshot.favoritePending)
                .putExtra("favoriteMessage", snapshot.favoriteMessage)
                .putExtra("appName", snapshot.appLabel)
                .putExtra("title", snapshot.title)
                .putExtra("artist", snapshot.artist)
                .putExtra("album", snapshot.album)
                .putExtra("artworkUri", snapshot.artworkUri)
                .putExtra("duration", snapshot.durationMs)
                .putExtra("position", snapshot.positionMs)
                .putExtra("playbackStatus", snapshot.status.name())
                .putExtra("favoriteState", snapshot.favoriteState.name())
                .putExtra("favoriteSupported", snapshot.favoriteWriteSupported);
        if (snapshot.sessionActivity != null) intent.putExtra("sessionActivity", snapshot.sessionActivity);
        try {
            startForegroundService(intent);
        } catch (RuntimeException error) {
            DiagnosticsLog.e("Unable to start bridge service", error);
        }
        sendStateChanged();
    }

    private void sendStateChanged() {
        sendBroadcast(BridgeEvents.stateIntent());
    }

    private void sendCommand(String command, Intent intent) {
        if (!connected || !settings.isBridgeEnabled() || TextUtils.isEmpty(command)) return;
        if (NotificationAccess.check(this) == NotificationAccess.State.DENIED) { invalidateInput(); return; }
        MediaController target = currentController;
        if (target == null) {
            DiagnosticsLog.w("Dropped control with no active MediaController command=" + command);
            return;
        }
        long eventGeneration = intent.getLongExtra("bridgeGeneration", -1L);
        String expectedPackage = intent.getStringExtra("expectedPackage");
        String expectedSession = intent.getStringExtra("expectedSession");
        if (eventGeneration != BridgeStateStore.getGeneration()) {
            DiagnosticsLog.w("Dropped stale control command=" + command + " generation=" + eventGeneration);
            return;
        }
        if (!TextUtils.isEmpty(expectedPackage) && !expectedPackage.equals(target.getPackageName())) {
            DiagnosticsLog.w("Dropped control for stale player command=" + command
                    + " expected=" + expectedPackage + " actual=" + target.getPackageName());
            return;
        }
        if (!TextUtils.isEmpty(expectedSession) && !expectedSession.equals(target.getSessionToken().toString())) {
            DiagnosticsLog.w("Dropped control for stale session command=" + command);
            return;
        }
        if (io.github.rhsr1024.interop.BridgeProtocol.managed(target.getPackageName())) {
            if (!java.util.Arrays.asList("PLAY", "PAUSE", "STOP", "TOGGLE", "NEXT", "PREVIOUS").contains(command)) {
                unsupported(command); return;
            }
            CarBridgeCompanionService.command(target.getPackageName(), command);
            return;
        }
        android.media.session.MediaController.TransportControls controls = target.getTransportControls();
        BridgeStateStore.setLastControl(command + "：已接收");
        DiagnosticsLog.i("Control received command=" + command + " package=" + target.getPackageName());
        try {
            if (PlayerFeatures.handleSpecialTransport(target, command)) return;
            PlaybackState state = target.getPlaybackState();
            switch (command.toUpperCase(Locale.ROOT)) {
                // Match the original bridge: forward explicit transport calls even when a
                // player omits action bits from PlaybackState.
                case "NEXT": controls.skipToNext(); break;
                case "PREVIOUS": controls.skipToPrevious(); break;
                case "TOGGLE": if (state != null && state.getState() == PlaybackState.STATE_PLAYING) controls.pause(); else controls.play(); break;
                case "LOOP":
                    String loop = PlayerFeatures.loopAction(intent.getIntExtra("loopMode", 0));
                    if (PlayerFeatures.supportsLoop(target.getPackageName()) && loop != null) controls.sendCustomAction(loop, null);
                    else unsupported(command);
                    break;
                case "PLAY": controls.play(); break;
                case "PAUSE": controls.pause(); break;
                case "STOP": controls.stop(); break;
                case "FAST_FORWARD": controls.fastForward(); break;
                case "REWIND": controls.rewind(); break;
                case "SEEK":
                    long position = Math.max(0L, intent.getLongExtra("position", 0L));
                    MediaMetadata meta = target.getMetadata();
                    long duration = meta == null ? 0L : meta.getLong(MediaMetadata.METADATA_KEY_DURATION);
                    if (duration > 0L) position = Math.min(position, duration);
                    controls.seekTo(position);
                    SessionRecord record = sessionRecords.get(target.getSessionToken());
                    if (record != null) record.positionTracker.seek(position, SystemClock.elapsedRealtime());
                    break;
                case "QUEUE_ITEM":
                    long queueId = intent.getLongExtra("queueId", MediaSession.QueueItem.UNKNOWN_ID);
                    QueueSnapshot currentQueue = BridgeStateStore.getQueueSnapshot();
                    PlayerSnapshot currentPlayer = BridgeStateStore.getSnapshot();
                    if (queueId == MediaSession.QueueItem.UNKNOWN_ID) unsupported(command);
                    else if (currentQueue == null || !currentQueue.matches(currentPlayer)
                            || currentQueue.find(queueId) == null) {
                        BridgeStateStore.setLastControl("QUEUE_ITEM：队列已变化，请刷新后重试");
                        DiagnosticsLog.w("Dropped stale queue item id=" + queueId);
                    } else controls.skipToQueueItem(queueId);
                    break;
                case "MEDIA_ID":
                    String mediaId = intent.getStringExtra("mediaId");
                    if (TextUtils.isEmpty(mediaId)) unsupported(command);
                    else controls.playFromMediaId(mediaId, null);
                    break;
                case "FAVORITE": sendFavorite(intent, state, target); break;
                default: DiagnosticsLog.w("Unsupported media command: " + command);
            }
        } catch (RuntimeException error) {
            if ("FAVORITE".equalsIgnoreCase(command)) {
                clearPendingFavorite();
                favoriteMessage = "收藏失败：发送操作时出现 " + error.getClass().getSimpleName();
                favoriteMessageTrack = BridgeStateStore.getSnapshot() == null ? null : BridgeStateStore.getSnapshot().trackId;
                notifyFavoriteFailure(favoriteMessage);
                if (connected && currentController != null) {
                    try { publish(currentController); }
                    catch (RuntimeException republishError) {
                        DiagnosticsLog.e("Favorite failure state refresh failed", republishError);
                    }
                }
            }
            DiagnosticsLog.e("Media command failed: " + command, error);
            BridgeStateStore.setLastControl(command + "：发送失败 " + error.getClass().getSimpleName());
        }
    }

    private void sendFavorite(Intent intent, PlaybackState state, MediaController target) {
        // Re-read metadata before checking the expected track; callbacks may still be queued.
        publish(target);
        PlayerSnapshot current = BridgeStateStore.getSnapshot();
        if (current == null || !current.trackId.equals(intent.getStringExtra("expectedTrack"))) {
            DiagnosticsLog.w("Favorite ignored: stale track");
            notifyFavoriteFailure("收藏失败：歌曲已切换，请在当前歌曲重试");
            return;
        }
        if (!settings.isShowCollectionEnabled() || settings.isFavoriteIgnored(current.packageName)) {
            DiagnosticsLog.w("Favorite ignored: disabled for package=" + current.packageName);
            return;
        }
        if (pendingFavorite != null) {
            BridgeStateStore.setLastControl("收藏：正在等待播放器确认，请稍后重试");
            return;
        }
        boolean desired = intent.getBooleanExtra("favorite", false);
        if (current.favoriteState != FavoriteState.UNKNOWN
                && (current.favoriteState == FavoriteState.FAVORITED) == desired) return;
        NotificationRecord notification = notificationFor(target);
        if (notification != null && ((!TextUtils.isEmpty(notification.title) && !notification.title.equals(current.title))
                || (!TextUtils.isEmpty(notification.artist) && !notification.artist.equals(current.artist)))) notification = null;
        PendingIntent notificationAction = notification == null ? null : desired ? notification.likeIntent : notification.unlikeIntent;
        FavoriteState actionState = current.favoriteState == FavoriteState.UNKNOWN
                ? (desired ? FavoriteState.NOT_FAVORITED : FavoriteState.FAVORITED) : current.favoriteState;
        String custom = PlayerFeatures.favoriteAction(current.packageName, state, desired, actionState);
        Rating rating = favoriteRating(target.getMetadata());
        int ratingType = target.getRatingType();
        if (ratingType == Rating.RATING_NONE && rating != null) ratingType = rating.getRatingStyle();
        boolean sent = false;
        if (notificationAction != null) {
            try { notificationAction.send(); sent = true; }
            catch (PendingIntent.CanceledException error) { DiagnosticsLog.e("Favorite notification canceled", error); }
        }
        if (!sent && custom != null) {
            target.getTransportControls().sendCustomAction(custom, null); sent = true;
        }
        if (!sent && PlayerFeatures.ratingSupported(current.packageName, state, ratingType)) {
            target.getTransportControls().setRating(ratingType == Rating.RATING_THUMB_UP_DOWN
                    ? Rating.newThumbRating(desired) : Rating.newHeartRating(desired)); sent = true;
        }
        // Some players implement setRating without advertising ACTION_SET_RATING. This
        // user-enabled trial sends only on an actual click and still waits for source feedback.
        if (!sent && (ratingType == Rating.RATING_NONE || ratingType == Rating.RATING_HEART
                || ratingType == Rating.RATING_THUMB_UP_DOWN)) {
            int attemptType = ratingType == Rating.RATING_THUMB_UP_DOWN
                    ? Rating.RATING_THUMB_UP_DOWN : Rating.RATING_HEART;
            DiagnosticsLog.i("FAVORITE_PROBE target=" + current.packageName
                    + " event=rating-attempt reportedType=" + ratingType + " attemptedType=" + attemptType
                    + " desired=" + desired);
            target.getTransportControls().setRating(attemptType == Rating.RATING_THUMB_UP_DOWN
                    ? Rating.newThumbRating(desired) : Rating.newHeartRating(desired));
            sent = true;
        }
        if (!sent) {
            favoriteMessage = "收藏失败：当前播放器未提供可调用的心形收藏操作";
            favoriteMessageTrack = current.trackId;
            notifyFavoriteFailure(favoriteMessage);
            publish(target);
            return;
        }
        pendingFavorite = desired; pendingFavoritePackage = current.packageName;
        pendingFavoriteSession = current.sessionId; pendingFavoriteTrack = current.trackId;
        pendingFavoriteTitle = current.title; pendingFavoriteArtist = current.artist;
        pendingFavoriteRevision = inputRevision; pendingFavoriteDeadline = SystemClock.elapsedRealtime() + 3000L;
        DiagnosticsLog.i("Favorite sent; awaiting source confirmation package=" + current.packageName);
        publish(target);
        mainHandler.postDelayed(() -> {
            if (pendingFavorite == null) return;
            if (connected && currentController != null) publish(currentController);
            else cancelPendingFavorite("播放器会话已结束");
        }, 3000L);
    }

    private void notifyFavoriteFailure(String message) {
        BridgeStateStore.setLastControl(message);
        DiagnosticsLog.w("FAVORITE_RESULT " + message);
        mainHandler.post(() -> Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show());
    }

    private void cancelPendingFavorite(String reason) {
        if (pendingFavorite == null) return;
        clearPendingFavorite();
        notifyFavoriteFailure("收藏结果无法确认：" + reason);
    }

    private static String playerLyrics(MediaMetadata metadata) {
        if (metadata == null) return "";
        for (String key : new String[]{"android.media.metadata.LYRICS", "lyrics", "lrc"}) {
            try { String value = metadata.getString(key); if (value != null && value.contains("[")) return value; }
            catch (RuntimeException ignored) {}
        }
        return "";
    }

    private static Rating favoriteRating(MediaMetadata metadata) {
        if (metadata == null) return null;
        Rating rating = metadata.getRating(MediaMetadata.METADATA_KEY_USER_RATING);
        return rating != null ? rating : metadata.getRating(MediaMetadata.METADATA_KEY_RATING);
    }

    private static Boolean favoriteValue(Rating rating) {
        if (rating == null || !rating.isRated()) return null;
        if (rating.getRatingStyle() == Rating.RATING_HEART) return rating.hasHeart();
        if (rating.getRatingStyle() == Rating.RATING_THUMB_UP_DOWN) return rating.isThumbUp();
        return null;
    }

    private static PlaybackState.CustomAction findFavoriteAction(PlaybackState state, boolean favorite) {
        if (state == null || state.getCustomActions() == null) return null;
        ArrayList<String> names = new ArrayList<>();
        for (PlaybackState.CustomAction action : state.getCustomActions()) {
            if (action != null) names.add(action.getAction());
        }
        String selected = FavoriteActionPolicy.select(names, favorite);
        if (selected == null) return null;
        for (PlaybackState.CustomAction action : state.getCustomActions()) {
            if (action != null && selected.equals(action.getAction())) return action;
        }
        return null;
    }

    private static boolean supports(long actions, long action) { return (actions & action) != 0L; }
    private static void unsupported(String command) {
        BridgeStateStore.setLastControl(command + "：播放器未声明支持");
        DiagnosticsLog.w("ACTION_UNSUPPORTED command=" + command);
    }

    private void clearPendingFavorite() {
        pendingFavorite = null;
        pendingFavoritePackage = null;
        pendingFavoriteSession = null;
        pendingFavoriteTrack = null;
        pendingFavoriteTitle = null;
        pendingFavoriteArtist = null;
        pendingFavoriteDeadline = 0L;
        pendingFavoriteTimeoutLogged = false;
    }

    public static void requestCommand(Context context, String command, long generation,
                                      String expectedPackage, String expectedSession) {
        Intent intent = BridgeEvents.listenerIntent(context, ACTION_MEDIA_COMMAND)
                .putExtra("command", command)
                .putExtra("bridgeGeneration", generation)
                .putExtra("expectedPackage", expectedPackage)
                .putExtra("expectedSession", expectedSession);
        deliverToConnectedListener(intent, "media command");
    }

    public static void requestSeek(Context context, long positionMs, long generation,
                                   String expectedPackage, String expectedSession) {
        Intent intent = BridgeEvents.listenerIntent(context, ACTION_MEDIA_COMMAND)
                .putExtra("command", "SEEK")
                .putExtra("position", positionMs)
                .putExtra("bridgeGeneration", generation)
                .putExtra("expectedPackage", expectedPackage)
                .putExtra("expectedSession", expectedSession);
        deliverToConnectedListener(intent, "seek");
    }

    public static void requestFavorite(Context context, boolean favorite, long generation,
                                       String expectedPackage, String expectedSession, String expectedTrack) {
        Intent intent = BridgeEvents.listenerIntent(context, ACTION_MEDIA_COMMAND)
                .putExtra("expectedTrack", expectedTrack)
                .putExtra("command", "FAVORITE")
                .putExtra("favorite", favorite)
                .putExtra("bridgeGeneration", generation)
                .putExtra("expectedPackage", expectedPackage)
                .putExtra("expectedSession", expectedSession);
        deliverToConnectedListener(intent, "favorite");
    }

    public static void requestQueueItem(Context context, long queueId, long generation,
                                        String expectedPackage, String expectedSession) {
        Intent intent = BridgeEvents.listenerIntent(context, ACTION_MEDIA_COMMAND)
                .putExtra("command", "QUEUE_ITEM")
                .putExtra("queueId", queueId)
                .putExtra("bridgeGeneration", generation)
                .putExtra("expectedPackage", expectedPackage)
                .putExtra("expectedSession", expectedSession);
        deliverToConnectedListener(intent, "queue item");
    }

    public static void requestMediaId(Context context, String mediaId, long generation,
                                      String expectedPackage, String expectedSession) {
        Intent intent = BridgeEvents.listenerIntent(context, ACTION_MEDIA_COMMAND)
                .putExtra("command", "MEDIA_ID")
                .putExtra("mediaId", mediaId)
                .putExtra("bridgeGeneration", generation)
                .putExtra("expectedPackage", expectedPackage)
                .putExtra("expectedSession", expectedSession);
        deliverToConnectedListener(intent, "media id");
    }

    public static void yieldToVehicleSource(String ownerPackage) {
        if (ownerPackage == null || ownerPackage.isEmpty()) return;
        BridgeStateStore.invalidatePendingInput();
        BridgeStateStore.setNativeOwner(ownerPackage);
        CarBridgeCompanionService.yieldPlayback("车机来源：" + ownerPackage);
        MediaListenerService instance = liveInstance;
        if (instance == null) { BridgeStateStore.clearSnapshot(); return; }
        runOnListenerThread(instance, () -> {
            instance.nativeBarrier = instance.playbackOrder;
            instance.yieldedOwner = ownerPackage;
            instance.lastResumeKey = "";
            instance.resumeSequence++;
            instance.unregisterController();
            instance.cancelPendingFavorite("已切换车机媒体来源");
            BridgeStateStore.clearSnapshot();
            BridgeStateStore.setInputState("已让位：" + instance.resolveLabel(ownerPackage) + "；等待第三方播放器开始播放");
            instance.invalidateVehicleSnapshot();
            instance.sendStateChanged();
        });
    }

    static boolean companionReady() { return liveInstance != null && liveInstance.connected; }

    /** User-originated vehicle callbacks only. Never called by owner polling, boot or refresh. */
    static boolean resumeLastPlayer(Context context, long generation, String event) {
        MediaListenerService value = liveInstance;
        if (value == null || !value.connected || !BridgeStateStore.isOutputRunning()
                || generation != BridgeStateStore.getGeneration()
                || !new SettingsRepository(context).isBridgeEnabled()
                || !LastPlayerStore.allowed(context, LastPlayerStore.get(context))) {
            DiagnosticsLog.w("VEHICLE_RESUME rejected=precondition event=" + event
                    + " listener=" + (value != null) + " connected=" + (value != null && value.connected)
                    + " output=" + BridgeStateStore.isOutputRunning()
                    + " generation=" + generation + " activeGeneration=" + BridgeStateStore.getGeneration()
                    + " enabled=" + new SettingsRepository(context).isBridgeEnabled()
                    + " previousPackage=" + LastPlayerStore.get(context)
                    + " previousAllowed=" + LastPlayerStore.allowed(context, LastPlayerStore.get(context)));
            return false;
        }
        long inputEpoch = BridgeStateStore.inputEpoch();
        runOnListenerThread(value, () -> value.resumeFromVehicle(generation, inputEpoch, event));
        return true; // Queued, not a claim that the remote player started.
    }

    private void resumeFromVehicle(long generation, long inputEpoch, String event) {
        if (!connected || !settings.isBridgeEnabled() || !BridgeStateStore.isOutputRunning()
                || generation != BridgeStateStore.getGeneration() || inputEpoch != BridgeStateStore.inputEpoch()
                || NotificationAccess.check(this) == NotificationAccess.State.DENIED
                || !settings.isResumeLastPlayerEnabled()) {
            DiagnosticsLog.w("VEHICLE_RESUME rejected=state_changed event=" + event); return;
        }
        String pkg = LastPlayerStore.get(this);
        if (!"source_selected".equals(event) && currentController != null
                && LastPlayerStore.allowed(this, currentController.getPackageName())) pkg = currentController.getPackageName();
        if (!LastPlayerStore.allowed(this, pkg)) {
            DiagnosticsLog.w("VEHICLE_RESUME rejected=no_eligible_previous_player package=" + pkg);
            BridgeStateStore.setLastControl("上次播放器不可用或已被忽略"); return;
        }
        MediaController target = returnController(pkg);
        String key = pkg + "|" + (target == null ? "no_session" : target.getSessionToken().toString());
        long now = SystemClock.elapsedRealtime();
        if (key.equals(lastResumeKey) && now - lastResumeAt < 1500) {
            DiagnosticsLog.i("VEHICLE_RESUME duplicate event=" + event); return;
        }
        lastResumeKey = key; lastResumeAt = now;
        long attempt = ++resumeSequence;
        DiagnosticsLog.i("VEHICLE_RESUME attempt=" + attempt + " event=" + event + " package=" + pkg + " liveSession=" + (target != null));
        if (target == null) {
            // An explicit user request may open a cold player, but no delayed PLAY is queued.
            if (!io.github.rhsr1024.interop.BridgeProtocol.managed(pkg)) {
                try { startActivity(new Intent(this, VehiclePlayerActivity.class).putExtra("resumePackage", pkg).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); }
                catch (RuntimeException error) { DiagnosticsLog.w("VEHICLE_RESUME cold_launch_failed package=" + pkg); }
            }
            BridgeStateStore.setLastControl("播放器会话已结束，请进入播放器恢复"); return;
        }
        try {
            if (io.github.rhsr1024.interop.BridgeProtocol.managed(pkg)) CarBridgeCompanionService.command(pkg, "PLAY");
            else if (!PlayerFeatures.handleSpecialTransport(target, "PLAY")) target.getTransportControls().play();
            BridgeStateStore.setLastControl("已请求上次播放器继续播放");
        } catch (RuntimeException error) {
            DiagnosticsLog.w("VEHICLE_RESUME play_failed package=" + pkg + " error=" + error.getClass().getSimpleName()); return;
        }
        String expectedPackage = pkg;
        mainHandler.postDelayed(() -> {
            if (!connected || attempt != resumeSequence || generation != BridgeStateStore.getGeneration()) return;
            PlayerSnapshot actual = BridgeStateStore.getSnapshot();
            boolean playing = actual != null && expectedPackage.equals(actual.packageName) && actual.status == PlaybackStatus.PLAYING;
            DiagnosticsLog.i("VEHICLE_RESUME attempt=" + attempt + " observedPlaying=" + playing + " package=" + expectedPackage);
            // Never retry or manufacture metadata/position if the player did not acknowledge.
        }, 5000);
    }

    private MediaController returnController(String pkg) {
        if (!LastPlayerStore.allowed(this, pkg)) return null;
        if (currentController != null && pkg.equals(currentController.getPackageName()) && !isControlOnlySession(currentController)) return currentController;
        MediaController candidate = null;
        int matches = 0;
        for (SessionRecord record : sessionRecords.values()) {
            MediaController value = record.controller;
            if (!pkg.equals(value.getPackageName()) || isControlOnlySession(value) || destroyedTokens.contains(value.getSessionToken())) continue;
            if (lastPlayingSession.equals(value.getSessionToken().toString())) return value;
            matches++; candidate = value;
        }
        return matches == 1 ? candidate : null;
    }

    static PendingIntent playerSessionActivity(String pkg) {
        MediaListenerService value = liveInstance;
        if (value == null || !value.connected) return null;
        MediaController target = value.returnController(pkg);
        try { return target == null ? null : target.getSessionActivity(); }
        catch (RuntimeException error) { return null; }
    }
    static void companionRefresh() {
        MediaListenerService value = liveInstance;
        if (value != null) runOnListenerThread(value, value::refreshFromSystem);
    }
    static PlayerSnapshot selectManaged(String pkg) {
        MediaListenerService value = liveInstance;
        if (value == null || !value.connected || !CarBridgeCompanionService.allows(pkg)) return null;
        for (SessionRecord record : value.sessionRecords.values()) {
            if (!pkg.equals(record.controller.getPackageName())) continue;
            value.nativeBarrier = -1; value.yieldedOwner = "";
            BridgeStateStore.setNativeOwner("");
            value.currentController = record.controller;
            value.publish(record.controller);
            return BridgeStateStore.getSnapshot();
        }
        return null;
    }
    public static void requestRefresh(Context context) {
        // Keep the wake-up behavior of the known-good 1.8 build. Some ECARX ROMs create the
        // listener component after authorization but do not complete the system bind until the
        // component has also received an explicit start. onStartCommand only refreshes state;
        // the protected NotificationListenerService bind remains owned by the system.
        Intent intent = BridgeEvents.listenerIntent(context, BridgeEvents.ACTION_REFRESH);
        try {
            context.startService(intent);
        } catch (RuntimeException error) {
            DiagnosticsLog.e("Unable to wake notification listener", error);
        }
    }

    private static void deliverToConnectedListener(Intent intent, String operation) {
        MediaListenerService instance = liveInstance;
        if (instance == null || !instance.connected) {
            BridgeStateStore.setLastControl(operation + "：通知监听尚未连接");
            DiagnosticsLog.w("Dropped " + operation + " because notification listener is not connected");
            return;
        }
        runOnListenerThread(instance, () -> {
            if (liveInstance == instance && instance.connected) {
                instance.sendCommand(intent.getStringExtra("command"), intent);
            }
        });
    }

    private static void runOnListenerThread(MediaListenerService instance, Runnable action) {
        if (Looper.myLooper() == instance.mainHandler.getLooper()) action.run();
        else instance.mainHandler.post(action);
    }

    private void invalidateVehicleSnapshot() {
        if (!settings.isBridgeEnabled() || !BridgeStateStore.isOutputRunning()) return;
        Intent intent = BridgeEvents.serviceIntent(this, BridgeEvents.ACTION_INPUT_INVALIDATED)
                .putExtra("inputEpoch", BridgeStateStore.inputEpoch());
        if (nativeBarrier >= 0) intent.putExtra("nativeOwner", yieldedOwner);
        try { startService(intent); }
        catch (RuntimeException error) { DiagnosticsLog.e("Unable to invalidate vehicle snapshot", error); }
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    public static void observeAccess(NotificationAccess.State state) {
        MediaListenerService instance = liveInstance;
        // A live system callback is authoritative. On this ROM the secure setting can lag behind
        // onListenerConnected(); invalidating a connected instance here tears down a valid bind.
        if (instance != null && state == NotificationAccess.State.DENIED
                && !instance.connected && instance.currentController != null)
            instance.mainHandler.post(instance::invalidateInput);
    }

    private void invalidateInput() {
        connected = false; removeSessionsListener(); unregisterController(); unregisterAllSessionRecords();
        notificationRecords.clear(); cancelPendingFavorite("通知访问已关闭"); BridgeStateStore.clearSnapshot();
        BridgeStateStore.setInputState("通知访问已关闭，无法读取播放器");
        invalidateVehicleSnapshot(); sendStateChanged();
    }

    private void checkAccessAndRecover() {
        long now = SystemClock.elapsedRealtime();
        if (now < nextAccessCheck) return;
        nextAccessCheck = now + 2000L;
        // Trust the framework lifecycle while connected. Revocation is delivered through
        // onListenerDisconnected(); Settings.Secure may briefly report the previous user state.
        if (connected) {
            settings.markNotificationAccessGranted();
            return;
        }
        NotificationAccess.State access = NotificationAccess.check(this);
        if (access == NotificationAccess.State.DENIED) {
            if (connected || currentController != null) invalidateInput();
            return;
        }
        if (access == NotificationAccess.State.UNKNOWN) {
            BridgeStateStore.setInputState("授权状态暂时无法确认"); return;
        }
        if (!connected && now >= nextRebind && settings.isBridgeEnabled()) {
            long delay = new BackoffPolicy().delayMs(++rebindAttempts);
            nextRebind = now + delay;
            try { requestRebind(new ComponentName(this, MediaListenerService.class)); }
            catch (RuntimeException error) { DiagnosticsLog.e("Listener rebind", error); }
            BridgeStateStore.setInputState("已授权，正在恢复监听；下次重试 " + delay / 1000 + " 秒");
            sendStateChanged();
        }
    }

    public static void requestLoop(Context context, int mode, long generation, String pkg, String session) {
        Intent intent = BridgeEvents.listenerIntent(context, ACTION_MEDIA_COMMAND)
                .putExtra("command", "LOOP").putExtra("loopMode", mode).putExtra("bridgeGeneration", generation)
                .putExtra("expectedPackage", pkg).putExtra("expectedSession", session);
        deliverToConnectedListener(intent, "loop");
    }
}
