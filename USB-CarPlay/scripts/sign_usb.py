from pathlib import Path
import argparse, subprocess
from toolchain import ROOT,java_home,android_sdk,public_key,public_cert
p=argparse.ArgumentParser();p.add_argument('--input',type=Path,default=ROOT/'_build/usb-unsigned.apk')
p.add_argument('--output',type=Path,default=ROOT/'_build/CarPlay-USBBox-Slim-MediaModes.apk');a=p.parse_args()
bt=android_sdk()/'build-tools/35.0.0';java=java_home()/'bin/java.exe'
def run(args):subprocess.run([str(x) for x in args],check=True)
a.output.parent.mkdir(parents=True,exist_ok=True)
aligned=a.output.with_suffix('.aligned.apk')
run([bt/'zipalign.exe','-f','-p','4',a.input,aligned])
run([java,'-jar',bt/'lib/apksigner.jar','sign','--key',public_key(),'--cert',public_cert(),
     '--v1-signing-enabled','true','--v2-signing-enabled','true','--v3-signing-enabled','true',
     '--out',a.output,aligned])
run([bt/'zipalign.exe','-c','-v','4',a.output])
run([java,'-jar',bt/'lib/apksigner.jar','verify','--verbose','--print-certs',a.output])
print('Signed:',a.output)
