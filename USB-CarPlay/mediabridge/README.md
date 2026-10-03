# MediaBridge 自维护版

这是面向博越 L 的媒体桥接工程，正式包名为 `com.mediabridge.app`，可与车机商店网易云共存。实现保留原版标准 MediaCenter 通道，并增加 F25 的 service-pool/OpenAPI/EAS 发现路径、断链恢复、收藏状态保护、歌词与封面缓存和诊断导出。原始 APK 与反编译参考不参与编译。

## 独立来源与原生优先

已补充车机媒体来源目录登记，保留其他来源；页面“应用忽略名单”是原生优先的配置依据，目录扫描不会自动改变名单。勾选的播放器播放时让位，未勾选播放器再次开始播放后恢复桥接。焦点切走后不凭旧元数据、旧暂停会话或重连抢回小窗。

独立包名接入小窗及第三方桥接已由用户实车确认，作为主线身份方案保留。26100101 固定小窗来源名称，并针对本车 CarPlay 空控制会话增加保护；这两项新增修复仍需实车验证。

新包需要重新开启通知访问并配置忽略名单。请在旧桥页面停止桥接，避免双桥竞争。登记接口、目录读回、小窗实际显示和方控需分别在本车验收，详见 [ADR-0002](docs/decisions/ADR-0002-independent-source-priority.md)。

## 本地构建

使用 JDK 17、Android SDK 34 和仓库内 Gradle Wrapper：

```powershell
$env:JAVA_HOME = 'C:\path\to\jdk-17'
$env:ANDROID_SDK_ROOT = 'C:\path\to\android-sdk'
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug assembleRelease --continue
```

中文路径导致 Gradle worker 异常时，按 [docs/BUILDING.md](docs/BUILDING.md) 将工程和工具链映射到 ASCII 盘符。当前版本为 `2.2.0-carplay-guard-26100101` / versionCode `26100101`。未提供正式签名参数时，release 产物为未签名 APK；本轮另生成了使用本机 Android Debug 证书的测试包，仅用于受控安装测试。

## 当前功能边界

- 标准 MediaCenter：EAS main binder、direct fallback、注册、元数据、进度、歌词、收藏和控制回传均有本地协议夹具测试。
- F25：已实现三种服务发现和共享 client 协议，但目标车的 descriptor、权限和实际车机显示仍需实车确认；代码不会把 Binder 调用成功伪报为车机显示成功。
- 收藏：未知状态或方向不明时不声明可写能力；请求按当前曲目/session/输入版本确认，超时显示 UNKNOWN。
- 在线歌词和在线封面默认关闭，只有用户在设置中明确开启后才会访问网络。
- 默认权限仅为网络、网络状态、前台服务、特殊用途前台服务、通知和开机广播；不使用 shared UID、系统设置、息屏、AVAS 或悬浮窗权限。

## 手机调试台（Debug 构建）

Debug APK 使用 `.dev` 包名，可在普通 Android 9 及以上手机中开启“手机调试模式”。该模式以本地调试界面替代 ECARX MediaCenter 输出，但继续复用正式版的播放器发现、元数据、封面、歌词、`LegacyMusicClient` 控制和收藏确认链。调试台支持当前曲目、实时进度、同步歌词、上一首、播放/暂停、下一首、收藏/取消收藏、真实 MediaSession Queue、公开 MediaBrowser 根目录探测，以及带超时和状态回传判定的自动验证报告。

Release 构建不会显示调试入口，调试 Activity 也处于禁用状态；MediaBrowser 的包可见性查询也只合入 Debug Manifest。手机调试只能验证播放器侧行为，不能替代 ECARX Binder、真实方向盘、中控/仪表/HUD、音频焦点和车辆生命周期的实车验收。播放列表与推荐列表仅在播放器真实提供 Queue 或 MediaBrowser 数据后接入，不在调试台中合成。

## 实车验证

安装后开启通知访问，先验证标准模式，再验证 F25。记录车机 ROM、MediaCenter/EAS 版本、播放器包名和版本、当前模式、卡片/仪表/HUD 显示以及标准 Binder 是否被拒绝。完整状态见 `docs/修复记录-2026-09-26.md`、`docs/COMPATIBILITY_MATRIX.md`、`docs/TEST_LOG.md` 和 `docs/PROTOCOL_NOTES.md`。
