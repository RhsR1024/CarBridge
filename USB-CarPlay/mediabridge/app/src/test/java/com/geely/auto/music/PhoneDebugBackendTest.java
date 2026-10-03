package com.geely.auto.music;

import android.app.Application;
import android.content.Context;
import android.media.MediaDescription;
import android.media.session.MediaSession;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.android.controller.ServiceController;

import static org.junit.Assert.*;

import java.util.Collections;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, application = Application.class)
public class PhoneDebugBackendTest {
    private Context context;
    private PhoneDebugBackend backend;

    @Before public void setUp() {
        context = RuntimeEnvironment.getApplication();
        context.getSharedPreferences("media_bridge_settings", 0).edit().clear().commit();
        new SettingsRepository(context).setOnlineLookupEnabled(false);
        BridgeStateStore.clearSnapshot();
        BridgeStateStore.setGeneration(42L);
        backend = new PhoneDebugBackend(context);
    }

    @After public void tearDown() {
        backend.close();
        BridgeStateStore.clearSnapshot();
    }

    @Test public void debugModeParsesAndUsesLegacyTuneBucket() {
        assertEquals(BackendMode.PHONE_DEBUG, BackendMode.from("phone-debug"));
        SettingsRepository settings = new SettingsRepository(context);
        settings.setTuneMs(BackendMode.LEGACY, 700);
        assertEquals(700, settings.getTuneMs(BackendMode.PHONE_DEBUG));
    }

    @Test public void settingCanEnablePhoneDebugInDebugBuild() {
        SettingsRepository settings = new SettingsRepository(context);
        assertFalse(settings.isPhoneDebugEnabled());
        settings.setPhoneDebugEnabled(true);
        assertTrue(settings.isPhoneDebugEnabled());
    }

    @Test public void serviceSelectsPhoneBackendWhenDebugSettingIsEnabled() {
        SettingsRepository settings = new SettingsRepository(context);
        settings.setBridgeEnabled(true);
        settings.setPhoneDebugEnabled(true);
        ServiceController<UniversalBridgeService> controller =
                Robolectric.buildService(UniversalBridgeService.class).create();
        try {
            controller.get().onStartCommand(
                    BridgeEvents.serviceIntent(context, BridgeEvents.ACTION_START), 0, 1);
            assertEquals(BackendMode.PHONE_DEBUG.label(), BridgeStateStore.getTargetMode());
            assertEquals(BackendMode.PHONE_DEBUG.label(), BridgeStateStore.getActiveMode());
            assertTrue(BridgeStateStore.getOutputState().contains("手机调试台"));
        } finally {
            controller.destroy();
        }
    }

    @Test public void backendRegistersPublishesAndRoutesVehicleStyleControls() {
        RecordingListener listener = new RecordingListener();
        backend.configure(0, false, true);
        backend.connect(42L, listener);
        assertEquals(BackendConnectionState.REGISTERED, listener.state);
        assertTrue(backend.isConnected());
        assertTrue(backend.isRegistered());

        PlayerSnapshot snapshot = new PlayerSnapshot(0, "player.test", "Test Player",
                "Track", "Artist", "Album", "", 180000L, 1000L,
                PlaybackStatus.PLAYING, FavoriteState.NOT_FAVORITED, true,
                null, System.currentTimeMillis(), "session-1", "track-1", 1f,
                "", "metadata", false, "未收藏");
        backend.publish(snapshot, 0, false, true);

        assertTrue(PhoneDebugController.isAvailable());
        assertTrue(PhoneDebugController.previous());
        assertTrue(PhoneDebugController.next());
        assertTrue(PhoneDebugController.togglePlayback());
        assertTrue(PhoneDebugController.favorite(true));
        assertTrue(PhoneDebugController.seekTo(5000L));
        MediaSession.QueueItem queueItem = new MediaSession.QueueItem(
                new MediaDescription.Builder().setMediaId("queue-track").setTitle("Queue track").build(), 7L);
        BridgeStateStore.setQueueSnapshot(QueueSnapshot.from("player.test", "session-1", 42L,
                "Queue", Collections.singletonList(queueItem), 7L));
        assertTrue(PhoneDebugController.playQueueItem(7L));
        assertFalse(PhoneDebugController.playQueueItem(8L));
        BrowserSnapshot browser = new BrowserSnapshot("player.test", "player/.Browser", "root", "",
                Collections.singletonList(new BrowserSnapshot.Entry("browser-track", "Browser track", "", true, false)));
        PhoneDebugController.approveBrowserSnapshot(browser);
        assertTrue(PhoneDebugController.playMediaId("browser-track"));
        assertFalse(PhoneDebugController.playMediaId("not-approved"));
        assertFalse(PhoneDebugController.playMediaId(""));

        backend.clear();
        assertFalse(PhoneDebugController.isAvailable());
        assertFalse(PhoneDebugController.next());
    }

    @Test public void staleGenerationAndCloseDisableAllControls() {
        backend.connect(42L, new RecordingListener());
        backend.publish(new PlayerSnapshot("player.test", "Player", "Track", "Artist",
                "Album", 1000L, 0L, PlaybackStatus.PAUSED, FavoriteState.UNKNOWN,
                true, null, 0L), 0, false, true);
        assertTrue(PhoneDebugController.isAvailable());

        BridgeStateStore.setGeneration(43L);
        assertFalse(PhoneDebugController.next());
        assertFalse(PhoneDebugController.favorite(true));

        backend.close();
        assertFalse(PhoneDebugController.isAvailable());
        assertFalse(PhoneDebugController.togglePlayback());
    }

    private static final class RecordingListener implements BackendListener {
        BackendConnectionState state;
        @Override public void onBackendState(long generation, BackendConnectionState state, String detail) {
            this.state = state;
        }
    }
}
