package com.geely.auto.music;

import android.app.Application;
import android.content.Context;
import android.os.*;
import com.ecarx.eas.framework.sdk.IEASFrameworkService;
import com.ecarx.eas.sdk.mediacenter.*;
import com.ecarx.sdk.openapi.msg.*;
import ecarx.xsf.mediacenter.*;
import org.json.JSONObject;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, application = Application.class)
public class ProtocolRegressionTest {
    private Context context;
    private JSONObject fixture;
    @Before public void setup() throws Exception {
        context = RuntimeEnvironment.getApplication();
        try (java.io.InputStream input = getClass().getClassLoader().getResourceAsStream("ecarx-mediacenter.json")) {
            assertNotNull(input); fixture = new JSONObject(new String(input.readAllBytes(), StandardCharsets.UTF_8));
        }
    }
    @Test public void easMessageAndReplyPreserveGoldenParcelFieldOrder() {
        Parcel data = Parcel.obtain();
        try {
            new EASFrameworkMessage("mediacenter", "MediaCenterAPI", "updatePlayState", new byte[]{1,2}, new byte[]{3}).writeToParcel(data, 0);
            data.setDataPosition(0);
            assertEquals("mediacenter", data.readString()); assertEquals("MediaCenterAPI", data.readString());
            assertEquals("updatePlayState", data.readString()); assertArrayEquals(new byte[]{1,2}, data.createByteArray());
            assertArrayEquals(new byte[]{3}, data.createByteArray()); assertEquals(0, data.dataAvail());
        } finally { data.recycle(); }
        EASFrameworkRetMessage reply = response();
        data = Parcel.obtain();
        try {
            reply.writeToParcel(data, 0); data.setDataPosition(0);
            EASFrameworkRetMessage restored = EASFrameworkRetMessage.CREATOR.createFromParcel(data);
            assertEquals(200, restored.mCode); assertEquals(200, restored.mRetMsg.mCode);
            assertArrayEquals("ok".getBytes(StandardCharsets.UTF_8), restored.mRetMsg.mData);
        } finally { data.recycle(); }
    }
    @Test public void allUsedMediaCenterTransactionsMatchFixtureAndArgumentOrder() throws Exception {
        GoldenMediaBinder binder = new GoldenMediaBinder(fixture.getJSONObject("mediaCenter").getJSONObject("transactions"));
        IMediaCenterSvc remote = IMediaCenterSvc.Stub.asInterface(binder);
        IMusicClient callback = new MusicClientWrapper(new MusicClient() {});
        IMediaCenterClientToken token = remote.registerInMusic("test.package", callback);
        assertNotNull(token); remote.registerMusic(callback);
        remote.updateMediaSourceTypeList(token, new int[]{6}); remote.updateCurrentSourceType(token, 6);
        remote.declareMediaCenterCapability(token, new int[]{0,2,3});
        assertTrue(remote.declareSupportCollectTypes(token, new int[]{0,3,4}));
        remote.updateCurrentProgress(token, 12345L); remote.updateCurrentLyric(token, "line");
        assertTrue(remote.updateMusicPlaybackState(token, MediaCenterAPI.createPlaybackInfoBinder(new MusicClient() {})));
        assertTrue(remote.requestPlay(token)); remote.unregister(token);
        assertEquals(new HashSet<>(Arrays.asList(1,4,5,6,7,8,10,14,15,19,30)), binder.seen);
    }
    @Test public void probeCallbacksLogArgumentsWithoutChangingResults() {
        MusicClient client = new MusicClient() {
            @Override public List getPlaylist(int type) { return Arrays.asList("one", "two"); }
            @Override public boolean onPlayMediaList(int type, int index) { return true; }
            @Override public boolean onMediaSelectedPlay(int type, String id) { return true; }
            @Override public boolean selectListMediaPlay(int type, int index, String id) { return true; }
        };
        MusicClientWrapper wrapper = new MusicClientWrapper(client);
        assertNull(wrapper.getMultiMediaList(new int[]{1, 2}));
        assertEquals(2, wrapper.getPlaylist(9).size());
        assertTrue(wrapper.onPlayMediaList(3, 4));
        assertFalse(wrapper.onPlayRecommend(null));
        wrapper.onSearchMusic("测试歌曲", "测试歌手", 7, true, false, null);
        assertTrue(wrapper.onMediaSelectedPlay(5, "media-id"));
        assertTrue(wrapper.selectListMediaPlay(6, 8, "list-id"));
        String log = DiagnosticsLog.dump();
        assertTrue(log.contains("ECARX_PROBE event=getMultiMediaList types=[1, 2] result=null"));
        assertTrue(log.contains("ECARX_PROBE event=getPlaylist arg1=9 resultSize=2"));
        assertTrue(log.contains("ECARX_PROBE event=onPlayMediaList arg1=3 arg2=4 result=true"));
        assertTrue(log.contains("ECARX_PROBE event=onPlayRecommend present=false result=false"));
        assertTrue(log.contains("ECARX_PROBE event=onSearchMusic arg1=\"测试歌曲\"(len=4) arg2=\"测试歌手\"(len=4)"));
        assertTrue(log.contains("ECARX_PROBE event=onMediaSelectedPlay arg1=5 arg2=\"media-id\"(len=8) result=true"));
        assertTrue(log.contains("ECARX_PROBE event=selectListMediaPlay arg1=6 arg2=8 arg3=\"list-id\"(len=7) result=true"));
    }
    @Test public void f25DirectoriesUseDistinctDescriptorsAndTransactionNumbers() throws Exception {
        for (boolean eas : new boolean[]{false, true}) {
            final IBinder media = new Binder();
            Binder directory = new Binder() {
                @Override protected boolean onTransact(int code, Parcel data, Parcel reply, int flags) {
                    data.enforceInterface(eas ? ServiceDirectory.EAS_DESCRIPTOR : ServiceDirectory.POOL_DESCRIPTOR);
                    if (code == (eas ? 7 : 1) || eas && code == 9) {
                        assertEquals(0, data.dataAvail()); reply.writeNoException(); reply.writeStringList(Collections.singletonList("mediacenter")); return true;
                    }
                    assertEquals(eas ? 8 : 2, code);
                    assertEquals(android.os.Process.myPid(), data.readInt()); assertEquals(android.os.Process.myUid(), data.readInt());
                    assertEquals("test.package", data.readString()); assertEquals("mediacenter", data.readString()); assertEquals(0, data.dataAvail());
                    reply.writeNoException(); reply.writeStrongBinder(media); return true;
                }
            };
            assertEquals(Collections.singletonList("mediacenter"), ServiceDirectory.available(directory, eas, false));
            if (eas) assertEquals(Collections.singletonList("mediacenter"), ServiceDirectory.available(directory, true, true));
            assertEquals(media, ServiceDirectory.get(directory, eas, "test.package"));
        }
    }
    @Test public void easProxyUsesOneFourSixAndPropagatesUnsupportedTransactions() throws Exception {
        List<Integer> seen = new ArrayList<>(); IBinder attached = new Binder();
        Binder binder = new Binder() {
            @Override protected boolean onTransact(int code, Parcel data, Parcel reply, int flags) {
                seen.add(code); data.enforceInterface(IEASFrameworkService.DESCRIPTOR);
                if (code == 1) assertArrayEquals(new String[]{"mediacenter"}, data.createStringArray());
                else {
                    assertEquals(1, data.readInt()); EASFrameworkMessage message = EASFrameworkMessage.CREATOR.createFromParcel(data);
                    assertEquals("mediacenter", message.mServiceName); assertEquals("MediaCenterAPI", message.mMoudleName);
                    if (code == 6) assertEquals(attached, data.readStrongBinder());
                }
                assertEquals(0, data.dataAvail()); reply.writeNoException();
                if (code != 1) { reply.writeInt(1); response().writeToParcel(reply, 0); }
                return true;
            }
        };
        IEASFrameworkService api = IEASFrameworkService.Stub.asInterface(binder);
        EASFrameworkMessage message = new EASFrameworkMessage("mediacenter", "MediaCenterAPI", "registerEx", new byte[0], new byte[0]);
        api.init(new String[]{"mediacenter"}); assertEquals(200, api.call(message).mCode); api.asyncBinderCall(message, attached);
        assertEquals(Arrays.asList(1,4,6), seen);
        try { IEASFrameworkService.Stub.asInterface(new Binder()).init(new String[]{"mediacenter"}); fail(); }
        catch (UnsupportedOperationException expected) {}
    }
    @Test public void rejectedPlaybackTokenTriggersRecoverableFailureWithoutBinderDeath() throws Exception {
        AtomicReference<Throwable> failure = new AtomicReference<>();
        MediaCenterAPI api = MediaCenterAPI.create(context, new BridgeIo.Lane(failure::set), false, failure::set);
        IMediaCenterSvc rejected = new IMediaCenterSvc.Default() {
            @Override public boolean updateMusicPlaybackState(IMediaCenterClientToken token, IMusicPlaybackInfo info) { return false; }
        };
        set(api, "service", rejected); set(api, "musicClient", new MusicClient() {});
        assertFalse(api.updateMusicPlaybackState(new IMediaCenterClientToken.Stub(){}, new MusicPlaybackInfo()));
        assertEquals(BackendConnectionState.RETRYABLE_FAILURE, ((BridgeFailure) failure.get()).state); api.close();
    }
    @Test public void playbackInfoBinderReadsLatestClientStateAfterRegistration() throws Exception {
        AtomicReference<MusicPlaybackInfo> current = new AtomicReference<>(new MusicPlaybackInfo());
        MusicClient client = new MusicClient() {
            @Override public MusicPlaybackInfo getMusicPlaybackInfo() { return current.get(); }
        };
        IMusicPlaybackInfo binder = MediaCenterAPI.createPlaybackInfoBinder(client);
        current.get().setTitle("first");
        assertEquals("first", binder.getTitle());
        MusicPlaybackInfo next = new MusicPlaybackInfo(); next.setTitle("second");
        next.setArtwork(android.net.Uri.parse("content://test/artwork.png"));
        next.setLyricContent("[00:01]late lyric");
        next.setCurrentLyricSentence("late lyric");
        current.set(next);
        assertEquals("second", binder.getTitle());
        assertEquals(next.getArtwork(), binder.getArtwork());
        assertEquals("[00:01]late lyric", binder.getLyricContent());
        assertEquals("late lyric", binder.getCurrentLyricSentence());
        current.set(null);
        assertEquals("", binder.getTitle()); assertNull(binder.getArtwork()); assertNull(binder.getLyricContent());
    }
    @Test public void accessDeniedIsTerminalAndRegisterInMusicFallsBackOnlyWhenUnavailable() throws Exception {
        AtomicReference<Throwable> failure = new AtomicReference<>(); AtomicReference<String> route = new AtomicReference<>();
        MediaCenterAPI api = MediaCenterAPI.create(context, new BridgeIo.Lane(failure::set), false, failure::set);
        set(api, "service", new IMediaCenterSvc.Default() {
            @Override public IMediaCenterClientToken registerInMusic(String pkg, IMusicClient client) { throw new UnsupportedOperationException(); }
            @Override public IMediaCenterClientToken registerMusic(IMusicClient client) { route.set("fallback"); return new IMediaCenterClientToken.Stub(){}; }
        });
        assertNotNull(api.registerMusic("test.package", new MusicClient(){})); assertEquals("fallback", route.get()); assertNull(failure.get());
        set(api, "service", new IMediaCenterSvc.Default() {
            @Override public IMediaCenterClientToken registerInMusic(String pkg, IMusicClient client) { throw new SecurityException("denied"); }
            @Override public IMediaCenterClientToken registerMusic(IMusicClient client) { fail("Must not bypass permission denial"); return null; }
        });
        assertNull(api.registerMusic("test.package", new MusicClient(){}));
        assertEquals(BackendConnectionState.ACCESS_DENIED, ((BridgeFailure) failure.get()).state); api.close();
    }
    @Test public void registrationExtensionsAndPlayStatePreserveOriginalEasPayloads() throws Exception {
        List<String> methods = new ArrayList<>(); AtomicReference<Throwable> failure = new AtomicReference<>();
        MediaCenterAPI api = MediaCenterAPI.create(context, new BridgeIo.Lane(failure::set), false, failure::set);
        set(api, "service", new IMediaCenterSvc.Default() {
            @Override public IMediaCenterClientToken registerInMusic(String pkg, IMusicClient client) { return new IMediaCenterClientToken.Stub(){}; }
        });
        set(api, "easService", new IEASFrameworkService.Stub() {
            @Override public void init(String[] names) {}
            @Override public EASFrameworkRetMessage call(EASFrameworkMessage message) {
                methods.add(message.mMethod);
                try {
                    assertEquals("mediacenter", message.mServiceName); assertEquals("MediaCenterAPI", message.mMoudleName);
                    JSONObject payload = new JSONObject(new String(message.mMethodParam, StandardCharsets.UTF_8));
                    assertEquals("test.package", payload.getString("packageName")); assertEquals(0, payload.getInt("displayId")); assertEquals(1, payload.getInt("playState"));
                } catch (Exception error) { throw new AssertionError(error); }
                return response();
            }
            @Override public EASFrameworkRetMessage asyncBinderCall(EASFrameworkMessage message, IBinder binder) {
                methods.add(message.mMethod); assertNotNull(binder);
                String payload = new String(message.mMethodParam, StandardCharsets.UTF_8);
                if (message.mMethod.equals("registerEx")) assertEquals("test.package", payload);
                else assertTrue(payload.contains("\"displayId\":0"));
                return response();
            }
        });
        assertNotNull(api.registerMusic("test.package", new MusicClient(){})); api.updatePlayState("test.package", 1, 0);
        assertEquals(Arrays.asList("registerEx", "registerClientWithRequest", "updatePlayState"), methods); assertNull(failure.get()); api.close();
    }
    private static EASFrameworkRetMessage response() {
        EASFrameworkRetMessage reply = new EASFrameworkRetMessage(); reply.mCode = 200; reply.mMsg = "ok";
        reply.mRetMsg = new SupportServiceRetMessage(200, "ok"); reply.mRetMsg.mData = "ok".getBytes(StandardCharsets.UTF_8); return reply;
    }
    private static void set(Object object, String name, Object value) throws Exception { Field field = object.getClass().getDeclaredField(name); field.setAccessible(true); field.set(object, value); }
    private static final class GoldenMediaBinder extends Binder {
        final JSONObject transactions; final Set<Integer> seen = new HashSet<>(); final IBinder token = new Binder();
        GoldenMediaBinder(JSONObject transactions) { this.transactions = transactions; }
        @Override protected boolean onTransact(int code, Parcel data, Parcel reply, int flags) {
            data.enforceInterface(IMediaCenterSvc.DESCRIPTOR); seen.add(code);
            try {
                if (code == transactions.getInt("registerInMusic")) { assertEquals("test.package", data.readString()); assertNotNull(data.readStrongBinder()); }
                else if (code == transactions.getInt("registerMusic")) assertNotNull(data.readStrongBinder());
                else {
                    assertEquals(token, data.readStrongBinder());
                    if (code == transactions.getInt("updateMusicPlaybackState")) assertNotNull(data.readStrongBinder());
                    if (code == transactions.getInt("updateMediaSourceTypeList")) assertArrayEquals(new int[]{6}, data.createIntArray());
                    if (code == transactions.getInt("updateCurrentSourceType")) assertEquals(6, data.readInt());
                    if (code == transactions.getInt("updateCurrentProgress")) assertEquals(12345L, data.readLong());
                    if (code == transactions.getInt("updateCurrentLyric")) assertEquals("line", data.readString());
                    if (code == transactions.getInt("declareSupportCollectTypes")) assertArrayEquals(new int[]{0,3,4}, data.createIntArray());
                    if (code == transactions.getInt("declareMediaCenterCapability")) assertArrayEquals(new int[]{0,2,3}, data.createIntArray());
                }
                assertEquals(0, data.dataAvail()); reply.writeNoException();
                if (code == 1 || code == 19) reply.writeStrongBinder(token);
                else if (code == 4 || code == 5 || code == 6 || code == 15) reply.writeInt(1);
                return true;
            } catch (Exception error) { throw new AssertionError(error); }
        }
    }
}
