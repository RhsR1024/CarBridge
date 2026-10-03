param(
    [Parameter(Mandatory = $true)]
    [string]$Apk,
    [string]$AndroidSdk = $env:ANDROID_SDK_ROOT,
    [string]$ExpectedPackage = 'com.mediabridge.app',
    [int]$ExpectedVersionCode = 26100101,
    [string]$ExpectedCertificateSha256 = ''
)

$ErrorActionPreference = 'Stop'
$resolvedApk = (Resolve-Path -LiteralPath $Apk).Path
if (-not $AndroidSdk) { throw 'Set ANDROID_SDK_ROOT or pass -AndroidSdk.' }
$buildToolsRoot = Join-Path $AndroidSdk 'build-tools'
$buildTools = Get-ChildItem -LiteralPath $buildToolsRoot -Directory |
    Sort-Object { [version]$_.Name } -Descending |
    Select-Object -First 1
if (-not $buildTools) { throw "No Android build-tools found under $buildToolsRoot" }

$aapt = Join-Path $buildTools.FullName 'aapt.exe'
$apksigner = Join-Path $buildTools.FullName 'apksigner.bat'
if (-not (Test-Path -LiteralPath $aapt)) { $aapt = Join-Path $buildTools.FullName 'aapt' }
if (-not (Test-Path -LiteralPath $apksigner)) { $apksigner = Join-Path $buildTools.FullName 'apksigner' }

# Some Windows aapt builds cannot open non-ASCII paths. Audit an exact temporary copy.
$temporary = [System.IO.Path]::Combine([System.IO.Path]::GetTempPath(),
    "mediabridge-audit-$([guid]::NewGuid().ToString('N')).apk")
Copy-Item -LiteralPath $resolvedApk -Destination $temporary
try {
    $hash = (Get-FileHash -LiteralPath $resolvedApk -Algorithm SHA256).Hash
    "APK: $resolvedApk"
    "SHA-256: $hash"
    $badging = & $aapt dump badging $temporary
    if ($LASTEXITCODE -ne 0) { throw 'aapt badging failed' }
    $packageLine = $badging | Where-Object { $_ -match '^package:' } | Select-Object -First 1
    if ($packageLine -notmatch "name='$([regex]::Escape($ExpectedPackage))'" -or
            $packageLine -notmatch "versionCode='$ExpectedVersionCode'") { throw "Unexpected APK identity: $packageLine" }
    if ($badging -notcontains "sdkVersion:'28'" -or $badging -notcontains "targetSdkVersion:'34'") { throw 'Unexpected SDK levels' }
    $badging | Where-Object {
        $_ -match "^(package:|sdkVersion:|targetSdkVersion:|application-label:|application-icon-|launchable-activity:)"
    }
    $permissions = & $aapt dump permissions $temporary
    if ($LASTEXITCODE -ne 0) { throw 'aapt permissions failed' }
    $allowed = @('android.permission.INTERNET', 'android.permission.ACCESS_NETWORK_STATE',
        'android.permission.FOREGROUND_SERVICE', 'android.permission.FOREGROUND_SERVICE_SPECIAL_USE',
        'android.permission.POST_NOTIFICATIONS', 'android.permission.RECEIVE_BOOT_COMPLETED',
        'android.permission.WRITE_EXTERNAL_STORAGE')
    $actual = @($permissions | ForEach-Object { if ($_ -match "uses-permission: name='([^']+)'") { $Matches[1] } })
    if (@(Compare-Object $allowed $actual).Count -ne 0) { throw "Permission allowlist mismatch: $actual" }
    $manifest = & $aapt dump xmltree $temporary AndroidManifest.xml
    if ($LASTEXITCODE -ne 0) { throw 'aapt manifest failed' }
    if ($manifest -match 'sharedUserId|ScreenOff|Avas|AVAS|WRITE_SECURE_SETTINGS|WRITE_SETTINGS|DEVICE_POWER|INJECT_EVENTS') { throw 'Forbidden manifest capability found' }
    $manifestText = $manifest -join "`n"
    $legacyStoragePermission = '(?s)uses-permission.*?android.permission.WRITE_EXTERNAL_STORAGE.*?android:maxSdkVersion.*?0x(?:0+)?1c'
    if (-not ($manifestText -match $legacyStoragePermission)) { throw 'Legacy storage permission must be limited to maxSdkVersion 28' }
    if (-not ($manifest -match 'com.geely.auto.music.MediaListenerService')) { throw 'Notification listener compatibility name missing' }
    if (-not ($manifest -match ([regex]::Escape($ExpectedPackage + '.artwork')))) { throw 'Artwork authority mismatch' }
    $certificates = & $apksigner verify --verbose --print-certs $temporary
    if ($LASTEXITCODE -ne 0) { throw "APK signature verification failed: $LASTEXITCODE" }
    if ($ExpectedCertificateSha256 -and -not ($certificates -match $ExpectedCertificateSha256.Replace(':', ''))) { throw 'Signing certificate mismatch' }
    $permissions
    $certificates
    'PASS: identity, SDK, permission allowlist, forbidden capabilities, listener, authority and signature'
} finally {
    Remove-Item -LiteralPath $temporary -Force -ErrorAction SilentlyContinue
}
