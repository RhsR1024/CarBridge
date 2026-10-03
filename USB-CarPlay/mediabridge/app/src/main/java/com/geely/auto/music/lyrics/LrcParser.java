package com.geely.auto.music.lyrics;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class LrcParser {
    private static final Pattern STAMP = Pattern.compile("\\[(\\d{1,6}):([0-5]\\d)(?:[.:](\\d{1,3}))?\\]");
    private static final Pattern OFFSET = Pattern.compile("(?im)\\[offset\\s*:\\s*([+-]?\\d+)\\]");
    public static final class LrcLine {
        public final String text;
        public final long timeMs;
        public LrcLine(long timeMs, String text) { this.timeMs = timeMs; this.text = text; }
        @Override public String toString() { return String.format(Locale.ROOT, "[%02d:%02d.%03d]%s", timeMs / 60000, timeMs / 1000 % 60, timeMs % 1000, text); }
    }
    public static List<LrcLine> parse(String content) {
        ArrayList<LrcLine> result = new ArrayList<>();
        if (content == null || content.length() > 2 * 1024 * 1024) return result;
        long offset = 0L;
        Matcher offsetTag = OFFSET.matcher(content);
        while (offsetTag.find()) {
            try { offset = Math.max(-3600000L, Math.min(3600000L, Long.parseLong(offsetTag.group(1)))); }
            catch (NumberFormatException ignored) {}
        }
        for (String raw : content.replace("\uFEFF", "").split("\\r?\\n")) {
            Matcher stamps = STAMP.matcher(raw);
            ArrayList<Long> times = new ArrayList<>();
            int end = 0;
            while (stamps.find()) {
                if (!raw.substring(end, stamps.start()).trim().isEmpty()) break;
                String fraction = stamps.group(3);
                long millis = fraction == null ? 0 : Long.parseLong((fraction + "000").substring(0, 3));
                long at = Long.parseLong(stamps.group(1)) * 60000L + Long.parseLong(stamps.group(2)) * 1000L + millis;
                times.add(Math.max(0, at - offset)); end = stamps.end();
            }
            String text = raw.substring(end).trim();
            for (long at : times) result.add(new LrcLine(at, text)); // Empty timed lines clear the previous sentence.
        }
        result.sort(Comparator.comparingLong(line -> line.timeMs));
        return result;
    }
    public static String findLineAtPosition(List<LrcLine> lines, long position) {
        if (lines == null || position < 0) return null;
        int low = 0, high = lines.size() - 1, found = -1;
        while (low <= high) {
            int middle = (low + high) >>> 1;
            if (lines.get(middle).timeMs <= position) { found = middle; low = middle + 1; }
            else high = middle - 1;
        }
        return found < 0 ? null : lines.get(found).text;
    }
}

