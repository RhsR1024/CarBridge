package com.geely.auto.music;

import android.app.Application;
import android.content.Context;
import android.media.MediaMetadata;
import android.media.session.*;
import android.os.SystemClock;
import java.lang.reflect.*;
import java.util.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.android.controller.ServiceController;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, application = Application.class)
@LooperMode(LooperMode.Mode.PAUSED)
public class SourcePriorityIntegrationTest {
    Context context;
    SettingsRepository settings;
    ServiceController<MediaListenerService> lifecycle;
    MediaListenerService service;
    @Before public void setup() throws Exception {
        context = RuntimeEnvironment.getApplication();
        context.getSharedPreferences("media_bridge_settings", 0).edit().clear().commit();
        settings = new SettingsRepository(context); settings.setOnlineLookupEnabled(false);
        settings.setRoundArtworkEnabled(false);
        BridgeStateStore.setNativeOwner(""); BridgeStateStore.clearSnapshot();
        BridgeStateStore.setOutputRunning(false); BridgeStateStore.setGeneration(123);
        lifecycle = Robolectric.buildService(MediaListenerService.class).create(); service = lifecycle.get();
        field("connected", true);
    }
    @After public void cleanup() {
        lifecycle.destroy(); BridgeStateStore.setNativeOwner(""); BridgeStateStore.clearSnapshot();
    }
    private void field(String name, Object value) throws Exception {
        Field f = MediaListenerService.class.getDeclaredField(name); f.setAccessible(true); f.set(service, value);
    }
    private MediaController controller(String pkg, int state) {
        MediaSession session = new MediaSession(context, pkg);
        MediaController c = new MediaController(context, session.getSessionToken());
        Shadows.shadowOf(c).setPackageName(pkg);
        Shadows.shadowOf(c).setMetadata(new MediaMetadata.Builder()
                .putString(MediaMetadata.METADATA_KEY_TITLE, pkg).putString(MediaMetadata.METADATA_KEY_MEDIA_ID, pkg).build());
        state(c, state); return c;
    }
    private void state(MediaController c, int state) {
        Shadows.shadowOf(c).setPlaybackState(new PlaybackState.Builder().setState(state, 0L, 1f).build());
    }
    private MediaController select(MediaController... controllers) throws Exception {
        SystemClock.sleep(1);
        Method sync = MediaListenerService.class.getDeclaredMethod("syncSessionRecords", List.class); sync.setAccessible(true);
        sync.invoke(service, Arrays.asList(controllers)); field("initialSessionScan", false);
        Method select = MediaListenerService.class.getDeclaredMethod("selectController", List.class); select.setAccessible(true);
        MediaController result = (MediaController) select.invoke(service, Arrays.asList(controllers));
        field("currentController", result); return result;
    }
    @Test public void onlyUserBridgeIgnoreListOverridesFixedPlayer() throws Exception {
        MediaController third = controller("third.party", PlaybackState.STATE_PLAYING);
        MediaController nativeApp = controller("com.netease.cloudmusic.iot", PlaybackState.STATE_PLAYING);
        settings.setSelectedPackage("third.party");
        settings.setFavoriteIgnored("com.netease.cloudmusic.iot", true);
        assertSame(third, select(third, nativeApp));
        assertTrue(settings.getBlacklist().isEmpty());
        settings.setBlacklisted("com.netease.cloudmusic.iot", true);
        assertNull(select(third, nativeApp));
        assertEquals("com.netease.cloudmusic.iot", BridgeStateStore.getNativeOwner());
    }
    @Test public void nativePauseAndOldMetadataCannotResumeStaleThirdPartyPlayback() throws Exception {
        MediaController third = controller("third.party", PlaybackState.STATE_PLAYING);
        MediaController nativeApp = controller("native.music", PlaybackState.STATE_PAUSED);
        settings.setBlacklisted("native.music", true);
        assertSame(third, select(third, nativeApp));
        state(nativeApp, PlaybackState.STATE_PLAYING); assertNull(select(third, nativeApp));
        state(nativeApp, PlaybackState.STATE_PAUSED);
        for (int i = 0; i < 20; i++) assertNull(select(nativeApp, third));
        state(third, PlaybackState.STATE_PAUSED); assertNull(select(third, nativeApp));
        state(third, PlaybackState.STATE_PLAYING); assertSame(third, select(third, nativeApp));
        assertEquals("", BridgeStateStore.getNativeOwner());
    }
    @Test public void ignoredBufferingSourceAlsoHasPriority() throws Exception {
        MediaController third = controller("third.party", PlaybackState.STATE_PLAYING);
        MediaController nativeApp = controller("native.music", PlaybackState.STATE_PAUSED);
        settings.setBlacklisted("native.music", true);
        assertSame(third, select(third, nativeApp));
        state(nativeApp, PlaybackState.STATE_BUFFERING); assertNull(select(third, nativeApp));
        state(nativeApp, PlaybackState.STATE_PAUSED); assertNull(select(third, nativeApp));
    }
    @Test public void freshThirdPartyPlaybackDuringNativeStopCanResumeAfterNativeActuallyPauses() throws Exception {
        MediaController third = controller("third.party", PlaybackState.STATE_PAUSED);
        MediaController nativeApp = controller("native.music", PlaybackState.STATE_PLAYING);
        settings.setBlacklisted("native.music", true);
        assertNull(select(third, nativeApp));
        state(third, PlaybackState.STATE_PLAYING); assertNull(select(third, nativeApp));
        state(nativeApp, PlaybackState.STATE_PAUSED); assertSame(third, select(third, nativeApp));
    }
    @Test public void vehicleSourceSelectionNeedsFreshPlayEvenWithoutNativeMediaSession() throws Exception {
        MediaController third = controller("third.party", PlaybackState.STATE_PLAYING);
        assertSame(third, select(third));
        long epoch = BridgeStateStore.inputEpoch();
        MediaListenerService.yieldToVehicleSource("factory.radio");
        Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();
        assertTrue(BridgeStateStore.inputEpoch() > epoch);
        assertNull(select(third));
        state(third, PlaybackState.STATE_PAUSED); assertNull(select(third));
        state(third, PlaybackState.STATE_PLAYING); assertSame(third, select(third));
    }
    @Test public void listenerRecreationDoesNotTreatCachedPlayingAsNewPlayback() throws Exception {
        MediaController third = controller("third.party", PlaybackState.STATE_PLAYING);
        assertSame(third, select(third));
        MediaListenerService.yieldToVehicleSource("factory.radio");
        Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();
        lifecycle.destroy();
        lifecycle = Robolectric.buildService(MediaListenerService.class).create(); service = lifecycle.get();
        field("connected", true);
        assertNull(select(third));
        state(third, PlaybackState.STATE_PAUSED); select(third);
        state(third, PlaybackState.STATE_PLAYING); assertSame(third, select(third));
    }
    @Test public void multipleIgnoredPlayersDoNotOscillateEpochOrSuppressARealLaterStart() throws Exception {
        MediaController third = controller("third.party", PlaybackState.STATE_PAUSED);
        MediaController first = controller("native.one", PlaybackState.STATE_PLAYING);
        MediaController second = controller("native.two", PlaybackState.STATE_PLAYING);
        settings.setBlacklisted("native.one", true); settings.setBlacklisted("native.two", true);
        assertNull(select(first, second, third));
        long epoch = BridgeStateStore.inputEpoch();
        for (int i = 0; i < 10; i++) assertNull(select(second, third, first));
        assertEquals(epoch, BridgeStateStore.inputEpoch());
        state(third, PlaybackState.STATE_PLAYING); assertNull(select(first, second, third));
        state(first, PlaybackState.STATE_PAUSED); state(second, PlaybackState.STATE_PAUSED);
        assertSame(third, select(first, second, third));
        assertTrue(BridgeStateStore.inputEpoch() > epoch);
    }
    @Test public void suspendedClientRejectsAllVehicleTransportAndCollectionCommands() {
        LegacyMusicClient client = new LegacyMusicClient(context, 123);
        PlayerSnapshot snapshot = new PlayerSnapshot("third.party", "Third", "Track", "Artist", "Album",
                1000, 0, PlaybackStatus.PLAYING, FavoriteState.FAVORITED, true, null, 0);
        client.update(snapshot, 0, false, true); client.setControlsEnabled(false);
        assertFalse(client.onPlay()); assertFalse(client.onPause()); assertFalse(client.onNext());
        assertFalse(client.onPrevious()); assertFalse(client.onSourceSelected(6));
        assertEquals(com.ecarx.eas.sdk.mediacenter.MusicClient.FAV_COLLECTED_NOT_SUPPORT, client.ctrlCollect(0, false));
        client.destroy();
    }

