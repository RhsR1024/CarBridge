param(
    [string]$Tooling = 'E:\DevTools\MediaBridgeTooling',
    [string]$MediaBridgeRoot = 'D:\CarSoft\MediaBridgeApp\MediaBridge-src',
    [string]$AuthenticationAssets = $env:DIPLAY_AUTH_ASSETS_DIR,
    [string]$OutputDirectory = '',
    [switch]$UseLocalTestKey,
    [switch]$IncludePhoneDebug
)
$ErrorActionPreference = 'Stop'
$CarBridgeRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$carBridgeVersion = [regex]::Match([IO.File]::ReadAllText((Join-Path $CarBridgeRoot 'mobile/build.gradle.kts')), 'versionName\s*=\s*"([^"]+)"').Groups[1].Value
$mediaBridgeVersion = [regex]::Match([IO.File]::ReadAllText((Join-Path $MediaBridgeRoot 'app/build.gradle.kts')), 'versionName\s*=\s*"([^"]+)"').Groups[1].Value
if (-not $carBridgeVersion -or -not $mediaBridgeVersion) { throw 'Could not read paired source versions.' }
if (-not $OutputDirectory) { $OutputDirectory = "D:\WorkSpace\CarPlay\deliverables\CarBridge-$carBridgeVersion-MediaBridge-$mediaBridgeVersion" }
if (-not $AuthenticationAssets) { throw 'Provide the explicitly authorized CarPlay authentication asset directory.' }
foreach ($name in @('identity.pk8', 'certificate.p7b')) {
    if (-not (Test-Path -LiteralPath (Join-Path $AuthenticationAssets "offline-mfi\$name") -PathType Leaf)) {
        throw 'Incomplete CarPlay authentication input.'
    }
}
$contract = 'src\main\java\io\github\rhsr1024\interop\BridgeProtocol.java'
if ((Get-FileHash (Join-Path $CarBridgeRoot "common\$contract")).Hash -ne
    (Get-FileHash (Join-Path $MediaBridgeRoot "app\$contract")).Hash) { throw 'Paired protocol source differs.' }
$envNames = @('JAVA_HOME','ANDROID_HOME','ANDROID_SDK_ROOT','GRADLE_USER_HOME','TEMP','TMP','GRADLE_OPTS','DIPLAY_AUTH_ASSETS_DIR',
    'ANDROID_KEYSTORE_PATH','ANDROID_KEYSTORE_PASSWORD','ANDROID_KEY_ALIAS','ANDROID_KEY_PASSWORD',
    'MEDIABRIDGE_STORE_FILE','MEDIABRIDGE_STORE_PASSWORD','MEDIABRIDGE_KEY_ALIAS','MEDIABRIDGE_KEY_PASSWORD')
$saved = @{}
foreach ($name in $envNames) { $saved[$name] = [Environment]::GetEnvironmentVariable($name, 'Process') }
try {
    if ($UseLocalTestKey) {
        $key = Join-Path $env:USERPROFILE '.android\debug.keystore'
        if (-not (Test-Path -LiteralPath $key)) { throw 'Local Android test keystore was not found.' }
        $env:ANDROID_KEYSTORE_PATH = $key; $env:ANDROID_KEYSTORE_PASSWORD = 'android'
        $env:ANDROID_KEY_ALIAS = 'androiddebugkey'; $env:ANDROID_KEY_PASSWORD = 'android'
        $env:MEDIABRIDGE_STORE_FILE = $key; $env:MEDIABRIDGE_STORE_PASSWORD = 'android'
        $env:MEDIABRIDGE_KEY_ALIAS = 'androiddebugkey'; $env:MEDIABRIDGE_KEY_PASSWORD = 'android'
    }
    foreach ($name in $envNames | Where-Object { $_ -match '^(ANDROID_KEY|MEDIABRIDGE_)' }) {
        if (-not [Environment]::GetEnvironmentVariable($name, 'Process')) { throw "Missing release signing input: $name" }
    }
    $env:ANDROID_HOME = Join-Path $Tooling 'android-sdk'
    $env:ANDROID_SDK_ROOT = $env:ANDROID_HOME
    $env:GRADLE_USER_HOME = Join-Path $Tooling 'gradle-user-home'
    $taskTemp = Join-Path $Tooling 'tmp\paired-release'
    New-Item -ItemType Directory -Path $taskTemp -Force | Out-Null
    $env:TEMP = $taskTemp; $env:TMP = $taskTemp
    $env:GRADLE_OPTS = "$($saved['GRADLE_OPTS']) `"-Djava.io.tmpdir=$taskTemp`""
    $env:DIPLAY_AUTH_ASSETS_DIR = (Resolve-Path -LiteralPath $AuthenticationAssets).Path
    $env:JAVA_HOME = Join-Path $Tooling 'jdk\carbridge-25\jdk-25.0.3+9'
    Push-Location $CarBridgeRoot
    try {
        & .\gradlew.bat :shared:testDebugUnitTest :common:testDebugUnitTest :mobile:lintRelease :mobile:verifyStandaloneAuthentication :mobile:assembleRelease --no-configuration-cache --console=plain
        if ($LASTEXITCODE -ne 0) { throw 'CarBridge checks failed.' }
    } finally { Pop-Location }
    $env:JAVA_HOME = Join-Path $Tooling 'jdk\jdk-17.0.16+8'
    Push-Location $MediaBridgeRoot
    try {
        $checks = @(':app:testDebugUnitTest', ':app:lintRelease', ':app:assembleRelease')
        if ($IncludePhoneDebug) { $checks += @(':app:lintDebug', ':app:assembleDebug') }
        & (Join-Path $Tooling 'gradle\gradle-8.9\bin\gradle.bat') @checks --console=plain
        if ($LASTEXITCODE -ne 0) { throw 'MediaBridge checks failed.' }
    } finally { Pop-Location }
    New-Item -ItemType Directory -Path $OutputDirectory -Force | Out-Null
    Copy-Item -LiteralPath (Join-Path $CarBridgeRoot 'mobile\build\outputs\apk\release\mobile-release.apk') -Destination (Join-Path $OutputDirectory "CarBridge-$carBridgeVersion.apk")
    Copy-Item -LiteralPath (Join-Path $MediaBridgeRoot 'app\build\outputs\apk\release\app-release.apk') -Destination (Join-Path $OutputDirectory "MediaBridge-$mediaBridgeVersion.apk")
    if ($IncludePhoneDebug) {
        Copy-Item -LiteralPath (Join-Path $MediaBridgeRoot 'app\build\outputs\apk\debug\app-debug.apk') -Destination (Join-Path $OutputDirectory "MediaBridge-$mediaBridgeVersion-dev.apk")
    }
    foreach ($apk in Get-ChildItem -LiteralPath $OutputDirectory -Filter '*.apk') {
        & (Join-Path $env:JAVA_HOME 'bin\java.exe') -jar (Join-Path $Tooling 'android-sdk\build-tools\37.0.0\lib\apksigner.jar') verify --print-certs $apk.FullName
        if ($LASTEXITCODE -ne 0) { throw "Signature verification failed: $($apk.Name)" }
        Get-FileHash -LiteralPath $apk.FullName -Algorithm SHA256
    }
} finally {
    foreach ($name in $envNames) { [Environment]::SetEnvironmentVariable($name, $saved[$name], 'Process') }
}
