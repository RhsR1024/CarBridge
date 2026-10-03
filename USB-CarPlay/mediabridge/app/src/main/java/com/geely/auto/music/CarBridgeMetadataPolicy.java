package com.geely.auto.music;

import io.github.rhsr1024.interop.BridgeProtocol;
import java.text.Normalizer;
import java.util.Locale;

/** CarBridge publishes its own MediaSession; its connection notification is not song data. */
public final class CarBridgeMetadataPolicy {
    private CarBridgeMetadataPolicy() {}

    public static boolean allowsLookup(String pkg, String title, String artist, long durationMs) {
        if (!BridgeProtocol.managed(pkg)) return true;
        return !normalize(title).isEmpty() && !normalize(artist).isEmpty() && durationMs > 0
                && !"CarPlay 已连接".equals(artist);
    }

    static boolean matches(String title, String artist, long durationMs,
                           String candidateTitle, String candidateArtist, long candidateDurationMs) {
        return !normalize(title).isEmpty() && !normalize(artist).isEmpty()
                && normalize(title).equals(normalize(candidateTitle))
                && normalize(artist).equals(normalize(candidateArtist))
                && durationMs > 0 && candidateDurationMs > 0
                && Math.abs((double) durationMs - candidateDurationMs) <= 3000;
    }

    private static String normalize(String value) {
        if (value == null) return "";
        return Normalizer.normalize(value, Normalizer.Form.NFKC).toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{L}\\p{N}]", "");
    }
}
