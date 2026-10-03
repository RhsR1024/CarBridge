package cn.manstep.phonemirrorBox.bridge;

import org.junit.Test;
import static org.junit.Assert.*;

public class CombinedTitleMetadataTest {
    @Test public void matchesCarBridgeFormatsAndNeverMutatesRawIdentity() {
        TrackState raw=new TrackState();raw.update("周铁男 - 三国杀",null,"Album",1);
        String id=raw.mediaId();long revision=raw.revision;
        assertEquals("周铁男 - 三国杀",raw.display().title);assertEquals("",raw.display().artist);
        raw.titleFormat="ARTIST_TITLE";
        assertEquals("三国杀",raw.display().title);assertEquals("周铁男",raw.display().artist);
        raw.update(null,null,null,2);
        assertEquals("三国杀",raw.display().title);assertEquals(id,raw.mediaId());assertEquals(revision,raw.revision);
        assertEquals("周铁男 - 三国杀",raw.title);assertEquals("",raw.artist);
        raw.update("三国杀 — 周铁男",null,null,null);raw.titleFormat="TITLE_ARTIST";
        assertEquals("三国杀",raw.display().title);assertEquals("周铁男",raw.display().artist);
    }
    @Test public void nativeArtistAmbiguityAndLongOrMultilineInputsRemainUntouched() {
        for(String title:new String[]{"周铁男-三国杀","周铁男 - 三国杀 - 现场版"," - 三国杀","周铁男 - ","歌词\n另一行 - 内容",
                "Singer - "+new String(new char[520]).replace('\0','a')}) {
            TrackState raw=new TrackState();raw.titleFormat="ARTIST_TITLE";raw.update(title,null,null,1);
            assertEquals(raw.title,raw.display().title);assertEquals("",raw.display().artist);
        }
        TrackState raw=new TrackState();raw.titleFormat="ARTIST_TITLE";raw.update("Singer - Song","原始歌手",null,1);
        assertEquals("Singer - Song",raw.display().title);assertEquals("原始歌手",raw.display().artist);
        assertEquals("native_artist_present",raw.display().reason);
    }
    @Test public void pendingTrackCannotResurrectAnOldCombinedTitle() {
        TrackState raw=new TrackState();raw.titleFormat="ARTIST_TITLE";raw.update("Singer - Song","Singer",null,1);
        raw.update(null,"Other",null,null);assertEquals("",raw.display().title);assertEquals("",raw.display().artist);
    }
    @Test public void lrcPortSupportsOffsetsRepeatedTimesAndUnknownProgress() {
        java.util.List<SynchronizedLyrics.Line> lines=SynchronizedLyrics.parse("[offset:-500]\n[00:01.25][00:02]first\n[00:02]second\n[00:60]invalid");
        assertEquals(3,lines.size());assertEquals("",SynchronizedLyrics.current(lines,-1));
        assertEquals("",SynchronizedLyrics.current(lines,749));assertEquals("first",SynchronizedLyrics.current(lines,750));
        assertEquals("second",SynchronizedLyrics.current(lines,1500));assertTrue(SynchronizedLyrics.parse("plain text").isEmpty());
    }
}
