package io.github.rhsr1024.interop;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import java.security.MessageDigest;

/** Versioned, bounded Messenger wire contract. Identical source in both applications. */
public final class BridgeProtocol {
    public static final int MAJOR=1, MINOR=0;
    public static final int HELLO=1, POLICY=2, PREPARE=3, PREPARED=4, COMMIT=5, READY=6,
        PLAY=7, GRANT=8, PAUSE=9, COMMAND=10, YIELD=11, PING=12, PONG=13, CLOSE=14, ERROR=15;
    public static final String SERVICE="com.geely.auto.music.CarBridgeCompanionService";
    public static final String CARBRIDGE="io.github.rhsr1024.carbridge";
    public static final String MEDIABRIDGE="com.mediabridge.app";
    // Public certificate fingerprint of the explicitly provisioned release-test signer.
    private static final String RELEASE_TEST_CERT="A539C794675FC8AD7FAC6E78604C066E15C98C3C65F62CC34895FA3FC65A23C7";
    private BridgeProtocol() {}
    public static boolean managed(String pkg) {
        return CARBRIDGE.equals(pkg) || (CARBRIDGE+".debug").equals(pkg);
    }
    public static boolean mediaBridge(String pkg) {
        return MEDIABRIDGE.equals(pkg) || (MEDIABRIDGE+".dev").equals(pkg);
    }
    public static boolean trusted(Context context, String pkg, int uid) {
        try {
            PackageManager pm=context.getPackageManager();
            if (pm.getApplicationInfo(pkg,0).uid!=uid) return false;
            if (pm.checkSignatures(context.getPackageName(),pkg)==PackageManager.SIGNATURE_MATCH) return true;
            byte[] pin=new byte[32];
            for(int i=0;i<32;i++) pin[i]=(byte)Integer.parseInt(RELEASE_TEST_CERT.substring(i*2,i*2+2),16);
            return pm.hasSigningCertificate(pkg,pin,PackageManager.CERT_INPUT_SHA256);
        } catch (Exception error) { return false; }
    }
    public static boolean valid(Bundle b) {
        try {
            if (b==null || b.size()>24 || b.getInt("major",0)!=MAJOR) return false;
            for(String key:b.keySet()) {
                Object value=b.get(key);
                if (!(value instanceof String || value instanceof Long || value instanceof Integer || value instanceof Boolean)) return false;
                if (value instanceof String && ((String)value).length()>512) return false;
            }
            return true;
        } catch(RuntimeException malformed) { return false; }
    }
    public static Bundle envelope(String instance,long epoch,String connection) {
        Bundle b=new Bundle(); b.putInt("major",MAJOR); b.putInt("minor",MINOR);
        b.putString("instance",instance); b.putLong("epoch",epoch); b.putString("connection",connection); return b;
    }
    public static boolean send(Messenger target,Messenger reply,int what,Bundle b) {
        if(target==null) return false;
        Message m=Message.obtain(null,what); m.setData(b); m.replyTo=reply;
        try { target.send(m); return true; } catch(RemoteException|RuntimeException error) { return false; }
    }
}
