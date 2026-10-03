package cn.manstep.phonemirrorBox.bridge;

import java.util.ArrayList;
import java.util.List;

/** Ordered lyrics-only queries. Never rewrites the displayed or protocol title. */
public final class LyricQuery {
    public final String title, stage;
    public final long duration;
    private LyricQuery(String title, long duration, String stage) {
        this.title = title; this.duration = duration; this.stage = stage;
    }
    public static String stripped(String title) {
        String value = title;
        for (int i = 0; i < 8; i++) {
            String next = value.replaceAll("\\([^()]*\\)|（[^（）]*）", "").replaceAll("\\s+", " ").trim();
            if (next.equals(value)) break;
            value = next;
        }
        return value.isEmpty() ? title : value;
    }
    public static List<LyricQuery> steps(String title, long duration) {
        List<LyricQuery> result = new ArrayList<>();
        String clean = stripped(title);
        result.add(new LyricQuery(title, duration, duration > 0 ? "original_identity_duration" : "original_identity"));
        if (!clean.equals(title)) result.add(new LyricQuery(clean, duration,
                duration > 0 ? "stripped_identity_duration" : "stripped_identity"));
        if (duration > 0) result.add(new LyricQuery(clean, 0, "identity_without_duration"));
        return result;
    }
}
