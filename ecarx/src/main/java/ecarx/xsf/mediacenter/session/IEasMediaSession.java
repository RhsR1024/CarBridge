package ecarx.xsf.mediacenter.session;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.Parcelable;

/* JADX INFO: compiled from: r8-map-id-3e69baf94e81d466d14d5c24482c5521bbb5f1d5dadfa8a3c310732898062d9f */
/* JADX INFO: loaded from: classes.dex */
public interface IEasMediaSession extends IInterface {
    static boolean transact(IBinder remote, int code, Parcel data, Parcel reply, int flags) {
        try { return remote.transact(code, data, reply, flags); } catch (RemoteException error) { return false; }
    }
    public static final String DESCRIPTOR = "ecarx.xsf.mediacenter.session.IEasMediaSession";

    /* JADX INFO: compiled from: r8-map-id-3e69baf94e81d466d14d5c24482c5521bbb5f1d5dadfa8a3c310732898062d9f */
    public static class Default implements IEasMediaSession {
        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }

        @Override // ecarx.xsf.mediacenter.session.IEasMediaSession
        public ISessionToken register(int i, int i2, String str, IEasSessionCallback iEasSessionCallback) {
            return null;
        }

        @Override // ecarx.xsf.mediacenter.session.IEasMediaSession
        public void setPlaybackInfo(ISessionToken iSessionToken, PlaybackInfo playbackInfo) {
        }

        @Override // ecarx.xsf.mediacenter.session.IEasMediaSession
        public void setPlaybackState(ISessionToken iSessionToken, PlaybackState playbackState) {
        }
    }

    /* JADX INFO: compiled from: r8-map-id-3e69baf94e81d466d14d5c24482c5521bbb5f1d5dadfa8a3c310732898062d9f */
    public static abstract class Stub extends Binder implements IEasMediaSession {
        static final int TRANSACTION_register = 1;
        static final int TRANSACTION_setPlaybackInfo = 2;
        static final int TRANSACTION_setPlaybackState = 3;

        /* JADX INFO: compiled from: r8-map-id-3e69baf94e81d466d14d5c24482c5521bbb5f1d5dadfa8a3c310732898062d9f */
        public static class Proxy implements IEasMediaSession {
            private IBinder mRemote;

            public Proxy(IBinder iBinder) {
                this.mRemote = iBinder;
            }

            @Override // android.os.IInterface
            public IBinder asBinder() {
                return this.mRemote;
            }

            public String getInterfaceDescriptor() {
                return IEasMediaSession.DESCRIPTOR;
            }

            @Override // ecarx.xsf.mediacenter.session.IEasMediaSession
            public ISessionToken register(int i, int i2, String str, IEasSessionCallback iEasSessionCallback) {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(IEasMediaSession.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    parcelObtain.writeString(str);
                    parcelObtain.writeStrongInterface(iEasSessionCallback);
                    IEasMediaSession.transact(this.mRemote, 1, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return ISessionToken.Stub.asInterface(parcelObtain2.readStrongBinder());
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override // ecarx.xsf.mediacenter.session.IEasMediaSession
            public void setPlaybackInfo(ISessionToken iSessionToken, PlaybackInfo playbackInfo) {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(IEasMediaSession.DESCRIPTOR);
                    parcelObtain.writeStrongInterface(iSessionToken);
                    _Parcel.writeTypedObject(parcelObtain, playbackInfo, 0);
                    IEasMediaSession.transact(this.mRemote, 2, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override // ecarx.xsf.mediacenter.session.IEasMediaSession
            public void setPlaybackState(ISessionToken iSessionToken, PlaybackState playbackState) {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(IEasMediaSession.DESCRIPTOR);
                    parcelObtain.writeStrongInterface(iSessionToken);
                    _Parcel.writeTypedObject(parcelObtain, playbackState, 0);
                    IEasMediaSession.transact(this.mRemote, 3, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }
        }

        public Stub() {
            attachInterface(this, IEasMediaSession.DESCRIPTOR);
        }

        public static IEasMediaSession asInterface(IBinder iBinder) {
            if (iBinder == null) {
                return null;
            }
            IInterface iInterfaceQueryLocalInterface = iBinder.queryLocalInterface(IEasMediaSession.DESCRIPTOR);
            return (iInterfaceQueryLocalInterface == null || !(iInterfaceQueryLocalInterface instanceof IEasMediaSession)) ? new Proxy(iBinder) : (IEasMediaSession) iInterfaceQueryLocalInterface;
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return this;
        }

        @Override // android.os.Binder
        public boolean onTransact(int i, Parcel parcel, Parcel parcel2, int i2) throws RemoteException {
            if (i >= 1 && i <= 16777215) {
                parcel.enforceInterface(IEasMediaSession.DESCRIPTOR);
            }
            if (i == 1598968902) {
                parcel2.writeString(IEasMediaSession.DESCRIPTOR);
                return true;
            }
            if (i == 1) {
                ISessionToken iSessionTokenRegister = register(parcel.readInt(), parcel.readInt(), parcel.readString(), IEasSessionCallback.Stub.asInterface(parcel.readStrongBinder()));
                parcel2.writeNoException();
                parcel2.writeStrongInterface(iSessionTokenRegister);
                return true;
            }
            if (i == 2) {
                setPlaybackInfo(ISessionToken.Stub.asInterface(parcel.readStrongBinder()), (PlaybackInfo) _Parcel.readTypedObject(parcel, PlaybackInfo.CREATOR));
                parcel2.writeNoException();
                return true;
            }
            if (i != 3) {
                return super.onTransact(i, parcel, parcel2, i2);
            }
            setPlaybackState(ISessionToken.Stub.asInterface(parcel.readStrongBinder()), (PlaybackState) _Parcel.readTypedObject(parcel, PlaybackState.CREATOR));
            parcel2.writeNoException();
            return true;
        }
    }

    /* JADX INFO: compiled from: r8-map-id-3e69baf94e81d466d14d5c24482c5521bbb5f1d5dadfa8a3c310732898062d9f */
    public static class _Parcel {
        /* JADX INFO: Access modifiers changed from: private */
        public static <T> T readTypedObject(Parcel parcel, Parcelable.Creator<T> creator) {
            if (parcel.readInt() != 0) {
                return creator.createFromParcel(parcel);
            }
            return null;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public static <T extends Parcelable> void writeTypedObject(Parcel parcel, T t, int i) {
            if (t == null) {
                parcel.writeInt(0);
            } else {
                parcel.writeInt(1);
                t.writeToParcel(parcel, i);
            }
        }
    }

    ISessionToken register(int i, int i2, String str, IEasSessionCallback iEasSessionCallback);

    void setPlaybackInfo(ISessionToken iSessionToken, PlaybackInfo playbackInfo);

    void setPlaybackState(ISessionToken iSessionToken, PlaybackState playbackState);
}



