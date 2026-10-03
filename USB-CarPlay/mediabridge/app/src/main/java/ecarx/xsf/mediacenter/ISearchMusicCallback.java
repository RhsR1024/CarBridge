package ecarx.xsf.mediacenter;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

/* JADX INFO: compiled from: r8-map-id-3e69baf94e81d466d14d5c24482c5521bbb5f1d5dadfa8a3c310732898062d9f */
/* JADX INFO: loaded from: classes.dex */
public interface ISearchMusicCallback extends IInterface {
    static boolean transact(IBinder remote, int code, Parcel data, Parcel reply, int flags) {
        try { return remote.transact(code, data, reply, flags); } catch (RemoteException error) { return false; }
    }
    public static final String DESCRIPTOR = "ecarx.xsf.mediacenter.ISearchMusicCallback";

    /* JADX INFO: compiled from: r8-map-id-3e69baf94e81d466d14d5c24482c5521bbb5f1d5dadfa8a3c310732898062d9f */
    public static class Default implements ISearchMusicCallback {
        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }

        @Override // ecarx.xsf.mediacenter.ISearchMusicCallback
        public void onSearchFail(String str) {
        }

        @Override // ecarx.xsf.mediacenter.ISearchMusicCallback
        public void onSearchSuccess(String str) {
        }
    }

    /* JADX INFO: compiled from: r8-map-id-3e69baf94e81d466d14d5c24482c5521bbb5f1d5dadfa8a3c310732898062d9f */
    public static abstract class Stub extends Binder implements ISearchMusicCallback {
        static final int TRANSACTION_onSearchFail = 2;
        static final int TRANSACTION_onSearchSuccess = 1;

        /* JADX INFO: compiled from: r8-map-id-3e69baf94e81d466d14d5c24482c5521bbb5f1d5dadfa8a3c310732898062d9f */
        public static class Proxy implements ISearchMusicCallback {
            private IBinder mRemote;

            public Proxy(IBinder iBinder) {
                this.mRemote = iBinder;
            }

            @Override // android.os.IInterface
            public IBinder asBinder() {
                return this.mRemote;
            }

            public String getInterfaceDescriptor() {
                return ISearchMusicCallback.DESCRIPTOR;
            }

            @Override // ecarx.xsf.mediacenter.ISearchMusicCallback
            public void onSearchFail(String str) {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(ISearchMusicCallback.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    ISearchMusicCallback.transact(this.mRemote, 2, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override // ecarx.xsf.mediacenter.ISearchMusicCallback
            public void onSearchSuccess(String str) {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(ISearchMusicCallback.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    ISearchMusicCallback.transact(this.mRemote, 1, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }
        }

        public Stub() {
            attachInterface(this, ISearchMusicCallback.DESCRIPTOR);
        }

        public static ISearchMusicCallback asInterface(IBinder iBinder) {
            if (iBinder == null) {
                return null;
            }
            IInterface iInterfaceQueryLocalInterface = iBinder.queryLocalInterface(ISearchMusicCallback.DESCRIPTOR);
            return (iInterfaceQueryLocalInterface == null || !(iInterfaceQueryLocalInterface instanceof ISearchMusicCallback)) ? new Proxy(iBinder) : (ISearchMusicCallback) iInterfaceQueryLocalInterface;
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return this;
        }

        @Override // android.os.Binder
        public boolean onTransact(int i, Parcel parcel, Parcel parcel2, int i2) throws RemoteException {
            if (i >= 1 && i <= 16777215) {
                parcel.enforceInterface(ISearchMusicCallback.DESCRIPTOR);
            }
            if (i == 1598968902) {
                parcel2.writeString(ISearchMusicCallback.DESCRIPTOR);
                return true;
            }
            if (i == 1) {
                onSearchSuccess(parcel.readString());
                parcel2.writeNoException();
            } else {
                if (i != 2) {
                    return super.onTransact(i, parcel, parcel2, i2);
                }
                onSearchFail(parcel.readString());
                parcel2.writeNoException();
            }
            return true;
        }
    }

    void onSearchFail(String str);

    void onSearchSuccess(String str);
}



