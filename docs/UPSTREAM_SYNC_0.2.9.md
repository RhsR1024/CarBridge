# CarBridge 0.1.4 — DiPlay 0.2.9 同步

日期：2026-10-02。本次为上游源码同步；原有吉利定制继续维护，CarBridge 版本独立递增。

## 不可变基线

| 项目 | 记录 |
| --- | --- |
| 同步前 CarBridge | `575a9b46d7f887dc7e79faf6a6639328d565ab8c`（0.1.3） |
| 原始上游基线 | `v0.2.8 / f2d06951b4e8114dbb62f551c12a32a845a3042f` |
| 已获取的 upstream/main / v0.2.9 | `18429e737228e8d75d9b6c850af89dcca2f591b6` |
| 集成分支 | `sync/diplay-0.2.9` |
| 合并提交 | `61c9771dd7f7d202bfff3074013f72ac8ff8af6b` |
| 历史保留 | 双亲 merge，第一父为同步前 CarBridge，第二父为上述 v0.2.9；本报告随合并提交保存 |
| CarBridge 版本 | mobile 与 automotive 均为 `0.1.4 / 104`；mobile debug 为 `0.1.4-debug` |
| 包名 | `io.github.rhsr1024.carbridge`；debug 后缀 `.debug`；automotive 后缀 `.automotive` |
| MediaBridge | 本次未修改；BridgeProtocol 保持 1.0 |

上游 v0.2.8 → v0.2.9 共 86 个文件变化，4,847 行增加、189 行删除。接入的功能包括中控地图悬浮卡片、启动器地图嵌入与示例、导航小组件、地图缩放修复、BYD 仪表歌曲、昼夜模式轮询、摄像头窗口策略、音道 0–20 与旧导航设置读取、乌克兰语，以及 GPS 未知方向处理。

## 定制保留与冲突决策

| 范围 | 合并结果与验证依据 |
| --- | --- |
| 吉利方控与双模式 | ECARX 模块、CarPlayMediaKeys、CarBridgeMediaRuntime、CarBridgeRouteManager 和协作协议源码保持；媒体按键与路由测试继续覆盖 |
| 媒体／导航音量 | GuidanceActivity、AudioFocusCoordinator、MusicOutputGate、PlaybackPolicy 保持；Host 的前台音量目标绑定与退出恢复保留；AndroidMediaSink 的音频处理保持原实现，本次新增视频镜像输出 |
| 音道设置 | 采用上游 0–20 范围；导航新设置键缺省时兼容读取旧键，显式选择 0 不被覆盖；AudioChannelPersistenceTest 覆盖 |
| 图标与车型 | 原有图片资源无变更，Geely 默认标签、用户覆盖和 BYD 备选资源保留；新增 BYD 仪表歌曲显式受车型开关控制，资源测试覆盖切换而不清除设置 |
| 歌曲和协作 | nowplaying、playback 与 `io/github/rhsr1024/interop` 保持；Controller 同时保留原歌曲管线和上游新增 CarPlayGlance 摘要 |
| 显示／触摸 | 默认旋转不重连，原可选旋转稳定后重连保持；接入摄像头窗口缩小与恢复不重连、在小窗口建立连接后放大需重连的上游策略；画面和触摸统一使用协商后的会话尺寸，缩放分辨率不再使用窗口尺寸代替画布 |
| 包名和文案 | 保留 CarBridge 包名、图标和独立版本；乌克兰语沿用已有品牌替换范围，新地图说明使用 CarBridge，保留上游署名／许可文字 |
| 构建 | settings 同时保留 ecarx 与新增 home/maphost；可选认证目录为空字符串时按未设置处理，不隐式引入认证文件 |

实际冲突文件为 common Manifest、CarPlayHostActivity、mobile 构建文件、settings、CarPlayTouchMapper 和 CarPlayController。按行为合并，没有整文件采用任一侧覆盖。原 CarPlayViewport 名称通过类型别名保留，既有调用与上游 CarPlayVideoLayout 共用几何实现。

新增地图悬浮／嵌入仍处于 BYD／仪表地图的设置范围；未扩展为已经在吉利验证的地图功能。导航摘要可供标准 Android 小组件使用。嵌入发现 action 保留 `com.shihab.diplay.action.EMBED_MAP` 以兼容协议，包名相关说明已注明 CarBridge 的实际身份。

## 自动化验证

