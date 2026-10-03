package ecarx.xsf.mediacenter.session;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import ecarx.xsf.mediacenter.ISearchMusicCallback;

/* JADX INFO: compiled from: r8-map-id-3e69baf94e81d466d14d5c24482c5521bbb5f1d5dadfa8a3c310732898062d9f */
/* JADX INFO: loaded from: classes.dex */
public interface IEasSessionCallback extends IInterface {
    static boolean transact(IBinder remote, int code, Parcel data, Parcel reply, int flags) {
        try { return remote.transact(code, data, reply, flags); } catch (RemoteException error) { return false; }
    }
    public static final String DESCRIPTOR = "ecarx.xsf.mediacenter.session.IEasSessionCallback";

    /* JADX INFO: compiled from: r8-map-id-3e69baf94e81d466d14d5c24482c5521bbb5f1d5dadfa8a3c310732898062d9f */
    public static class Default implements IEasSessionCallback {
        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }

        @Override // ecarx.xsf.mediacenter.session.IEasSessionCallback
        public void onDownload(String str, String str2, boolean z) {
        }

        @Override // ecarx.xsf.mediacenter.session.IEasSessionCallback
        public void onFastForward() {
        }

        @Override // ecarx.xsf.mediacenter.session.IEasSessionCallback
        public void onFavorite(String str, String str2, boolean z) {
        }

        @Override // ecarx.xsf.mediacenter.session.IEasSessionCallback
        public void onLoopModeChanged(int i) {
        }

        @Override // ecarx.xsf.mediacenter.session.IEasSessionCallback
        public void onMediaEffectChange(int i) {
        }

        @Override // ecarx.xsf.mediacenter.session.IEasSessionCallback
        public void onMediaQualityChange(int i) {
        }

        @Override // ecarx.xsf.mediacenter.session.IEasSessionCallback
        public void onMediaSourceTypeChanged(int i) {
        }

        @Override // ecarx.xsf.mediacenter.session.IEasSessionCallback
        public void onMediaSourceTypeChangedWithZone(int i, String str, int i2) {
        }

        @Override // ecarx.xsf.mediacenter.session.IEasSessionCallback
        public void onNext() {
        }

        @Override // ecarx.xsf.mediacenter.session.IEasSessionCallback
        public void onPause() {
        }

        @Override // ecarx.xsf.mediacenter.session.IEasSessionCallback
        public void onPlay() {
        }

        @Override // ecarx.xsf.mediacenter.session.IEasSessionCallback
        public void onPrevious() {
        }

        @Override // ecarx.xsf.mediacenter.session.IEasSessionCallback
        public void onRadioModeChanged(int i) {
        }

        @Override // ecarx.xsf.mediacenter.session.IEasSessionCallback
        public void onRewind() {
        }

        @Override // ecarx.xsf.mediacenter.session.IEasSessionCallback
        public void onSearchMusic(String str, String str2, String str3, int i, boolean z, boolean z2, ISearchMusicCallback iSearchMusicCallback) {
        }

        @Override // ecarx.xsf.mediacenter.session.IEasSessionCallback
        public void onSeekTo(long j) {
        }

        @Override // ecarx.xsf.mediacenter.session.IEasSessionCallback
        public void onSelect(String str, String str2, boolean z) {
        }

        @Override // ecarx.xsf.mediacenter.session.IEasSessionCallback
        public void onSelectFrom(String str, String str2, boolean z, String str3) {
        }

        @Override // ecarx.xsf.mediacenter.session.IEasSessionCallback
        public void onStop() {
        }

