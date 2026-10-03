package com.geely.auto.music;

import android.app.Application;
import android.app.PendingIntent;
import android.content.*;
import android.content.pm.*;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.MediaMetadata;
import android.media.Rating;
import android.media.session.*;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import com.geely.auto.music.lyrics.LyricsManager;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;
import org.robolectric.android.controller.ServiceController;
import org.robolectric.shadows.ShadowMediaController;
import java.io.File;
import java.lang.reflect.*;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, application = Application.class)
@LooperMode(LooperMode.Mode.PAUSED)
public class AndroidRegressionTest {
    private Context context;
    @Before public void setup() {
        context = RuntimeEnvironment.getApplication();
        context.getSharedPreferences("media_bridge_settings", 0).edit().clear().commit();
        new SettingsRepository(context).setOnlineLookupEnabled(false);
        // Existing repository tests exercise the verified V3 pass-through path. Tests for the
        // optional safe-area compositor enable it explicitly.
        new SettingsRepository(context).setRoundArtworkEnabled(false);
        BridgeStateStore.clearSnapshot(); BridgeStateStore.setGeneration(500);
        BridgeStateStore.setOutputRunning(false);
        grant(true);
    }
    private void grant(boolean enabled) {
        Settings.Secure.putString(context.getContentResolver(), "enabled_notification_listeners", enabled
                ? new ComponentName(context, MediaListenerService.class).flattenToString() : "");
    }
    @Test public void settingsMigrationPreservesSeparateChannelTunesAndOldKeys() {
        context.getSharedPreferences("media_bridge_settings", 0).edit().clear().commit();
        context.getSharedPreferences("mb_ui_prefs", 0).edit().putInt("lyric_tune_ms", 5000).commit();
        SettingsRepository settings = new SettingsRepository(context);
        assertEquals(3000, settings.getTuneMs(BackendMode.LEGACY));
        settings.setTuneMs(BackendMode.LEGACY, 1700); settings.setTuneMs(BackendMode.F25, -800);
        context.getSharedPreferences("media_bridge_settings", 0).edit().putInt("settings_schema_version", 2).commit();
        SettingsRepository migrated = new SettingsRepository(context);
        assertEquals(1700, migrated.getTuneMs(BackendMode.LEGACY)); assertEquals(-800, migrated.getTuneMs(BackendMode.F25));
        assertTrue(migrated.isOnlineLookupEnabled()); assertEquals(5, migrated.getLyricsSources().size());
    }
    @Test public void onlineUpgradeRestoresOriginalDefaultButPreservesAnExplicitOptOut() {
        SharedPreferences prefs = context.getSharedPreferences("media_bridge_settings", 0);
        prefs.edit().clear().putInt("settings_schema_version", 3).commit();
        assertTrue(new SettingsRepository(context).isOnlineLookupEnabled());
        new SettingsRepository(context).setOnlineLookupEnabled(false);
        prefs.edit().putInt("settings_schema_version", 3).commit();
        assertFalse(new SettingsRepository(context).isOnlineLookupEnabled());
        assertFalse(new SettingsRepository(context).isOnlineLookupEnabled());
    }
    @Test public void roundArtworkAdaptationDefaultsOnAndCanBeDisabled() {
        context.getSharedPreferences("media_bridge_settings", 0).edit().clear().commit();
        SettingsRepository settings = new SettingsRepository(context);
        assertTrue(settings.isRoundArtworkEnabled());
        settings.setRoundArtworkEnabled(false);
        assertFalse(new SettingsRepository(context).isRoundArtworkEnabled());
    }
    @Test public void favoriteIgnoreListDefaultsEmptyPersistsAndDoesNotBlockThePlayer() {
        SettingsRepository settings = new SettingsRepository(context);
        assertTrue(settings.getFavoriteIgnoredPackages().isEmpty());
        settings.setFavoriteIgnored("test.player", true);
        assertTrue(new SettingsRepository(context).isFavoriteIgnored("test.player"));
        assertFalse(new SettingsRepository(context).getBlacklist().contains("test.player"));
        settings.setFavoriteIgnored("test.player", false);
        assertFalse(new SettingsRepository(context).isFavoriteIgnored("test.player"));
        settings.setFavoriteIgnored("test.player", true);
        settings.clearFavoriteIgnoredPackages();
        assertTrue(new SettingsRepository(context).getFavoriteIgnoredPackages().isEmpty());
    }
    @Test public void yandexAndZvukUseOnlyVerifiedStateAndDirectionalOrKnownToggleActions() {
        PlaybackState navi = new PlaybackState.Builder().addCustomAction(
                "com.yandex.music.sdk.helper.foreground.mediasession.LIKE_CUSTOM_ACTION", "Like", 1).build();
        assertEquals(Boolean.FALSE, PlayerFeatures.favoriteFromState(PlayerFeatures.NAVI, navi));
        assertEquals("com.yandex.music.sdk.helper.action.ADD_LIKE", PlayerFeatures.favoriteAction(
                PlayerFeatures.NAVI, navi, true, FavoriteState.NOT_FAVORITED));
        assertNull(PlayerFeatures.favoriteAction(PlayerFeatures.NAVI, navi, true, FavoriteState.FAVORITED));
        PlaybackState zvuk = new PlaybackState.Builder().addCustomAction("com.zvooq.openplay.CUSTOM_ACTION_LIKE", "Like", 2131231640).build();
        assertEquals(Boolean.TRUE, PlayerFeatures.favoriteFromState(PlayerFeatures.ZVUK, zvuk));
        assertNotNull(PlayerFeatures.favoriteAction(PlayerFeatures.ZVUK, zvuk, false, FavoriteState.FAVORITED));
        assertNull(PlayerFeatures.favoriteAction(PlayerFeatures.ZVUK, zvuk, false, FavoriteState.UNKNOWN));
        PlaybackState unknownIcon = new PlaybackState.Builder().addCustomAction("com.zvooq.openplay.CUSTOM_ACTION_LIKE", "Like", 222).build();
        assertNull(PlayerFeatures.favoriteFromState(PlayerFeatures.ZVUK, unknownIcon));
    }
    @Test public void artworkFileNameMatchesTheKnownGood18Format() {
        Bitmap a = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888);
        Bitmap b = a.copy(Bitmap.Config.ARGB_8888, true); b.setPixel(1, 6, 0xff12ab34);
        Uri first = ArtworkHelper.saveArtwork(context, a), second = ArtworkHelper.saveArtwork(context, b);
        assertNotNull(first); assertEquals(first, second);
        assertTrue(first.getLastPathSegment().matches("artwork_[0-9a-f]{12}\\.png"));
        assertEquals(first, ArtworkHelper.saveArtwork(context, a));
    }
    @Test public void artworkSaveRejectsARepositoryGenerationInvalidatedByClearCache() {
        Bitmap image = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888);
        long before = ArtworkRepository.cacheVersion();
        ArtworkHelper.clearCache(context);
        assertNull(ArtworkHelper.saveArtwork(context, image, before));
    }
    @Test public void v3PreservesSmallPlayerArtworkWithoutUpscaling() {
        Bitmap thumbnail = Bitmap.createBitmap(70, 70, Bitmap.Config.ARGB_8888);
        thumbnail.eraseColor(0xff2468ac);
        Uri saved = ArtworkHelper.saveArtwork(context, thumbnail);
        assertNotNull(saved);
        Bitmap decoded = BitmapFactory.decodeFile(new File(ArtworkHelper.getArtworkDir(context),
                saved.getLastPathSegment()).getAbsolutePath());
        assertNotNull(decoded);
        assertEquals(70, decoded.getWidth()); assertEquals(70, decoded.getHeight());
    }
    @Test public void roundArtworkSafeAreaPreservesSizeAndUsesAnOpaqueSoftenedBorder() {
        Bitmap source = Bitmap.createBitmap(240, 240, Bitmap.Config.ARGB_8888);
        source.eraseColor(0xffff0000);
        Bitmap decoded = null;
        try {
            Uri original = ArtworkHelper.saveArtwork(context, source, false);
            Uri adapted = ArtworkHelper.saveArtwork(context, source, true);
            assertNotNull(original); assertNotNull(adapted); assertNotEquals(original, adapted);
            decoded = BitmapFactory.decodeFile(new File(ArtworkHelper.getArtworkDir(context),
                    adapted.getLastPathSegment()).getAbsolutePath());
            assertNotNull(decoded);
            assertEquals(240, decoded.getWidth()); assertEquals(240, decoded.getHeight());
            assertEquals(169, ArtworkHelper.roundSafeInnerEdge(240));
            assertEquals(0xff, Color.alpha(decoded.getPixel(0, 0)));
        } finally {
            source.recycle();
            if (decoded != null) decoded.recycle();
        }
    }
    @Test public void unreadablePlayerArtworkUriIsPreservedDuringTransitionLikeV3() {
        ArtworkRepository artwork = new ArtworkRepository(context);
        AtomicInteger loaded = new AtomicInteger();
        try {
            // Robolectric cannot open a player HTTP URL. V3 preserves the announced URI for
            // the first snapshot so MediaCenter does not cache an empty artwork entry.
            String source = "https://player.private/album/42";
            assertEquals(source, artwork.resolve("track", source, null, null,
                    "title", "artist", 1, loaded::incrementAndGet));
            assertEquals("播放器原始封面 URI", BridgeStateStore.getArtworkState());
            assertEquals(0, loaded.get());
        } finally { artwork.close(); }
    }
    @Test public void usableEmbeddedBitmapIsCachedBeforePassingThroughRemoteUri() {
        ArtworkRepository artwork = new ArtworkRepository(context);
        Bitmap trackArtwork = Bitmap.createBitmap(320, 320, Bitmap.Config.ARGB_8888);
        trackArtwork.eraseColor(0xff12ab34);
        try {
            String source = "https://player.private/album/42";
            assertEquals(source, artwork.resolve("track", source, null, null,
                    "title", "artist", 1, () -> {}));
            String resolved = artwork.resolve("track", source, trackArtwork, null,
                    "title", "artist", 1, () -> {});
            assertTrue(resolved.startsWith("content://"));
            assertEquals("播放器内嵌封面", BridgeStateStore.getArtworkState());
        } finally {
            trackArtwork.recycle(); artwork.close();
        }
    }
    @Test public void lowResolutionBitmapDoesNotReplaceAFullSizeRemoteCover() {
        ArtworkRepository artwork = new ArtworkRepository(context);
        Bitmap thumbnail = Bitmap.createBitmap(129, 129, Bitmap.Config.ARGB_8888);
        thumbnail.eraseColor(0xff12ab34);
        try {
            String source = "https://player.private/album/full-size";
            assertEquals(source, artwork.resolve("track", source, thumbnail, null,
                    "title", "artist", 1, () -> {}));
            assertEquals("播放器原始封面 URI", BridgeStateStore.getArtworkState());
        } finally {
            thumbnail.recycle(); artwork.close();
        }
    }
    @Test public void lowResolutionBitmapIsStillUsedWhenNoRemoteCoverExists() {
        ArtworkRepository artwork = new ArtworkRepository(context);
        Bitmap thumbnail = Bitmap.createBitmap(70, 70, Bitmap.Config.ARGB_8888);
        thumbnail.eraseColor(0xff12ab34);
        try {
            String resolved = artwork.resolve("track", "", thumbnail, null,
                    "title", "artist", 1, () -> {});
            assertTrue(resolved.startsWith("content://"));
            assertEquals("播放器内嵌封面", BridgeStateStore.getArtworkState());
        } finally {
            thumbnail.recycle(); artwork.close();
        }
    }
    @Test public void v3ArtworkProviderIsExportedForTheFinalVehicleRenderer() {
        ProviderInfo provider = context.getPackageManager().resolveContentProvider(
                ArtworkContentProvider.AUTHORITY, PackageManager.GET_META_DATA);
        assertNotNull(provider);
        assertTrue(provider.exported);
        assertTrue(provider.grantUriPermissions);
    }
    @Test public void embeddedTrackArtworkIsUsedWhenPlayerHasNoUri() {
        ArtworkRepository artwork = new ArtworkRepository(context);
        Bitmap trackArtwork = Bitmap.createBitmap(240, 240, Bitmap.Config.ARGB_8888);
        trackArtwork.eraseColor(0xff12ab34);
        try {
            String resolved = artwork.resolve("track", "", trackArtwork, null,
                    "title", "artist", 1, () -> {});
            assertTrue(resolved.startsWith("content://"));
            assertEquals("播放器内嵌封面", BridgeStateStore.getArtworkState());
        } finally {
            trackArtwork.recycle(); artwork.close();
        }
    }
    @Test public void sameTrackDoesNotDowngradeToASmallerArtworkCandidate() {
        ArtworkRepository artwork = new ArtworkRepository(context);
        Bitmap larger = Bitmap.createBitmap(129, 129, Bitmap.Config.ARGB_8888);
        Bitmap smaller = Bitmap.createBitmap(70, 70, Bitmap.Config.ARGB_8888);
        larger.eraseColor(0xff224466); smaller.eraseColor(0xffaa3300);
        try {
            String first = artwork.resolve("track", "", larger, null, "title", "artist", 1, () -> {});
            assertFalse(first.isEmpty());
            assertEquals(first, artwork.resolve("track", "", smaller, null,
                    "title", "artist", 2, () -> {}));
        } finally { artwork.close(); }
    }
    @Test public void localArtworkIsReadyForTheFirstVehicleSnapshotAndTracksSourceChanges() throws Exception {
        ArtworkRepository artwork = new ArtworkRepository(context);
        AtomicInteger loaded = new AtomicInteger();
        Bitmap first = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888);
        try {
            String published = artwork.resolve("track", "", first, null, "", "", 1, loaded::incrementAndGet);
            assertTrue(published.startsWith("content://"));
            assertEquals("播放器内嵌封面", BridgeStateStore.getArtworkState());
            for (int i = 0; i < 20; i++) {
                Bitmap copy = first.copy(Bitmap.Config.ARGB_8888, true);
                assertEquals(published, artwork.resolve("track", "", copy, null, "", "", 1, loaded::incrementAndGet));
            }
            assertEquals(0, loaded.get());
            Bitmap changed = first.copy(Bitmap.Config.ARGB_8888, true); changed.setPixel(4, 4, 0xffab1200);
            String updated = artwork.resolve("track", "", changed, null, "", "", 2, loaded::incrementAndGet);
            assertNotEquals(published, updated); assertFalse(updated.isEmpty());
            assertEquals(updated, artwork.resolve("track", "", null, null, "", "", 3, loaded::incrementAndGet));
            assertEquals(published, artwork.resolve("next-track", "", first, null, "", "", 3, loaded::incrementAndGet));
        } finally { artwork.close(); }
    }
    @Test public void trackArtWinsOverAlbumArtLikeFlymeV3() throws Exception {
        Bitmap albumThumbnail = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888);
        albumThumbnail.eraseColor(0xff0000ff);
        Bitmap trackArtwork = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888);
        trackArtwork.eraseColor(0xffff0000);
        try {
            MediaMetadata metadata = new MediaMetadata.Builder()
                    .putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, albumThumbnail)
                    .putBitmap(MediaMetadata.METADATA_KEY_ART, trackArtwork)
                    .putString(MediaMetadata.METADATA_KEY_ALBUM_ART_URI, "content://test/album-thumbnail")
                    .putString(MediaMetadata.METADATA_KEY_ART_URI, "content://test/current-track")
                    .build();
            Bitmap selected = MediaListenerService.preferredArtworkBitmap(metadata);
            assertNotNull(selected); assertEquals(0xffff0000, selected.getPixel(4, 4));
            assertEquals("content://test/current-track", MediaListenerService.preferredArtworkUri(metadata));

            MediaMetadata fallback = new MediaMetadata.Builder()
                    .putBitmap(MediaMetadata.METADATA_KEY_ART, trackArtwork)
                    .putString(MediaMetadata.METADATA_KEY_ART_URI, "content://test/current-track")
                    .build();
            assertEquals(0xffff0000, MediaListenerService.preferredArtworkBitmap(fallback).getPixel(4, 4));
            assertEquals("content://test/current-track", MediaListenerService.preferredArtworkUri(fallback));
        } finally {
            albumThumbnail.recycle(); trackArtwork.recycle();
        }
    }
    @Test public void kugouAndKuwoKeepAlbumUriAndBitmapAsOneCompleteCoverCandidate() {
        Bitmap croppedArt = Bitmap.createBitmap(80, 80, Bitmap.Config.ARGB_8888);
        Bitmap completeAlbum = Bitmap.createBitmap(240, 240, Bitmap.Config.ARGB_8888);
        try {
            MediaMetadata metadata = new MediaMetadata.Builder()
                    .putBitmap(MediaMetadata.METADATA_KEY_ART, croppedArt)
                    .putString(MediaMetadata.METADATA_KEY_ART_URI, "content://test/cropped-art")
                    .putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, completeAlbum)
                    .putString(MediaMetadata.METADATA_KEY_ALBUM_ART_URI, "content://test/complete-album")
                    .build();
            MediaListenerService.ArtworkSelection kugou = MediaListenerService.preferredArtwork(
                    metadata, "com.kugou.android.lite");
            assertEquals("album", kugou.source);
            assertSame(completeAlbum, kugou.bitmap);
            assertEquals("content://test/complete-album", kugou.uri);
            MediaListenerService.ArtworkSelection kuwo = MediaListenerService.preferredArtwork(
                    metadata, "cn.kuwo.kwmusiccar");
            assertEquals("album", kuwo.source);
            assertSame(completeAlbum, kuwo.bitmap);
            MediaListenerService.ArtworkSelection lx = MediaListenerService.preferredArtwork(
                    metadata, PlayerFeatures.LX_MUSIC);
            assertEquals("album", lx.source);
            assertSame(completeAlbum, lx.bitmap);
            MediaListenerService.ArtworkSelection generic = MediaListenerService.preferredArtwork(
                    metadata, "test.player");
            assertEquals("art", generic.source);
            assertSame(croppedArt, generic.bitmap);
            assertEquals("content://test/cropped-art", generic.uri);
        } finally {
            croppedArt.recycle(); completeAlbum.recycle();
        }
    }

    @Test public void phoneDebugMaterializesRemoteArtworkThatImageViewCannotRenderDirectly() {
        assertTrue(ArtworkRepository.shouldMaterializeRemoteForDebug(true, "https"));
        assertTrue(ArtworkRepository.shouldMaterializeRemoteForDebug(true, "HTTP"));
        assertFalse(ArtworkRepository.shouldMaterializeRemoteForDebug(false, "https"));
        assertFalse(ArtworkRepository.shouldMaterializeRemoteForDebug(true, "content"));
    }
    @Test public void kuwoWaitsBrieflyForItsRemoteOriginalInsteadOfPublishingAThumbnail() {
        Bitmap thumbnail = Bitmap.createBitmap(129, 129, Bitmap.Config.ARGB_8888);
        try {
            assertTrue(ArtworkRepository.shouldAwaitRemoteCover(
                    "cn.kuwo.kwmusiccar", "", thumbnail));
            assertFalse(ArtworkRepository.shouldAwaitRemoteCover(
                    "cn.kuwo.kwmusiccar", "https://player/cover", thumbnail));
            assertFalse(ArtworkRepository.shouldAwaitRemoteCover(
                    "com.kugou.android.lite", "", thumbnail));
        } finally { thumbnail.recycle(); }
    }
    @Test public void smartArtworkOnlyAdaptsKugouAndOnlyLooksUpItsEmbeddedOnlyCover() {
        Bitmap embedded = Bitmap.createBitmap(240, 240, Bitmap.Config.ARGB_8888);
        try {
            assertTrue(ArtworkRepository.shouldAdaptArtwork("com.kugou.android.lite", true));
            assertFalse(ArtworkRepository.shouldAdaptArtwork("com.luna.music.car", true));
            assertFalse(ArtworkRepository.shouldAdaptArtwork("cn.kuwo.kwmusiccar", true));
            assertFalse(ArtworkRepository.shouldAdaptArtwork("cn.toside.music.mobile", true));
            assertFalse(ArtworkRepository.shouldAdaptArtwork("com.kugou.android.lite", false));
            assertTrue(ArtworkRepository.shouldPreferCompleteCoverLookup(
                    "com.kugou.android.lite", true, true, "", embedded, "title"));
            assertFalse(ArtworkRepository.shouldPreferCompleteCoverLookup(
                    "com.kugou.android.lite", true, true, "https://player/full", embedded, "title"));
            assertFalse(ArtworkRepository.shouldPreferCompleteCoverLookup(
                    "com.luna.music.car", true, true, "", embedded, "title"));
        } finally { embedded.recycle(); }
    }
    @Test public void sodaKeepsItsOriginalArtworkWhenSmartAdaptationIsEnabled() {
        SettingsRepository settings = new SettingsRepository(context);
        settings.setRoundArtworkEnabled(true);
        Bitmap source = Bitmap.createBitmap(426, 426, Bitmap.Config.ARGB_8888);
        source.eraseColor(0xfff4a020);
        ArtworkRepository repository = new ArtworkRepository(context);
        Bitmap decoded = null;
        try {
            String result = repository.resolve("com.luna.music.car", "track", "", source,
                    null, "title", "artist", 1, () -> {});
            assertTrue(result.startsWith("content://"));
            Uri uri = Uri.parse(result);
            decoded = BitmapFactory.decodeFile(new File(ArtworkHelper.getArtworkDir(context),
                    uri.getLastPathSegment()).getAbsolutePath());
            assertNotNull(decoded);
            assertEquals(0xfff4a020, decoded.getPixel(0, 0));
        } finally {
            repository.close(); source.recycle();
            if (decoded != null) decoded.recycle();
        }
    }
    @Test public void completeRemoteCoverBeatsKugouEmbeddedThumbnail() {
        new SettingsRepository(context).setRoundArtworkEnabled(true);
        Bitmap completeAlbum = Bitmap.createBitmap(240, 240, Bitmap.Config.ARGB_8888);
        completeAlbum.eraseColor(0xff336699);
        ArtworkRepository kugou = new ArtworkRepository(context);
        ArtworkRepository kuwo = new ArtworkRepository(context);
        try {
            String kugouResult = kugou.resolve("com.kugou.android.lite", "track",
                    "https://player/complete", completeAlbum, null, "title", "artist", 1, () -> {});
            assertEquals("https://player/complete", kugouResult);
            String kuwoResult = kuwo.resolve("cn.kuwo.kwmusiccar", "track",
                    "https://player/full-size", completeAlbum, null, "title", "artist", 1, () -> {});
            assertEquals("https://player/full-size", kuwoResult);
        } finally {
            kugou.close(); kuwo.close(); completeAlbum.recycle();
        }
    }
    @Test public void nonBitmapNotificationArtworkCanBeRendered() {
        android.graphics.drawable.ColorDrawable drawable = new android.graphics.drawable.ColorDrawable(0xff123456);
        drawable.setBounds(0, 0, 24, 18);
        Bitmap bitmap = ArtworkHelper.drawableToBitmap(drawable);
        assertNotNull(bitmap); assertEquals(24, bitmap.getWidth()); assertEquals(18, bitmap.getHeight());
    }
    @Test public void embeddedArtworkIsPresentInTheFirstPublishedSnapshot() throws Exception {
        ServiceController<MediaListenerService> lifecycle = Robolectric.buildService(MediaListenerService.class).create();
        try {
            MediaListenerService service = lifecycle.get(); set(service, "connected", true);
            MediaController player = controller("cover song", false, 0);
            Bitmap cover = Bitmap.createBitmap(32, 24, Bitmap.Config.ARGB_8888);
            Shadows.shadowOf(player).setMetadata(new MediaMetadata.Builder(metadata("cover song", false))
                    .putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, cover).build());
            set(service, "currentController", player); publish(service, player);
            assertTrue(BridgeStateStore.getSnapshot().artworkUri.startsWith("content://"));
            assertEquals("播放器内嵌封面", BridgeStateStore.getArtworkState());
        } finally { lifecycle.destroy(); }
    }
    @Test public void legacyClientKeepsTheV3PlaybackObjectIdentity() {
        LegacyMusicClient client = new LegacyMusicClient(context, 500);
        try {
            PlayerSnapshot first = new PlayerSnapshot("test.player", "Player", "one", "artist", "album",
                    "content://test/one.png", 10000, 0, PlaybackStatus.PLAYING,
                    FavoriteState.UNKNOWN, false, null, System.currentTimeMillis());
            client.update(first, 0, false, false);
            com.ecarx.eas.sdk.mediacenter.MusicPlaybackInfo retained = client.getMusicPlaybackInfo();
            assertEquals(Uri.parse("content://test/one.png"), retained.getArtwork());

            PlayerSnapshot second = new PlayerSnapshot("test.player", "Player", "two", "artist", "album",
                    "content://test/two.png", 10000, 0, PlaybackStatus.PLAYING,
                    FavoriteState.UNKNOWN, false, null, System.currentTimeMillis());
            client.update(second, 0, false, false);
            assertSame(retained, client.getMusicPlaybackInfo());
            assertEquals("two", retained.getTitle());
            assertEquals(Uri.parse("content://test/two.png"), retained.getArtwork());
        } finally { client.destroy(); }
    }
    @Test public void legacyPlaybackDedupIgnoresProgressButTracksVisibleState() {
        PlayerSnapshot first = new PlayerSnapshot("test.player", "Player", "song", "artist", "album",
                "content://test/cover.png", 10000, 1000, PlaybackStatus.PLAYING,
                FavoriteState.UNKNOWN, false, null, 1);
        PlayerSnapshot progressed = new PlayerSnapshot("test.player", "Player", "song", "artist", "album",
                "content://test/cover.png", 10000, 5000, PlaybackStatus.PLAYING,
                FavoriteState.UNKNOWN, false, null, 2);
        PlayerSnapshot newArtwork = new PlayerSnapshot("test.player", "Player", "song", "artist", "album",
                "content://test/new-cover.png", 10000, 5000, PlaybackStatus.PLAYING,
                FavoriteState.UNKNOWN, false, null, 3);
        assertEquals(LegacyBackend.playbackStateKey(first, false, true),
                LegacyBackend.playbackStateKey(progressed, false, true));
        assertNotEquals(LegacyBackend.playbackStateKey(first, false, true),
                LegacyBackend.playbackStateKey(newArtwork, false, true));
    }
    private static void awaitArtwork(AtomicInteger loaded, int expected) throws Exception {
        long deadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(5);
        while (loaded.get() < expected && System.nanoTime() < deadline) {
            Shadows.shadowOf(Looper.getMainLooper()).idle();
            Thread.sleep(10);
        }
        assertEquals(expected, loaded.get());
    }
    @Test public void artworkProviderRejectsTraversal() throws Exception {
        ArtworkContentProvider provider = Robolectric.buildContentProvider(ArtworkContentProvider.class).create().get();
        for (String path : new String[]{"../escape.png", "nested/file.png"}) {
            try { provider.openFile(Uri.parse("content://" + ArtworkContentProvider.AUTHORITY + "/" + path), "r"); fail(); }
            catch (java.io.FileNotFoundException expected) {}
        }
    }
    @Test public void artworkUriAndProviderMatchTheKnownGood18Contract() throws Exception {
        Bitmap bitmap = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888);
        Uri saved = ArtworkHelper.saveArtwork(context, bitmap);
        assertNotNull(saved);
        assertEquals(ArtworkContentProvider.AUTHORITY, saved.getAuthority());
        assertFalse(saved.toString().contains("@"));
        ArtworkContentProvider provider = Robolectric.buildContentProvider(ArtworkContentProvider.class).create().get();
        try (android.os.ParcelFileDescriptor descriptor = provider.openFile(saved, "r")) {
            assertNotNull(descriptor);
        }
    }
    @Test public void labelCacheChangesWithVersionAndNeverSurvivesUninstall() {
        installPlayer("old label", 1); PlayerNameResolver names = new PlayerNameResolver(context);
        assertEquals("old label", names.resolve("test.player").label);
        installPlayer("new label", 2); assertEquals("new label", names.resolve("test.player").label);
        Shadows.shadowOf(context.getPackageManager()).removePackage("test.player");
        assertEquals("test.player", names.resolve("test.player").label);
    }
    private void installPlayer(String label, int version) {
        PackageInfo info = new PackageInfo(); info.packageName = "test.player"; info.versionCode = version;
        info.firstInstallTime = 1; info.lastUpdateTime = version;
        info.applicationInfo = new ApplicationInfo(); info.applicationInfo.packageName = info.packageName;
        info.applicationInfo.nonLocalizedLabel = label;
        Shadows.shadowOf(context.getPackageManager()).installPackage(info);
    }
    @Test public void embeddedLyricsIgnoreNetworkAndDiscardPostedCallbacksForPreviousTrack() {
        LyricsManager lyrics = new LyricsManager(context); AtomicInteger old = new AtomicInteger(), current = new AtomicInteger();
        lyrics.onTrackChanged("p", "a", "one", "artist", 10000, "[00:01]old", result -> old.incrementAndGet());
        lyrics.onTrackChanged("p", "b", "two", "artist", 10000, "[00:01]new", result -> current.incrementAndGet());
        Shadows.shadowOf(Looper.getMainLooper()).idle();
        assertEquals(0, old.get()); assertEquals(1, current.get());
        assertEquals("new", lyrics.getCurrentLine(0));
        lyrics.setBaseMs(0); assertNull(lyrics.getCurrentLine(0)); lyrics.setTuneMs(1000); assertEquals("new", lyrics.getCurrentLine(0));
        LyricsManager.clearCache(context); assertNull(lyrics.getCurrentLyrics()); lyrics.destroy();
    }
    @Test public void stoppedBridgeDoesNotStartOutputOnEverySnapshot() throws Exception {
        SettingsRepository settings = new SettingsRepository(context); settings.setBridgeEnabled(false);
        ServiceController<MediaListenerService> lifecycle = Robolectric.buildService(MediaListenerService.class).create();
        MediaListenerService service = lifecycle.get(); set(service, "connected", true);
        MediaController player = controller("song", false, PlaybackState.ACTION_SET_RATING);
        set(service, "currentController", player);
        Shadows.shadowOf((Application) context).clearStartedServices();
        for (int i = 0; i < 10; i++) publish(service, player);
        assertNull(Shadows.shadowOf((Application) context).getNextStartedService()); lifecycle.destroy();
    }
    @Test public void vehicleControlsReachAPlayerWithNoAdvertisedActions() throws Exception {
        ServiceController<MediaListenerService> lifecycle = Robolectric.buildService(MediaListenerService.class).create();
        LegacyMusicClient client = new LegacyMusicClient(context, 500);
        try {
            MediaListenerService service = lifecycle.get(); set(service, "connected", true);
            MediaController player = controller("song", false, 0); set(service, "currentController", player);
            publish(service, player);
            client.update(BridgeStateStore.getSnapshot(), 0, false, false);
            com.ecarx.eas.sdk.mediacenter.MusicClientWrapper vehicle = new com.ecarx.eas.sdk.mediacenter.MusicClientWrapper(client);
            org.robolectric.shadows.ShadowApplication app = Shadows.shadowOf((Application) context);
            app.clearStartedServices();
            assertTrue(vehicle.onNext());
            assertEquals(BridgeStateStore.getLastControl(), PlaybackState.ACTION_SKIP_TO_NEXT,
                    Shadows.shadowOf(player.getTransportControls()).getLastPerformedAction());
            assertTrue(vehicle.onPrevious());
            assertEquals(BridgeStateStore.getLastControl(), PlaybackState.ACTION_SKIP_TO_PREVIOUS,
                    Shadows.shadowOf(player.getTransportControls()).getLastPerformedAction());
            assertTrue(vehicle.onPlay());
            assertEquals(BridgeStateStore.getLastControl(), PlaybackState.ACTION_PLAY,
                    Shadows.shadowOf(player.getTransportControls()).getLastPerformedAction());
            assertTrue(vehicle.onPause());
            assertEquals(BridgeStateStore.getLastControl(), PlaybackState.ACTION_PAUSE,
                    Shadows.shadowOf(player.getTransportControls()).getLastPerformedAction());
            BridgeStateStore.setGeneration(501);
            assertFalse(vehicle.onNext()); assertNull(app.getNextStartedService());
        } finally { client.destroy(); lifecycle.destroy(); }
    }
    @Test public void compatibilityControlsStillRejectStaleAndUnauthenticatedRequests() throws Exception {
        ServiceController<MediaListenerService> lifecycle = Robolectric.buildService(MediaListenerService.class).create();
        try {
            MediaListenerService service = lifecycle.get(); set(service, "connected", true);
            MediaController player = controller("song", false, 0); set(service, "currentController", player);
            Intent valid = BridgeEvents.listenerIntent(context, BridgeEvents.ACTION_MEDIA_COMMAND)
                    .putExtra("command", "NEXT").putExtra("bridgeGeneration", 500L)
                    .putExtra("expectedPackage", "test.player").putExtra("expectedSession", player.getSessionToken().toString());
            service.onStartCommand(valid, 0, 1);
            long sent = Shadows.shadowOf(player.getTransportControls()).getLastPerformedAction();
            assertEquals(PlaybackState.ACTION_SKIP_TO_NEXT, sent);
            Intent stale = new Intent(valid).putExtra("command", "PLAY").putExtra("bridgeGeneration", 499L);
            service.onStartCommand(stale, 0, 1);
            service.onStartCommand(new Intent(valid).putExtra("command", "PLAY").putExtra("expectedSession", "old-session"), 0, 1);
            service.onStartCommand(new Intent(valid).putExtra("command", "PLAY").putExtra("expectedPackage", "other.player"), 0, 1);
            service.onStartCommand(new Intent(BridgeEvents.ACTION_MEDIA_COMMAND).putExtra("command", "PLAY").putExtra("bridgeGeneration", 500L), 0, 1);
            Intent missingGeneration = new Intent(valid).putExtra("command", "PLAY"); missingGeneration.removeExtra("bridgeGeneration");
            service.onStartCommand(missingGeneration, 0, 1);
            assertEquals(sent, Shadows.shadowOf(player.getTransportControls()).getLastPerformedAction());
            set(service, "connected", false);
            service.onStartCommand(new Intent(valid).putExtra("command", "PLAY"), 0, 1);
            assertEquals(sent, Shadows.shadowOf(player.getTransportControls()).getLastPerformedAction());
        } finally { lifecycle.destroy(); }
    }
    @Test public void iouProbeIntentRequiresInternalAuthentication() {
        Intent authorized = BridgeEvents.listenerIntent(context, BridgeEvents.ACTION_PROBE_IOU);
        assertTrue(BridgeEvents.isInternalServiceAction(BridgeEvents.ACTION_PROBE_IOU));
        assertTrue(BridgeEvents.isTrustedServiceIntent(context, authorized));
        assertFalse(BridgeEvents.isTrustedServiceIntent(context, new Intent(BridgeEvents.ACTION_PROBE_IOU)));
    }
    @Test public void favoriteWaitsForSourceFeedbackAndReportsNoConfirmation() throws Exception {
        ServiceController<MediaListenerService> lifecycle = Robolectric.buildService(MediaListenerService.class).create();
        MediaListenerService service = lifecycle.get(); set(service, "connected", true);
        MediaController player = controller("song", false, PlaybackState.ACTION_SET_RATING); set(service, "currentController", player);
        publish(service, player); PlayerSnapshot initial = BridgeStateStore.getSnapshot(); assertTrue(initial.favoriteWriteSupported);
        favorite(service, initial, true);
        PlayerSnapshot pending = BridgeStateStore.getSnapshot(); assertTrue(pending.favoritePending);
        assertEquals(FavoriteState.NOT_FAVORITED, pending.favoriteState); assertFalse(pending.favoriteWriteSupported);
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(3100)); publish(service, player);
        assertTrue(BridgeStateStore.getLastControl().contains("收藏结果无法确认"));
        lifecycle.destroy();
    }
    @Test public void favoriteCanConfirmAfterSameSongMetadataIdentityChanges() throws Exception {
        ServiceController<MediaListenerService> lifecycle = Robolectric.buildService(MediaListenerService.class).create();
        try {
            MediaListenerService service = lifecycle.get(); set(service, "connected", true);
            MediaController player = controller("song", false, PlaybackState.ACTION_SET_RATING);
            set(service, "currentController", player); publish(service, player);
            favorite(service, BridgeStateStore.getSnapshot(), true);
            assertTrue(BridgeStateStore.getSnapshot().favoritePending);
            Shadows.shadowOf(player).setMetadata(new MediaMetadata.Builder()
                    .putString(MediaMetadata.METADATA_KEY_MEDIA_ID, "refreshed-id")
                    .putString(MediaMetadata.METADATA_KEY_TITLE, "song")
                    .putString(MediaMetadata.METADATA_KEY_ARTIST, "artist")
                    .putString(MediaMetadata.METADATA_KEY_ALBUM, "refreshed-album")
                    .putLong(MediaMetadata.METADATA_KEY_DURATION, 10020)
                    .putRating(MediaMetadata.METADATA_KEY_USER_RATING, Rating.newHeartRating(true)).build());
            set(service, "inputRevision", (Long) get(service, "inputRevision") + 1L);
            publish(service, player);
            assertFalse(BridgeStateStore.getSnapshot().favoritePending);
            assertEquals(FavoriteState.FAVORITED, BridgeStateStore.getSnapshot().favoriteState);
            assertFalse(BridgeStateStore.getLastControl().contains("歌曲已切换"));
        } finally { lifecycle.destroy(); }
    }
    @Test public void unadvertisedRatingCanBeTriedButIgnoredPlayersCannotBeRated() throws Exception {
        ServiceController<MediaListenerService> lifecycle = Robolectric.buildService(MediaListenerService.class).create();
        MediaListenerService service = lifecycle.get(); set(service, "connected", true);
        MediaController player = controller("song", false, 0); set(service, "currentController", player);
        publish(service, player);
        PlayerSnapshot first = BridgeStateStore.getSnapshot();
        assertFalse(first.favoriteWriteSupported);
        favorite(service, first, true);
        assertTrue(BridgeStateStore.getSnapshot().favoritePending);
        Shadows.shadowOf(player).setMetadata(metadata("second", false));
        publish(service, player);
        new SettingsRepository(context).setFavoriteIgnored("test.player", true);
        favorite(service, BridgeStateStore.getSnapshot(), true);
        assertFalse(BridgeStateStore.getSnapshot().favoritePending);
        new SettingsRepository(context).setFavoriteIgnored("test.player", false);
        Shadows.shadowOf(player).setRatingType(Rating.RATING_NONE);
        Shadows.shadowOf(player).setMetadata(new MediaMetadata.Builder()
                .putString(MediaMetadata.METADATA_KEY_TITLE, "unknown-rating").build());
        publish(service, player);
        assertEquals(FavoriteState.UNKNOWN, BridgeStateStore.getSnapshot().favoriteState);
        favorite(service, BridgeStateStore.getSnapshot(), true);
        assertTrue(BridgeStateStore.getSnapshot().favoritePending);
        lifecycle.destroy();
    }
    @Test public void vehicleFavoriteControlRemainsVisibleWhileStatusIsUnknownOrPending() {
        LegacyMusicClient client = new LegacyMusicClient(context, 500);
        try {
            PlayerSnapshot unknown = new PlayerSnapshot("test.player", "Player", "Track", "Artist", "Album",
                    100L, 0L, PlaybackStatus.PAUSED, FavoriteState.UNKNOWN, false, null, 0L);
            client.update(unknown, 0, false, true);
            assertTrue(client.getInfo().isSupportCollect());
            PlayerSnapshot pending = new PlayerSnapshot(-1, "test.player", "Player", "Track", "Artist", "Album", "",
                    100L, 0L, PlaybackStatus.PAUSED, FavoriteState.NOT_FAVORITED, false, null, 0L,
                    "session", "track", 1f, "", "", true, "等待播放器确认收藏状态");
            client.update(pending, 0, false, true);
            assertTrue(client.getInfo().isSupportCollect());
            client.update(pending, 0, false, false);
            assertFalse(client.getInfo().isSupportCollect());
        } finally { client.destroy(); }
    }
    @Test public void favoriteOldTrackRequestAndPendingTargetDoNotAffectNextSong() throws Exception {
        ServiceController<MediaListenerService> lifecycle = Robolectric.buildService(MediaListenerService.class).create();
        MediaListenerService service = lifecycle.get(); set(service, "connected", true);
        MediaController player = controller("first", false, PlaybackState.ACTION_SET_RATING); set(service, "currentController", player);
        publish(service, player); PlayerSnapshot first = BridgeStateStore.getSnapshot(); favorite(service, first, true);
        Shadows.shadowOf(player).setMetadata(metadata("second", false));
        publish(service, player); assertFalse(BridgeStateStore.getSnapshot().favoritePending);
        assertTrue(BridgeStateStore.getLastControl().contains("歌曲已切换"));
        favorite(service, first, true); assertFalse(BridgeStateStore.getSnapshot().favoritePending);
        assertEquals(FavoriteState.NOT_FAVORITED, BridgeStateStore.getSnapshot().favoriteState); lifecycle.destroy();
    }
    @Test public void repeatingDesiredFavoriteAndUnknownCapabilitiesDoNotWrite() throws Exception {
        ServiceController<MediaListenerService> lifecycle = Robolectric.buildService(MediaListenerService.class).create();
        MediaListenerService service = lifecycle.get(); set(service, "connected", true);
        MediaController player = controller("song", true, PlaybackState.ACTION_SET_RATING); set(service, "currentController", player);
        publish(service, player); favorite(service, BridgeStateStore.getSnapshot(), true);
        assertFalse(BridgeStateStore.getSnapshot().favoritePending);
        Shadows.shadowOf(player).setRatingType(Rating.RATING_NONE);
        Shadows.shadowOf(player).setMetadata(new MediaMetadata.Builder().putString(MediaMetadata.METADATA_KEY_TITLE, "unknown").build());
        Shadows.shadowOf(player).setPlaybackState(new PlaybackState.Builder().setState(PlaybackState.STATE_PAUSED, 0, 1).build());
        publish(service, player); assertFalse(BridgeStateStore.getSnapshot().favoriteWriteSupported); lifecycle.destroy();
    }
    @Test public void listenerDisconnectInvalidatesSourceController() throws Exception {
        ServiceController<MediaListenerService> lifecycle = Robolectric.buildService(MediaListenerService.class).create();
        MediaListenerService service = lifecycle.get(); set(service, "connected", true);
        MediaController player = controller("song", false, PlaybackState.ACTION_SET_RATING); set(service, "currentController", player);
        publish(service, player); grant(false); service.onListenerDisconnected();
        Shadows.shadowOf(Looper.getMainLooper()).idle();
        assertNull(BridgeStateStore.getSnapshot()); assertNull(get(service, "currentController")); lifecycle.destroy();
    }
    @Test public void connectedCallbackWinsOverLaggingSecureSettingsState() throws Exception {
        grant(false);
        ServiceController<MediaListenerService> lifecycle = Robolectric.buildService(MediaListenerService.class).create();
        MediaListenerService service = lifecycle.get(); service.onListenerConnected();
        MediaController player = controller("song", false, 0);
        set(service, "currentController", player);
        MediaListenerService.observeAccess(NotificationAccess.State.DENIED);
        Shadows.shadowOf(Looper.getMainLooper()).idle();
        assertEquals(Boolean.TRUE, get(service, "connected"));
        assertSame(player, get(service, "currentController"));
        assertFalse(BridgeStateStore.getInputState().contains("已关闭")); lifecycle.destroy();
    }
    @Test public void refreshPreservesVehicleRomListenerWakeup() {
        Shadows.shadowOf((Application) context).clearStartedServices();
        MediaListenerService.requestRefresh(context);
        Intent started = Shadows.shadowOf((Application) context).getNextStartedService();
        assertNotNull(started);
        assertEquals(BridgeEvents.ACTION_REFRESH, started.getAction());
        assertEquals(new ComponentName(context, MediaListenerService.class), started.getComponent());
    }
    @Test public void controlsDoNotStartNotificationListenerBeforeSystemBind() {
        Shadows.shadowOf((Application) context).clearStartedServices();
        MediaListenerService.requestCommand(context, "PLAY", 500L, "test.player", "session");
        MediaListenerService.requestSeek(context, 1000L, 500L, "test.player", "session");
        MediaListenerService.requestFavorite(context, true, 500L, "test.player", "session", "track");
        MediaListenerService.requestLoop(context, 1, 500L, "test.player", "session");
        Shadows.shadowOf(Looper.getMainLooper()).idle();
        assertNull(Shadows.shadowOf((Application) context).getNextStartedService());
    }
    @Test public void bootRestoreAndStandardModeAreAlwaysEnabledAfterUpgrade() {
        SettingsRepository settings = new SettingsRepository(context);
        settings.setBootRestoreEnabled(false); settings.setBackendMode(BackendMode.F25.value());
        assertTrue(settings.isBootRestoreEnabled());
        assertEquals(BackendMode.LEGACY.value(), settings.getBackendMode());
        assertEquals(BackendMode.LEGACY.value(), settings.getLastSuccessfulMode());
        assertEquals("", settings.getPendingMode());
    }
    @Test public void fixedPlayerSelectionRemainsStableAcrossOneHundredSessionOrderChanges() throws Exception {
        new SettingsRepository(context).setSelectedPackage("test.player");
        ServiceController<MediaListenerService> lifecycle = Robolectric.buildService(MediaListenerService.class).create();
        MediaListenerService service = lifecycle.get(); MediaController first = controller("first", false, 0), second = controller("second", false, 0);
        set(service, "currentController", second);
        Method select = MediaListenerService.class.getDeclaredMethod("selectController", java.util.List.class); select.setAccessible(true);
        for (int i = 0; i < 100; i++) assertSame(second, select.invoke(service, i % 2 == 0 ? java.util.Arrays.asList(first, second) : java.util.Arrays.asList(second, first)));
        lifecycle.destroy();
    }
    @Test public void diagnosticsExportReadsPreviousProcessFilesAndExceptions() throws Exception {
        java.io.File directory = new java.io.File(context.getFilesDir(), "diagnostics"); directory.mkdirs();
        java.nio.file.Files.write(new java.io.File(directory, "mediabridge-1.log").toPath(),
                "previous-process sentinel\nIOException: persisted\ntitle=private song\n".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        String output = DiagnosticsLog.exportText(context, false);
        assertTrue(output.contains("previous-process sentinel")); assertTrue(output.contains("IOException: persisted")); assertFalse(output.contains("private song"));
    }
    @Test public void diagnosticsCanExportToPublicDownloadsWithoutPickerOnApi28() throws Exception {
        assertNull(DiagnosticsLog.exportToDownloads(null, false));
        String path = DiagnosticsLog.exportToDownloads(context, false);
        assertNotNull(path);
        assertTrue(path.replace('\\', '/').contains("/Download/MediaBridge/mediabridge-probe-"));
        java.io.File exported = new java.io.File(path);
        assertTrue(exported.isFile());
        String contents = new String(java.nio.file.Files.readAllBytes(exported.toPath()), java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(contents.contains("MediaBridge diagnostics"));
        assertTrue(contents.contains("Detailed track fields: false"));
        assertTrue(exported.delete());
    }
    private MediaController controller(String title, boolean favorite, long actions) {
        MediaSession session = new MediaSession(context, "regression");
        MediaController player = new MediaController(context, session.getSessionToken());
        ShadowMediaController shadow = Shadows.shadowOf(player); shadow.setPackageName("test.player");
        shadow.setRatingType(Rating.RATING_HEART); shadow.setMetadata(metadata(title, favorite));
        shadow.setPlaybackState(new PlaybackState.Builder().setState(PlaybackState.STATE_PAUSED, 0, 1).setActions(actions).build());
        return player;
    }
    private MediaMetadata metadata(String title, boolean favorite) {
        return new MediaMetadata.Builder().putString(MediaMetadata.METADATA_KEY_MEDIA_ID, title)
                .putString(MediaMetadata.METADATA_KEY_TITLE, title).putString(MediaMetadata.METADATA_KEY_ARTIST, "artist")
                .putLong(MediaMetadata.METADATA_KEY_DURATION, 10000).putRating(MediaMetadata.METADATA_KEY_USER_RATING, Rating.newHeartRating(favorite)).build();
    }
    private void favorite(MediaListenerService service, PlayerSnapshot source, boolean desired) {
        Intent command = BridgeEvents.listenerIntent(context, BridgeEvents.ACTION_MEDIA_COMMAND)
                .putExtra("command", "FAVORITE").putExtra("favorite", desired).putExtra("bridgeGeneration", 500L)
                .putExtra("expectedPackage", source.packageName).putExtra("expectedSession", source.sessionId).putExtra("expectedTrack", source.trackId);
        service.onStartCommand(command, 0, 1);
    }
    private static void publish(MediaListenerService service, MediaController player) throws Exception {
        Method method = MediaListenerService.class.getDeclaredMethod("publish", MediaController.class); method.setAccessible(true); method.invoke(service, player);
    }
    private static Object get(Object object, String name) throws Exception { Field field = object.getClass().getDeclaredField(name); field.setAccessible(true); return field.get(object); }
    private static void set(Object object, String name, Object value) throws Exception { Field field = object.getClass().getDeclaredField(name); field.setAccessible(true); field.set(object, value); }
}
