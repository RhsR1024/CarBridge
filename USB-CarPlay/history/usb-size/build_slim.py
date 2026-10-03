from pathlib import Path
import copy, hashlib, json, re, zipfile

ROOT = Path(__file__).resolve().parents[2]
SOURCE = ROOT / 'deliverables/USBBox-F25-MultiTouch-Fix-20261002/CarPlay-USBBox-F25-MultiTouch-Fix.apk'
EXPECTED = '79ffe2174498a84f9b84522780fc56cb6a9693ca8e6d8aab6340e4b92029c139'
OUT = ROOT / 'deliverables/USBBox-Slim-MediaBridge-20261003'
OUT.mkdir(exist_ok=True)
assert hashlib.sha256(SOURCE.read_bytes()).hexdigest() == EXPECTED

def sig(n): return bool(re.fullmatch(r'META-INF/(?:MANIFEST\.MF|[^/]+\.(?:SF|RSA|DSA|EC))', n, re.I))
with zipfile.ZipFile(SOURCE) as src:
    names = [n for n in src.namelist() if re.fullmatch(r'assets/p+\.bin', n)]
    assert len(names) == 1
    padding = names[0]
    data = src.read(padding)
    assert len(data) == 23396044 and data == b'P' * len(data)
    needles = [padding.encode(), padding.removeprefix('assets/').encode(),
               padding.encode('utf-16le'), padding.removeprefix('assets/').encode('utf-16le')]
    references = []
    for entry in src.infolist():
        if entry.filename == padding or sig(entry.filename): continue
        body = src.read(entry.filename)
        if any(needle in body for needle in needles): references.append(entry.filename)
    assert not references, references
    unsigned = ROOT / 'analysis/usb-size/slim-unsigned.apk'
    with zipfile.ZipFile(unsigned, 'w', compression=zipfile.ZIP_DEFLATED, compresslevel=9) as dst:
        for entry in src.infolist():
            if entry.filename == padding or sig(entry.filename): continue
            dst.writestr(copy.copy(entry), src.read(entry.filename), compresslevel=9)
    with zipfile.ZipFile(unsigned) as dst:
        assert dst.testzip() is None
        assert {n for n in src.namelist() if not sig(n)} - set(dst.namelist()) == {padding}
        for n in dst.namelist(): assert dst.read(n) == src.read(n), n
report = {'source_sha256': EXPECTED, 'source_bytes': SOURCE.stat().st_size,
          'removed_asset': padding, 'removed_bytes': len(data), 'unique_byte': 'P (0x50)',
          'literal_references_outside_signatures': references,
          'all_remaining_file_contents_identical': True, 'dex_unchanged': True,
          'native_libraries_resources_manifest_unchanged': True,
          'note': 'Pure padding removal; no code logic changes. Dynamic construction cannot be disproved solely by literal scanning.'}
(OUT / 'slim-verification.json').write_text(json.dumps(report, indent=2), encoding='utf-8')
print(json.dumps(report, indent=2))
