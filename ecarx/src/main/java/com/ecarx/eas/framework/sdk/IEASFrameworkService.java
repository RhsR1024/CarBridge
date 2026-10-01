package com.ecarx.eas.framework.sdk;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

import com.ecarx.sdk.openapi.msg.EASFrameworkMessage;
import com.ecarx.sdk.openapi.msg.EASFrameworkRetMessage;

/** Minimal client-side proxy for the EAS Framework service used by MediaCenter. */
public interface IEASFrameworkService extends IInterface {
    String DESCRIPTOR = "com.ecarx.eas.framework.sdk.IEASFrameworkService";

    void init(String[] services) throws RemoteException;

    EASFrameworkRetMessage call(EASFrameworkMessage message) throws RemoteException;
    EASFrameworkRetMessage asyncBinderCall(EASFrameworkMessage message, IBinder binder)
            throws RemoteException;

    abstract class Stub extends Binder implements IEASFrameworkService {
        private static final int TRANSACTION_INIT = 1;
        private static final int TRANSACTION_CALL = 4;
        private static final int TRANSACTION_ASYNC_BINDER_CALL = 6;

        protected Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IEASFrameworkService asInterface(IBinder binder) {
            if (binder == null) return null;
            IInterface local = binder.queryLocalInterface(DESCRIPTOR);
            return local instanceof IEASFrameworkService
                    ? (IEASFrameworkService) local : new Proxy(binder);
        }

        @Override public IBinder asBinder() { return this; }

        @Override protected boolean onTransact(int code, Parcel data, Parcel reply, int flags)
                throws RemoteException {
            if (code == INTERFACE_TRANSACTION) {
                reply.writeString(DESCRIPTOR);
                return true;
            }
            if (code == TRANSACTION_INIT) {
                data.enforceInterface(DESCRIPTOR);
                init(data.createStringArray());
                reply.writeNoException();
                return true;
            }
            if (code == TRANSACTION_CALL) {
                data.enforceInterface(DESCRIPTOR);
                EASFrameworkMessage message = readTyped(data, EASFrameworkMessage.CREATOR);
                EASFrameworkRetMessage result = call(message);
                reply.writeNoException();
                writeTyped(reply, result, 1);
                return true;
            }
            if (code == TRANSACTION_ASYNC_BINDER_CALL) {
                data.enforceInterface(DESCRIPTOR);
                EASFrameworkMessage message = readTyped(data, EASFrameworkMessage.CREATOR);
                EASFrameworkRetMessage result = asyncBinderCall(message, data.readStrongBinder());
                reply.writeNoException();
                writeTyped(reply, result, 1);
                return true;
            }
            return super.onTransact(code, data, reply, flags);
        }

        private static final class Proxy implements IEASFrameworkService {
            private final IBinder remote;

            Proxy(IBinder remote) { this.remote = remote; }

            @Override public IBinder asBinder() { return remote; }

            @Override public void init(String[] services) throws RemoteException {
                Parcel data = Parcel.obtain();
                Parcel reply = Parcel.obtain();
                try {
                    data.writeInterfaceToken(DESCRIPTOR);
                    data.writeStringArray(services);
                    if (!remote.transact(TRANSACTION_INIT, data, reply, 0)) throw new UnsupportedOperationException("EAS init unsupported");
                    reply.readException();
                } finally {
                    reply.recycle();
                    data.recycle();
                }
            }

            @Override public EASFrameworkRetMessage call(EASFrameworkMessage message)
                    throws RemoteException {
                Parcel data = Parcel.obtain();
                Parcel reply = Parcel.obtain();
                try {
                    data.writeInterfaceToken(DESCRIPTOR);
                    writeTyped(data, message, 0);
                    if (!remote.transact(TRANSACTION_CALL, data, reply, 0)) throw new UnsupportedOperationException("EAS call unsupported");
                    reply.readException();
                    return readTyped(reply, EASFrameworkRetMessage.CREATOR);
                } finally {
                    reply.recycle();
                    data.recycle();
                }
            }

            @Override public EASFrameworkRetMessage asyncBinderCall(
                    EASFrameworkMessage message, IBinder binder) throws RemoteException {
                Parcel data = Parcel.obtain();
                Parcel reply = Parcel.obtain();
                try {
                    data.writeInterfaceToken(DESCRIPTOR);
                    writeTyped(data, message, 0);
                    data.writeStrongBinder(binder);
                    if (!remote.transact(TRANSACTION_ASYNC_BINDER_CALL, data, reply, 0)) throw new UnsupportedOperationException("EAS asyncBinderCall unsupported");
                    reply.readException();
                    return readTyped(reply, EASFrameworkRetMessage.CREATOR);
                } finally {
                    reply.recycle();
                    data.recycle();
                }
            }
        }

        private static <T extends android.os.Parcelable> void writeTyped(Parcel parcel, T value, int flags) {
            if (value == null) {
                parcel.writeInt(0);
            } else {
                parcel.writeInt(1);
                value.writeToParcel(parcel, flags);
            }
        }

        private static <T> T readTyped(Parcel parcel, android.os.Parcelable.Creator<T> creator) {
            return parcel.readInt() == 0 ? null : creator.createFromParcel(parcel);
        }
    }
}
