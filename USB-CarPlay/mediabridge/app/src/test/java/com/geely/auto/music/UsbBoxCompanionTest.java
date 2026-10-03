package com.geely.auto.music;

import android.content.Context;
import android.content.pm.*;
import android.os.*;
import java.io.InputStream;
import java.security.cert.CertificateFactory;
import java.lang.reflect.*;
import java.util.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;
import static io.github.rhsr1024.interop.BridgeProtocol.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=30)
public class UsbBoxCompanionTest {
    private Context context;
    private CarBridgeCompanionService service;
    private final List<Message> replies=new ArrayList<>();
    private String server;
    private long policy;
    private final int uid=10391;
    @Before public void setup() throws Exception {
        context=RuntimeEnvironment.getApplication();
        Signature signature;
        try(InputStream in=getClass().getResourceAsStream("/usbbox-public-cert.pem")) {
            assertNotNull(in); signature=new Signature(CertificateFactory.getInstance("X.509").generateCertificate(in).getEncoded());
        }
        install(USBBOX,uid,signature);
        UniversalBridgeService.live=null;CarBridgeCompanionService.uncertainOutput=false;
        service=Robolectric.buildService(CarBridgeCompanionService.class).create().get();
        assertFalse(CarBridgeCompanionService.allows(USBBOX));
        Bundle hello=envelope("usb-instance",0,"phone-1");hello.putString("package",USBBOX);send(HELLO,hello,uid);
        Bundle b=replies.get(replies.size()-1).getData();
        assertTrue(b.getBoolean("usbBox"));server=b.getString("server");policy=b.getLong("policy");
    }
    @After public void teardown(){if(service!=null)service.onDestroy();UniversalBridgeService.live=null;}
    private void install(String pkg,int uid,Signature sig){
        PackageInfo p=new PackageInfo();p.packageName=pkg;p.signatures=new Signature[]{sig};
        p.signingInfo=org.robolectric.util.ReflectionHelpers.callConstructor(SigningInfo.class);
        Shadows.shadowOf(p.signingInfo).setSignatures(p.signatures);
        Shadows.shadowOf(p.signingInfo).setPastSigningCertificates(p.signatures);
        p.applicationInfo=new ApplicationInfo();p.applicationInfo.packageName=pkg;p.applicationInfo.uid=uid;
        Shadows.shadowOf(context.getPackageManager()).installPackage(p);
    }
    private Bundle request(long epoch){
        Bundle b=envelope("usb-instance",epoch,"phone-1");b.putString("server",server);b.putLong("policy",policy);return b;
    }
    private void send(int what,Bundle b,int sender)throws Exception{
        Message m=Message.obtain(null,what);m.setData(b);m.sendingUid=sender;
        m.replyTo=new Messenger(new Handler(Looper.getMainLooper(),reply->{replies.add(Message.obtain(reply));return true;}));
        Method r=CarBridgeCompanionService.class.getDeclaredMethod("receive",Message.class);r.setAccessible(true);r.invoke(service,m);
        Shadows.shadowOf(Looper.getMainLooper()).idle();
    }
    private void set(String field,Object value)throws Exception{
        Field f=CarBridgeCompanionService.class.getDeclaredField(field);f.setAccessible(true);f.set(service,value);
    }
    @Test public void originalUsbSignatureIsScopedToExactPackageAndUid(){
        assertTrue(managed(USBBOX));assertTrue(trusted(context,USBBOX,uid));
        assertFalse(trusted(context,USBBOX,uid+1));assertFalse(managed(USBBOX+".fake"));
        install(USBBOX,uid,new Signature("1234"));assertFalse(trusted(context,USBBOX,uid));
    }
    @Test public void directExcludesUsbInputAndDrainKeepsUsbIdentity()throws Exception{
        Bundle b=request(1);b.putString("target","DIRECT");send(PREPARE,b,uid);send(COMMIT,request(1),uid);
        assertEquals(READY,replies.get(replies.size()-1).what);assertFalse(CarBridgeCompanionService.allows(USBBOX));
        assertEquals(USBBOX,CarBridgeCompanionService.managedPackage());
        CarBridgeCompanionService.backendStopping();assertEquals(USBBOX,CarBridgeCompanionService.managedPackage());
        assertFalse(CarBridgeCompanionService.allows(USBBOX));
    }
    @Test public void bridgeHandoverAllowsOnlyUsbCommandsAndCloseRevokesIt()throws Exception{
        set("ready",true);set("enabled",true);set("ignored",false);
        Bundle b=request(1);b.putString("target","BRIDGE");send(PREPARE,b,uid);send(COMMIT,request(1),uid);
        assertTrue(CarBridgeCompanionService.allows(USBBOX));assertFalse(CarBridgeCompanionService.allows(CARBRIDGE));
        CarBridgeCompanionService.command(USBBOX,"PLAY");Shadows.shadowOf(Looper.getMainLooper()).idle();
        Message command=replies.get(replies.size()-1);assertEquals(COMMAND,command.what);assertEquals("PLAY",command.getData().getString("command"));
        int count=replies.size();CarBridgeCompanionService.command(CARBRIDGE,"PAUSE");Shadows.shadowOf(Looper.getMainLooper()).idle();assertEquals(count,replies.size());
        send(CLOSE,request(1),uid);assertFalse(CarBridgeCompanionService.allows(USBBOX));
    }
    @Test public void uncertainPreviousOutputDoesNotGrantUsbDirectOwnership()throws Exception{
        CarBridgeCompanionService.uncertainOutput=true;
        Bundle b=request(1);b.putString("target","DIRECT");send(PREPARE,b,uid);
        assertFalse(replies.get(replies.size()-1).getData().getBoolean("ok"));
        send(COMMIT,request(1),uid);assertNotEquals(READY,replies.get(replies.size()-1).what);
    }
    @Test public void usbMetadataWithoutKnownDurationDoesNotGuessOnlineIdentity(){
        assertFalse(CarBridgeMetadataPolicy.allowsLookup(USBBOX,"Song","Artist",0));
        assertTrue(CarBridgeMetadataPolicy.allowsLookup("ordinary.player","Song","",0));
    }
}
