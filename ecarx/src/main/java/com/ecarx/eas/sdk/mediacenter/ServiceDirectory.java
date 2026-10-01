package com.ecarx.eas.sdk.mediacenter;

import android.os.IBinder;
import android.os.Parcel;
import android.os.Process;
import android.os.RemoteException;
import java.util.List;

/** F25 directory wire format, ported from Cplay IServicePool / IEASFrameworkService. */
public final class ServiceDirectory {
    public static final String OPENAPI_ACTION = "ecarx.intent.action.OpenAPIService";
    public static final String POOL_DESCRIPTOR = "com.ecarx.sdk.openapi.IServicePool";
    public static final String EAS_DESCRIPTOR = "com.ecarx.eas.framework.sdk.IEASFrameworkService";
    private ServiceDirectory() {}
    public static List<String> available(IBinder remote, boolean eas, boolean nativeEas) throws RemoteException {
        Parcel data = Parcel.obtain(), reply = Parcel.obtain();
        try {
            data.writeInterfaceToken(eas ? EAS_DESCRIPTOR : POOL_DESCRIPTOR);
            if (!remote.transact(eas ? (nativeEas ? 9 : 7) : 1, data, reply, 0))
                throw new UnsupportedOperationException("Service directory unavailable");
            reply.readException(); return reply.createStringArrayList();
        } finally { reply.recycle(); data.recycle(); }
    }
    public static IBinder get(IBinder remote, boolean eas, String packageName) throws RemoteException {
        Parcel data = Parcel.obtain(), reply = Parcel.obtain();
        try {
            data.writeInterfaceToken(eas ? EAS_DESCRIPTOR : POOL_DESCRIPTOR);
            data.writeInt(Process.myPid()); data.writeInt(Process.myUid());
            data.writeString(packageName); data.writeString("mediacenter");
            if (!remote.transact(eas ? 8 : 2, data, reply, 0))
                throw new UnsupportedOperationException("Service directory getService unavailable");
            reply.readException(); return reply.readStrongBinder();
        } finally { reply.recycle(); data.recycle(); }
    }
}
