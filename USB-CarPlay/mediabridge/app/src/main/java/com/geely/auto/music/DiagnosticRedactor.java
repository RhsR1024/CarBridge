package com.geely.auto.music;

public final class DiagnosticRedactor {
    private DiagnosticRedactor() {}
    public static String redact(String value, boolean includeTracks) {
        if (value == null) return "";
        String result = value.replaceAll("(?im)(authorization|cookie|set-cookie)\\s*[:=][^\\r\\n]*", "$1=[已隐藏]")
                .replaceAll("(?i)(?:https?|content|file)://[^\\s]+", "[URI 已隐藏]")
                .replaceAll("(?i)(token|cookie|authorization|vin|serial|android_id)\\s*[:=]\\s*[^\\s,;]+", "$1=[已隐藏]");
        if (!includeTracks) result = result.replaceAll("(?im)(title|artist|album|query|track|lyrics|歌名|歌手|专辑)\\s*[:=].*$", "$1=[已隐藏]");
        return result;
    }
}
