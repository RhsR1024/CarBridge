package cn.manstep.phonemirrorBox.bridge;

import java.util.*;
import java.util.regex.*;

/** CarBridge's bounded LRC timeline; plain online text never gets invented timing. */
final class SynchronizedLyrics {
    private static final Pattern TIME = Pattern.compile("\\[(\\d{1,3}):(\\d{2})(?:[.:](\\d{1,3}))?]");
    private static final Pattern OFFSET = Pattern.compile("\\[offset:([+-]?\\d{1,7})]", Pattern.CASE_INSENSITIVE);
    static final class Line {
        final long time; final String text;
        Line(long time, String text) { this.time = time; this.text = text; }
    }
    static List<Line> parse(String text) {
        ArrayList<Line> lines = new ArrayList<>();
        if (text == null || text.length() > 65536) return lines;
        Matcher offset = OFFSET.matcher(text); long shift = offset.find() ? Long.parseLong(offset.group(1)) : 0;
        String[] rows = text.split("\\r\\n|\\n|\\r", 3001);
        for (int row = 0; row < rows.length && row < 3000; row++) {
            Matcher times = TIME.matcher(rows[row]); ArrayList<Long> values = new ArrayList<>(); int end = rows[row].length(), count = 0;
            while (count < 20 && times.find()) {
                count++; end = times.end(); long seconds = Long.parseLong(times.group(2));
                if (seconds >= 60) continue;
                String fraction = times.group(3); fraction = ((fraction == null ? "" : fraction) + "000").substring(0, 3);
                values.add(Math.max(0, Long.parseLong(times.group(1)) * 60000 + seconds * 1000 + Long.parseLong(fraction) + shift));
            }
            String body = CombinedTitleMetadata.trim(rows[row].substring(end));
            for (long value : values) lines.add(new Line(value, body));
        }
        Collections.sort(lines, Comparator.comparingLong(line -> line.time)); return lines;
    }
    static String current(List<Line> lines, long position) {
        if (position < 0) return "";
        int left = 0, right = lines.size() - 1, match = -1;
        while (left <= right) { int middle = (left + right) >>> 1;
            if (lines.get(middle).time <= position) { match = middle; left = middle + 1; } else right = middle - 1; }
        return match < 0 ? "" : lines.get(match).text;
    }
}
