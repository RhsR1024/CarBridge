"""Enable the existing multi-touch sender for CarPlay; do not rebuild other code."""
from pathlib import Path
import copy
import hashlib
import json
import re
import struct
import sys
import zipfile
import zlib

HERE = Path(__file__).resolve().parent
ROOT = HERE.parent.parent
sys.path.insert(0, str(ROOT / 'analysis/tools/python'))
from loguru import logger
logger.remove()
from androguard.core.dex import DEX

SOURCE = ROOT / 'CarPlay - 副本.apk'
EXPECTED = '14659850cd85dda1b1f156ca65438d2beb866de6d7c6101408a62866c4dc1e6d'
assert hashlib.sha256(SOURCE.read_bytes()).hexdigest() == EXPECTED

def target(data):
    dex = DEX(data)
    methods = [m for c in dex.get_classes()
               if c.get_name() == 'Lcn/manstep/phonemirrorBox/p0/c;'
               for m in c.get_methods() if m.get_name() == 'a' and m.get_descriptor() == '()Z']
    assert len(methods) == 1
    return methods[0]

def signature_entry(name):
    return bool(re.fullmatch(r'META-INF/(?:MANIFEST\.MF|[^/]+\.(?:SF|RSA|DSA|EC))', name, re.I))

with zipfile.ZipFile(SOURCE) as original:
    old = original.read('classes.dex')
    method = target(old)
    instructions = list(method.get_instructions())
    assert [bytes(i.get_raw()) for i in instructions] == [b'\x12\x00', b'\x0f\x00']
    offset = method.get_code().get_off() + 16
    assert old[offset:offset + 4] == b'\x12\x00\x0f\x00'
    patched = bytearray(old)
    patched[offset + 1] = 0x10  # const/4 v0, 1; return v0
    patched[12:32] = hashlib.sha1(patched[32:]).digest()
    struct.pack_into('<I', patched, 8, zlib.adler32(patched[12:]) & 0xffffffff)
    changes = [i for i, (a, b) in enumerate(zip(old, patched)) if a != b]
    assert [i for i in changes if i >= 32] == [offset + 1]
    assert [bytes(i.get_raw()) for i in target(bytes(patched)).get_instructions()] == [b'\x12\x10', b'\x0f\x00']
    unsigned = HERE / 'probe-unsigned.apk'
    with zipfile.ZipFile(unsigned, 'w') as output:
        for entry in original.infolist():
            if signature_entry(entry.filename):
                continue
            data = bytes(patched) if entry.filename == 'classes.dex' else original.read(entry.filename)
            output.writestr(copy.copy(entry), data)
    with zipfile.ZipFile(unsigned) as output:
        assert output.testzip() is None
        for name in output.namelist():
            if name != 'classes.dex':
                assert output.read(name) == original.read(name), name

report = {
    'original_apk_sha256': EXPECTED,
    'modified_method': 'Lcn/manstep/phonemirrorBox/p0/c;->a()Z',
    'instruction_offset': offset,
    'old_instructions_hex': '12000f00',
    'new_instructions_hex': '12100f00',
    'non_header_dex_changed_offsets': [offset + 1],
    'other_zip_payloads_unchanged': True,
    'original_apk_unchanged': hashlib.sha256(SOURCE.read_bytes()).hexdigest() == EXPECTED,
    'purpose': 'Experimental existing USB message 23 path in CarPlay; requires box firmware validation',
}
(HERE / 'patch-verification.json').write_text(json.dumps(report, indent=2), encoding='utf-8')
print(json.dumps(report, indent=2))
