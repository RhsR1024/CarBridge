package com.ecarx.eas.xsf.mediacenter;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.RemoteException;

/** Binder contract used by MediaCenter's registerEx extension. */
public interface IExCallback extends IInterface {
    String DESCRIPTOR = "com.ecarx.eas.xsf.mediacenter.IExCallback";

    String onAction(int type, String action, String payload, IBinder binder) throws RemoteException;
    IExContent onExAction(int type, String action, String payload, IExContent content,
                          IBinder binder) throws RemoteException;

    abstract class Stub extends Binder implements IExCallback {
        private static final int TRANSACTION_ON_ACTION = 1;
        private static final int TRANSACTION_ON_EX_ACTION = 2;

        protected Stub() { attachInterface(this, DESCRIPTOR); }

        @Override public IBinder asBinder() { return this; }

        @Override protected boolean onTransact(int code, Parcel data, Parcel reply, int flags)
                throws RemoteException {
            if (code == INTERFACE_TRANSACTION) {
                reply.writeString(DESCRIPTOR);
                return true;
            }
            data.enforceInterface(DESCRIPTOR);
            if (code == TRANSACTION_ON_ACTION) {
                String result = onAction(data.readInt(), data.readString(), data.readString(),
                        data.readStrongBinder());
                reply.writeNoException();
                reply.writeString(result);
                return true;
            }
            if (code == TRANSACTION_ON_EX_ACTION) {
                int type = data.readInt();
                String action = data.readString();
                String payload = data.readString();
                IExContent content = data.readInt() == 0 ? null : IExContent.CREATOR.createFromParcel(data);
                IExContent result = onExAction(type, action, payload, content, data.readStrongBinder());
                reply.writeNoException();
                if (result == null) reply.writeInt(0);
                else {
                    reply.writeInt(1);
                    result.writeToParcel(reply, Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
                }
                return true;
            }
            return super.onTransact(code, data, reply, flags);
        }
    }
}
