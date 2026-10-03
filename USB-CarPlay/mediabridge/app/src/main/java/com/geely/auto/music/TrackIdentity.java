package com.geely.auto.music;

/** Length-prefixed fields avoid delimiter collisions and preserve exact duration/media identity. */
public final class TrackIdentity {
    private TrackIdentity() {}
    public static String of(String session, String mediaId, String title, String artist, String album, long duration) {
        StringBuilder key = new StringBuilder();
        for (String value : new String[]{session, mediaId, title, artist, album}) {
            String part = value == null ? "" : value;
            key.append(part.length()).append(':').append(part);
        }
        return key.append(':').append(duration).toString();
    }
}
