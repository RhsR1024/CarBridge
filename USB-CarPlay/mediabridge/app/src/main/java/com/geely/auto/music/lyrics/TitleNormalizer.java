package com.geely.auto.music.lyrics;

import java.util.Locale;
import java.util.regex.Pattern;

/* JADX INFO: loaded from: classes.dex */
public final class TitleNormalizer {
    private static final Pattern BRACKETS = Pattern.compile("[\\(\\[\\{（【].*?[\\)\\]\\}）】]");
    private static final Pattern FEAT = Pattern.compile("(?i)\\s*(feat\\.?|ft\\.?|with)\\s+.+");
    private static final Pattern NOISE = Pattern.compile("(?i)\\b(official\\s*(audio|video|mv|music\\s*video)?|official|hd|hq|radio\\s*edit|(single|album)\\s*version|explicit|clean|\\d{4}\\s*remaster(ed)?|live|karaoke|instrumental)\\b");
    private static final Pattern MULTI_SPACE = Pattern.compile("\\s+");
    private static final Pattern PUNCT = Pattern.compile("[,.:;!?'\"\\-—–]+");

    private TitleNormalizer() {
    }

    public static String clean(String str) {
        if (str == null) {
            return "";
        }
        String strTrim = str.trim();
        if (strTrim.isEmpty()) {
            return "";
        }
        return MULTI_SPACE.matcher(PUNCT.matcher(NOISE.matcher(FEAT.matcher(BRACKETS.matcher(strTrim).replaceAll(" ")).replaceAll(" ")).replaceAll(" ")).replaceAll(" ")).replaceAll(" ").trim();
    }

    public static String normKey(String str) {
        return clean(str).toLowerCase(Locale.ROOT);
    }

    public static int scoreMatch(String str, String str2, String str3, String str4) {
        String strNormKey = normKey(str);
        String strNormKey2 = normKey(str2);
        String strNormKey3 = normKey(str3);
        String strNormKey4 = normKey(str4);
        int i = 0;
        if (!strNormKey.isEmpty() && !strNormKey3.isEmpty()) {
            if (strNormKey.equals(strNormKey3)) {
                i = 100;
            } else {
                if (!strNormKey3.contains(strNormKey) && !strNormKey.contains(strNormKey3)) {
                    return 0;
                }
                i = 50;
            }
        }
        if (!strNormKey2.isEmpty() && !strNormKey4.isEmpty()) {
            if (strNormKey2.equals(strNormKey4)) {
                return i + 80;
            }
            return (strNormKey4.contains(strNormKey2) || strNormKey2.contains(strNormKey4)) ? i + 40 : i;
        }
        return i;
    }
}
