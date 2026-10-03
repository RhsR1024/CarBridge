from pathlib import Path
import copy, hashlib, os, re, subprocess, tempfile, zipfile
R = Path(__file__).resolve().parents[1]
H = R/'_build'; H.mkdir(exist_ok=True)
WORK = Path(tempfile.mkdtemp(prefix='compile-', dir=H))
from toolchain import java_home, android_sdk, jadx_jar
BASE = R/'baselines/CarPlay-USBBox-F25-MultiTouch-Slim.apk'
assert hashlib.sha256(BASE.read_bytes()).hexdigest() == '10dc853023f48cf0a11f44f99a6da1dd64cb08010550e0b1a72e8030cf94d796'
JDK = java_home()
SDK = android_sdk()
JAR = jadx_jar()
ANDROID = SDK/'platforms/android-34/android.jar'
CP = str(JAR)+os.pathsep+str(H)
def run(args): subprocess.run([str(x) for x in args],check=True)
protocol = R/'mediabridge/app/src/main/java/io/github/rhsr1024/interop/BridgeProtocol.java'
dest = R/'src/io/github/rhsr1024/interop/BridgeProtocol.java'
assert dest.read_bytes() == protocol.read_bytes(), 'Protocol copies differ'
classes=WORK/'classes'; classes.mkdir(exist_ok=True)
sources=list((R/'src').rglob('*.java'))+list((R/'stubs').rglob('*.java'))
args=H/'javac.args'
args.write_text('\n'.join('"'+str(p).replace('\\','/')+'"' for p in sources),encoding='utf-8')
run([JDK/'bin/javac.exe','-encoding','UTF-8','-source','8','-target','8','-cp',ANDROID,'-d',classes,'@'+str(args)])
helper=H/'helper.jar'
with zipfile.ZipFile(helper,'w') as z:
    for p in classes.rglob('*.class'):
        n=p.relative_to(classes).as_posix()
        if n.startswith('cn/manstep/phonemirrorBox/bridge/') or n=='io/github/rhsr1024/interop/BridgeProtocol.class':
            z.write(p,n)
out=WORK/'d8'; out.mkdir(exist_ok=True)
run([JDK/'bin/java.exe','-cp',SDK/'build-tools/35.0.0/lib/d8.jar','com.android.tools.r8.D8',
     '--min-api','16','--lib',ANDROID,'--classpath',classes,'--output',out,helper])
assert {p.name for p in out.glob('*.dex')} == {'classes.dex'}, 'Unexpected multidex helper output'
run([JDK/'bin/javac.exe','-cp',JAR,'-d',H,R/'tools/BridgeDexTool.java'])
smali=WORK/'smali'
run([JDK/'bin/java.exe','-cp',CP,'BridgeDexTool','dis',BASE,smali])
def edit(path,signature,fn):
    p=smali/path; s=p.read_text(encoding='utf-8')
    pattern=r'(?m)^\.method '+re.escape(signature)+r'\n[\s\S]*?^\.end method'
    matches=list(re.finditer(pattern,s)); assert len(matches)==1,signature
    m=matches[0]; s=s[:m.start()]+fn(m.group())+s[m.end():]; p.write_text(s,encoding='utf-8')
def init(m):
    assert '    .registers 4' in m
    # Keep the original F25 setup on pre-Android-9 systems, where MediaBridge
    # itself cannot run. New helper classes are desugared at the original minSdk.
    return m.replace('    .registers 4','''    .registers 4
    sget v0, Landroid/os/Build$VERSION;->SDK_INT:I
    const/16 v1, 0x1c
    if-lt v0, v1, :usb_bridge_original_init
    iget-object v0, p0, Lcom/zqsdk/OooOo00;->OooO0OO:Landroid/content/Context;
    iget-object v1, p0, Lcom/zqsdk/OooOo00;->OooO0Oo:Lcom/zqsdk/callBack/IInputCallback;
    invoke-static {v0, v1}, Lcn/manstep/phonemirrorBox/bridge/UsbMediaBridge;->start(Landroid/content/Context;Lcom/zqsdk/callBack/IInputCallback;)V
    return-void
    :usb_bridge_original_init''')
edit('com/zqsdk/OooOo00.smali','public final OooO00o()V',init)
def metadata(m):
    # p1 has been reused by the original method. v0 still holds its parsed JSON;
    # all original instructions/branches/registers remain in their original order.
    anchor='    :try_end_bd'
    assert m.count(anchor)==1
    return m.replace(anchor,'    invoke-static {v0}, Lcn/manstep/phonemirrorBox/bridge/UsbMediaBridge;->metadataObject(Lorg/json/JSONObject;)V\n'+anchor)
edit('cn/manstep/phonemirrorBox/v0/d.smali','public C(Ljava/lang/String;)V',metadata)
def close(m):
    assert m.count('    return-void')==1
    return m.replace('    return-void','    invoke-static {}, Lcn/manstep/phonemirrorBox/bridge/UsbMediaBridge;->close()V\n    return-void')
edit('cn/manstep/phonemirrorBox/third/ZqUtil.smali','protected release()V',close)
def settings(m):
    assert m.count('    return-object p1')==1
    return m.replace('    return-object p1','    invoke-static {p1}, Lcn/manstep/phonemirrorBox/bridge/BridgeSettings;->wrap(Landroid/view/View;)Landroid/view/View;\n    move-result-object p1\n    return-object p1')
edit('cn/manstep/phonemirrorBox/y.smali','public N0(Landroid/view/LayoutInflater;Landroid/view/ViewGroup;Landroid/os/Bundle;)Landroid/view/View;',settings)
dex=H/'classes.dex'
run([JDK/'bin/java.exe','-cp',CP,'BridgeDexTool','merge',BASE,smali,out/'classes.dex',dex])
def signature(n): return bool(re.fullmatch(r'META-INF/(?:MANIFEST\.MF|[^/]+\.(?:SF|RSA|DSA|EC))',n,re.I))
with zipfile.ZipFile(BASE) as base, zipfile.ZipFile(H/'usb-unsigned.apk','w') as z:
    for info in base.infolist():
        if not signature(info.filename):
            z.writestr(copy.copy(info),dex.read_bytes() if info.filename=='classes.dex' else base.read(info.filename),compresslevel=9)
print('Built',H/'usb-unsigned.apk')