| 检查 | 结果 |
| --- | --- |
| shared 单元测试 | 341 通过，0 失败，0 跳过 |
| common 单元测试 | 129 通过，0 失败，0 跳过 |
| home 单元测试 | 4 通过，0 失败，0 跳过 |
| 合计 | **474 通过**，0 失败／错误／跳过 |
| mobile lintDebug / lintRelease | 0 错误，分别 18 / 4 个警告 |
| home / maphost lintDebug | 0 错误，分别 5 / 2 个警告 |
| mobile / automotive / home / maphost assembleDebug | 全部成功 |
| public tree 检查 | 通过，无凭证容器或私钥块 |
| 差异与资源检查 | 无冲突标记、差异格式错误；已有图片资源与同步前一致 |
| 文档链接 | 51 个本地链接全部有效 |

最终完整命令退出码为 0：`BUILD SUCCESSFUL in 5m 15s`，411 个任务中 145 个执行、266 个复用缓存。警告主要涉及未使用资源、已有 TLS TrustManager、界面字符串和上游示例图标／构造器提示，本次没有宣称 lint 零警告。

重点测试包含 GuidanceActivityTest、GuidanceFocusTest、MusicOutputGateTest、PlaybackPolicyTest、CarPlayMediaCallbackTest、CarBridgeRouteManagerTest、CarBridgeRotationRestartTest、CarPlayHostDisplaySizeTest、AudioChannelPersistenceTest、CarBridgeResourcesTest，以及新地图／小组件摘要相关测试。

本地证据：`build/sync-0.2.9/checks.log`、`artifact-checks.json`，各模块的 `build/test-results/testDebugUnitTest` 和 `build/reports/lint-results-*.xml`。这些属于未纳入 Git 的构建输出。

四个 APK 的包名和版本经 aapt 核对，均未包含 runtime 认证资产。mobile debug 继续沿用既有测试 Manifest 的桌面名称 `DiPlay HUD Test`；正式应用资源和 automotive 显示 CarBridge，乌克兰语资源保持同一品牌。没有构建或签署本次正式车测包。

| 源码构建产物 | SHA-256 |
| --- | --- |
| `mobile/build/outputs/apk/debug/mobile-debug.apk` | `fc088737e22ec37d2cdbcb49791c29d3b3b6685e66f49f532c0561ade17422e7` |
| `automotive/build/outputs/apk/debug/automotive-debug.apk` | `c5d29e8ddc82bc9e35cde597a6642338dd57a49f79d5020659c10ff5f382c9fc` |
| `samples/home/build/outputs/apk/debug/home-debug.apk` | `b278fb0b686a82f4ea41335004161842a82fee49bc87d17e42687a7da2e26eed` |
| `samples/maphost/build/outputs/apk/debug/maphost-debug.apk` | `1a28b2c035c9e36259d015c0ae0e7161755d1d9cb06219a3a05f841485e40622` |


复现检查：

```powershell
python scripts/check_public_tree.py
.\gradlew.bat :shared:testDebugUnitTest :common:testDebugUnitTest :home:testDebugUnitTest :mobile:lintDebug :mobile:lintRelease :home:lintDebug :maphost:lintDebug :mobile:assembleDebug :automotive:assembleDebug :home:assembleDebug :maphost:assembleDebug --no-configuration-cache --console=plain
```

工具链为 JDK 25.0.3+9、Android SDK 37、NDK 28.2.13676358 及仓库 Gradle wrapper。新增启动器测试需要 Robolectric 的 API 36 运行库；本机下载曾停滞，已从 Maven Central 补齐并验证 SHA-512 后重跑。

## 设备验证边界与回退

0.1.3 + MediaBridge 2.3.3-carbridge-26100204 的吉利 FX11-A2 / Android 11 车测记录仍见 [既有车辆报告](VEHICLE_VERIFIED_0.1.3.md)。该记录确认的是旧版，不作为本次 0.1.4 的实车验收。此次没有安装车机、连接 iPhone 或重新验证 ROM 的私有音道行为，也未修改外部 MediaBridge 仓库。

后续车测重点为：直连与桥接方控、歌曲切换与暂停／恢复、音乐互斥、导航播报期间滚轮调节导航音量及结束后恢复媒体音量、已有音道设置、图标、横竖屏与倒车窗口往返。使用原签名及明确提供的运行时认证资产构建车测包并覆盖升级，以保留设置；普通源码 debug APK 不作为独立连接 iPhone 的交付包。

同步前提交和原上游标签保留。需要回退时可在独立分支检出同步前提交；若对共享主线撤回本次同步，应以 merge 第一父为主线创建 revert 提交。当前仅完成本地集成，未推送远端或发布版本。
