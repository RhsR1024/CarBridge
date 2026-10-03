package com.geely.auto.music;

import android.app.Application;
import android.content.*;
import android.media.MediaMetadata;
import android.media.session.*;
import android.os.Looper;
import android.provider.Settings;
import java.lang.reflect.*;
import java.util.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.android.controller.ServiceController;
import org.robolectric.annotation.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, application = Application.class)
@LooperMode(LooperMode.Mode.PAUSED)
public class VehicleResumeTest {
    Context context;
    SettingsRepository settings;
    ServiceController<MediaListenerService> lifecycle;
    MediaListenerService service;
    LegacyMusicClient client;
    @Before public void setup() throws Exception {
        context = RuntimeEnvironment.getApplication();
        context.getSharedPreferences("media_bridge_settings", 0).edit().clear().commit();
        context.getSharedPreferences("last_bridge_player", 0).edit().clear().commit();
        settings = new SettingsRepository(context); settings.setOnlineLookupEnabled(false);
        Settings.Secure.putString(context.getContentResolver(), "enabled_notification_listeners",
                new ComponentName(context, MediaListenerService.class).flattenToString());
        BridgeStateStore.clearSnapshot(); BridgeStateStore.setNativeOwner("");
        BridgeStateStore.setGeneration(700); BridgeStateStore.setOutputRunning(true);
        lifecycle = Robolectric.buildService(MediaListenerService.class).create(); service = lifecycle.get();
        field("connected", true);
        client = new LegacyMusicClient(context, 700); client.setControlsEnabled(false);
        client.setResumeCallbacksReady(true);
    }
    @After public void cleanup() {
        client.destroy(); lifecycle.destroy(); BridgeStateStore.clearSnapshot();
        BridgeStateStore.setNativeOwner(""); BridgeStateStore.setOutputRunning(false);
    }
    private void field(String name, Object value) throws Exception {
        Field f = MediaListenerService.class.getDeclaredField(name); f.setAccessible(true); f.set(service, value);
    }
    private Object invoke(String name, Class<?>[] types, Object... args) throws Exception {
        Method m = MediaListenerService.class.getDeclaredMethod(name, types); m.setAccessible(true); return m.invoke(service, args);
    }
    private MediaController player(String pkg) throws Exception {
        MediaSession session = new MediaSession(context, pkg);
        MediaController c = new MediaController(context, session.getSessionToken());
        Shadows.shadowOf(c).setPackageName(pkg);
        Shadows.shadowOf(c).setMetadata(new MediaMetadata.Builder().putString(MediaMetadata.METADATA_KEY_TITLE, "Song").build());
        Shadows.shadowOf(c).setPlaybackState(new PlaybackState.Builder().setState(PlaybackState.STATE_PAUSED, 31000, 1).build());
        return c;
    }
    private void sessions(MediaController... players) throws Exception {
        invoke("syncSessionRecords", new Class<?>[]{List.class}, Arrays.asList(players));
        field("initialSessionScan", false); field("currentController", null);
    }
    private long action(MediaController c) { return Shadows.shadowOf(c.getTransportControls()).getLastPerformedAction(); }

