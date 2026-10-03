package ecarx.xsf.mediacenter.session;

import android.app.PendingIntent;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

/* JADX INFO: compiled from: r8-map-id-3e69baf94e81d466d14d5c24482c5521bbb5f1d5dadfa8a3c310732898062d9f */
/* JADX INFO: loaded from: classes.dex */
public class PlaybackInfo implements Parcelable {
    public static final Parcelable.Creator<PlaybackInfo> CREATOR = new Parcelable.Creator<PlaybackInfo>() { // from class: ecarx.xsf.mediacenter.session.PlaybackInfo.1
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public PlaybackInfo createFromParcel(Parcel parcel) {
            return new PlaybackInfo(parcel);
        }

        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public PlaybackInfo[] newArray(int i) {
            return new PlaybackInfo[i];
        }
    };
    private long appId;
    private String appName;
    private Uri iconUri;
    private long initialProgress;
    private PendingIntent launchIntent;
    private MediaItem mediaItem;
    private String pkgName;
    private PendingIntent playerIntent;
    private int zoneId;

    /* JADX INFO: compiled from: r8-map-id-3e69baf94e81d466d14d5c24482c5521bbb5f1d5dadfa8a3c310732898062d9f */
    public static class Builder {
        private final PlaybackInfo info = new PlaybackInfo();

        public Builder appId(long j) {
            this.info.appId = j;
            return this;
        }

        public Builder appName(String str) {
            this.info.appName = str;
            return this;
        }

        public PlaybackInfo build() {
            return this.info;
        }

        public Builder iconUri(Uri uri) {
            this.info.iconUri = uri;
            return this;
        }

        public Builder initialProgress(long j) {
            this.info.initialProgress = j;
            return this;
        }

        public Builder launchIntent(PendingIntent pendingIntent) {
            this.info.launchIntent = pendingIntent;
            return this;
        }

        public Builder mediaItem(MediaItem mediaItem) {
            this.info.mediaItem = mediaItem;
            return this;
        }

        public Builder pkgName(String str) {
            this.info.pkgName = str;
            return this;
        }

        public Builder playerIntent(PendingIntent pendingIntent) {
            this.info.playerIntent = pendingIntent;
            return this;
        }

        public Builder zoneId(int i) {
            this.info.zoneId = i;
            return this;
        }
    }

    public PlaybackInfo(Parcel parcel) {
        this.pkgName = parcel.readString();
        this.iconUri = (Uri) parcel.readParcelable(Uri.class.getClassLoader());
        this.appName = parcel.readString();
        this.launchIntent = (PendingIntent) parcel.readParcelable(PendingIntent.class.getClassLoader());
        this.playerIntent = (PendingIntent) parcel.readParcelable(PendingIntent.class.getClassLoader());
        this.initialProgress = parcel.readLong();
        this.mediaItem = (MediaItem) parcel.readParcelable(MediaItem.class.getClassLoader());
        this.zoneId = parcel.readInt();
        this.appId = parcel.readLong();
    }

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(this.pkgName);
        parcel.writeParcelable(this.iconUri, i);
        parcel.writeString(this.appName);
        parcel.writeParcelable(this.launchIntent, i);
        parcel.writeParcelable(this.playerIntent, i);
        parcel.writeLong(this.initialProgress);
        parcel.writeParcelable(this.mediaItem, i);
        parcel.writeInt(this.zoneId);
        parcel.writeLong(this.appId);
    }

    public PlaybackInfo() {
    }
}

