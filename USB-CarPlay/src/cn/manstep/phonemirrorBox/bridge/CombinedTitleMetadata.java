package cn.manstep.phonemirrorBox.bridge;

import java.util.regex.Pattern;

/** Port of CarBridge CombinedTitleMetadata. Raw box state is never rewritten. */
final class CombinedTitleMetadata {
    static final String[] FORMATS = {"ORIGINAL", "ARTIST_TITLE", "TITLE_ARTIST"};
    static final String[] LABELS = {"保留原始信息", "歌手 - 歌曲名", "歌曲名 - 歌手"};
    private static final Pattern SEPARATOR = Pattern.compile("[ \\t]+[-–—][ \\t]+");
    final String title, artist, reason;
    private CombinedTitleMetadata(String title, String artist, String reason) {
        this.title = title; this.artist = artist; this.reason = reason;
    }
    static String normalizeFormat(String format) {
        return "ARTIST_TITLE".equals(format) || "TITLE_ARTIST".equals(format) ? format : "ORIGINAL";
    }
    static int index(String format) {
        for (int i = 0; i < FORMATS.length; i++) if (FORMATS[i].equals(format)) return i;
        return 0;
    }
    static String trim(String value) {
        int start = 0, end = value.length();
        while (start < end && whitespace(value.charAt(start))) start++;
        while (end > start && whitespace(value.charAt(end - 1))) end--;
        return value.substring(start, end);
    }
    private static boolean whitespace(char value) { return Character.isWhitespace(value) || Character.isSpaceChar(value); }
    static CombinedTitleMetadata resolve(String title, String artist, String format, boolean eligible) {
        title = title == null ? "" : title; artist = artist == null ? "" : artist;
        format = normalizeFormat(format);
        String reason;
        if ("ORIGINAL".equals(format)) reason = "original_format";
        else if (!trim(artist).isEmpty()) reason = "native_artist_present";
        else if (title.isEmpty()) reason = "missing_title";
        else if (!eligible || title.length() > 512 || title.indexOf('\n') >= 0 || title.indexOf('\r') >= 0) reason = "invalid_title";
        else {
            String[] parts = SEPARATOR.split(title, -1);
            if (parts.length == 2 && !trim(parts[0]).isEmpty() && !trim(parts[1]).isEmpty()) {
                boolean artistFirst = "ARTIST_TITLE".equals(format);
                return new CombinedTitleMetadata(trim(parts[artistFirst ? 1 : 0]), trim(parts[artistFirst ? 0 : 1]), "split");
            }
            reason = "ambiguous_or_missing_separator";
        }
        return new CombinedTitleMetadata(title, artist, reason);
    }
}
