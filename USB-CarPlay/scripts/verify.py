"""All-original-method and payload regression check, relative to the 7.55 MB baseline."""
from pathlib import Path
import hashlib, json, re, sys, zipfile
R=Path(__file__).resolve().parents[1]; H=R/'_build'; H.mkdir(exist_ok=True)
# Install scripts/requirements.txt into a local virtual environment.
from loguru import logger
logger.remove()
from androguard.core.dex import DEX
base=R/'baselines/CarPlay-USBBox-F25-MultiTouch-Slim.apk'
target=Path(sys.argv[1]) if len(sys.argv)>1 else H/'usb-unsigned.apk'
def sig(n):return bool(re.fullmatch(r'META-INF/(?:MANIFEST\.MF|[^/]+\.(?:SF|RSA|DSA|EC))',n,re.I))
with zipfile.ZipFile(base) as a,zipfile.ZipFile(target) as b:
    assert b.testzip() is None
    an={n for n in a.namelist() if not sig(n)};bn={n for n in b.namelist() if not sig(n)}
    assert an==bn,(an-bn,bn-an)
    for n in an-{'classes.dex'}:assert a.read(n)==b.read(n),n
    old=DEX(a.read('classes.dex'));new=DEX(b.read('classes.dex'))
def key(m):return m.get_class_name(),m.get_name(),m.get_descriptor()
def methods(d):return {key(m):m for c in d.get_classes() for m in c.get_methods()}
def insns(m):return [(i.get_name(),i.get_output()) for i in m.get_instructions()]
om,nm=methods(old),methods(new);assert om.keys()<=nm.keys()
changed={k for k in om if insns(om[k])!=insns(nm[k])}
expected={('Lcom/zqsdk/OooOo00;','OooO00o','()V'),
 ('Lcn/manstep/phonemirrorBox/v0/d;','C','(Ljava/lang/String;)V'),
 ('Lcn/manstep/phonemirrorBox/v0/d;','A','([B)V'),
 ('Lcn/manstep/phonemirrorBox/third/ZqUtil;','release','()V'),
 ('Lcn/manstep/phonemirrorBox/y;','N0','(Landroid/view/LayoutInflater; Landroid/view/ViewGroup; Landroid/os/Bundle;)Landroid/view/View;')}
assert changed==expected,('unexpected',changed-expected,'missing',expected-changed)
# Pre-Android-9 F25 initialization remains exactly the baseline implementation.
init_key=('Lcom/zqsdk/OooOo00;','OooO00o','()V')
assert insns(nm[init_key])[-len(insns(om[init_key])):]==insns(om[init_key])
oc={c.get_name():c for c in old.get_classes()};nc={c.get_name():c for c in new.get_classes()}
def fields(c):return [(f.get_name(),f.get_descriptor(),f.get_access_flags()) for f in c.get_fields()]
for n,c in oc.items():
    assert fields(c)==fields(nc[n]),n
    assert c.get_access_flags()==nc[n].get_access_flags(),n
    assert c.get_superclassname()==nc[n].get_superclassname(),n
    assert c.get_interfaces()==nc[n].get_interfaces(),n
for k in om:
    assert om[k].get_access_flags()==nm[k].get_access_flags(),k
    if k not in changed and om[k].get_code():
        assert om[k].get_code().get_registers_size()==nm[k].get_code().get_registers_size(),k
for n in nc.keys()-oc.keys():
    assert n.startswith('Lcn/manstep/phonemirrorBox/bridge/') or n=='Lio/github/rhsr1024/interop/BridgeProtocol;',n
# For the appended hooks the original instruction stream must remain literally identical
# after removing only the expected extra instructions (no branch rewriting is necessary).
for k in expected:
    if k[0]=='Lcom/zqsdk/OooOo00;':continue
    seq=insns(nm[k]); before=insns(om[k]); cleaned=[]; skip_result=False
    for op,operand in seq:
        if 'Lcn/manstep/phonemirrorBox/bridge/' in operand:
            skip_result='->wrap(' in operand;continue
        if skip_result:
            assert op=='move-result-object';skip_result=False;continue
        cleaned.append((op,operand))
    # Four hooks are inserted at method tail, leaving branch deltas unchanged.
    assert cleaned==before,k
for prefix in ['Lcn/manstep/phonemirrorBox/BoxInterface/', 'Lcn/manstep/phonemirrorBox/e0/',
               'Lcn/manstep/phonemirrorBox/t0/', 'Lcn/manstep/phonemirrorBox/p0/']:
    assert not any(k[0].startswith(prefix) for k in changed),prefix
assert insns(nm[('Lcn/manstep/phonemirrorBox/p0/c;','a','()Z')])==[('const/4','v0, 1'),('return','v0')]
refs=[]
for k in nm.keys()-om.keys():
    for op,out in insns(nm[k]):
        # The only references to original app logic are READ of connection and
        # constructing/calling the original control callback.
        if 'Lcn/manstep/phonemirrorBox/' in out and '/bridge/' not in out:
            assert op=='sget-boolean' and 'BoxInterface/f;->P Z' in out,(k,op,out)
        if re.search(r'Lcom/zqsdk/[^;]+;->',out) and op.startswith('invoke-'):
            assert 'Lcom/zqsdk/OooOo0;->' in out,(k,op,out)
            assert any('->'+name in out for name in ['<init>(', 'onPlay()', 'onPause()', 'onNext()', 'onPrevious()', 'onForward()', 'onRewind()']),(k,out)
            refs.append({'caller':''.join(k),'call':out})
# Verify all added references into original app/SDK resolve to a real member,
# including inherited methods. This catches compile-only ABI stub mistakes.
def resolve_method(cls,name,desc,visited=None):
    if (cls,name,desc) in nm:return True
    seen=set() if visited is None else visited
    if cls in seen or cls not in nc:return False
    seen.add(cls);c=nc[cls]
    parents=[c.get_superclassname()]+list(c.get_interfaces())
    return any(resolve_method(p,name,desc,seen) for p in parents)
for k in nm.keys()-om.keys():
    for op,out in insns(nm[k]):
        if not op.startswith('invoke-'):continue
        match=re.search(r'(L(?:com/(?:zqsdk|ecarx)/|cn/manstep/phonemirrorBox/)[^;]+;)->([^ (]+)(\(.*)',out)
        if match:
            assert resolve_method(*match.groups()),(k,out)
report={'baseline':base.name,'baseline_sha256':hashlib.sha256(base.read_bytes()).hexdigest(),
 'target':target.name,'bytes':target.stat().st_size,'sha256':hashlib.sha256(target.read_bytes()).hexdigest(),
 'original_method_count':len(om),'original_unchanged_methods':len(om)-len(changed),
 'changed_original_methods':[''.join(k) for k in sorted(changed)],'new_classes':len(nc)-len(oc),
 'all_original_class_fields_and_signatures_preserved':True,
 'four_tail_hooks_preserve_all_original_instructions':True,
 'usb_transport_audio_focus_touch_and_original_control_methods_unchanged':True,
 'native_libraries_resources_manifest_and_other_payloads_unchanged':True,
 'multitouch_enabled':True,'added_original_app_references_verified':refs,
 'scope':'Static DEX/payload/ABI verification; Android tests are separate; no physical vehicle test.'}
(H/'regression-verification.json').write_text(json.dumps(report,indent=2,ensure_ascii=False),encoding='utf-8')
print(json.dumps({k:v for k,v in report.items() if k!='added_original_app_references_verified'},ensure_ascii=False,indent=2))
