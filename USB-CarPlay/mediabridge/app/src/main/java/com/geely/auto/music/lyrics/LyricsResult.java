package com.geely.auto.music.lyrics;

import java.util.List;

/* JADX INFO: compiled from: r8-map-id-3e69baf94e81d466d14d5c24482c5521bbb5f1d5dadfa8a3c310732898062d9f */
/* JADX INFO: loaded from: classes.dex */
public class LyricsResult {
    private final List<LrcParser.LrcLine> lines;
    private final String lrcContent;

    public LyricsResult(String str, List<LrcParser.LrcLine> list) {
        this.lrcContent = str;
        this.lines = list;
    }

    public String getLineAtPosition(long j) {
        List<LrcParser.LrcLine> list = this.lines;
        if (list == null || list.isEmpty()) {
            return null;
        }
        return LrcParser.findLineAtPosition(this.lines, j);
    }

    public List<LrcParser.LrcLine> getLines() {
        return this.lines;
    }

    public String getLrcContent() {
        return this.lrcContent;
    }

    public boolean isEmpty() {
        List<LrcParser.LrcLine> list = this.lines;
        return list == null || list.isEmpty();
    }
}