    @Test public void carPlayEmptyPlayingSessionCannotReplaceThirdPartyOrBecomeFallback() throws Exception {
        MediaController third = controller("third.party", PlaybackState.STATE_PLAYING);
        MediaController carplay = controller("com.flyme.auto.energy", PlaybackState.STATE_PAUSED);
        Shadows.shadowOf(carplay).setMetadata(null);
        assertSame(third, select(third, carplay));
        state(carplay, PlaybackState.STATE_PLAYING);
        assertSame(third, select(third, carplay));
        assertNull(select(carplay));
        state(carplay, PlaybackState.STATE_PAUSED);
        assertNull(select(carplay));
        assertTrue(settings.getBlacklist().isEmpty());
    }

    @Test public void ignoredCarPlayEmptyPlayingSessionDoesNotPermanentlyBlockThirdParty() throws Exception {
        MediaController third = controller("third.party", PlaybackState.STATE_PAUSED);
        MediaController carplay = controller("com.flyme.auto.energy", PlaybackState.STATE_PLAYING);
        Shadows.shadowOf(carplay).setMetadata(new MediaMetadata.Builder().build());
        settings.setBlacklisted("com.flyme.auto.energy", true);
        select(third, carplay);
        state(third, PlaybackState.STATE_PLAYING);
        for (int i = 0; i < 10; i++) assertSame(third, select(carplay, third));
        assertEquals("", BridgeStateStore.getNativeOwner());
        state(carplay, PlaybackState.STATE_BUFFERING);
        assertSame(third, select(carplay, third));
    }

