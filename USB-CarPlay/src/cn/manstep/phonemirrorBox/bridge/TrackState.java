package cn.manstep.phonemirrorBox.bridge;

/** Incremental box metadata, independent of Android focus and UI state. */
public final class TrackState {
    public String title = "", artist = "", album = "";
    public int status = -1;
    public long revision;

    public void reset() {
        title = artist = album = "";
        status = -1;
        revision++;
    }

    public void update(String nextTitle, String nextArtist, String nextAlbum, Integer nextStatus) {
        if (nextTitle != null) {
            nextTitle = bounded(nextTitle);
            if (!nextTitle.equals(title)) {
                // A new track must never retain the previous track's artist/album.
                artist = album = "";
                revision++;
            }
            title = nextTitle;
        }
        if (nextArtist != null) artist = bounded(nextArtist);
        if (nextAlbum != null) album = bounded(nextAlbum);
        if (nextStatus != null && nextStatus >= 0 && nextStatus <= 2) status = nextStatus;
    }

    public void pause() { status = 2; }
    public boolean playing() { return status == 1; }
    public boolean hasTrack() { return !title.isEmpty() || !artist.isEmpty() || !album.isEmpty(); }
    public String mediaId() { return hasTrack() ? title + "\n" + artist + "\n" + album : ""; }
    private static String bounded(String value) {
        value = value.trim();
        return value.length() <= 512 ? value : value.substring(0, 512);
    }
}
