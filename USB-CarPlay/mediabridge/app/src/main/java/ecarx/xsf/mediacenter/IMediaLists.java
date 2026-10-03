package ecarx.xsf.mediacenter;

import android.os.Parcel;
import android.os.Parcelable;

/* JADX INFO: compiled from: r8-map-id-3e69baf94e81d466d14d5c24482c5521bbb5f1d5dadfa8a3c310732898062d9f */
/* JADX INFO: loaded from: classes.dex */
public class IMediaLists implements Parcelable {
    public static final Parcelable.Creator<IMediaLists> CREATOR = new Parcelable.Creator<IMediaLists>() { // from class: ecarx.xsf.mediacenter.IMediaLists.1
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public IMediaLists createFromParcel(Parcel parcel) {
            return new IMediaLists(parcel);
        }

        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public IMediaLists[] newArray(int i) {
            return new IMediaLists[i];
        }
    };

    public IMediaLists() {
    }

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel parcel, int i) {
    }

    public IMediaLists(Parcel parcel) {
    }
}

