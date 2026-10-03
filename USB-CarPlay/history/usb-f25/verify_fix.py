"""Compare all DEX methods, then exercise actual patched instructions with service doubles.

This is a bounded bytecode regression harness, not an Android/vehicle runtime.
Unsupported opcodes fail rather than silently emulating Android behavior.
"""
from pathlib import Path
import hashlib
import json
import sys
import zipfile

HERE = Path(__file__).resolve().parent
ROOT = HERE.parent.parent
sys.path.insert(0, str(ROOT / 'analysis/tools/python'))
from loguru import logger
logger.remove()
from androguard.core.dex import DEX

BASE = ROOT / 'deliverables/USBBox-MultiTouch-Probe-20261002/CarPlay-USBBox-MultiTouch-Probe.apk'
with zipfile.ZipFile(BASE) as z: old = DEX(z.read('classes.dex'))
new = DEX((HERE / 'classes.dex').read_bytes())

def key(m): return (m.get_class_name(), m.get_name(), m.get_descriptor())
def methods(d): return {key(m): m for c in d.get_classes() for m in c.get_methods()}
om, nm = methods(old), methods(new)
assert om.keys() == nm.keys()
assert {c.get_name() for c in old.get_classes()} == {c.get_name() for c in new.get_classes()}
def insns(m):
    return [(i.get_name(), i.get_output()) for i in m.get_instructions()]
changed = {k for k in om if insns(om[k]) != insns(nm[k])}
F25 = 'Lcom/zqsdk/OooOo00;'
CLIENT = 'Lcom/zqsdk/OooOo0;'
INFO = 'Lcom/zqsdk/OooOo0O;'
ZQ = 'Lcn/manstep/phonemirrorBox/third/ZqUtil$c;'
FOCUS = 'Lcn/manstep/phonemirrorBox/e0/c;'
RUN = 'Lcn/manstep/phonemirrorBox/e0/c$a$a;'
STATE = 'Lcn/manstep/phonemirrorBox/t0/a;'
CONTROL = 'Lcn/manstep/phonemirrorBox/t0/e;'
ROUTER = 'Lcn/manstep/phonemirrorBox/third/c;'
expected = {(F25, 'OooO00o', d) for d in ['(Z)V', '(Ljava/lang/String; Ljava/lang/String;)V', '(Ljava/lang/String; Ljava/lang/String; Z)V']}
expected |= {(CLIENT, n, '()Z') for n in ['onPlay', 'onPause', 'onNext', 'onPrevious']}
expected |= {(ZQ, 'onKey', '(I)V'), (RUN, 'run', '()V'), ('Lcom/zqsdk/OooOo00$OooO0O0;', 'onAPIReady', '(Z)V')}
assert changed == expected, ('unexpected', changed - expected, 'missing', expected - changed)
for k in om:
    assert om[k].get_access_flags() == nm[k].get_access_flags(), k
    if k not in changed and om[k].get_code():
        assert om[k].get_code().get_registers_size() == nm[k].get_code().get_registers_size(), k
def fields(d):
    return {(c.get_name(), f.get_name(), f.get_descriptor(), f.get_access_flags())
            for c in d.get_classes() for f in c.get_fields()}
assert fields(old) == fields(new)
assert insns(nm[('Lcn/manstep/phonemirrorBox/p0/c;', 'a', '()Z')]) == [('const/4', 'v0, 1'), ('return', 'v0')]

# Check legal branch targets and register/invoke limits in every edited body.
for k in changed:
    m = nm[k]
    code = m.get_code()
    offsets = dict(m.get_instructions_idx())
    for pc, i in offsets.items():
        op = i.get_name()
        operands = i.get_operands()
        for operand in operands:
            if int(operand[0]) == 0: assert 0 <= operand[1] < code.get_registers_size(), (k, pc)
        if op.startswith(('if-', 'goto')):
            assert pc + operands[-1][1] * 2 in offsets, (k, pc)
        if op.startswith('invoke-'):
            assert sum(int(o[0]) == 0 for o in operands) <= code.get_outs_size(), (k, pc)

class Obj(dict):
    def __init__(self, cls, **values): super().__init__(values); self.cls = cls
    def __bool__(self): return True

