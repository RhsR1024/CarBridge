package com.geely.auto.music;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSession;
import android.os.Looper;
import com.geely.auto.music.lyrics.LyricsManager;
import java.lang.reflect.*;
import java.util.Map;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.android.controller.ServiceController;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;
import static org.junit.Assert.*;
import static io.github.rhsr1024.interop.BridgeProtocol.CARBRIDGE;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, application = Application.class)
@LooperMode(LooperMode.Mode.PAUSED)
public class CarBridgeMetadataTest {
    private Context context;
    @Before public void setup() {
        context = RuntimeEnvironment.getApplication();
        context.getSharedPreferences("media_bridge_settings", 0).edit().clear().commit();
        new SettingsRepository(context).setOnlineLookupEnabled(true);
        BridgeStateStore.clearSnapshot();
    }

    @Test public void lyricOnlyUpdatesNeverStartArtworkOrLyricsLookup() throws Exception {
        ArtworkRepository artwork = new ArtworkRepository(context);
        LyricsManager lyrics = new LyricsManager(context);
        try {
            for (String line : new String[]{"华丽得无法低调", "为什么 全世界的恋", "为所有的悲剧 当特约演员"}) {
                assertEquals("", artwork.resolve(CARBRIDGE, line, "", null, null, line, "", 0, 1, () -> {}));
                assertNull(field(artwork, "task")); assertNull(field(artwork, "scope"));
                lyrics.onTrackChanged(CARBRIDGE, line, line, "", 0, "", ignored -> {});
                assertNull(field(lyrics, "currentFetch")); assertNull(field(lyrics, "scope"));
                assertNull(lyrics.getCurrentLyrics());
                assertTrue(lyrics.getSource().contains("信息不完整"));
            }
        } finally { artwork.close(); lyrics.destroy(); }
    }

    @Test public void incompleteMetadataStillAllowsNativeArtworkAndTimedLyrics() {
        ArtworkRepository artwork = new ArtworkRepository(context);
        LyricsManager lyrics = new LyricsManager(context);
        Bitmap bitmap = Bitmap.createBitmap(16, 16, Bitmap.Config.ARGB_8888);
        try {
            assertTrue(artwork.resolve(CARBRIDGE, "track", "", bitmap, null, "Title", "", 0, 1, () -> {}).startsWith("content://"));
            lyrics.onTrackChanged(CARBRIDGE, "track", "Title", "", 0, "[00:01]native line", ignored -> {});
            assertEquals("native line", lyrics.getCurrentLyrics().getLineAtPosition(1500));
        } finally { artwork.close(); lyrics.destroy(); bitmap.recycle(); }
    }

    @Test public void carBridgeNotificationCannotBecomeArtistButRealTrackChangesArePublished() throws Exception {
        ServiceController<MediaListenerService> lifecycle = Robolectric.buildService(MediaListenerService.class).create();
        MediaSession session = new MediaSession(context, "carbridge-test");
        try {
            MediaListenerService service = lifecycle.get(); set(service, "connected", true);
            MediaController player = new MediaController(context, session.getSessionToken());
            Shadows.shadowOf(player).setPackageName(CARBRIDGE);
            Class<?> record = Class.forName(MediaListenerService.class.getName() + "$NotificationRecord");
            Constructor<?> constructor = record.getDeclaredConstructors()[0]; constructor.setAccessible(true);
            Object notification = constructor.newInstance(CARBRIDGE, player.getSessionToken(), player,
                    "华丽得无法低调", "CarPlay 已连接", "", null, null, null, null, 1L);
            @SuppressWarnings("unchecked") Map<String, Object> notifications = (Map<String, Object>) field(service, "notificationRecords");
            notifications.put("notification", notification);
            Shadows.shadowOf(player).setMetadata(new MediaMetadata.Builder().putString(MediaMetadata.METADATA_KEY_TITLE, "华丽得无法低调").build());
            publish(service, player);
            assertEquals("", BridgeStateStore.getSnapshot().artist);
            assertEquals("", BridgeStateStore.getSnapshot().artworkUri);
            assertEquals(FavoriteState.UNKNOWN, BridgeStateStore.getSnapshot().favoriteState);
            assertFalse(BridgeStateStore.getSnapshot().favoriteWriteSupported);
            new SettingsRepository(context).setOnlineLookupEnabled(false);
            String first = null;
            for (String title : new String[]{"真实歌曲一", "真实歌曲二"}) {
                Shadows.shadowOf(player).setMetadata(new MediaMetadata.Builder()
                        .putString(MediaMetadata.METADATA_KEY_TITLE, title).putString(MediaMetadata.METADATA_KEY_ARTIST, "歌手")
                        .putLong(MediaMetadata.METADATA_KEY_DURATION, 180000).build());
                publish(service, player);
                assertEquals(title, BridgeStateStore.getSnapshot().title);
                assertEquals("歌手", BridgeStateStore.getSnapshot().artist);
                if (first != null) assertNotEquals(first, BridgeStateStore.getSnapshot().trackId);
                first = BridgeStateStore.getSnapshot().trackId;
            }
        } finally { lifecycle.destroy(); session.release(); }
    }

