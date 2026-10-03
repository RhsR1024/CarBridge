package cn.manstep.phonemirrorBox.bridge;

import android.content.*;
import android.media.MediaMetadata;
import android.media.session.*;
import android.os.*;
import android.view.View;
import android.widget.LinearLayout;
import com.ecarx.eas.sdk.ECarXApiClient;
import com.ecarx.eas.sdk.mediacenter.*;
import java.lang.reflect.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;
import static org.junit.Assert.*;
import static io.github.rhsr1024.interop.BridgeProtocol.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=30, manifest=Config.NONE, shadows=UsbBridgeTest.RecordingSession.class)
@LooperMode(LooperMode.Mode.PAUSED)
public class UsbBridgeTest {
    @org.robolectric.annotation.Implements(MediaSession.class)
    public static class RecordingSession extends org.robolectric.shadows.ShadowMediaSession {
        MediaMetadata metadata; PlaybackState state;
        @org.robolectric.annotation.Implementation protected void setMetadata(MediaMetadata v) { metadata=v; }
        @org.robolectric.annotation.Implementation protected void setPlaybackState(PlaybackState v) { state=v; }
    }
    private Context context;
    private final List<Integer> keys = new ArrayList<>();
    private BridgeRoute route;
    private Object runtime;
    private MediaSession session;
    @Before public void setup() throws Exception {
        context=RuntimeEnvironment.getApplication();
        context.getSharedPreferences("usb_media_route_v1",0).edit().clear().commit();
        cn.manstep.phonemirrorBox.BoxInterface.f.P=true;
        ((java.util.concurrent.atomic.AtomicBoolean)staticField(F25Direct.class,"LEASE")).set(false);
    }
    @After public void teardown() {
        UsbMediaBridge.close(); idle();
        if(route!=null) route.close();
        cn.manstep.phonemirrorBox.BoxInterface.f.P=false;
    }
    private static Object staticField(Class<?> cls,String name) throws Exception {
        Field f=cls.getDeclaredField(name); f.setAccessible(true); return f.get(null);
    }
    private static Object field(Object obj,String name) throws Exception {
        Field f=obj.getClass().getDeclaredField(name); f.setAccessible(true); return f.get(obj);
    }
    private static void set(Object obj,String name,Object value) throws Exception {
        Field f=obj.getClass().getDeclaredField(name); f.setAccessible(true); f.set(obj,value);
    }
    private static Object call(Object obj,String name,Class<?>[] types,Object... values) throws Exception {
        Method m=obj.getClass().getDeclaredMethod(name,types); m.setAccessible(true); return m.invoke(obj,values);
    }
    private static void idle() { Shadows.shadowOf(Looper.getMainLooper()).idle(); }
    private void runtime() throws Exception {
        Class<?> cls=Class.forName(UsbMediaBridge.class.getName()+"$Runtime");
        Constructor<?> constructor=cls.getDeclaredConstructor(Context.class,com.zqsdk.callBack.IInputCallback.class);
        constructor.setAccessible(true);runtime=constructor.newInstance(context,(com.zqsdk.callBack.IInputCallback)keys::add);
        Field live=UsbMediaBridge.class.getDeclaredField("live");live.setAccessible(true);live.set(null,runtime);
        route=(BridgeRoute)field(runtime,"route");
        set(runtime,"observed",true);set(runtime,"connected",true);set(runtime,"stamp",1L);
        set(route,"connectionId","1");
        session=(MediaSession)field(runtime,"session");
        set(route,"route","BRIDGE"); set(route,"server","server"); set(route,"policyRevision",1L);
        set(route,"peerUid",12001);
        set(route,"peer",new Messenger(new Handler(Looper.getMainLooper(),m->true)));
    }
    private void metadata(String json) { UsbMediaBridge.metadata(json); idle(); }
    private void command(String value) throws Exception { call(runtime,"command",new Class<?>[]{String.class},value); idle(); }
    private Bundle envelopeForRoute() throws Exception {
        Bundle b=envelope((String)field(route,"instance"),(Long)field(route,"epoch"),(String)field(route,"connectionId"));
        b.putString("server","server"); b.putLong("policy",1); return b;
    }
    private void receive(int what,Bundle b,int uid) throws Exception {
        Message m=Message.obtain(null,what);m.sendingUid=uid;m.setData(b);
        call(route,"receive",new Class<?>[]{Message.class},m); idle();
    }
    @Test public void modeSelectionCoversAllPolicyCombinations() {
        for(boolean ignored:new boolean[]{false,true}) for(boolean enabled:new boolean[]{false,true}) for(boolean ready:new boolean[]{false,true}) {
            assertEquals("DIRECT",BridgeSettings.choose("DIRECT",ignored,enabled,ready));
            assertEquals(ignored?"IGNORED":enabled&&ready?"BRIDGE":"WAIT",BridgeSettings.choose("BRIDGE",ignored,enabled,ready));
            assertEquals(ignored||!enabled?"DIRECT":ready?"BRIDGE":"WAIT",BridgeSettings.choose("AUTO",ignored,enabled,ready));
        }
    }
    @Test public void incrementalMetadataAndNewSongNeverRetainOldArtist() {
        TrackState t=new TrackState(); t.update("A","Singer","Album",1);
        t.update(null,null,null,2); assertEquals("A",t.title); assertFalse(t.playing());
        t.update("B",null,null,null); assertEquals("",t.artist); assertEquals("",t.album);
        t.update(null,"New singer",null,-1); assertEquals("B",t.title); assertEquals(2,t.status);
        t.reset(); assertFalse(t.hasTrack()); assertEquals(-1,t.status);
    }
    @Test public void pausedSessionRemainsActiveAndPlayPauseUseOriginalCallbacksOnce() throws Exception {
        runtime(); metadata("{\"MediaSongName\":\"Song\",\"MediaArtistName\":\"Artist\",\"MediaPlayStatus\":2}");
        assertTrue(session.isActive());
        RecordingSession output=org.robolectric.shadow.api.Shadow.extract(session);
        assertEquals(PlaybackState.STATE_PAUSED,output.state.getState());
        assertEquals("Song",output.metadata.getString(MediaMetadata.METADATA_KEY_TITLE));
        assertFalse(output.metadata.containsKey(MediaMetadata.METADATA_KEY_DURATION));
        assertEquals(0,output.state.getActions() & PlaybackState.ACTION_SEEK_TO);
        command("PLAY"); command("PAUSE"); command("NEXT"); command("PREVIOUS");
        assertEquals(Arrays.asList(126,127,87,88),keys);
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(700));
        assertEquals(Arrays.asList(126,127,87,88),keys); // no delayed resume from an old grant
    }
    @Test public void metadataModeChangeYieldAndHeartbeatFailureNeverSendUsbCommands() throws Exception {
        runtime(); metadata("{\"MediaSongName\":\"Song\",\"MediaPlayStatus\":1}");
        metadata("{\"MediaArtistName\":\"Artist\"}");
        set(route,"activeIntent","intent"); Bundle yielded=envelopeForRoute(); yielded.putString("intent","intent");
        receive(YIELD,yielded,12001);
        UsbMediaBridge.refresh(); idle();
        call(route,"fail",new Class<?>[]{String.class},"timeout");
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(700));
        assertTrue(keys.isEmpty()); assertTrue(cn.manstep.phonemirrorBox.BoxInterface.f.P);
    }
    @Test public void staleAndRepeatedCommandsCannotReachPhone() throws Exception {
        runtime(); metadata("{\"MediaSongName\":\"Song\",\"MediaPlayStatus\":2}");
        Bundle b=envelopeForRoute(); b.putString("id","one"); b.putString("command","NEXT");
        receive(COMMAND,b,12002); assertTrue(keys.isEmpty());
        Bundle stale=new Bundle(b); stale.putLong("epoch",-1); receive(COMMAND,stale,12001); assertTrue(keys.isEmpty());
        receive(COMMAND,b,12001); receive(COMMAND,b,12001); assertEquals(Arrays.asList(87),keys);
        Bundle oldConnection=new Bundle(b);oldConnection.putString("connection","old");oldConnection.putString("id","two");
        receive(COMMAND,oldConnection,12001); assertEquals(Arrays.asList(87),keys);
    }
    @Test public void disconnectedMetadataIsDroppedAndReconnectStartsEmpty() throws Exception {
        runtime(); metadata("{\"MediaSongName\":\"Old\",\"MediaPlayStatus\":1}");
        cn.manstep.phonemirrorBox.BoxInterface.f.P=false;
        metadata("{\"MediaSongName\":\"Stale\"}");
        assertFalse(session.isActive()); assertFalse(((TrackState)field(runtime,"track")).hasTrack());
        cn.manstep.phonemirrorBox.BoxInterface.f.P=true;
        metadata("{\"MediaPlayStatus\":2}");
        assertFalse(((TrackState)field(runtime,"track")).hasTrack()); assertTrue(keys.isEmpty());
    }
    @Test public void oldCompanionWithoutUsbCapabilityCannotAuthorizeRoute() throws Exception {
        runtime(); Bundle b=envelopeForRoute(); b.putBoolean("ready",true); b.putBoolean("enabled",true);
        receive(POLICY,b,12001); assertFalse(route.ready()); assertTrue(route.status.contains("更新")); assertTrue(keys.isEmpty());
    }
    @Test public void bridgeRequiresPreparedAndReadyAndNeitherHandoverSendsPhoneControl() throws Exception {
        runtime(); metadata("{\"MediaSongName\":\"Song\",\"MediaPlayStatus\":2}");
        List<Integer> wire=new ArrayList<>();
        set(route,"peer",new Messenger(new Handler(Looper.getMainLooper(),m->{wire.add(m.what);return true;})));
        set(route,"peerPackage",MEDIABRIDGE);set(route,"enabled",true);set(route,"peerReady",true);
        route.refresh();idle();assertEquals(Arrays.asList(PREPARE),wire);assertFalse(route.ready());
        Bundle prepared=envelopeForRoute();prepared.putString("target","BRIDGE");prepared.putBoolean("ok",true);
        receive(PREPARED,prepared,12001);assertEquals(Arrays.asList(PREPARE,COMMIT),wire);assertFalse(route.ready());
        Bundle ready=envelopeForRoute();ready.putString("target","BRIDGE");receive(READY,ready,12001);
        assertEquals("BRIDGE",route.route());assertTrue(session.isActive());assertTrue(keys.isEmpty());
        context.getSharedPreferences("usb_media_route_v1",0).edit().putString("mode","DIRECT").commit();
        route.refresh();idle();assertFalse(route.ready());assertNull(field(route,"direct"));
        Bundle rejected=envelopeForRoute();rejected.putString("target","DIRECT");rejected.putBoolean("ok",false);
        receive(PREPARED,rejected,12001);assertFalse(route.ready());assertNull(field(route,"direct"));assertTrue(keys.isEmpty());
    }
    @Test public void settingsRetainsTheExactOriginalViewAndItsChildren() {
        LinearLayout original=new LinearLayout(context); View child=new View(context); child.setId(7823); original.addView(child);
        View wrapped=BridgeSettings.wrap(original);
        assertNotSame(original,wrapped); assertSame(child,wrapped.findViewById(7823));
        assertSame(original,((LinearLayout)wrapped).getChildAt(1));
    }
    private static class FakeApi extends MediaCenterAPI {
        final CountDownLatch registered=new CountDownLatch(1), released=new CountDownLatch(1);
        MusicClient client; volatile int requests; boolean unregisterOk=true;
        @Override public void init(Context c,ECarXApiClient.Callback cb){cb.onAPIReady(true);}
        @Override public Object registerMusic(String p,MusicClient c){client=c;registered.countDown();return this;}
        @Override public boolean unregister(Object t){released.countDown();return unregisterOk;}
        @Override public boolean requestPlay(Object t){requests++;return true;}
        @Override public void updateCurrentSourceType(Object t,int s){}
        @Override public boolean updateMusicPlaybackState(Object t,MusicPlaybackInfo i){return true;}
    }
    @Test public void directRegistrationAndMetadataDoNotAutoplayAndClosedCallbacksAreFenced() throws Exception {
        FakeApi api=new FakeApi(); MediaCenterAPI.fake=api;
        CountDownLatch ready=new CountDownLatch(1);
        F25Direct d=new F25Direct(context,keys::add,ok->{if(ok)ready.countDown();});
        d.start(); assertTrue(api.registered.await(3,TimeUnit.SECONDS));
        // Drain worker via a serial barrier, then the main-thread completion.
        ((ThreadPoolExecutor)field(d,"io")).submit(()->{}).get(3,TimeUnit.SECONDS); idle();
        assertEquals(0,ready.getCount()); assertEquals(0,api.requests); assertTrue(keys.isEmpty());
        TrackState track=new TrackState();track.update("Song","Artist","Album",2);d.update(track);
        ((ThreadPoolExecutor)field(d,"io")).submit(()->{}).get(3,TimeUnit.SECONDS); idle();
        assertEquals(0,api.requests); assertTrue(api.client.onPlay()); assertEquals(Arrays.asList(126),keys);
        assertFalse(api.client.onForward());assertFalse(api.client.onRewind());
        assertEquals(Arrays.asList(126,90,89),keys);
        d.close(ok->{});assertFalse(api.client.onNext());assertTrue(api.released.await(3,TimeUnit.SECONDS));idle();
        assertEquals(Arrays.asList(126,90,89),keys);
    }
    @Test public void failedDirectUnregisterBlocksNewRoute() throws Exception {
        FakeApi api=new FakeApi();api.unregisterOk=false;MediaCenterAPI.fake=api;
        route=new BridgeRoute(context,keys::add,new BridgeRoute.Listener(){
            public void changed(){}public void ready(){}public void command(String c){}public void yielded(){}
        });
        F25Direct d=new F25Direct(context,keys::add,ok->{}); d.start();
        assertTrue(api.registered.await(3,TimeUnit.SECONDS));
        ((ThreadPoolExecutor)field(d,"io")).submit(()->{}).get(3,TimeUnit.SECONDS);idle();
        set(route,"direct",d);set(route,"route","DIRECT");
        context.getSharedPreferences("usb_media_route_v1",0).edit().putString("mode","BRIDGE").commit();
        call(route,"reconcile",new Class<?>[]{});
        assertTrue(api.released.await(3,TimeUnit.SECONDS));
        // Callback is posted after unregister returns; drain its worker, then main.
        ((ThreadPoolExecutor)field(d,"io")).awaitTermination(3,TimeUnit.SECONDS);idle();
        assertTrue((Boolean)field(route,"releaseUncertain"));assertFalse(route.ready());
        route.refresh();idle();assertFalse(route.ready());assertTrue(keys.isEmpty());
    }
}
