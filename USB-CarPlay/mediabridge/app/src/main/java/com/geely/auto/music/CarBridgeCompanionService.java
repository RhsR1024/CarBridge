package com.geely.auto.music;

import android.app.Service;
import android.content.Intent;
import android.os.*;
import io.github.rhsr1024.interop.BridgeProtocol;
import java.util.UUID;
import static io.github.rhsr1024.interop.BridgeProtocol.*;

/** Explicit, authenticated collaboration endpoint. Its absence never enables managed input. */
public final class CarBridgeCompanionService extends Service {
    private static CarBridgeCompanionService live;
    static volatile boolean uncertainOutput;
    private final Handler main = new Handler(Looper.getMainLooper(), this::receive);
    private final Messenger endpoint = new Messenger(main);
    private final String server = UUID.randomUUID().toString();
    private Messenger peer;
    private String pkg = "", instance = "", connection = "", route = "", prepared = "", intent = "";
    private long epoch, policyRevision, lastHeartbeat, commandSequence, highestIntent = -1;
    private Boolean lastGrant;
    private int peerUid = -1;
    private boolean ignored, enabled, ready, preparedOk;
    private String unavailable = "";
    private SettingsRepository settings;
    private final Runnable ticker = new Runnable() {
        @Override public void run() {
            refreshPolicy();
            if (peer != null && SystemClock.elapsedRealtime()-lastHeartbeat>3500) revoke("协作连接超时");
            main.postDelayed(this, 1000);
        }
    };
    @Override public void onCreate() {
        super.onCreate(); live=this; settings=new SettingsRepository(this); main.post(ticker);
    }
    @Override public IBinder onBind(Intent i) { return endpoint.getBinder(); }
    @Override public void onDestroy() {
        revoke("协作服务停止"); main.removeCallbacksAndMessages(null); if(live==this) live=null; super.onDestroy();
    }
    static boolean allows(String pkg) {
        CarBridgeCompanionService s=live;
        return s!=null && s.peer!=null && s.pkg.equals(pkg) && s.route.equals("BRIDGE") && s.ready && !s.ignored && s.enabled
            && SystemClock.elapsedRealtime()-s.lastHeartbeat<=3500;
    }
    static String managedPackage() {
        return live == null || live.pkg.isEmpty() ? CARBRIDGE : live.pkg;
    }
    static void backendStopping() {
        if(live!=null) { uncertainOutput |= allows(live.pkg); live.revoke("车机桥接停止"); }
    }
    static void command(String pkg,String action) {
        CarBridgeCompanionService s=live;
        if(!allows(pkg)) return;
        Bundle b=s.message(); b.putString("command",action); b.putString("id",s.server+":"+(++s.commandSequence));
        s.send(COMMAND,b);
    }
    static void yieldPlayback(String reason) {
        CarBridgeCompanionService s=live;
        if(s==null || s.intent.isEmpty() || !"BRIDGE".equals(s.route)) return;
        String replaced=s.intent; s.intent="";
        UniversalBridgeService.quiesceManaged(s.pkg, ok -> {});
        Bundle b=s.message(); b.putString("intent",replaced); b.putString("reason",reason); s.send(YIELD,b);
    }
    private Bundle message() {
        Bundle b=envelope(instance,epoch,connection); b.putString("server",server); b.putLong("policy",policyRevision); return b;
    }
    private void send(int what,Bundle b) {
        if(!BridgeProtocol.send(peer,endpoint,what,b)) revoke("对端不可达");
    }
    private void policy() {
        if(peer==null) return;
        Bundle b=message(); b.putBoolean("ignored",ignored); b.putBoolean("enabled",enabled); b.putBoolean("ready",ready);
        b.putBoolean("usbBox",true);
        b.putString("unavailable",unavailable);
        b.putString("package",getPackageName()); send(POLICY,b);
    }
    private void refreshPolicy() {
        boolean nextIgnored=settings.getBlacklist().contains(pkg);
        boolean nextEnabled=settings.isBridgeEnabled();
        boolean listenerReady=MediaListenerService.companionReady();
        boolean backendReady=UniversalBridgeService.companionReady();
        NotificationAccess.State access=NotificationAccess.check(this);
        boolean nextReady=nextEnabled && listenerReady && backendReady && access!=NotificationAccess.State.DENIED;
        String nextUnavailable=!nextEnabled ? "DISABLED" : access==NotificationAccess.State.DENIED ? "NOTIFICATION_ACCESS"
            : !listenerReady ? "LISTENER_DISCONNECTED" : !backendReady ? "BACKEND_NOT_READY" : "";
        boolean changed=ignored!=nextIgnored || enabled!=nextEnabled || ready!=nextReady;
        if(!changed && unavailable.equals(nextUnavailable)) return;
        ignored=nextIgnored; enabled=nextEnabled; ready=nextReady; unavailable=nextUnavailable;
        // Diagnostic-only changes do not invalidate an in-flight ownership handover.
        if(changed) policyRevision++;
        DiagnosticsLog.i("CARBRIDGE policy enabled="+enabled+" ignored="+ignored+" listener="+listenerReady
            +" backend="+backendReady+" access="+access+" ready="+ready+" unavailable="+unavailable);
        if("BRIDGE".equals(route) && (ignored || !ready)) {
            yieldPlayback("桥接策略或授权变化"); route=""; preparedOk=false;
            UniversalBridgeService.quiesceManaged(pkg,ok -> {});
            MediaListenerService.companionRefresh();
        }
        policy();
    }
    private boolean receive(Message message) {
        Bundle b=message.getData();
        if(!valid(b)) return true;
        if(message.what==HELLO) {
            String requested=b.getString("package","");
            if(!managed(requested) || !trusted(this,requested,message.sendingUid) || message.replyTo==null) return true;
            // A different live peer cannot silently displace the owner, including debug/stable variants.
            if(peer!=null && (!requested.equals(pkg) || !b.getString("instance","").equals(instance))) {
                Bundle busy=envelope(b.getString("instance",""),b.getLong("epoch"),b.getString("connection",""));
                busy.putString("reason","已有协作连接，等待退出"); BridgeProtocol.send(message.replyTo,endpoint,ERROR,busy); return true;
            }
            if(peer==null) {
                pkg=requested; instance=b.getString("instance",""); peerUid=message.sendingUid;
                connection=b.getString("connection",""); epoch=0; route=""; preparedOk=false;
            }
            peer=message.replyTo; lastHeartbeat=SystemClock.elapsedRealtime(); refreshPolicy(); policy(); return true;
        }
        if(peer==null || message.sendingUid!=peerUid || !instance.equals(b.getString("instance")) || !server.equals(b.getString("server"))) return true;
        long requestedEpoch=b.getLong("epoch",-1);
        if(message.what==PING) { lastHeartbeat=SystemClock.elapsedRealtime(); send(PONG,message()); return true; }
        if(message.what==PREPARE) {
            if (requestedEpoch == epoch && connection.equals(b.getString("connection", "")) && prepared.equals(b.getString("target", ""))) {
                if (route.equals(prepared) && !route.isEmpty()) { Bundle ack=message(); ack.putString("target",route); send(READY,ack); }
                else if (preparedOk) { Bundle ack=message(); ack.putBoolean("ok",true); ack.putString("target",prepared); send(PREPARED,ack); }
                return true;
            }
            if(requestedEpoch<=epoch || b.getLong("policy",-1)!=policyRevision) { policy(); return true; }
            if (!connection.equals(b.getString("connection", ""))) highestIntent=-1;
            epoch=requestedEpoch; connection=b.getString("connection",""); route=""; intent=""; preparedOk=false;
            prepared=b.getString("target","");
            if(!prepared.equals("BRIDGE") && !prepared.equals("DIRECT")) return true;
            long expected=epoch; String owner=instance;
            UniversalBridgeService.quiesceManaged(pkg,ok -> {
                if(peer==null || epoch!=expected || !instance.equals(owner)) return;
                preparedOk=ok; if(ok) uncertainOutput=false;
                Bundle ack=message(); ack.putBoolean("ok",ok); ack.putString("target",prepared); send(PREPARED,ack);
            });
            MediaListenerService.companionRefresh(); return true;
        }
        if(requestedEpoch!=epoch || !connection.equals(b.getString("connection",""))) return true;
        switch(message.what) {
            case COMMIT:
                if (!route.isEmpty() && route.equals(prepared) && b.getLong("policy",-1)==policyRevision) {
                    Bundle repeated=message(); repeated.putString("target",route); send(READY,repeated); break;
                }
                if(!preparedOk || b.getLong("policy",-1)!=policyRevision) { policy(); break; }
                if(prepared.equals("BRIDGE") && (!ready || ignored)) { policy(); break; }
                route=prepared; preparedOk=false;
                Bundle ack=message(); ack.putString("target",route); send(READY,ack);
                MediaListenerService.companionRefresh();
                if (route.equals("BRIDGE")) UniversalBridgeService.prepareManaged(MediaListenerService.selectManaged(pkg));
                break;
            case PLAY:
                if(!allows(pkg)) { rejectPlay(b); break; }
                String next=b.getString("intent","");
                long revision=b.getLong("intentRevision",-1);
                if (next.equals(intent) && lastGrant!=null) { Bundle repeated=message(); repeated.putString("intent",next); repeated.putBoolean("ok",lastGrant); send(GRANT,repeated); break; }
                if(next.isEmpty() || revision<=highestIntent) { rejectPlay(b); break; }
                highestIntent=revision; lastGrant=null; intent=next;
                PlayerSnapshot snapshot=MediaListenerService.selectManaged(pkg);
                if(snapshot==null || !pkg.equals(snapshot.packageName)) { rejectPlay(b); break; }
                final long expected=epoch;
                UniversalBridgeService.playManaged(snapshot,ok -> {
                    if(peer==null || epoch!=expected || !intent.equals(next)) return;
                    lastGrant=ok; Bundle result=message(); result.putString("intent",next); result.putBoolean("ok",ok); send(GRANT,result);
                }); break;
            case PAUSE:
                if(!intent.equals(b.getString("intent",""))) break;
                intent=""; UniversalBridgeService.pauseManaged(pkg); break;
            case CLOSE: revoke("CarBridge 已断开"); break;
            default: break;
        }
        return true;
    }
    private void rejectPlay(Bundle input) {
        lastGrant=false; Bundle b=message(); b.putString("intent",input.getString("intent","")); b.putBoolean("ok",false); send(GRANT,b);
    }
    private void revoke(String reason) {
        if(peer==null) return;
        yieldPlayback(reason);
        route=""; intent=""; preparedOk=false; peer=null; peerUid=-1;
        UniversalBridgeService.quiesceManaged(pkg,ok -> { if(ok) uncertainOutput=false; });
        MediaListenerService.companionRefresh();
        DiagnosticsLog.i("CARBRIDGE route revoked reason="+reason+" epoch="+epoch);
    }
}
