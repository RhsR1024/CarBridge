package ecarx.xsf.mediacenter.session;

import android.app.PendingIntent;
import android.os.Parcel;
import android.os.Parcelable;

/* JADX INFO: compiled from: r8-map-id-3e69baf94e81d466d14d5c24482c5521bbb5f1d5dadfa8a3c310732898062d9f */
/* JADX INFO: loaded from: classes.dex */
public class PlaybackState implements Parcelable {
    public static final Parcelable.Creator<PlaybackState> CREATOR = new Parcelable.Creator<PlaybackState>() { // from class: ecarx.xsf.mediacenter.session.PlaybackState.1
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public PlaybackState createFromParcel(Parcel parcel) {
            return new PlaybackState(parcel);
        }

        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public PlaybackState[] newArray(int i) {
            return new PlaybackState[i];
        }
    };
    public static final int STATE_BUFFERING = 3;
    public static final int STATE_ERROR = 7;
    public static final int STATE_PAUSED = 0;
    public static final int STATE_PLAYING = 1;
    private long appId;
    private int errorCode;
    private String errorMessage;
    private PendingIntent errorPendingIntent;
    private String itemId;
    private int loopMode;
    private String pkgName;
    private String queueId;
    private int radioMode;
    private int sourceType;
    private int state;
    private int zoneId;

    /* JADX INFO: compiled from: r8-map-id-3e69baf94e81d466d14d5c24482c5521bbb5f1d5dadfa8a3c310732898062d9f */
    public static class Builder {
        private final PlaybackState state = new PlaybackState();

        public Builder appId(long j) {
            this.state.appId = j;
            return this;
        }

        public PlaybackState build() {
            return this.state;
        }

        public Builder errorCode(int i) {
            this.state.errorCode = i;
            return this;
        }

        public Builder errorMessage(String str) {
            this.state.errorMessage = str;
            return this;
        }

        public Builder errorPendingIntent(PendingIntent pendingIntent) {
            this.state.errorPendingIntent = pendingIntent;
            return this;
        }

        public Builder itemId(String str) {
            this.state.itemId = str;
            return this;
        }

        public Builder loopMode(int i) {
            this.state.loopMode = i;
            return this;
        }

        public Builder pkgName(String str) {
            this.state.pkgName = str;
            return this;
        }

        public Builder queueId(String str) {
            this.state.queueId = str;
            return this;
        }

        public Builder radioMode(int i) {
            this.state.radioMode = i;
            return this;
        }

        public Builder sourceType(int i) {
            this.state.sourceType = i;
            return this;
        }

        public Builder state(int i) {
            this.state.state = i;
            return this;
        }

        public Builder zoneId(int i) {
            this.state.zoneId = i;
            return this;
        }
    }

    public PlaybackState(Parcel parcel) {
        this.state = parcel.readInt();
        this.loopMode = parcel.readInt();
        this.radioMode = parcel.readInt();
        this.sourceType = parcel.readInt();
        this.pkgName = parcel.readString();
        this.zoneId = parcel.readInt();
        this.appId = parcel.readLong();
        this.queueId = parcel.readString();
        this.itemId = parcel.readString();
        this.errorCode = parcel.readInt();
        this.errorMessage = parcel.readString();
        this.errorPendingIntent = (PendingIntent) parcel.readParcelable(PendingIntent.class.getClassLoader());
    }

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.state);
        parcel.writeInt(this.loopMode);
        parcel.writeInt(this.radioMode);
        parcel.writeInt(this.sourceType);
        parcel.writeString(this.pkgName);
        parcel.writeInt(this.zoneId);
        parcel.writeLong(this.appId);
        parcel.writeString(this.queueId);
        parcel.writeString(this.itemId);
        parcel.writeInt(this.errorCode);
        parcel.writeString(this.errorMessage);
        parcel.writeParcelable(this.errorPendingIntent, i);
    }

    public PlaybackState() {
    }
}

