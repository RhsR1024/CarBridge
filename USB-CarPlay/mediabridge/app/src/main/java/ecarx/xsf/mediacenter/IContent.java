package ecarx.xsf.mediacenter;

import android.os.Parcel;
import android.os.Parcelable;

/* JADX INFO: compiled from: r8-map-id-3e69baf94e81d466d14d5c24482c5521bbb5f1d5dadfa8a3c310732898062d9f */
/* JADX INFO: loaded from: classes.dex */
public class IContent implements Parcelable {
    public static final Parcelable.Creator<IContent> CREATOR = new Parcelable.Creator<IContent>() { // from class: ecarx.xsf.mediacenter.IContent.1
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public IContent createFromParcel(Parcel parcel) {
            return new IContent(parcel);
        }

        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public IContent[] newArray(int i) {
            return new IContent[i];
        }
    };

    public IContent() {
    }

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel parcel, int i) {
    }

    public IContent(Parcel parcel) {
    }
}

