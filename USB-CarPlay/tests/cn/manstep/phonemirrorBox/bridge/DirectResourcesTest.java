package cn.manstep.phonemirrorBox.bridge;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Looper;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import org.json.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=30)
@LooperMode(LooperMode.Mode.PAUSED)
public class DirectResourcesTest {
    Context context;
    final List<DirectMusicResources> resources=new ArrayList<>();
    @Before public void setup(){context=RuntimeEnvironment.getApplication();}
    @After public void cleanup(){for(DirectMusicResources r:resources)r.close();}
    private DirectSnapshot snapshot(String title,long duration) {
        TrackState t=new TrackState();t.update(title,"Artist","Album",1);t.durationMs=duration;return new DirectSnapshot(t);
    }
    private DirectMusicResources resource(DirectMusicResources.Result result,DirectMusicResources.Downloader download) {
        DirectMusicResources r=new DirectMusicResources(context,result,download);resources.add(r);return r;
    }
    private void drain(DirectMusicResources r)throws Exception {
        // A barrier submitted to the one-slot production queue can evict the request itself.
        java.lang.reflect.Field field=DirectMusicResources.class.getDeclaredField("task");field.setAccessible(true);
        Future<?> task=(Future<?>)field.get(r);if(task!=null&&!task.isCancelled())task.get(5,TimeUnit.SECONDS);
        Shadows.shadowOf(Looper.getMainLooper()).idle();
    }
    private static byte[] bytes(String value){return value.getBytes(StandardCharsets.UTF_8);}
    private byte[] png() {
        Bitmap image=Bitmap.createBitmap(64,64,Bitmap.Config.ARGB_8888);image.eraseColor(0xff448899);
        ByteArrayOutputStream out=new ByteArrayOutputStream();image.compress(Bitmap.CompressFormat.PNG,100,out);image.recycle();return out.toByteArray();
    }
    private byte[] lyrics(String title)throws Exception {
        return bytes(new JSONObject().put("trackName",title).put("artistName","Artist").put("duration",180)
                .put("syncedLyrics","[00:01]line").toString());
    }
    @Test public void defaultOffDoesNotUseNetworkAndUnknownTimeLooksUpLyricsOnly()throws Exception {
        List<String> requests=new ArrayList<>();
        DirectMusicResources r=resource((id,art,lrc)->{},(url,limit)->{requests.add(url);return bytes("{}");});
        assertFalse(BridgeSettings.onlineResources(context));assertEquals("ORIGINAL",BridgeSettings.titleFormat(context));
        r.update(snapshot("Disabled",180000),false);drain(r);
        assertTrue(requests.isEmpty());
        r.update(snapshot("Unknown",0),true);drain(r);assertEquals(1,requests.size());assertTrue(requests.get(0).contains("lrclib.net/api/search"));
    }
    @Test public void verifiedOnlineResourcesCacheAndOriginalFileProviderCanServeCover()throws Exception {
        List<String> requests=new ArrayList<>(),results=new ArrayList<>();byte[] picture=png();
        DirectMusicResources r=resource((id,art,lrc)->{results.add(art);results.add(lrc);},(url,limit)->{
            requests.add(url);
            if(url.contains("lrclib"))return lyrics("Song");
            if(url.contains("itunes"))return bytes("{\"results\":[{\"trackName\":\"Wrong\",\"artistName\":\"Artist\"},"
                    +"{\"trackName\":\"Song\",\"artistName\":\"Artist\",\"trackTimeMillis\":180000,\"collectionName\":\"Album\","
                    +"\"artworkUrl100\":\"https://is1-ssl.mzstatic.com/art/100x100bb.jpg\"}]}");
            assertTrue(url.contains("600x600bb"));return picture;
        });
        r.update(snapshot("Song",180000),true);drain(r);assertEquals(3,requests.size());
        assertTrue(results.get(0).startsWith("content://"));assertEquals("[00:01]line",results.get(1));
        Uri uri=Uri.parse(results.get(0));File file=new File(new File(context.getCacheDir(),"usbbox-artwork"),uri.getLastPathSegment());
        // FileProvider runs on Android's slash paths; Robolectric uses the Windows host separator.
        String providerUri=androidx.core.content.FileProvider.getUriForFile(context,context.getPackageName()+".fileprovider",file).toString();
        assertEquals(uri.toString(),providerUri.replace("%5C","/"));
        try(InputStream input=context.getContentResolver().openInputStream(uri)){assertNotNull(input);assertEquals(0x89,input.read());}
        r.update(snapshot("Song",180000),false);drain(r);assertEquals(3,requests.size());
        assertEquals(org.robolectric.shadows.ShadowLog.getLogsForTag("USBMediaBridge").toString(),4,results.size());
        assertEquals(results.get(0),results.get(2));assertEquals(results.get(1),results.get(3));
    }
    @Test public void nativeContentWinsAndMismatchesOrUnapprovedArtworkHostsAreRejected()throws Exception {
        List<String> requests=new ArrayList<>(),results=new ArrayList<>();
        DirectMusicResources r=resource((id,art,lrc)->{results.add(art);results.add(lrc);},(url,limit)->{
            requests.add(url);if(url.contains("lrclib"))return lyrics("Wrong");
            return bytes("{\"results\":[{\"trackName\":\"Song\",\"artistName\":\"Artist\",\"trackTimeMillis\":180000,"
                    +"\"collectionName\":\"Album\",\"artworkUrl100\":\"https://untrusted.invalid/cover.jpg\"}]}");
        });
        r.update(snapshot("Song",180000),true);drain(r);assertEquals(Arrays.asList("",""),results);assertEquals(3,requests.size());
        TrackState nativeTrack=new TrackState();nativeTrack.update("Native","Artist","Album",1);nativeTrack.durationMs=180000;
        nativeTrack.artworkUri="content://native/cover";nativeTrack.lyrics="native line";
        r.update(new DirectSnapshot(nativeTrack),true);drain(r);assertEquals(3,requests.size());
        assertFalse(DirectMusicResources.matches("Song","Song (Live)"));assertTrue(DirectMusicResources.matches("Ｓong","song"));
    }
    @Test public void lateResultsAfterNewTrackOrCloseNeverReachTheClient()throws Exception {
        for(boolean close:new boolean[]{false,true}) {
            CountDownLatch started=new CountDownLatch(1),resume=new CountDownLatch(1),finished=new CountDownLatch(1);
            List<String> results=new ArrayList<>();
            DirectMusicResources r=resource((id,art,lrc)->results.add(lrc),(url,limit)->{
                started.countDown();
                while(resume.getCount()>0)try{resume.await();}catch(InterruptedException ignored){}
                try{return lyrics("Old");}finally{finished.countDown();}
            });
            r.update(snapshot("Old",180000),true);assertTrue(started.await(3,TimeUnit.SECONDS));
            if(close)r.close();else r.update(snapshot("New",0),true);
            resume.countDown();assertTrue(finished.await(3,TimeUnit.SECONDS));
            if(close)assertTrue(r.worker.awaitTermination(3,TimeUnit.SECONDS));else drain(r);
            Shadows.shadowOf(Looper.getMainLooper()).idle();assertFalse(results.contains("[00:01]line"));
            if(close)assertTrue(results.isEmpty()); // A new track may publish its own empty lookup result.
        }
    }
    @Test public void cacheRejectsForeignAndTraversalUris()throws Exception {
        assertFalse(DirectArtworkCache.readable(context,"content://other/cache_path/usbbox-artwork/anything.png"));
        assertFalse(DirectArtworkCache.readable(context,"content://"+context.getPackageName()+".fileprovider/cache_path/usbbox-artwork/../private"));
        assertEquals("",DirectArtworkCache.save(context,bytes("not an image")));
    }
    @Test public void lyricsQueryOrderUsesOriginalThenStrippedThenNoDuration()throws Exception {
        List<LyricQuery> steps=LyricQuery.steps("Song（Live） (Remastered)",60000);
        assertEquals(3,steps.size());assertEquals("Song（Live） (Remastered)",steps.get(0).title);
        assertEquals("Song",steps.get(1).title);assertEquals(60000,steps.get(1).duration);assertEquals(0,steps.get(2).duration);
        List<String> requests=new ArrayList<>(),results=new ArrayList<>();
        DirectMusicResources r=resource((id,art,lrc)->results.add(lrc),(url,limit)->{
            requests.add(url);
            if(url.contains("itunes"))return bytes("{\"results\":[]}");
            if(url.contains("/get"))throw new IOException("no duration match");
            return bytes("["+new String(lyrics("Song"),StandardCharsets.UTF_8)+"]");
        });
        r.update(snapshot("Song（Live）",60000),true);drain(r);
        assertEquals("[00:01]line",results.get(0));assertEquals(4,requests.size());
        assertTrue(requests.get(0).contains("/get?track_name=Song%EF%BC%88Live%EF%BC%89"));
        assertTrue(requests.get(1).contains("/get?track_name=Song&artist_name=Artist&duration=60"));
        assertTrue(requests.get(2).contains("/search?track_name=Song&artist_name=Artist"));
        org.json.JSONArray songs=new org.json.JSONArray().put(new org.json.JSONObject(new String(lyrics("Song"),StandardCharsets.UTF_8)));
        assertEquals("",DirectMusicResources.selectLyrics(songs,"Song","Artist",60000));
        assertEquals("[00:01]line",DirectMusicResources.selectLyrics(songs,"Song","Artist",0));
        assertEquals("",DirectMusicResources.selectLyrics(songs,"Song","Wrong artist",0));
    }
}
