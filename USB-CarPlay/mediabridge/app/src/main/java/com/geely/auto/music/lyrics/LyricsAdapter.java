package com.geely.auto.music.lyrics;

/* JADX INFO: compiled from: r8-map-id-3e69baf94e81d466d14d5c24482c5521bbb5f1d5dadfa8a3c310732898062d9f */
/* JADX INFO: loaded from: classes.dex */
public interface LyricsAdapter {
    LyricsResult fetchLyrics(String str, String str2, long j);

    String getDisplayName();
}
