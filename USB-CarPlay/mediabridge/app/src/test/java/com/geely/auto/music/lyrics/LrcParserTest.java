package com.geely.auto.music.lyrics;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class LrcParserTest {
    @Test public void parsesAndSortsLines() {
        LyricsResult result = new LyricsResult("[00:02.00]later\n[00:01.00]first", LrcParser.parse("[00:02.00]later\n[00:01.00]first"));
        assertEquals(2, result.getLines().size());
        assertEquals("first", result.getLineAtPosition(1500));
        assertEquals("later", result.getLineAtPosition(2500));
    }

    @Test public void ignoresMalformedAndEmptyLines() {
        assertTrue(LrcParser.parse("not lrc\n[00:01.00]ok\n[bad]x").size() == 1);
    }
    @Test public void expandsRepeatedTimeTagsAndAcceptsWholeSeconds() {
        java.util.List<LrcParser.LrcLine> lines = LrcParser.parse("[00:01][00:02.5][00:03.050]hello");
        assertEquals(3, lines.size()); assertEquals(1000, lines.get(0).timeMs);
        assertEquals(2500, lines.get(1).timeMs); assertEquals(3050, lines.get(2).timeMs);
        assertEquals("hello", lines.get(2).text);
    }
    @Test public void positiveFileOffsetMakesLyricsEarlier() {
        assertEquals(500, LrcParser.parse("[offset:+500]\n[00:01]line").get(0).timeMs);
        assertEquals(1500, LrcParser.parse("[00:01]line\n[offset:-500]").get(0).timeMs);
    }
    @Test public void timedEmptyLineClearsPreviousLyrics() {
        java.util.List<LrcParser.LrcLine> lines = LrcParser.parse("\uFEFF[00:01]line\r\n[00:02]\r\n[ar:artist]");
        assertEquals("", LrcParser.findLineAtPosition(lines, 2000));
        org.junit.Assert.assertNull(LrcParser.findLineAtPosition(lines, 999));
    }
    @Test public void invalidTimesAndHugeOffsetsNeverCrash() {
        assertTrue(LrcParser.parse("[99:99]bad\n[offset:999999999999999999999999999]").isEmpty());
        assertEquals(0, LrcParser.parse("[offset:5000]\n[00:01]early").get(0).timeMs);
    }
}
