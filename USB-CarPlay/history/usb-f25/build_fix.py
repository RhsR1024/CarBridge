"""Build a scoped F25 playback repair on the car-tested multitouch APK."""
from pathlib import Path
import copy
import hashlib
import json
import re
import subprocess
import sys
import zipfile

HERE = Path(__file__).resolve().parent
ROOT = HERE.parent.parent
BASE = ROOT / 'deliverables/USBBox-MultiTouch-Probe-20261002/CarPlay-USBBox-MultiTouch-Probe.apk'
BASE_SHA = '87420bcf9f6bf8729d835e81bebb7f9eb7a92d239053c4c55d63d205ed6d8cb9'
ORIGINAL = ROOT / 'CarPlay - 副本.apk'
ORIGINAL_SHA = '14659850cd85dda1b1f156ca65438d2beb866de6d7c6101408a62866c4dc1e6d'
JAVA = Path('D:/CarSoft/MediaBridgeApp/tooling/jdk/jdk-17.0.16+8/bin/java.exe')
CP = str(ROOT / 'analysis/tools/jadx/lib/jadx-1.5.6-all.jar') + ';' + str(HERE)
SMALI = HERE / 'smali'
assert hashlib.sha256(BASE.read_bytes()).hexdigest() == BASE_SHA
assert hashlib.sha256(ORIGINAL.read_bytes()).hexdigest() == ORIGINAL_SHA
subprocess.run([str(JAVA), '-cp', CP, 'DexTool', 'dis', str(BASE), str(SMALI)], check=True)

def edit_method(text, signature, transform):
    pattern = r'(?m)^\.method ' + re.escape(signature) + r'\n[\s\S]*?^\.end method'
    found = list(re.finditer(pattern, text))
    assert len(found) == 1, signature
    m = found[0]
    return text[:m.start()] + transform(m.group()) + text[m.end():]

def replace_body(text, signature, body):
    return edit_method(text, signature, lambda _: '.method ' + signature + '\n' + body.strip() + '\n.end method')

# Preserve explicit Android media key semantics through the ZQ adapter.
path = SMALI / 'cn/manstep/phonemirrorBox/third/ZqUtil$c.smali'
s = path.read_text(encoding='utf-8')
def key_tail(method):
    a = method.index('    const/16 v1, 0x7e')
    return method[:a] + '''    invoke-interface {v0, p1}, Lcn/manstep/phonemirrorBox/third/b;->b(I)V

    :cond_40
    return-void
.end method'''
s = edit_method(s, 'public onKey(I)V', key_tail)
path.write_text(s, encoding='utf-8')

# F25 acknowledges supported callbacks. Explicit pause also cancels Android
# focus/resume state before forwarding the phone pause command.
path = SMALI / 'com/zqsdk/OooOo0.smali'
s = path.read_text(encoding='utf-8')
for name, key in [('onPlay', 126), ('onPause', 127), ('onNext', 87), ('onPrevious', 88)]:
    pause = '''
    invoke-static {}, Lcn/manstep/phonemirrorBox/e0/c;->n()Lcn/manstep/phonemirrorBox/e0/c;
    move-result-object v1
    invoke-virtual {v1}, Lcn/manstep/phonemirrorBox/e0/c;->s()V
''' if name == 'onPause' else ''
    s = replace_body(s, f'public {name}()Z', f'''
    .registers 3
    iget-object v0, p0, Lcom/zqsdk/OooOo0;->OooO0O0:Lcom/zqsdk/callBack/IInputCallback;
    if-eqz v0, :unhandled
    {pause}
    const/16 v1, {key:#x}
    invoke-interface {{v0, v1}}, Lcom/zqsdk/callBack/IInputCallback;->onKey(I)V
    const/4 v0, 0x1
    return v0
    :unhandled
    const/4 v0, 0x0
    return v0
''')
path.write_text(s, encoding='utf-8')

# Registration and pure metadata updates must not claim playback.
path = SMALI / 'com/zqsdk/OooOo00$OooO0O0.smali'
s = path.read_text(encoding='utf-8')
call = '    invoke-static {p1}, Lcom/zqsdk/OooOo00;->OooO0oo(Lcom/zqsdk/OooOo00;)V'
assert s.count(call) == 2
s = s.replace(call, '    # Registration publishes state without requesting playback.')
path.write_text(s, encoding='utf-8')

path = SMALI / 'com/zqsdk/OooOo00.smali'
s = path.read_text(encoding='utf-8')
call = '    invoke-virtual {p0}, Lcom/zqsdk/OooOo00;->OooO0O0()V'
def metadata_only(m):
    assert m.count(call) == 1
    return m.replace(call, '    # Metadata is not a play request.')
