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
    @Test public void recordedUsbFieldSequenceCannotMixTwoSongsOrUseSongAsArtist() {
        TrackState t=new TrackState(); t.update("如愿","王菲","如愿",1);
        t.update(null,"如愿",null,null); assertEquals("王菲",t.artist);
        t.update(null,"任素汐",null,null); assertEquals("",t.title); assertEquals("",t.artist);
        assertTrue(t.hasTrack()); // Explicit play/pause stays available while identity is pending.
        t.update(null,"亲爱的你啊",null,null); assertEquals("",t.artist);
        t.update("亲爱的你啊","任素汐","亲爱的你啊",null);
        assertEquals("亲爱的你啊",t.title); assertEquals("任素汐",t.artist);
        t.updateLyrics("此刻的歌词"); t.update(null,"冯沁苑",null,null);
        assertEquals("",t.lyrics); assertEquals("",t.title);
        t.update("起风了",null,null,null); assertEquals("",t.artist);
        t.update(null,"冯沁苑",null,null); assertEquals("冯沁苑",t.artist);
    }
    @Test public void timeUnitsRequireRepeatedAdvancingSamplesAndRejectSeekGuesses() {
        for(int scale:new int[]{1,1000}) {
            BoxClock clock=new BoxClock();
            clock.update(180000L/scale,10000L/scale,true,1000); assertEquals(0,clock.duration());
            clock.update(null,12000L/scale,true,3000); assertEquals(0,clock.duration());
            clock.update(null,14000L/scale,true,5000);
            assertEquals(180000,clock.duration()); assertEquals(14000,clock.position());
            clock.newTrack(); assertEquals(0,clock.duration()); assertEquals(-1,clock.position());
        }
        BoxClock jump=new BoxClock(); jump.update(180L,10L,true,1000);
        jump.update(null,80L,true,3000); jump.update(null,30L,true,5000);
        assertEquals(0,jump.duration()); assertEquals(-1,jump.position());
    }
    @Test public void sameTitleRecordingCanBeEstablishedByACompletePair() {
        TrackState t=new TrackState(); t.update("如愿","王菲","如愿",1);
        t.updateLyrics("old line"); long revision=t.revision;
        t.update("如愿","另一位歌手","翻唱",null);
        assertEquals("如愿",t.title); assertEquals("另一位歌手",t.artist);
        assertEquals("",t.lyrics); assertTrue(t.revision>revision);
        t.update(null,"第三位歌手",null,null); assertFalse(t.complete());
        t.update("如愿","第三位歌手","另一版本",null); assertTrue(t.complete());
        assertEquals("第三位歌手",t.artist);
    }
    @Test public void nativeLyricsAndCoverReachSessionAndNewTrackDropsOldResources() throws Exception {
        runtime(); metadata("{\"MediaSongName\":\"Song\",\"MediaArtistName\":\"Artist\",\"MediaLyrics\":\"Live line\",\"MediaPlayStatus\":1}");
        android.graphics.Bitmap bitmap=android.graphics.Bitmap.createBitmap(64,64,android.graphics.Bitmap.Config.ARGB_8888);
        java.io.ByteArrayOutputStream bytes=new java.io.ByteArrayOutputStream();
        bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,bytes);
        UsbMediaBridge.artwork(bytes.toByteArray()); idle();
        long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(3);
        while(field(runtime,"cover")==null && System.nanoTime()<deadline){Thread.sleep(5);idle();}
        RecordingSession output=org.robolectric.shadow.api.Shadow.extract(session);
        assertNotNull(output.metadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART));
        assertEquals("Live line",output.metadata.getString("android.media.metadata.LYRICS"));
        metadata("{\"MediaSongName\":\"Next\",\"MediaArtistName\":\"Singer\"}");
        assertNull(output.metadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART));
        assertEquals("",output.metadata.getString("android.media.metadata.LYRICS"));
        assertTrue(keys.isEmpty());
    }
    @Test public void titleFormatAppliesOnReconnectAndBridgeKeepsTheRawTrackId()throws Exception {
        runtime();assertEquals("ORIGINAL",BridgeSettings.titleFormat(context));assertFalse(BridgeSettings.onlineResources(context));
        context.getSharedPreferences("usb_media_route_v1",0).edit().putString("combined_title_format","ARTIST_TITLE").putString("mode","BRIDGE").commit();
        metadata("{\"MediaSongName\":\"Singer - Song\"}");
        RecordingSession output=org.robolectric.shadow.api.Shadow.extract(session);
        assertEquals("Singer - Song",output.metadata.getString(MediaMetadata.METADATA_KEY_TITLE));
        cn.manstep.phonemirrorBox.BoxInterface.f.P=false;metadata("{}");
        cn.manstep.phonemirrorBox.BoxInterface.f.P=true;metadata("{}");
        set(route,"route","BRIDGE");metadata("{\"MediaSongName\":\"Singer - Song\"}");
        assertEquals("Song",output.metadata.getString(MediaMetadata.METADATA_KEY_TITLE));
        assertEquals("Singer",output.metadata.getString(MediaMetadata.METADATA_KEY_ARTIST));
        TrackState raw=(TrackState)field(runtime,"track");assertEquals("Singer - Song",raw.title);assertEquals("",raw.artist);
        assertEquals(raw.mediaId(),output.metadata.getString(MediaMetadata.METADATA_KEY_MEDIA_ID));assertTrue(keys.isEmpty());
    }
    @Test public void phoneDisconnectReleasesCompanionAndDoesNotReacquireItWhileIdle() throws Exception {
        runtime(); List<Integer> sent=new ArrayList<>();
        set(route,"peer",new Messenger(new Handler(Looper.getMainLooper(),m->{sent.add(m.what);return true;})));
        metadata("{\"MediaSongName\":\"Song\",\"MediaPlayStatus\":2}");
        cn.manstep.phonemirrorBox.BoxInterface.f.P=false; metadata("{}");
        assertEquals(Arrays.asList(CLOSE),sent); assertNull(field(route,"peer"));
        assertEquals("",field(route,"connectionId")); assertFalse(route.ready());
        route.start(); Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(8));
        assertNull(field(route,"peer")); assertNull(field(route,"direct"));
        assertEquals(Arrays.asList(CLOSE),sent); assertTrue(keys.isEmpty());
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
        assertEquals(Arrays.asList(126,127,87,126,88,126),keys);
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(700));
        assertEquals(Arrays.asList(126,127,87,126,88,126),keys); // only the user's skips add PLAY; no delayed grant does
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
        receive(COMMAND,b,12001); receive(COMMAND,b,12001); assertEquals(Arrays.asList(87,126),keys);
        Bundle oldConnection=new Bundle(b);oldConnection.putString("connection","old");oldConnection.putString("id","two");
        receive(COMMAND,oldConnection,12001); assertEquals(Arrays.asList(87,126),keys);
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
        LinearLayout original=new LinearLayout(context); View toolbar=new View(context);original.addView(toolbar);
        android.widget.ScrollView scroll=new android.widget.ScrollView(context);scroll.setId(0x7f090204);
        LinearLayout list=new LinearLayout(context);list.setOrientation(LinearLayout.VERTICAL);
        View child=new View(context); child.setId(7823);list.addView(child);scroll.addView(list);original.addView(scroll);
        View wrapped=BridgeSettings.wrap(original);
        assertSame(original,wrapped); assertSame(child,wrapped.findViewById(7823));
        assertSame(toolbar,original.getChildAt(0));assertSame(scroll,original.getChildAt(1));
        assertEquals(2,list.getChildCount());assertEquals(3,((LinearLayout)list.getChildAt(0)).getChildCount());
        BridgeSettings.wrap(original);assertEquals(2,list.getChildCount());
    }
    @Test public void connectionWithoutTrackPublishesControlsButNeverStartsPlayback() throws Exception {
        runtime(); call(runtime,"publish",new Class<?>[]{});
        RecordingSession output=org.robolectric.shadow.api.Shadow.extract(session);
        assertTrue(session.isActive());assertEquals(PlaybackState.STATE_PAUSED,output.state.getState());
        assertEquals("",output.metadata.getString(MediaMetadata.METADATA_KEY_TITLE));
        assertEquals("1",output.metadata.getString("usb.media.connection"));assertTrue(keys.isEmpty());
        command("NEXT");assertEquals(Arrays.asList(87,126),keys);
        cn.manstep.phonemirrorBox.BoxInterface.f.P=false;command("NEXT");assertEquals(Arrays.asList(87,126),keys);
    }
    @Test public void titleFormatChangesDisplayImmediatelyWithoutChangingRawIdentityOrSendingKeys() throws Exception {
        runtime();metadata("{\"MediaSongName\":\"雷佳 - 人世间\",\"MediaPlayStatus\":2}");
        TrackState track=(TrackState)field(runtime,"track");String identity=track.mediaId();
        context.getSharedPreferences("usb_media_route_v1",0).edit().putString("combined_title_format","ARTIST_TITLE").commit();
        UsbMediaBridge.titleFormatChanged();idle();
        RecordingSession output=org.robolectric.shadow.api.Shadow.extract(session);
        assertEquals("人世间",output.metadata.getString(MediaMetadata.METADATA_KEY_TITLE));
        assertEquals("雷佳",output.metadata.getString(MediaMetadata.METADATA_KEY_ARTIST));
        assertEquals(identity,track.mediaId());assertEquals("",track.artist);assertTrue(keys.isEmpty());
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
    @Test public void directPublishesNativeResourcesAndProgressWithoutPhoneControl()throws Exception {
        FakeApi api=new FakeApi();MediaCenterAPI.fake=api;
        F25Direct d=new F25Direct(context,keys::add,ok->{});
        try {
            TrackState track=new TrackState();track.titleFormat="ARTIST_TITLE";track.update("Singer - Song",null,"Album",1);
            track.artworkUri="content://native/cover";track.lyrics="[00:01]first\n[00:02]second";
            track.durationMs=180000;track.positionMs=1500;track.positionAtMs=SystemClock.elapsedRealtime();
            d.update(track);d.start();assertTrue(api.registered.await(3,TimeUnit.SECONDS));
            ((ThreadPoolExecutor)field(d,"io")).submit(()->{}).get(3,TimeUnit.SECONDS);idle();
            MusicPlaybackInfo output=api.client.getMusicPlaybackInfo();assertEquals("Song",output.getTitle());assertEquals("Singer",output.getArtist());
            assertEquals("content://native/cover",output.getArtwork().toString());assertEquals(180000,output.getDuration());
            assertEquals("first",output.getCurrentLyricSentence());
            Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(750));
            assertEquals("second",output.getCurrentLyricSentence());assertTrue(api.client.getCurrentProgress()>=2250);
            track.update("Next","Other",null,2);track.artworkUri="";track.lyrics="live line";track.positionMs=-1;
            d.update(track);assertNull(api.client.getMusicPlaybackInfo().getArtwork());
            assertEquals("live line",api.client.getMusicPlaybackInfo().getCurrentLyricSentence());
            assertEquals(0,api.requests);assertTrue(keys.isEmpty());
        } finally {d.close(ok->{});}
    }
    @Test public void failedDirectUnregisterBlocksNewRoute() throws Exception {
        FakeApi api=new FakeApi();api.unregisterOk=false;MediaCenterAPI.fake=api;
        route=new BridgeRoute(context,keys::add,new BridgeRoute.Listener(){
            public void changed(){}public void ready(){}public void command(String c){}public void yielded(){}
        });
        F25Direct d=new F25Direct(context,keys::add,ok->{}); d.start();
        assertTrue(api.registered.await(3,TimeUnit.SECONDS));
        ((ThreadPoolExecutor)field(d,"io")).submit(()->{}).get(3,TimeUnit.SECONDS);idle();
        set(route,"direct",d);set(route,"route","DIRECT");set(route,"connectionId","phone-1");
        context.getSharedPreferences("usb_media_route_v1",0).edit().putString("mode","BRIDGE").commit();
        call(route,"reconcile",new Class<?>[]{});
        assertTrue(api.released.await(3,TimeUnit.SECONDS));
        // Callback is posted after unregister returns; drain its worker, then main.
        ((ThreadPoolExecutor)field(d,"io")).awaitTermination(3,TimeUnit.SECONDS);idle();
        assertTrue((Boolean)field(route,"releaseUncertain"));assertFalse(route.ready());
        route.refresh();idle();assertFalse(route.ready());assertTrue(keys.isEmpty());
    }
}
