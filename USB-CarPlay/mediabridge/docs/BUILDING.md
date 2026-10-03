# 构建说明

## 固定工具链

- JDK 17
- Gradle Wrapper 8.9
- Android Gradle Plugin 8.7.3
- Android compileSdk/targetSdk 34，minSdk 28

工程不依赖反编译目录参与编译，也不在 Gradle 文件中保存开发机绝对路径、签名密码或商业凭据。

## 当前 Windows 工作站路径

以下路径是 `DESKTOP-IQJOR7V` 上已验证的本地工具与测试签名位置；它们是构建操作说明，不写入 Gradle 源码，也不代表其他机器必须使用相同路径：

- JDK：`D:\CarSoft\MediaBridgeApp\tooling\jdk\jdk-17.0.16+8`
- Android SDK：`D:\CarSoft\MediaBridgeApp\tooling\android-sdk`
- Gradle 8.9：`D:\CarSoft\MediaBridgeApp\tooling\gradle\gradle-8.9\bin\gradle.bat`
- Gradle 缓存：`D:\CarSoft\MediaBridgeApp\tooling\gradle-user-home`
- 构建/测试临时目录：`D:\CarSoft\MediaBridgeApp\tooling\tmp\mediabridge-tests`
- Android Debug 测试证书：`C:\Users\public.DESKTOP-IQJOR7V\.android\debug.keystore`

当前工作站的 C 盘空间很小。运行测试和构建时必须把 `TEMP`、`TMP`、`GRADLE_USER_HOME` 和 `java.io.tmpdir` 指到上面的 D 盘目录，否则 Robolectric 可能以 `NativeLibraryLoader` / `FileOutputStream` 报错，造成全套测试同时假失败。

```powershell
$taskTemp = 'D:\CarSoft\MediaBridgeApp\tooling\tmp\mediabridge-tests'
$env:JAVA_HOME = 'D:\CarSoft\MediaBridgeApp\tooling\jdk\jdk-17.0.16+8'
$env:ANDROID_SDK_ROOT = 'D:\CarSoft\MediaBridgeApp\tooling\android-sdk'
$env:GRADLE_USER_HOME = 'D:\CarSoft\MediaBridgeApp\tooling\gradle-user-home'
$env:TEMP = $taskTemp
$env:TMP = $taskTemp
$env:GRADLE_OPTS = "-Djava.io.tmpdir=$taskTemp"
& 'D:\CarSoft\MediaBridgeApp\tooling\gradle\gradle-8.9\bin\gradle.bat' --no-daemon testDebugUnitTest lintDebug assembleDebug
```

测试证书的别名/口令不得写入仓库；签名时通过环境变量或 Gradle property 注入。证书用途和覆盖安装限制见 `SIGNING_AND_ROLLBACK.md`。

## Windows

```powershell
$env:JAVA_HOME = 'C:\path\to\jdk-17'
$env:ANDROID_SDK_ROOT = 'C:\path\to\android-sdk'
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug
```

若工程、SDK、JDK 或 Gradle 缓存路径含中文，某些 Windows JDK/Gradle Worker 组合会错误解析 classpath。应把它们的共同父目录映射到 ASCII 盘符，并从映射后的目录运行；这不改变源码或产物：

```powershell
$workspace = (Resolve-Path ..).Path # 此目录同时包含 MediaBridge-src 与本地 tooling
subst M: $workspace
try {
    $env:JAVA_HOME = 'M:\tooling\jdk\jdk-17.0.16+8'
    $env:ANDROID_SDK_ROOT = 'M:\tooling\android-sdk'
    $env:GRADLE_USER_HOME = 'M:\tooling\gradle-user-home'
    Push-Location 'M:\MediaBridge-src'
    .\gradlew.bat testDebugUnitTest lintDebug assembleDebug assembleRelease --continue
    Pop-Location
} finally { subst M: /d }
```

## macOS / Linux

```bash
export JAVA_HOME=/path/to/jdk-17
export ANDROID_SDK_ROOT=/path/to/android-sdk
./gradlew testDebugUnitTest lintDebug assembleDebug
```

macOS 步骤是可执行构建说明；当前版本尚未在一台干净 macOS 主机上实跑，结果必须记录在 `TEST_LOG.md`，不能以 Windows 通过替代 T23 的双平台验收。

## Release 签名

不提供签名参数时，`assembleRelease` 生成未签名 release 包。需要签名时，从环境变量或同名 Gradle property 注入以下四项：

- `MEDIABRIDGE_STORE_FILE`
- `MEDIABRIDGE_STORE_PASSWORD`
- `MEDIABRIDGE_KEY_ALIAS`
- `MEDIABRIDGE_KEY_PASSWORD`

示例：

```powershell
$env:MEDIABRIDGE_STORE_FILE = 'D:\secure\mediabridge.keystore'
$env:MEDIABRIDGE_STORE_PASSWORD = '<从安全存储读取>'
$env:MEDIABRIDGE_KEY_ALIAS = '<alias>'
$env:MEDIABRIDGE_KEY_PASSWORD = '<从安全存储读取>'
.\gradlew.bat assembleRelease
```

签名身份和覆盖安装限制见 `SIGNING_AND_ROLLBACK.md`。