    @Test public void explicitSourceSelectionResumesRetainedSessionWithoutPublishingFakePlayback() throws Exception {
        MediaController third = player("third.party"); sessions(third); LastPlayerStore.remember(context, "third.party");
        MediaListenerService.yieldToVehicleSource("factory.radio"); Shadows.shadowOf(Looper.getMainLooper()).idle();
        long before = action(third);
        assertNull(BridgeStateStore.getSnapshot());
        assertEquals(before, action(third));
        assertTrue(client.onSourceSelected(6));
        assertEquals(PlaybackState.ACTION_PLAY, action(third));
        assertNull(BridgeStateStore.getSnapshot());
        assertEquals("factory.radio", BridgeStateStore.getNativeOwner());
    }
    @Test public void duplicateSelectionDoesNotSendAgainAndNoNextOrPauseFallbackExists() throws Exception {
        MediaController third = player("third.party"); sessions(third); LastPlayerStore.remember(context, "third.party");
        client.onSourceSelected(6); third.getTransportControls().pause(); client.onSourceSelected(6);
        assertEquals(PlaybackState.ACTION_PAUSE, action(third));
        assertFalse(client.onNext()); assertFalse(client.onPause()); assertFalse(client.onSourceSelected(9));
    }
    @Test public void newForeignSelectionAllowsAnotherImmediateExplicitReturn() throws Exception {
        MediaController third = player("third.party"); sessions(third); LastPlayerStore.remember(context, "third.party");
        client.onSourceSelected(6); third.getTransportControls().pause();
        MediaListenerService.yieldToVehicleSource("factory.radio"); Shadows.shadowOf(Looper.getMainLooper()).idle();
        client.onSourceSelected(6);
        assertEquals(PlaybackState.ACTION_PLAY, action(third));
    }
    @Test public void ignoredFixedMismatchDisabledAndOldGenerationCannotResume() throws Exception {
        MediaController third = player("third.party"); sessions(third); LastPlayerStore.remember(context, "third.party");
        long initial = action(third);
        settings.setBlacklisted("third.party", true); assertFalse(client.onSourceSelected(6));
        settings.setBlacklisted("third.party", false); settings.setSelectedPackage("different.player"); assertFalse(client.onSourceSelected(6));
        settings.setSelectedPackage(""); settings.setResumeLastPlayerEnabled(false); client.onSourceSelected(6); assertFalse(client.onPlay());
        settings.setResumeLastPlayerEnabled(true); BridgeStateStore.setGeneration(701); assertFalse(client.onSourceSelected(6));
        assertEquals(initial, action(third));
    }
    @Test public void delayedSelectionCannotOverrideNewerForeignOwner() throws Exception {
        MediaController third = player("third.party"); sessions(third); LastPlayerStore.remember(context, "third.party");
        long initial = action(third), epoch = BridgeStateStore.inputEpoch();
        BridgeStateStore.invalidatePendingInput();
        invoke("resumeFromVehicle", new Class<?>[]{long.class,long.class,String.class}, 700L, epoch, "source_selected");
        assertEquals(initial, action(third));
    }
    @Test public void onlyPlayingPublishedInputIsRememberedAndSurvivesClear() throws Exception {
        MediaController third = player("third.party"); sessions(third);
        invoke("publish", new Class<?>[]{MediaController.class}, third); assertEquals("", LastPlayerStore.get(context));
        Shadows.shadowOf(third).setPlaybackState(new PlaybackState.Builder().setState(PlaybackState.STATE_PLAYING, 31000, 1).build());
        invoke("publish", new Class<?>[]{MediaController.class}, third);
        assertEquals("third.party", LastPlayerStore.get(context));
        MediaListenerService.yieldToVehicleSource("factory.radio"); Shadows.shadowOf(Looper.getMainLooper()).idle();
        assertEquals("third.party", LastPlayerStore.get(context)); assertNull(BridgeStateStore.getSnapshot());
    }
    @Test public void coldPlayerOnlyOpensEntryWithoutDelayedPlay() {
        LastPlayerStore.remember(context, "third.party");
        assertTrue(client.onSourceSelected(6));
        Intent launch = Shadows.shadowOf((Application) context).getNextStartedActivity();
        assertEquals(VehiclePlayerActivity.class.getName(), launch.getComponent().getClassName());
        assertEquals("third.party", launch.getStringExtra("resumePackage"));
        assertNull(BridgeStateStore.getSnapshot());
    }
    @Test public void entryRemainsSeparateFromLauncherAndSurvivesEmptyCard() {
        LastPlayerStore.remember(context, "third.party");
        assertEquals("third.party", VehiclePlayerActivity.target(context));
        client.clear();
        Intent entry = Shadows.shadowOf(client.getInfo().getLaunchIntent()).getSavedIntent();
        assertEquals(VehiclePlayerActivity.class.getName(), entry.getComponent().getClassName());
        assertFalse(entry.hasCategory(Intent.CATEGORY_LAUNCHER));
        Intent launcher = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
        assertNotNull(launcher); assertEquals(MainActivity.class.getName(), launcher.getComponent().getClassName());
        settings.setBlacklisted("third.party", true); assertEquals("", VehiclePlayerActivity.target(context));
    }
    @Test public void disconnectedCarBridgeIsNeverResumedFromSavedHint() {
        LastPlayerStore.remember(context, "io.github.rhsr1024.carbridge");
        assertFalse(client.onSourceSelected(6)); assertEquals("", VehiclePlayerActivity.target(context));
    }
    private void installPlayerLauncher(String pkg) {
        android.content.pm.ResolveInfo resolved = new android.content.pm.ResolveInfo();
        resolved.activityInfo = new android.content.pm.ActivityInfo();
        resolved.activityInfo.packageName = pkg;
        resolved.activityInfo.name = pkg + ".PlayerActivity";
        resolved.activityInfo.applicationInfo = new android.content.pm.ApplicationInfo();
        resolved.activityInfo.applicationInfo.packageName = pkg;
        Shadows.shadowOf(context.getPackageManager()).addResolveInfoForIntent(
                new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setPackage(pkg), resolved);
    }
    @Test public void launcherOpensPausedCurrentPlayerWithoutPlayingAndFinishes() throws Exception {
        String pkg = "third.party";
        installPlayerLauncher(pkg);
        MediaController third = player(pkg); sessions(third);
        BridgeStateStore.setSnapshot(new PlayerSnapshot(pkg, "Player", "Song", "Artist", "", 60000, 1000,
                PlaybackStatus.PAUSED, FavoriteState.UNKNOWN, false, null, 1));
        LastPlayerStore.remember(context, "other.player");
        long before = action(third);
        try (org.robolectric.android.controller.ActivityController<MainActivity> screen = Robolectric.buildActivity(MainActivity.class).create()) {
            Intent opened = Shadows.shadowOf(screen.get()).getNextStartedActivity();
            assertEquals(pkg, opened.getComponent().getPackageName());
            assertTrue(screen.get().isFinishing());
            assertEquals(before, action(third));
            assertEquals(PlaybackStatus.PAUSED, BridgeStateStore.getSnapshot().status);
        }
    }
    @Test public void launcherOpensRememberedPlayerWhenSnapshotEmpty() {
        installPlayerLauncher("third.party"); LastPlayerStore.remember(context, "third.party");
        try (org.robolectric.android.controller.ActivityController<MainActivity> screen = Robolectric.buildActivity(MainActivity.class).create()) {
            assertEquals("third.party", Shadows.shadowOf(screen.get()).getNextStartedActivity().getComponent().getPackageName());
            assertTrue(screen.get().isFinishing());
        }
    }
    @Test @Config(sdk = 30)
    public void sessionDispatchIsLoggedWithoutClaimingForegroundOrLaunchingTwice() throws Exception {
        String pkg = "third.party";
        installPlayerLauncher(pkg);
        MediaController third = player(pkg); sessions(third); LastPlayerStore.remember(context, pkg);
        android.app.PendingIntent session = android.app.PendingIntent.getActivity(context, 91,
                new Intent().setComponent(new ComponentName(pkg, pkg + ".NowPlayingActivity")),
                android.app.PendingIntent.FLAG_IMMUTABLE);
        Shadows.shadowOf(session).setCreatorPackage(pkg);
        Shadows.shadowOf(third).setSessionActivity(session);
        long before = action(third);
        try (org.robolectric.android.controller.ActivityController<MainActivity> screen = Robolectric.buildActivity(MainActivity.class).create()) {
            Intent opened = Shadows.shadowOf((Application) context).getNextStartedActivity();
            assertEquals(pkg + ".NowPlayingActivity", opened.getComponent().getClassName());
            assertNull(Shadows.shadowOf((Application) context).getNextStartedActivity());
            assertTrue(screen.get().isFinishing());
            assertEquals(before, action(third));
            String log = DiagnosticsLog.dump();
            assertTrue(log.contains("type=unknown_pre31 eligible=true"));
            assertTrue(log.contains("event=session_send_returned"));
            assertTrue(log.contains("foreground=unverified"));
            assertTrue(log.contains("event=finish_requested"));
            assertFalse(log.contains("event=launcher_start_requested"));
        }
    }
    @Test @Config(sdk = 30)
    public void canceledSessionRecordsFailureAndFallsBackToLauncher() throws Exception {
        String pkg = "third.party";
        installPlayerLauncher(pkg);
        MediaController third = player(pkg); sessions(third); LastPlayerStore.remember(context, pkg);
        android.app.PendingIntent session = android.app.PendingIntent.getActivity(context, 92,
                new Intent().setComponent(new ComponentName(pkg, pkg + ".NowPlayingActivity")),
                android.app.PendingIntent.FLAG_IMMUTABLE);
        Shadows.shadowOf(session).setCreatorPackage(pkg);
        session.cancel(); Shadows.shadowOf(third).setSessionActivity(session);
        try (org.robolectric.android.controller.ActivityController<MainActivity> screen = Robolectric.buildActivity(MainActivity.class).create()) {
            Intent opened = Shadows.shadowOf(screen.get()).getNextStartedActivity();
            assertEquals(pkg + ".PlayerActivity", opened.getComponent().getClassName());
            assertTrue(screen.get().isFinishing());
            String log = DiagnosticsLog.dump();
            assertTrue(log.contains("event=session_send_failed"));
            assertTrue(log.contains("error=CanceledException"));
            assertTrue(log.contains("event=launcher_start_returned"));
        }
    }
    @Test public void newIntentDiagnosticsDoNotRelaunchOrExposeIncomingData() {
        installPlayerLauncher("third.party"); LastPlayerStore.remember(context, "third.party");
        try (org.robolectric.android.controller.ActivityController<MainActivity> screen = Robolectric.buildActivity(MainActivity.class).create()) {
            assertNotNull(Shadows.shadowOf(screen.get()).getNextStartedActivity());
            Intent incoming = new Intent("https://private.invalid/secret-action")
                    .setData(android.net.Uri.parse("custom://private/secret-data"))
                    .putExtra("password", "secret-extra")
                    .putExtra(Intent.EXTRA_REFERRER, android.net.Uri.parse("android-app://caller.app/private-secret"));
            screen.newIntent(incoming);
            assertNull(Shadows.shadowOf(screen.get()).getNextStartedActivity());
            String log = DiagnosticsLog.dump();
            assertTrue(log.contains("VEHICLE_ENTRY_LIFECYCLE event=new_intent"));
            assertTrue(log.contains("referrerPackage=caller.app"));
            assertFalse(log.contains("secret-action"));
            assertFalse(log.contains("secret-data"));
            assertFalse(log.contains("secret-extra"));
            assertFalse(log.contains("private-secret"));
        }
    }
    @Test public void missingPlayerFallsBackToSeparateSettingsTaskWithoutLoop() {
        LastPlayerStore.remember(context, "missing.player");
        try (org.robolectric.android.controller.ActivityController<MainActivity> screen = Robolectric.buildActivity(MainActivity.class).create()) {
            Intent opened = Shadows.shadowOf(screen.get()).getNextStartedActivity();
            assertEquals(SettingsActivity.class.getName(), opened.getComponent().getClassName());
            assertTrue((opened.getFlags() & Intent.FLAG_ACTIVITY_NEW_TASK) != 0);
            assertTrue(screen.get().isFinishing());
        }
    }
    @Test public void settingsIconIsExportedAndHasSeparateTaskFromVehicleLauncher() throws Exception {
        android.content.pm.PackageManager pm = context.getPackageManager();
        android.content.pm.ActivityInfo settingsInfo = pm.getActivityInfo(new ComponentName(context, SettingsActivity.class), 0);
        android.content.pm.ActivityInfo mainInfo = pm.getActivityInfo(new ComponentName(context, MainActivity.class), 0);
        assertTrue(settingsInfo.exported);
        assertNotEquals(mainInfo.taskAffinity, settingsInfo.taskAffinity);
        List<android.content.pm.ResolveInfo> icons = pm.queryIntentActivities(
                new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setPackage(context.getPackageName()), 0);
        assertTrue(icons.stream().anyMatch(r -> SettingsActivity.class.getName().equals(r.activityInfo.name)));
        assertTrue(icons.stream().anyMatch(r -> MainActivity.class.getName().equals(r.activityInfo.name)));
        assertEquals(MainActivity.class.getName(), pm.getLaunchIntentForPackage(context.getPackageName()).getComponent().getClassName());
    }
    @Test public void multipleSessionsWithoutOriginalTokenAreNotGuessed() throws Exception {
        sessions(player("third.party"), player("third.party")); LastPlayerStore.remember(context, "third.party");
        assertNull(MediaListenerService.playerSessionActivity("third.party"));
        client.onSourceSelected(6);
        assertNotNull(Shadows.shadowOf((Application) context).getNextStartedActivity());
    }
    @Test public void registrationAndOwnRequestEchoCannotAutoplayHistory() throws Exception {
        MediaController third = player("third.party"); sessions(third); LastPlayerStore.remember(context, "third.party");
        long initial = action(third);
        client.setResumeCallbacksReady(false); assertFalse(client.onSourceSelected(6));
        client.setResumeCallbacksReady(true); client.suppressResumeEcho(); client.onSourceSelected(6);
        assertEquals(initial, action(third));
    }
    @Test public void bridgeCarBridgeResumeUsesOneCompanionCommandInsteadOfTransportControl() throws Exception {
        String pkg = "io.github.rhsr1024.carbridge";
        ServiceController<CarBridgeCompanionService> companion = Robolectric.buildService(CarBridgeCompanionService.class).create();
        try {
            List<android.os.Message> commands = new ArrayList<>();
            android.os.Messenger peer = new android.os.Messenger(new android.os.Handler(Looper.getMainLooper(), message -> {
                if (message.what == io.github.rhsr1024.interop.BridgeProtocol.COMMAND) commands.add(android.os.Message.obtain(message));
                return true;
            }));
            Object value = companion.get();
            Map<String,Object> fields = new HashMap<>();
            fields.put("pkg", pkg); fields.put("peer", peer); fields.put("route", "BRIDGE");
            fields.put("enabled", true); fields.put("ready", true); fields.put("ignored", false);
            fields.put("lastHeartbeat", android.os.SystemClock.elapsedRealtime());
            for (Map.Entry<String,Object> entry : fields.entrySet()) {
                Field f = CarBridgeCompanionService.class.getDeclaredField(entry.getKey()); f.setAccessible(true); f.set(value, entry.getValue());
            }
            MediaController player = player(pkg); sessions(player); LastPlayerStore.remember(context, pkg);
            long initial = action(player);
            assertTrue(client.onSourceSelected(6)); Shadows.shadowOf(Looper.getMainLooper()).idle();
            assertEquals(1, commands.size()); assertEquals("PLAY", commands.get(0).getData().getString("command"));
            assertEquals(initial, action(player));
        } finally { companion.destroy(); }
    }
}