class VM:
    def __init__(self):
        self.events = []
        self.focus = Obj(FOCUS, i=1, j=False, k=0, f1312e=0)
        self.state = Obj(STATE, playing=False)
        self.route = Obj(ROUTER)
        self.callback = Obj(ZQ, a=Obj('Lcn/manstep/phonemirrorBox/third/ZqUtil;', callback=self.route))
        self.client = Obj(CLIENT, OooO0O0=self.callback)
        self.info = Obj(INFO, OooO0OO=0)
        self.manager = Obj(F25, OooO00o=1, OooO0O0=Obj('token'), OooO0OO=Obj('context'),
                           OooO0o0=Obj('media-api'), OooO0oO=self.info)

    def invoke(self, cls, name, desc, args):
        if cls == FOCUS and name == 'n': return self.focus
        if cls == STATE and name == 't': return self.state
        if cls == STATE and name == 'y': return self.state['playing']
        if cls == STATE and name in ('w', 'v', 'C'): return False  # no call/Siri
        if cls == FOCUS and name == 's':
            # Android abandon-focus is external. Keep the actual app's documented
            # postconditions, separately checked against its unchanged DEX below.
            self.focus.update(i=0, j=False)
            self.events.append(('abandonFocus',))
            return
        if cls == CONTROL and name == 'H':
            # Boundary at the app's existing sender; assert the actual router's command.
            self.events.append(('boxCommand', args[0]))
            return
        if 'IMediaCenterAPI;' in cls:
            if name == 'requestPlay': self.events.append(('requestPlay',)); return 1
            if name == 'updateMusicPlaybackState': self.events.append(('publish', args[-1]['OooO0OO'])); return 1
            if name == 'updateCurrentSourceType': return
        if cls == 'Lcom/zqsdk/OooOOOo;': return 'decoded'
        if cls == 'Lf/d0/d/l;': return
        if cls == 'Lcom/zqsdk/OooOo0o;': return True
        if cls == 'Lcn/manstep/phonemirrorBox/util/s;': return
        if cls == 'Ljava/lang/StringBuilder;': return 'log' if name == 'toString' else args[0]
        if cls == 'Lcom/zqsdk/callBack/IInputCallback;': cls = args[0].cls
        if cls == 'Lcn/manstep/phonemirrorBox/third/b;': cls = args[0].cls
        return self.run((cls, name, desc), args)

    def run(self, method_key, args):
        m = nm[method_key]
        code = m.get_code()
        r = [0] * code.get_registers_size()
        assert len(args) == code.get_ins_size(), (method_key, args)
        r[len(r)-len(args):] = args
        instructions = dict(m.get_instructions_idx())
        pc, result, steps = 0, None, 0
        while True:
            steps += 1
            assert steps < 4000, method_key
            i = instructions[pc]
            op, a = i.get_name(), i.get_operands()
            nxt = pc + i.get_length()
            if op == 'nop': pass
            elif op.startswith('const-string'): r[a[0][1]] = a[-1][-1]
            elif op.startswith('const'): r[a[0][1]] = a[1][1]
            elif op.startswith('move-result'): r[a[0][1]] = result
            elif op.startswith('move'): r[a[0][1]] = r[a[1][1]]
            elif op.startswith('iget'):
                ref = a[-1][-1]; field = ref.split('->')[1].split(' ')[0]
                r[a[0][1]] = r[a[1][1]].get(field, 0)
            elif op.startswith('iput'):
                ref = a[-1][-1]; field = ref.split('->')[1].split(' ')[0]
                r[a[1][1]][field] = r[a[0][1]]
            elif op.startswith('sget'): r[a[0][1]] = Obj('singleton')
            elif op == 'new-instance': r[a[0][1]] = Obj(a[-1][-1])
            elif op == 'new-array': r[a[0][1]] = [0] * r[a[1][1]]
            elif op == 'fill-array-data': pass  # only XOR logging strings in tested paths
            elif op.startswith('aput'): r[a[1][1]][r[a[2][1]]] = r[a[0][1]]
            elif op.startswith('if-'):
                left = r[a[0][1]]
                right = 0 if op.endswith('z') else r[a[1][1]]
                cond = op[3:].rstrip('z')
                ok = (left == right) if cond == 'eq' else (left != right) if cond == 'ne' else None
                assert ok is not None, op
                if ok: nxt = pc + a[-1][1] * 2
            elif op.startswith('goto'): nxt = pc + a[0][1] * 2
            elif op.startswith('invoke-'):
                values = [r[o[1]] for o in a if int(o[0]) == 0]
                ref = a[-1][-1]
                cls, rest = ref.split('->', 1)
                pos = rest.index('(')
                result = self.invoke(cls, rest[:pos], rest[pos:], values)
            elif op == 'return-void': return None
            elif op.startswith('return'): return r[a[0][1]]
            else: raise AssertionError((method_key, pc, op, a))
            pc = nxt

tests = []
def test(name, fn): fn(); tests.append(name)
def cb(vm, name): return vm.run((CLIENT, name, '()Z'), [vm.client])
def state(vm, playing): vm.run((F25, 'OooO00o', '(Z)V'), [vm.manager, int(playing)])
def resume(vm):
    obj = Obj(RUN, c=Obj('Lcn/manstep/phonemirrorBox/e0/c$a;', a=vm.focus))
    vm.run((RUN, 'run', '()V'), [obj])