s = edit_method(s, 'public final OooO00o(Ljava/lang/String;Ljava/lang/String;)V', metadata_only)
def metadata_state(m):
    start = m.index('    :goto_c0\n')
    end = m.index('    :try_end_df', start)
    return m[:start] + '''    :goto_c0
    invoke-virtual {p0, p3}, Lcom/zqsdk/OooOo00;->OooO00o(Z)V
''' + m[end:]
s = edit_method(s, 'public final OooO00o(Ljava/lang/String;Ljava/lang/String;Z)V', metadata_state)
s = replace_body(s, 'public final OooO00o(Z)V', '''
    .registers 7
    iget-object v0, p0, Lcom/zqsdk/OooOo00;->OooO0o0:Lcom/ecarx/eas/sdk/mediacenter/MediaCenterAPI;
    if-eqz v0, :done
    iget-boolean v1, p0, Lcom/zqsdk/OooOo00;->OooO00o:Z
    if-eqz v1, :done
    iget-object v1, p0, Lcom/zqsdk/OooOo00;->OooO0OO:Landroid/content/Context;
    if-eqz v1, :done
    iget-object v1, p0, Lcom/zqsdk/OooOo00;->OooO0oO:Lcom/zqsdk/OooOo0O;
    if-eqz v1, :done
    invoke-virtual {v1}, Lcom/zqsdk/OooOo0O;->getPlaybackStatus()I
    move-result v2
    invoke-virtual {v1, p1}, Lcom/zqsdk/OooOo0O;->OooO00o(I)V
    iget-object v3, p0, Lcom/zqsdk/OooOo00;->OooO0O0:Ljava/lang/Object;
    const/4 v4, 0x6
    invoke-interface {v0, v3, v4}, Lcom/ecarx/eas/sdk/mediacenter/IMediaCenterAPI;->updateCurrentSourceType(Ljava/lang/Object;I)V
    invoke-interface {v0, v3, v1}, Lcom/ecarx/eas/sdk/mediacenter/IMediaCenterAPI;->updateMusicPlaybackState(Ljava/lang/Object;Lcom/ecarx/eas/sdk/mediacenter/MusicPlaybackInfo;)Z
    # A real transition into playing can select the source; repeated echoes and
    # every paused update only publish state.
    if-eqz p1, :done
    const/4 v4, 0x1
    if-eq v2, v4, :done
    invoke-virtual {p0}, Lcom/zqsdk/OooOo00;->OooO0O0()V
    :done
    return-void
''')
path.write_text(s, encoding='utf-8')

# Re-check cancellation and focus at execution time, retaining the original
# already-playing check and consuming the pending resume before sending play.
path = SMALI / 'cn/manstep/phonemirrorBox/e0/c$a$a.smali'
s = path.read_text(encoding='utf-8')
s = replace_body(s, 'public run()V', '''
    .registers 4
    iget-object v0, p0, Lcn/manstep/phonemirrorBox/e0/c$a$a;->c:Lcn/manstep/phonemirrorBox/e0/c$a;
    iget-object v0, v0, Lcn/manstep/phonemirrorBox/e0/c$a;->a:Lcn/manstep/phonemirrorBox/e0/c;
    invoke-static {v0}, Lcn/manstep/phonemirrorBox/e0/c;->i(Lcn/manstep/phonemirrorBox/e0/c;)Z
    move-result v1
    if-eqz v1, :done
    invoke-virtual {v0}, Lcn/manstep/phonemirrorBox/e0/c;->o()Z
    move-result v1
    if-eqz v1, :done
    const/4 v1, 0x0
    invoke-static {v0, v1}, Lcn/manstep/phonemirrorBox/e0/c;->j(Lcn/manstep/phonemirrorBox/e0/c;Z)Z
    invoke-static {}, Lcn/manstep/phonemirrorBox/t0/a;->t()Lcn/manstep/phonemirrorBox/t0/a;
    move-result-object v2
    invoke-virtual {v2}, Lcn/manstep/phonemirrorBox/t0/a;->y()Z
    move-result v1
    if-nez v1, :done
    const/16 v1, 0xc9
    invoke-static {v1}, Lcn/manstep/phonemirrorBox/t0/e;->H(I)V
    :done
    return-void
''')
path.write_text(s, encoding='utf-8')

DEX_OUT = HERE / 'classes.dex'
subprocess.run([str(JAVA), '-cp', CP, 'DexTool', 'merge', str(BASE), str(SMALI), str(DEX_OUT)], check=True)
UNSIGNED = HERE / 'f25-unsigned.apk'
def signature_entry(name):
    return bool(re.fullmatch(r'META-INF/(?:MANIFEST\.MF|[^/]+\.(?:SF|RSA|DSA|EC))', name, re.I))
with zipfile.ZipFile(BASE) as base, zipfile.ZipFile(UNSIGNED, 'w') as out:
    for entry in base.infolist():
        if signature_entry(entry.filename): continue
        data = DEX_OUT.read_bytes() if entry.filename == 'classes.dex' else base.read(entry.filename)
        out.writestr(copy.copy(entry), data)
with zipfile.ZipFile(BASE) as base, zipfile.ZipFile(UNSIGNED) as out:
    assert out.testzip() is None
    for name in out.namelist():
        if name != 'classes.dex': assert out.read(name) == base.read(name), name
print('Built:', UNSIGNED)
assert hashlib.sha256(BASE.read_bytes()).hexdigest() == BASE_SHA
assert hashlib.sha256(ORIGINAL.read_bytes()).hexdigest() == ORIGINAL_SHA