    @Test public void guardedLookupRequiresSongIdentityAndDoesNotChangeOtherPlayers() {
        assertFalse(CarBridgeMetadataPolicy.allowsLookup(CARBRIDGE, "line", "", 0));
        assertFalse(CarBridgeMetadataPolicy.allowsLookup(CARBRIDGE, "line", "CarPlay 已连接", 180000));
        assertFalse(CarBridgeMetadataPolicy.allowsLookup(CARBRIDGE, "Song", "Artist", 0));
        assertTrue(CarBridgeMetadataPolicy.allowsLookup(CARBRIDGE, "Song", "Artist", 180000));
        assertTrue(CarBridgeMetadataPolicy.allowsLookup("test.player", "Song", "", 0));
    }

    @Test public void coverMatchRejectsWrongFirstResultSingerAndVersion() {
        assertTrue(CarBridgeMetadataPolicy.matches("Ｓong", "Artist", 180000, "song", "Artist", 181000));
        assertFalse(CarBridgeMetadataPolicy.matches("Song", "Artist", 180000, "上一首情歌", "Artist", 180000));
        assertFalse(CarBridgeMetadataPolicy.matches("Song", "Artist", 180000, "Song", "Other", 180000));
        assertFalse(CarBridgeMetadataPolicy.matches("Song", "Artist", 180000, "Song (Live)", "Artist", 180000));
        assertFalse(CarBridgeMetadataPolicy.matches("Song", "Artist", 180000, "Song", "Artist", 210000));
        assertFalse(CarBridgeMetadataPolicy.matches("Song", "Artist", 180000, "Song", "Artist", 0));
    }

    @Test public void metadataProbeDoesNotExposeTitlesInRedactedExports() {
        String probe = "CARBRIDGE_METADATA source=MediaSession notificationFallback=false\ntitle=私密歌名\nartist=私密歌手\nalbum=私密专辑";
        assertFalse(DiagnosticRedactor.redact(probe, false).contains("私密"));
        assertTrue(DiagnosticRedactor.redact(probe, true).contains("私密歌名"));
    }

    @Test public void unsupportedFavoriteDoesNotReportSuccessOrChangeState() throws Exception {
        ServiceController<MediaListenerService> lifecycle = Robolectric.buildService(MediaListenerService.class).create();
        MediaSession session = new MediaSession(context, "favorite-test");
        try {
            MediaListenerService service = lifecycle.get(); set(service, "connected", true);
            new SettingsRepository(context).setBridgeEnabled(true);
            android.provider.Settings.Secure.putString(context.getContentResolver(), "enabled_notification_listeners",
                    new android.content.ComponentName(context, MediaListenerService.class).flattenToString());
            MediaController player = new MediaController(context, session.getSessionToken());
            Shadows.shadowOf(player).setPackageName(CARBRIDGE); set(service, "currentController", player);
            Method method = MediaListenerService.class.getDeclaredMethod("sendCommand", String.class, Intent.class);
            method.setAccessible(true);
            method.invoke(service, "FAVORITE", new Intent().putExtra("bridgeGeneration", BridgeStateStore.getGeneration()));
            assertTrue(BridgeStateStore.getLastControl().contains("未声明支持"));
            assertNull(BridgeStateStore.getSnapshot());
        } finally { lifecycle.destroy(); session.release(); }
    }

    private static Object field(Object object, String name) throws Exception {
        Field field = object.getClass().getDeclaredField(name); field.setAccessible(true); return field.get(object);
    }
    private static void set(Object object, String name, Object value) throws Exception {
        Field field = object.getClass().getDeclaredField(name); field.setAccessible(true); field.set(object, value);
    }
    private static void publish(MediaListenerService service, MediaController player) throws Exception {
        Method method = MediaListenerService.class.getDeclaredMethod("publish", MediaController.class);
        method.setAccessible(true); method.invoke(service, player);
    }
}