def play_without_focus():
    vm = VM(); vm.focus.update(i=0)
    assert cb(vm, 'onPlay') == 1
    assert vm.events == [('boxCommand', 201)]
test('paused / no Android focus -> explicit PLAY 201', play_without_focus)

def pause_twice():
    vm = VM(); vm.focus.update(j=True)
    assert cb(vm, 'onPause') == cb(vm, 'onPause') == 1
    assert [e for e in vm.events if e[0] == 'boxCommand'] == [('boxCommand', 202)] * 2
    assert vm.focus['j'] is False
    vm.focus['i'] = 1  # a late gain callback must not revive cancelled resume
    resume(vm)
    assert ('boxCommand', 201) not in vm.events
test('repeated PAUSE is idempotent; queued resume stays cancelled', pause_twice)

def transitions():
    vm = VM(); state(vm, False); state(vm, False)
    assert ('requestPlay',) not in vm.events
    state(vm, True); state(vm, True)
    assert vm.events.count(('requestPlay',)) == 1
    state(vm, False)
    assert vm.events[-1] == ('publish', 0)
    state(vm, True)
    assert vm.events.count(('requestPlay',)) == 2
test('pause/repeated state never claims; each new playing transition claims once', transitions)

def metadata():
    for playing in (0, 1):
        vm = VM(); vm.info['OooO0OO'] = playing
        vm.run((F25, 'OooO00o', '(Ljava/lang/String; Ljava/lang/String;)V'), [vm.manager, 'title', 'artist'])
        assert ('requestPlay',) not in vm.events
        vm.run((F25, 'OooO00o', '(Ljava/lang/String; Ljava/lang/String; Z)V'), [vm.manager, 'title', 'artist', playing])
        assert ('requestPlay',) not in vm.events
    vm = VM()
    vm.run((F25, 'OooO00o', '(Ljava/lang/String; Ljava/lang/String; Z)V'), [vm.manager, 'title', 'artist', 1])
    state(vm, True)
    assert vm.events.count(('requestPlay',)) == 1
test('metadata and metadata+state use the same deduplicated state path', metadata)

def delayed():
    for pending, focus, playing, count in [(False, 1, False, 0), (True, 0, False, 0), (True, 1, True, 0), (True, 1, False, 1)]:
        vm = VM(); vm.focus.update(i=focus, j=pending); vm.state['playing'] = playing
        resume(vm)
        assert vm.events.count(('boxCommand', 201)) == count
        if focus == 1: assert not vm.focus['j']
        resume(vm)
        assert vm.events.count(('boxCommand', 201)) == count
test('delayed resume requires pending + focus + paused; executes at most once', delayed)

def callback_ack():
    for name, command in [('onPlay', 201), ('onPause', 202), ('onNext', 204), ('onPrevious', 205)]:
        vm = VM(); assert cb(vm, name) == 1
        assert ('boxCommand', command) in vm.events
        vm = VM(); vm.client['OooO0O0'] = 0
        assert cb(vm, name) == 0 and not vm.events
test('F25 supported callbacks acknowledge dispatch; absent callback stays unhandled', callback_ack)

def guards():
    for field in ('OooO00o', 'OooO0o0', 'OooO0OO', 'OooO0oO'):
        vm = VM(); vm.manager[field] = 0; state(vm, True); assert not vm.events
test('unready API / missing context / missing info do not claim', guards)

registration = insns(nm[('Lcom/zqsdk/OooOo00$OooO0O0;', 'onAPIReady', '(Z)V')])
assert not any('->OooO0oo(' in o or '->requestPlay(' in o for _, o in registration)
tests.append('F25 registration contains no playback request')
# Confirm the boundary stubs match the original unchanged app mechanisms.
release = insns(nm[(FOCUS, 's', '()V')])
assert any('->j Z' in o and op == 'iput-boolean' for op, o in release)
sender = insns(nm[(CONTROL, 'H', '(I)V')])
assert any('->u()I' in o for _, o in sender)
report = {
    'scope': 'Actual DEX control-flow regression with external Android/ECARX/USB service doubles; no car runtime test',
    'method_count': len(nm), 'unchanged_method_instruction_streams': len(nm) - len(changed),
    'changed_methods': [''.join(k) for k in sorted(changed)],
    'class_and_field_sets_unchanged': True, 'multitouch_still_enabled': True,
    'tests_passed': tests,
    'dex_sha256': hashlib.sha256((HERE / 'classes.dex').read_bytes()).hexdigest(),
}
(HERE / 'regression-verification.json').write_text(json.dumps(report, indent=2, ensure_ascii=False), encoding='utf-8')
print(json.dumps(report, indent=2, ensure_ascii=False))