    @Test public void fixedSelectionDoesNotBypassCarPlayControlSessionGuard() throws Exception {
        MediaController third = controller("third.party", PlaybackState.STATE_PLAYING);
        MediaController carplay = controller("com.flyme.auto.energy", PlaybackState.STATE_PLAYING);
        Shadows.shadowOf(carplay).setMetadata(null);
        settings.setSelectedPackage("com.flyme.auto.energy");
        assertNull(select(third, carplay));
    }

    @Test public void realCarPlayMetadataCannotBypassCompanionHandshakeOrUserIgnoreList() throws Exception {
        MediaController third = controller("third.party", PlaybackState.STATE_PLAYING);
        MediaController carplay = controller("com.flyme.auto.energy", PlaybackState.STATE_PAUSED);
        assertSame(third, select(third, carplay));
        state(carplay, PlaybackState.STATE_PLAYING);
        // USB CarPlay is now a managed source: real metadata alone cannot authorize bridging.
        assertNull(select(third, carplay));
        settings.setBlacklisted("com.flyme.auto.energy", true);
        assertNull(select(third, carplay));
        assertEquals("com.flyme.auto.energy", BridgeStateStore.getNativeOwner());
        Shadows.shadowOf(carplay).setMetadata(null);
        assertNull(select(third, carplay));
        state(third, PlaybackState.STATE_PAUSED); select(third, carplay);
        state(third, PlaybackState.STATE_PLAYING); assertSame(third, select(third, carplay));
    }

    @Test public void vehicleSelectingCarPlayStillYieldsAndRequiresFreshThirdPartyPlay() throws Exception {
        MediaController third = controller("third.party", PlaybackState.STATE_PLAYING);
        MediaController carplay = controller("com.flyme.auto.energy", PlaybackState.STATE_PLAYING);
        Shadows.shadowOf(carplay).setMetadata(null);
        settings.setBlacklisted("com.flyme.auto.energy", true);
        assertSame(third, select(third, carplay));
        MediaListenerService.yieldToVehicleSource("com.flyme.auto.energy");
        Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();
        assertEquals("com.flyme.auto.energy", BridgeStateStore.getNativeOwner());
        for (int i = 0; i < 10; i++) assertNull(select(third, carplay));
        state(third, PlaybackState.STATE_PAUSED); assertNull(select(third, carplay));
        state(third, PlaybackState.STATE_PLAYING); assertSame(third, select(third, carplay));
    }

    @Test public void otherNativeAndThirdPartyPlayersCanStillStartWithoutMetadata() throws Exception {
        MediaController third = controller("third.party", PlaybackState.STATE_PLAYING);
        MediaController nativeApp = controller("com.netease.cloudmusic.iot", PlaybackState.STATE_PAUSED);
        Shadows.shadowOf(third).setMetadata(null); Shadows.shadowOf(nativeApp).setMetadata(null);
        settings.setBlacklisted("com.netease.cloudmusic.iot", true);
        assertSame(third, select(third, nativeApp));
        state(nativeApp, PlaybackState.STATE_PLAYING);
        assertNull(select(third, nativeApp));
        assertEquals("com.netease.cloudmusic.iot", BridgeStateStore.getNativeOwner());
    }

    @Test public void sourceIdentityRemainsMediaBridgeWhileInputPlayerAndTrackChange() {
        LegacyMusicClient client = new LegacyMusicClient(context, 123);
        try {
            String icon = client.getMusicPlaybackInfo().getAppIcon();
            String[] packages = {"com.flyme.auto.energy", "com.luna.music.car", "cn.toside.music.mobile"};
            String[] labels = {"CarPlay", "汽水音乐", "LX Music"};
            for (int i = 0; i < packages.length; i++) {
                String label = labels[i];
                PlayerSnapshot snapshot = new PlayerSnapshot(packages[i], label, "Track " + label, "Artist", "Album",
                        1000, 0, PlaybackStatus.PLAYING, FavoriteState.UNKNOWN, false, null, 0);
                client.update(snapshot, 0, false, false);
                assertEquals("媒体桥接", client.getMusicPlaybackInfo().getAppName());
                assertEquals(context.getPackageName(), client.getMusicPlaybackInfo().getPackageName());
                assertEquals(icon, client.getMusicPlaybackInfo().getAppIcon());
                assertEquals("Track " + label, client.getMusicPlaybackInfo().getTitle());
                assertEquals(label, client.getSnapshot().appLabel);
            }
            client.clear();
            assertEquals("媒体桥接", client.getMusicPlaybackInfo().getAppName());
        } finally { client.destroy(); }
    }
}
