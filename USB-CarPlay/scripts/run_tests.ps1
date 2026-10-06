$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
if (-not $env:JAVA_HOME) { $env:JAVA_HOME = 'E:\DevTools\MediaBridgeTooling\jdk\jdk-17.0.16+8' }
if (-not $env:ANDROID_SDK_ROOT) { $env:ANDROID_SDK_ROOT = 'E:\DevTools\MediaBridgeTooling\android-sdk' }
if (-not $env:GRADLE_USER_HOME) { $env:GRADLE_USER_HOME = 'E:\DevTools\MediaBridgeTooling\gradle-user-home' }
$taskTemp = Join-Path $projectRoot '_build/tmp'
New-Item -ItemType Directory -Force -Path $taskTemp | Out-Null
$env:TEMP = $taskTemp
$env:TMP = $taskTemp
$env:GRADLE_OPTS = "-Djava.io.tmpdir=$taskTemp"
$gradleBin = $env:GRADLE_BIN
if (-not $gradleBin) { $gradleBin = 'E:\DevTools\MediaBridgeTooling\gradle\gradle-8.9\bin\gradle.bat' }
& $gradleBin --no-daemon -p (Join-Path $projectRoot 'test-project') testDebugUnitTest
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
$mediaBridgeRoot = $env:MEDIABRIDGE_ROOT
if (-not $mediaBridgeRoot) { $mediaBridgeRoot = 'D:\WorkSpace\MediaBridge-src' }
& $gradleBin --no-daemon -p $mediaBridgeRoot testDebugUnitTest lintDebug assembleRelease
exit $LASTEXITCODE
