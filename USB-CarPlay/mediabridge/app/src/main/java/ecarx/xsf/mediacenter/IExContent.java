package ecarx.xsf.mediacenter;

import android.app.PendingIntent;
import android.os.Parcel;
import android.os.Parcelable;
import java.util.List;

/* JADX INFO: compiled from: r8-map-id-3e69baf94e81d466d14d5c24482c5521bbb5f1d5dadfa8a3c310732898062d9f */
/* JADX INFO: loaded from: classes.dex */
public class IExContent implements Parcelable {
    public static final Parcelable.Creator<IExContent> CREATOR = new Parcelable.Creator<IExContent>() { // from class: ecarx.xsf.mediacenter.IExContent.1
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public IExContent createFromParcel(Parcel parcel) {
            return new IExContent(parcel);
        }

        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public IExContent[] newArray(int i) {
            return new IExContent[i];
        }
    };
    private String data;
    private List<PendingIntent> pendingIntents;

    public IExContent(Parcel parcel) {
        this.data = parcel.readString();
        this.pendingIntents = parcel.createTypedArrayList(PendingIntent.CREATOR);
    }

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }

    public String getData() {
        return this.data;
    }

    public List<PendingIntent> getPendingIntents() {
        return this.pendingIntents;
    }

    public void setData(String str) {
        this.data = str;
    }

    public void setPendingIntents(List<PendingIntent> list) {
        this.pendingIntents = list;
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(this.data);
        parcel.writeTypedList(this.pendingIntents);
    }

    public IExContent() {
    }
}

