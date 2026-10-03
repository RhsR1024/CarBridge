package cn.manstep.phonemirrorBox.bridge;

/** Incremental box metadata, independent of Android focus and UI state. */
public final class TrackState {
    public String title = "", artist = "", album = "", lyrics = "";
    public int status = -1;
    public long revision;
    String titleFormat = "ORIGINAL", artworkUri = "";
    long durationMs, positionMs = -1, positionAtMs;
    private boolean titleFormatEligible = true;
    private String lastTitle = "", lastArtist = "";
    private boolean receivedTrack, awaitingTitle;

    public void reset() {
        title = artist = album = lyrics = lastTitle = lastArtist = "";
        receivedTrack = awaitingTitle = false;
        artworkUri = ""; durationMs = positionAtMs = 0; positionMs = -1; titleFormatEligible = true;
        status = -1;
        revision++;
    }

    public void update(String nextTitle, String nextArtist, String nextAlbum, Integer nextStatus) {
        if (nextTitle != null) {
            titleFormatEligible = nextTitle.length() <= 512 && nextTitle.indexOf('\n') < 0 && nextTitle.indexOf('\r') < 0;
            nextTitle = bounded(nextTitle);
        }
        if (nextArtist != null) nextArtist = bounded(nextArtist);
        // A lyrics-over-Bluetooth update can put the song name in the artist slot.
        // Do not invent a reversed mapping, or combine a new artist with the old song.
        boolean shiftedArtist = nextArtist != null && !lastTitle.isEmpty() && nextArtist.equals(lastTitle);
        boolean artistChanged = nextArtist != null && !nextArtist.isEmpty() && !lastArtist.isEmpty()
                && !nextArtist.equals(lastArtist);
        // A complete pair in one box message also identifies a different recording
        // with the same title. Artist-only fragments cannot establish that boundary.
        boolean completePair = nextTitle != null && !nextTitle.isEmpty() && nextArtist != null
                && !nextArtist.isEmpty() && !nextTitle.equals(nextArtist);
        boolean newTitle = nextTitle != null && (!nextTitle.equals(lastTitle)
                || completePair && (artistChanged || awaitingTitle));
        if (!newTitle && shiftedArtist) nextArtist = null;
        else if (!newTitle && artistChanged) {
            if (!awaitingTitle) { revision++; lyrics = ""; }
            awaitingTitle = true; title = artist = album = "";
            nextArtist = null;
        }
        if (nextTitle != null) {
            if (newTitle) {
                // A new track must never retain the previous track's artist/album.
                artist = album = lyrics = lastArtist = "";
                awaitingTitle = false;
                revision++;
            }
            lastTitle = nextTitle;
            if (!awaitingTitle) title = nextTitle;
            receivedTrack |= !nextTitle.isEmpty();
        }
        if (!awaitingTitle) {
            if (nextArtist != null && !nextArtist.equals(title)) artist = lastArtist = nextArtist;
            if (nextAlbum != null) album = bounded(nextAlbum);
        }
        if (nextStatus != null && nextStatus >= 0 && nextStatus <= 2) status = nextStatus;
    }

    public void updateLyrics(String value) {
        if (value != null && !awaitingTitle) lyrics = value.length() <= 32768 ? value : value.substring(0, 32768);
    }

    public void pause() { status = 2; }
    public boolean playing() { return status == 1; }
    public boolean hasTrack() { return receivedTrack; }
    public boolean complete() { return !title.isEmpty() && !artist.isEmpty(); }
    CombinedTitleMetadata display() { return CombinedTitleMetadata.resolve(title, artist, titleFormat, titleFormatEligible); }
    public String mediaId() { return hasTrack() ? "usb-track:" + revision + ":" + title + "\n" + artist + "\n" + album : ""; }
    private static String bounded(String value) {
        value = value.trim();
        return value.length() <= 512 ? value : value.substring(0, 512);
    }
}
