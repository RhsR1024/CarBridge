package com.geely.auto.music;

/** A favorite response may refresh metadata without changing the song the driver sees. */
final class FavoriteTrackIdentity {
    private FavoriteTrackIdentity() {}

    static boolean sameSong(String expectedTrack, String expectedTitle, String expectedArtist,
                            String actualTrack, String actualTitle, String actualArtist) {
        if (expectedTrack != null && expectedTrack.equals(actualTrack)) return true;
        return present(expectedTitle) && present(expectedArtist)
                && expectedTitle.equals(actualTitle) && expectedArtist.equals(actualArtist);
    }

    private static boolean present(String value) { return value != null && !value.trim().isEmpty(); }
}
