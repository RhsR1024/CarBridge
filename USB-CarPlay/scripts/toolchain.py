from pathlib import Path
import hashlib, os
ROOT=Path(__file__).resolve().parents[1]
JADX_SHA='fe3e12c45acf75f92369685fd02d1d7a7323385dc725680a9b98a0dac0ea554b'
KEY_SHA='1ad8ef556870edb70f69a9d3c112544c07de5162ba440d84d33f8bb0c5962875'
CERT_SHA='9837de028f460c35cc8d3fa45f14eecce30f6fbfe4b93d399aef1acb80c20d14'
def checked(path,digest):
    path=Path(path)
    if not path.is_file(): raise SystemExit(f'Missing {path}; run scripts/prepare_toolchain.py or configure its path.')
    if hashlib.sha256(path.read_bytes()).hexdigest()!=digest: raise SystemExit(f'Unexpected SHA-256: {path}')
    return path
def java_home():
    p=Path(os.environ.get('JAVA_HOME','E:/DevTools/MediaBridgeTooling/jdk/jdk-17.0.16+8'))
    if not (p/'bin/java.exe').is_file():raise SystemExit('Set JAVA_HOME to JDK 17 on Windows.')
    return p
def android_sdk():
    p=Path(os.environ.get('ANDROID_SDK_ROOT','E:/DevTools/MediaBridgeTooling/android-sdk'))
    if not (p/'platforms/android-34/android.jar').is_file():raise SystemExit('Set ANDROID_SDK_ROOT; install platform 34 and build-tools 35.0.0.')
    return p
def jadx_jar():return checked(os.environ.get('JADX_JAR',ROOT/'_tooling/jadx-1.5.6-all.jar'),JADX_SHA)
def public_key():return checked(ROOT/'_tooling/aosp-public-platform.pk8',KEY_SHA)
def public_cert():return checked(ROOT/'_tooling/aosp-platform.x509.pem',CERT_SHA)