        @Override // ecarx.xsf.mediacenter.session.IEasSessionCallback
        public void operationType(int i) {
        }
    }

    /* JADX INFO: compiled from: r8-map-id-3e69baf94e81d466d14d5c24482c5521bbb5f1d5dadfa8a3c310732898062d9f */
    public static abstract class Stub extends Binder implements IEasSessionCallback {
        static final int TRANSACTION_onDownload = 1;
        static final int TRANSACTION_onFastForward = 2;
        static final int TRANSACTION_onFavorite = 3;
        static final int TRANSACTION_onLoopModeChanged = 4;
        static final int TRANSACTION_onMediaEffectChange = 18;
        static final int TRANSACTION_onMediaQualityChange = 17;
        static final int TRANSACTION_onMediaSourceTypeChanged = 5;
        static final int TRANSACTION_onMediaSourceTypeChangedWithZone = 19;
        static final int TRANSACTION_onNext = 6;
        static final int TRANSACTION_onPause = 7;
        static final int TRANSACTION_onPlay = 8;
        static final int TRANSACTION_onPrevious = 9;
        static final int TRANSACTION_onRadioModeChanged = 10;
        static final int TRANSACTION_onRewind = 11;
        static final int TRANSACTION_onSearchMusic = 20;
        static final int TRANSACTION_onSeekTo = 12;
        static final int TRANSACTION_onSelect = 13;
        static final int TRANSACTION_onSelectFrom = 16;
        static final int TRANSACTION_onStop = 14;
        static final int TRANSACTION_operationType = 15;

        /* JADX INFO: compiled from: r8-map-id-3e69baf94e81d466d14d5c24482c5521bbb5f1d5dadfa8a3c310732898062d9f */
        public static class Proxy implements IEasSessionCallback {
            private IBinder mRemote;

            public Proxy(IBinder iBinder) {
                this.mRemote = iBinder;
            }

            @Override // android.os.IInterface
            public IBinder asBinder() {
                return this.mRemote;
            }

            public String getInterfaceDescriptor() {
                return IEasSessionCallback.DESCRIPTOR;
            }

            @Override // ecarx.xsf.mediacenter.session.IEasSessionCallback
            public void onDownload(String str, String str2, boolean z) {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(IEasSessionCallback.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeString(str2);
                    parcelObtain.writeInt(z ? 1 : 0);
                    IEasSessionCallback.transact(this.mRemote, 1, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override // ecarx.xsf.mediacenter.session.IEasSessionCallback
            public void onFastForward() {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(IEasSessionCallback.DESCRIPTOR);
                    IEasSessionCallback.transact(this.mRemote, 2, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override // ecarx.xsf.mediacenter.session.IEasSessionCallback
            public void onFavorite(String str, String str2, boolean z) {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(IEasSessionCallback.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeString(str2);
                    parcelObtain.writeInt(z ? 1 : 0);
                    IEasSessionCallback.transact(this.mRemote, 3, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override // ecarx.xsf.mediacenter.session.IEasSessionCallback
            public void onLoopModeChanged(int i) {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(IEasSessionCallback.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    IEasSessionCallback.transact(this.mRemote, 4, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override // ecarx.xsf.mediacenter.session.IEasSessionCallback
            public void onMediaEffectChange(int i) {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(IEasSessionCallback.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    IEasSessionCallback.transact(this.mRemote, Stub.TRANSACTION_onMediaEffectChange, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override // ecarx.xsf.mediacenter.session.IEasSessionCallback
            public void onMediaQualityChange(int i) {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(IEasSessionCallback.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    IEasSessionCallback.transact(this.mRemote, Stub.TRANSACTION_onMediaQualityChange, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override // ecarx.xsf.mediacenter.session.IEasSessionCallback
            public void onMediaSourceTypeChanged(int i) {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(IEasSessionCallback.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    IEasSessionCallback.transact(this.mRemote, 5, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override // ecarx.xsf.mediacenter.session.IEasSessionCallback
            public void onMediaSourceTypeChangedWithZone(int i, String str, int i2) {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(IEasSessionCallback.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(i2);
                    IEasSessionCallback.transact(this.mRemote, Stub.TRANSACTION_onMediaSourceTypeChangedWithZone, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override // ecarx.xsf.mediacenter.session.IEasSessionCallback
            public void onNext() {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(IEasSessionCallback.DESCRIPTOR);
                    IEasSessionCallback.transact(this.mRemote, 6, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override // ecarx.xsf.mediacenter.session.IEasSessionCallback
            public void onPause() {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(IEasSessionCallback.DESCRIPTOR);
                    IEasSessionCallback.transact(this.mRemote, 7, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override // ecarx.xsf.mediacenter.session.IEasSessionCallback
            public void onPlay() {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(IEasSessionCallback.DESCRIPTOR);
                    IEasSessionCallback.transact(this.mRemote, Stub.TRANSACTION_onPlay, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override // ecarx.xsf.mediacenter.session.IEasSessionCallback
            public void onPrevious() {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(IEasSessionCallback.DESCRIPTOR);
                    IEasSessionCallback.transact(this.mRemote, Stub.TRANSACTION_onPrevious, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override // ecarx.xsf.mediacenter.session.IEasSessionCallback
            public void onRadioModeChanged(int i) {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(IEasSessionCallback.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    IEasSessionCallback.transact(this.mRemote, Stub.TRANSACTION_onRadioModeChanged, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override // ecarx.xsf.mediacenter.session.IEasSessionCallback
            public void onRewind() {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(IEasSessionCallback.DESCRIPTOR);
                    IEasSessionCallback.transact(this.mRemote, Stub.TRANSACTION_onRewind, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override // ecarx.xsf.mediacenter.session.IEasSessionCallback
            public void onSearchMusic(String str, String str2, String str3, int i, boolean z, boolean z2, ISearchMusicCallback iSearchMusicCallback) {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(IEasSessionCallback.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeString(str2);
                    parcelObtain.writeString(str3);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(z ? 1 : 0);
                    parcelObtain.writeInt(z2 ? 1 : 0);
                    parcelObtain.writeStrongInterface(iSearchMusicCallback);
                    IEasSessionCallback.transact(this.mRemote, Stub.TRANSACTION_onSearchMusic, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override // ecarx.xsf.mediacenter.session.IEasSessionCallback
            public void onSeekTo(long j) {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(IEasSessionCallback.DESCRIPTOR);
                    parcelObtain.writeLong(j);
                    IEasSessionCallback.transact(this.mRemote, Stub.TRANSACTION_onSeekTo, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override // ecarx.xsf.mediacenter.session.IEasSessionCallback
            public void onSelect(String str, String str2, boolean z) {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(IEasSessionCallback.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeString(str2);
                    parcelObtain.writeInt(z ? 1 : 0);
                    IEasSessionCallback.transact(this.mRemote, Stub.TRANSACTION_onSelect, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override // ecarx.xsf.mediacenter.session.IEasSessionCallback
            public void onSelectFrom(String str, String str2, boolean z, String str3) {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(IEasSessionCallback.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeString(str2);
                    parcelObtain.writeInt(z ? 1 : 0);
                    parcelObtain.writeString(str3);
                    IEasSessionCallback.transact(this.mRemote, Stub.TRANSACTION_onSelectFrom, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override // ecarx.xsf.mediacenter.session.IEasSessionCallback
            public void onStop() {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(IEasSessionCallback.DESCRIPTOR);
                    IEasSessionCallback.transact(this.mRemote, Stub.TRANSACTION_onStop, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override // ecarx.xsf.mediacenter.session.IEasSessionCallback
            public void operationType(int i) {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(IEasSessionCallback.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    IEasSessionCallback.transact(this.mRemote, Stub.TRANSACTION_operationType, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }
        }

        public Stub() {
            attachInterface(this, IEasSessionCallback.DESCRIPTOR);
        }

        public static IEasSessionCallback asInterface(IBinder iBinder) {
            if (iBinder == null) {
                return null;
            }
            IInterface iInterfaceQueryLocalInterface = iBinder.queryLocalInterface(IEasSessionCallback.DESCRIPTOR);
            return (iInterfaceQueryLocalInterface == null || !(iInterfaceQueryLocalInterface instanceof IEasSessionCallback)) ? new Proxy(iBinder) : (IEasSessionCallback) iInterfaceQueryLocalInterface;
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return this;
        }

        @Override // android.os.Binder
        public boolean onTransact(int i, Parcel parcel, Parcel parcel2, int i2) throws RemoteException {
            if (i >= 1 && i <= 16777215) {
                parcel.enforceInterface(IEasSessionCallback.DESCRIPTOR);
            }
            if (i == 1598968902) {
                parcel2.writeString(IEasSessionCallback.DESCRIPTOR);
                return true;
            }
            switch (i) {
                case 1:
                    onDownload(parcel.readString(), parcel.readString(), parcel.readInt() != 0);
                    parcel2.writeNoException();
                    return true;
                case 2:
                    onFastForward();
                    parcel2.writeNoException();
                    return true;
                case 3:
                    onFavorite(parcel.readString(), parcel.readString(), parcel.readInt() != 0);
                    parcel2.writeNoException();
                    return true;
                case 4:
                    onLoopModeChanged(parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                case 5:
                    onMediaSourceTypeChanged(parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                case 6:
                    onNext();
                    parcel2.writeNoException();
                    return true;
                case 7:
                    onPause();
                    parcel2.writeNoException();
                    return true;
                case TRANSACTION_onPlay /* 8 */:
                    onPlay();
                    parcel2.writeNoException();
                    return true;
                case TRANSACTION_onPrevious /* 9 */:
                    onPrevious();
                    parcel2.writeNoException();
                    return true;
                case TRANSACTION_onRadioModeChanged /* 10 */:
                    onRadioModeChanged(parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                case TRANSACTION_onRewind /* 11 */:
                    onRewind();
                    parcel2.writeNoException();
                    return true;
                case TRANSACTION_onSeekTo /* 12 */:
                    onSeekTo(parcel.readLong());
                    parcel2.writeNoException();
                    return true;
                case TRANSACTION_onSelect /* 13 */:
                    onSelect(parcel.readString(), parcel.readString(), parcel.readInt() != 0);
                    parcel2.writeNoException();
                    return true;
                case TRANSACTION_onStop /* 14 */:
                    onStop();
                    parcel2.writeNoException();
                    return true;
                case TRANSACTION_operationType /* 15 */:
                    operationType(parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                case TRANSACTION_onSelectFrom /* 16 */:
                    onSelectFrom(parcel.readString(), parcel.readString(), parcel.readInt() != 0, parcel.readString());
                    parcel2.writeNoException();
                    return true;
                case TRANSACTION_onMediaQualityChange /* 17 */:
                    onMediaQualityChange(parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                case TRANSACTION_onMediaEffectChange /* 18 */:
                    onMediaEffectChange(parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                case TRANSACTION_onMediaSourceTypeChangedWithZone /* 19 */:
                    onMediaSourceTypeChangedWithZone(parcel.readInt(), parcel.readString(), parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                case TRANSACTION_onSearchMusic /* 20 */:
                    onSearchMusic(parcel.readString(), parcel.readString(), parcel.readString(), parcel.readInt(), parcel.readInt() != 0, parcel.readInt() != 0, ISearchMusicCallback.Stub.asInterface(parcel.readStrongBinder()));
                    parcel2.writeNoException();
                    return true;
                default:
                    return super.onTransact(i, parcel, parcel2, i2);
            }
        }
    }

    void onDownload(String str, String str2, boolean z);

    void onFastForward();

    void onFavorite(String str, String str2, boolean z);

    void onLoopModeChanged(int i);

    void onMediaEffectChange(int i);

    void onMediaQualityChange(int i);

    void onMediaSourceTypeChanged(int i);

    void onMediaSourceTypeChangedWithZone(int i, String str, int i2);

    void onNext();

    void onPause();

    void onPlay();

    void onPrevious();

    void onRadioModeChanged(int i);

    void onRewind();

    void onSearchMusic(String str, String str2, String str3, int i, boolean z, boolean z2, ISearchMusicCallback iSearchMusicCallback);

    void onSeekTo(long j);

    void onSelect(String str, String str2, boolean z);

    void onSelectFrom(String str, String str2, boolean z, String str3);

    void onStop();

    void operationType(int i);
}



