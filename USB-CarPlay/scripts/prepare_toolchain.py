"""Fetch pinned public tools/sample signer, or import verified local copies. No private keys required."""
from pathlib import Path
import argparse, base64, io, shutil, urllib.request, zipfile
from toolchain import ROOT, checked, JADX_SHA, KEY_SHA, CERT_SHA
p=argparse.ArgumentParser();p.add_argument('--jadx');p.add_argument('--key');p.add_argument('--cert');args=p.parse_args()
cache=ROOT/'_tooling';cache.mkdir(exist_ok=True)
def obtain(name,source,digest,url=None,decode=False):
    dst=cache/name
    if dst.exists():return checked(dst,digest)
    if source:shutil.copyfile(source,dst)
    elif url:
        with urllib.request.urlopen(url,timeout=90) as response:data=response.read()
        dst.write_bytes(base64.b64decode(data) if decode else data)
    return checked(dst,digest)
jar=cache/'jadx-1.5.6-all.jar'
if args.jadx or jar.exists():obtain(jar.name,args.jadx,JADX_SHA)
else:
    url='https://github.com/skylot/jadx/releases/download/v1.5.6/jadx-1.5.6.zip'
    with urllib.request.urlopen(url,timeout=90) as response,zipfile.ZipFile(io.BytesIO(response.read())) as z:
        matches=[n for n in z.namelist() if n.endswith('/jadx-1.5.6-all.jar') or n=='jadx-1.5.6-all.jar']
        assert len(matches)==1;jar.write_bytes(z.read(matches[0]))
    checked(jar,JADX_SHA)
upstream='https://android.googlesource.com/platform/build/+/refs/tags/android-14.0.0_r1/target/product/security/'
obtain('aosp-public-platform.pk8',args.key,KEY_SHA,upstream+'platform.pk8?format=TEXT',True)
obtain('aosp-platform.x509.pem',args.cert,CERT_SHA,upstream+'platform.x509.pem?format=TEXT',True)
print('Pinned tool and PUBLIC AOSP sample signer are ready in',cache)
