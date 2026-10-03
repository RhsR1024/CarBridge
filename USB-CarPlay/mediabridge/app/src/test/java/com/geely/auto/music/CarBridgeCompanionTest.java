package com.geely.auto.music;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.Signature;
import android.os.*;
import java.util.ArrayList;
import java.util.List;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;
import static io.github.rhsr1024.interop.BridgeProtocol.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=30)
public class CarBridgeCompanionTest {
    private CarBridgeCompanionService service;
    private final List<Message> replies=new ArrayList<>();
    private String server;
    private long policy;
    private final int uid=10421;
    @Before public void setup() throws Exception {
        Context context=RuntimeEnvironment.getApplication();
        PackageInfo self=context.getPackageManager().getPackageInfo(context.getPackageName(),0);
        self.signatures=new Signature[]{new Signature("1234")};
        Shadows.shadowOf(context.getPackageManager()).installPackage(self);
        PackageInfo peer=new PackageInfo(); peer.packageName=CARBRIDGE; peer.signatures=self.signatures;
        peer.applicationInfo=new ApplicationInfo(); peer.applicationInfo.packageName=CARBRIDGE; peer.applicationInfo.uid=uid;
        Shadows.shadowOf(context.getPackageManager()).installPackage(peer);
        UniversalBridgeService.live=null; CarBridgeCompanionService.uncertainOutput=false;
        service=Robolectric.buildService(CarBridgeCompanionService.class).create().get();
        Bundle hello=envelope("phone-process",0,"connection-1"); hello.putString("package",CARBRIDGE);
        send(HELLO,hello,uid);
        assertFalse(replies.isEmpty());
        Message state=replies.get(replies.size()-1);
        assertEquals(POLICY,state.what); server=state.getData().getString("server"); policy=state.getData().getLong("policy");
    }
    @After public void teardown() { if(service!=null) service.onDestroy(); UniversalBridgeService.live=null; }
    private Bundle request(long epoch) {
        Bundle b=envelope("phone-process",epoch,"connection-1"); b.putString("server",server); b.putLong("policy",policy); return b;
    }
    private void send(int what,Bundle b,int sender) throws Exception {
        Message m=Message.obtain(null,what); m.setData(b); m.sendingUid=sender;
        m.replyTo=new Messenger(new Handler(Looper.getMainLooper(), reply->{replies.add(Message.obtain(reply));return true;}));
        java.lang.reflect.Method receive=CarBridgeCompanionService.class.getDeclaredMethod("receive",Message.class);
        receive.setAccessible(true); receive.invoke(service,m); Shadows.shadowOf(Looper.getMainLooper()).idle();
    }
    @Test public void managedInputIsExcludedBeforeHandshakeAndInDirectMode() throws Exception {
        assertFalse(CarBridgeCompanionService.allows(CARBRIDGE));
        Bundle prepare=request(1); prepare.putString("target","DIRECT"); send(PREPARE,prepare,uid);
        assertEquals(PREPARED,replies.get(replies.size()-1).what);
        send(COMMIT,request(1),uid);
        assertEquals(READY,replies.get(replies.size()-1).what);
        assertEquals("DIRECT",replies.get(replies.size()-1).getData().getString("target"));
        assertFalse(CarBridgeCompanionService.allows(CARBRIDGE));
        int count=replies.size(); send(COMMIT,request(0),uid); assertEquals(count,replies.size());
    }
    @Test public void wrongUidAndOldProcessCannotPrepareOrReceiveCommands() throws Exception {
        int count=replies.size(); Bundle prepare=request(1); prepare.putString("target","DIRECT");
        send(PREPARE,prepare,uid+1); assertEquals(count,replies.size());
        prepare.putString("instance","old-process"); send(PREPARE,prepare,uid); assertEquals(count,replies.size());
        CarBridgeCompanionService.command(CARBRIDGE,"NEXT"); assertEquals(count,replies.size());
    }
    @Test public void unconfirmedBackendReleaseDoesNotAuthorizeDirectHandover() throws Exception {
        CarBridgeCompanionService.uncertainOutput=true;
        Bundle prepare=request(1); prepare.putString("target","DIRECT"); send(PREPARE,prepare,uid);
        Message ack=replies.get(replies.size()-1);
        assertEquals(PREPARED,ack.what); assertFalse(ack.getData().getBoolean("ok"));
        send(COMMIT,request(1),uid);
        assertNotEquals(READY,replies.get(replies.size()-1).what);
    }
    @Test public void protocolRejectsOversizedOrArbitraryParcelableInputs() {
        Bundle b=request(1); b.putString("oversized",new String(new char[513])); assertFalse(valid(b));
        b=request(1); b.putParcelable("arbitrary",new Bundle()); assertFalse(valid(b));
        b=request(1); b.putInt("major",2); assertFalse(valid(b));
    }

    @Test public void grantedAccessWithoutSystemBindRemainsUnreadyAndDoesNotChangeOwnershipRevision() throws Exception {
        Context context=RuntimeEnvironment.getApplication();
        new SettingsRepository(context).setBridgeEnabled(true);
        java.lang.reflect.Method refresh=CarBridgeCompanionService.class.getDeclaredMethod("refreshPolicy");
        refresh.setAccessible(true);
        android.provider.Settings.Secure.putString(context.getContentResolver(), "enabled_notification_listeners", "");
        refresh.invoke(service); Shadows.shadowOf(Looper.getMainLooper()).idle();
        Bundle denied=replies.get(replies.size()-1).getData();
        assertEquals("NOTIFICATION_ACCESS",denied.getString("unavailable"));
        assertFalse(denied.getBoolean("ready"));
        long ownershipRevision=denied.getLong("policy");

        String component=new android.content.ComponentName(context,MediaListenerService.class).flattenToString();
        android.provider.Settings.Secure.putString(context.getContentResolver(), "enabled_notification_listeners", component);
        refresh.invoke(service); Shadows.shadowOf(Looper.getMainLooper()).idle();
        Bundle waiting=replies.get(replies.size()-1).getData();
        assertEquals("LISTENER_DISCONNECTED",waiting.getString("unavailable"));
        assertFalse(waiting.getBoolean("ready"));
        assertEquals(ownershipRevision,waiting.getLong("policy"));
        assertFalse(CarBridgeCompanionService.allows(CARBRIDGE));
    }
}
