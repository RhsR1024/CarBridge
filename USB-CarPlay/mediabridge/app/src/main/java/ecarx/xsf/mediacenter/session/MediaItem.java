package ecarx.xsf.mediacenter.session;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

/* JADX INFO: compiled from: r8-map-id-3e69baf94e81d466d14d5c24482c5521bbb5f1d5dadfa8a3c310732898062d9f */
/* JADX INFO: loaded from: classes.dex */
public class MediaItem implements Parcelable {
    public static final Parcelable.Creator<MediaItem> CREATOR = new Parcelable.Creator<MediaItem>() { // from class: ecarx.xsf.mediacenter.session.MediaItem.1
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public MediaItem createFromParcel(Parcel parcel) {
            return new MediaItem(parcel);
        }

        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public MediaItem[] newArray(int i) {
            return new MediaItem[i];
        }
    };
    private String album;
    private String artist;
    private Uri artwork;
    private Uri artworkPath;
    private long dateTime;
    private boolean downloadSupported;
    private boolean downloaded;
    private long duration;
    private boolean favoriteSupported;
    private boolean favorited;
    private String id;
    private boolean loopModeSupported;
    private String lyricContent;
    private Uri lyricUri;
    private int mediaType;
    private String radioBand;
    private String radioFrequency;
    private String radioName;
    private int sourceType;
    private String subtitle;
    private String title;
    private boolean vipNeeded;

    /* JADX INFO: compiled from: r8-map-id-3e69baf94e81d466d14d5c24482c5521bbb5f1d5dadfa8a3c310732898062d9f */
    public static class Builder {
        private final MediaItem item = new MediaItem();

        public Builder album(String str) {
            this.item.album = str;
            return this;
        }

        public Builder artist(String str) {
            this.item.artist = str;
            return this;
        }

        public Builder artwork(Uri uri) {
            this.item.artwork = uri;
            return this;
        }

        public Builder artworkPath(Uri uri) {
            this.item.artworkPath = uri;
            return this;
        }

        public MediaItem build() {
            return this.item;
        }

        public Builder dateTime(long j) {
            this.item.dateTime = j;
            return this;
        }

        public Builder downloadSupported(boolean z) {
            this.item.downloadSupported = z;
            return this;
        }

        public Builder downloaded(boolean z) {
            this.item.downloaded = z;
            return this;
        }

        public Builder duration(long j) {
            this.item.duration = j;
            return this;
        }

        public Builder favoriteSupported(boolean z) {
            this.item.favoriteSupported = z;
            return this;
        }

        public Builder favorited(boolean z) {
            this.item.favorited = z;
            return this;
        }

        public Builder id(String str) {
            this.item.id = str;
            return this;
        }

        public Builder loopModeSupported(boolean z) {
            this.item.loopModeSupported = z;
            return this;
        }

        public Builder lyricContent(String str) {
            this.item.lyricContent = str;
            return this;
        }

        public Builder lyricUri(Uri uri) {
            this.item.lyricUri = uri;
            return this;
        }

        public Builder mediaType(int i) {
            this.item.mediaType = i;
            return this;
        }

        public Builder radioBand(String str) {
            this.item.radioBand = str;
            return this;
        }

        public Builder radioFrequency(String str) {
            this.item.radioFrequency = str;
            return this;
        }

        public Builder radioName(String str) {
            this.item.radioName = str;
            return this;
        }

        public Builder sourceType(int i) {
            this.item.sourceType = i;
            return this;
        }

        public Builder subtitle(String str) {
            this.item.subtitle = str;
            return this;
        }

        public Builder title(String str) {
            this.item.title = str;
            return this;
        }

        public Builder vipNeeded(boolean z) {
            this.item.vipNeeded = z;
            return this;
        }
    }

    public MediaItem(Parcel parcel) {
        this.id = parcel.readString();
        this.sourceType = parcel.readInt();
        this.title = parcel.readString();
        this.subtitle = parcel.readString();
        this.album = parcel.readString();
        this.artist = parcel.readString();
        this.artwork = (Uri) parcel.readParcelable(Uri.class.getClassLoader());
        this.lyricUri = (Uri) parcel.readParcelable(Uri.class.getClassLoader());
        this.lyricContent = parcel.readString();
        this.dateTime = parcel.readLong();
        this.duration = parcel.readLong();
        this.favoriteSupported = parcel.readByte() != 0;
        this.downloadSupported = parcel.readByte() != 0;
        this.vipNeeded = parcel.readByte() != 0;
        this.downloaded = parcel.readByte() != 0;
        this.favorited = parcel.readByte() != 0;
        this.radioFrequency = parcel.readString();
        this.radioName = parcel.readString();
        this.radioBand = parcel.readString();
        this.artworkPath = (Uri) parcel.readParcelable(Uri.class.getClassLoader());
        this.loopModeSupported = parcel.readByte() != 0;
        this.mediaType = parcel.readInt();
    }

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(this.id);
        parcel.writeInt(this.sourceType);
        parcel.writeString(this.title);
        parcel.writeString(this.subtitle);
        parcel.writeString(this.album);
        parcel.writeString(this.artist);
        parcel.writeParcelable(this.artwork, i);
        parcel.writeParcelable(this.lyricUri, i);
        parcel.writeString(this.lyricContent);
        parcel.writeLong(this.dateTime);
        parcel.writeLong(this.duration);
        parcel.writeByte(this.favoriteSupported ? (byte) 1 : (byte) 0);
        parcel.writeByte(this.downloadSupported ? (byte) 1 : (byte) 0);
        parcel.writeByte(this.vipNeeded ? (byte) 1 : (byte) 0);
        parcel.writeByte(this.downloaded ? (byte) 1 : (byte) 0);
        parcel.writeByte(this.favorited ? (byte) 1 : (byte) 0);
        parcel.writeString(this.radioFrequency);
        parcel.writeString(this.radioName);
        parcel.writeString(this.radioBand);
        parcel.writeParcelable(this.artworkPath, i);
        parcel.writeByte(this.loopModeSupported ? (byte) 1 : (byte) 0);
        parcel.writeInt(this.mediaType);
    }

    public MediaItem() {
    }
}

