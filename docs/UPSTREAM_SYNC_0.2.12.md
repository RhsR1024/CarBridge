# CarBridge 0.1.10 — DiPlay 0.2.12 同步

日期：2026-10-05。本轮同步指定版本的代码，不推送、不发布、不将本机验证视为实车验收。

## 不可变基线

| 项目 | 记录 |
| --- | --- |
| 同步前 CarBridge | `e54215a1db794a9e334ab20011ba6646f9866f6c`（0.1.9） |
| 已集成上游 | `abe750f79dfb7ee4be613e36f2952a43778b3a04`（v0.2.11 后 main） |
| 本轮目标 | 稳定标签 `v0.2.12 / 22d2aacedcadc4ec1aef0d74b161b05321f708a7`；上游仍标注为公开预览版 |
| 集成分支 | `sync/diplay-0.2.12`；本报告随双亲合并提交记录，第一父为同步前 CarBridge，第二父为目标标签 |
| 版本 | mobile / automotive 均为 `0.1.10 / 110`；mobile debug 为 `0.1.10-debug` |
| 应用身份 | `io.github.rhsr1024.carbridge`；mobile debug 后缀 `.debug`，automotive 后缀 `.automotive` |
| MediaBridge | 未修改外部仓库，协作协议仍为 1.0 |

本轮不跟随标签之后的移动 main。上游相对旧基线有 220 个文件变更、14,746 行增加、889 行删除。主要包括现有 Wi-Fi／同一局域网连接、热点就绪和有界重试、Wi-Fi 扫描暂停与恢复、USB 权限辅助、分屏及虚拟仪表布局、独立系统栏、昼夜／环境光模式、画面调节、30–160% 自定义分辨率、可选方控缩放／摇杆、BYD 仪表恢复，以及诊断导出。

## 定制保留与冲突处理

十四个文本冲突文件：CHANGELOG、common AndroidManifest、CarPlayHostActivity、CarPlayMediaKeys、DiPlayActivity、英文／中文 strings、CarPlayHostDisplaySizeTest、CarPlayMediaCallbackTest、mobile 构建、BydNavigationOutputs、BydOutputSettings、CarPlayVideoLayout、CarPlayController。逐块合并，不整文件以原项目覆盖本项目。

- **吉利方控与媒体控制**：CarPlayMediaKeys 继续委托 CarBridgeMediaRuntime，不启用上游独立 MediaSession／焦点路径。保留 ECARX、直连／MediaBridge 单一路由、明确 PLAY/PAUSE、命令映射和音乐互斥。新增方控缩放和摇杆默认关闭，语音键 Siri 的 BYD 门控不变。
- **音道与音量**：AndroidMediaSink、GuidanceActivity、MusicOutputGate、PlaybackPolicy 核心代码未改；保留媒体／导航独立音道、导航播报期间前台音量目标及结束恢复、通话／Siri 优先级。Activity 生命周期同时处理原有导航音量绑定和上游昼夜控制器，避免重复回调。
- **歌曲与封面**：保留按连接／曲目代次接收、解析和发布的原生封面，以及只读 content URI。CarPlayController 保留 nowPlaying.begin／attachFiles／陈旧连接守卫。上游“下一首封面尚未到达时沿用上一首封面”与本项目防串图策略不兼容，明确不采用；相应仅用于上游独立媒体实现的三个辅助函数测试也不引入，原有代次及媒体回调测试保留。
- **图标与配置**：保留 Geely 原始图标资源、默认 Geely 标签、自定义图标与替换入口、默认车型／焦点／HEVC／60 fps。新增设置不覆盖媒体与导航音道或自定义 OEM 图标。新增六种语言界面的品牌文字统一为 CarBridge，未改协议身份或来源链接。
- **车型隔离**：BydOutputSettings 的新固件／接收器检测继续受所选 VehicleProfile 限制。BYD 恢复、仪表／HUD 输出只在 BYD 配置使用；通用 Wi-Fi 恢复放在车型返回之前。保存过的 BYD 仪表开关不能越过 GEELY／GENERIC 限制。
- **旋转与窗口**：保留默认旋转不重连、可选稳定旋转重连、触摸与会话画布映射及释放旧触点；接入新分屏／窗口尺寸处理和系统栏设置。CarPlayVideoLayout 保留 contains，采用上游无效尺寸回退。
- **身份与权限**：ECARX／MediaBridge 查询、封面 Provider 和独立包名保留。新增诊断 Provider 仅通过授权 URI 分享；USB 辅助与方控辅助为需用户启用的无障碍服务，未自动启用。

## 原有工作保护

原目录存在七个未提交文件，先保存补丁与所有 1,144 个已跟踪文件的 SHA-256，再在独立工作树合并。将原补丁复制到隔离目录参与组合验证，但不纳入同步提交：

- USB-CarPlay：BoxClock.java、BridgeSettings.java、TrackState.java、UsbMediaBridge.java、UsbBridgeTest.java。
- common：CarBridgeMediaRuntime.kt、CarBridgeRouteManager.kt。

验证前确认原目录 1,144 个文件均未变化，七个未提交文件在两目录逐字节相同，补丁相同。集成提交排除全部七个文件；原目录通过快进接收同步，不 stash、不 reset。原有 R3–R9 七个未跟踪 APK 也保持不动。USB-CarPlay、ecarx、原生 Now Playing／音乐互斥与核心音频模块不包含本轮同步修改。工作树间单纯 CRLF／LF 差异不作为功能变更；最终以原目录快进前后的哈希再次核验非同步范围。

## 测试适配与自动化结果

BYD 专属测试显式选择 BYD：BydClusterSongNoteTest、BydClusterSongTimerTest、DiLink3ClusterRecoveryTest、AdbClusterSelectionTest、ClusterActivityOutputTest、ClusterSafeAreaPersistenceTest、VirtualClusterDisplayTest，以及 CarPlayHostDisplaySizeTest 的仪表接管用例。Host 通用尺寸测试仍使用 GENERIC 以避免厂商服务绑定。没有放宽生产车型门控，也没有跳过失败用例。

新增 CarBridgeUpstreamCompatibilityTest 六项：固件检测不能覆盖车型选择、保存的仪表开关不能越过车型限制、方控新模式默认关闭且焦点保持开启、画面设置不覆盖音道和自定义图标、恢复 Geely 图标不重置音道、注入的媒体键映射保留明确播放／暂停且不吞音量键。

| 检查 | 结果 |
| --- | --- |
| shared 单元测试 | 700 通过，0 失败／错误／跳过 |
| common 单元测试 | 552 通过，0 失败／错误／跳过 |
| home 单元测试 | 4 通过，0 失败／错误／跳过 |
| USB 独立单元测试 | 32 通过，0 失败／错误／跳过；未调用外部 MediaBridge 构建 |
| mobile lint debug / release | 均 0 错误，分别 18 / 4 个警告 |
| home / maphost lint debug | 均 0 错误，分别 5 / 2 个警告 |
| 四模块 debug 构建 | mobile、automotive、home、maphost 均成功 |
| 资源／格式 | 无重复资源键、无冲突标记，git diff --check 通过 |
| public tree 检查 | 既有 9 个已跟踪 USB 分发 APK／公开证书导致失败；与同步前一致，并非本轮引入 |

主项目共 **1,256** 项，连同 USB 合计 **1,288** 项测试通过。最终主构建为 `BUILD SUCCESSFUL in 4m 6s`，411 个任务，退出码 0；USB 为 2m 10s，29 个任务，退出码 0。最初的重复 onPause 编译冲突和 BYD 测试默认车型问题已修复后重跑，不把初次失败作为最终结果。

主项目工具链：JDK 25.0.3+9、Gradle 9.5、Android SDK 37、NDK 28.2.13676358；USB 独立测试使用 JDK 17／Gradle 8.9。复用已安装缓存，离线运行：

```powershell
.\gradlew.bat :shared:testDebugUnitTest :common:testDebugUnitTest :home:testDebugUnitTest :mobile:lintDebug :mobile:lintRelease :home:lintDebug :maphost:lintDebug :mobile:assembleDebug :automotive:assembleDebug :home:assembleDebug :maphost:assembleDebug --offline --no-configuration-cache --console=plain
# USB 使用其专用 Gradle 8.9 / JDK 17，仅运行 test-project，不修改配套仓库。
gradle --no-daemon --offline --console=plain -p USB-CarPlay/test-project testDebugUnitTest
python scripts/check_public_tree.py
```

APK 包名、版本与 ZIP 资产已核对，均不含运行时认证身份。以下是组合验证产物（包含复制的原有未提交工作），不是正式发布包，也不是仅凭同步提交可复现哈希的车测交付：

| 模块 | SHA-256 |
| --- | --- |
| mobile | `c00f9c57639a9d998e559d4f4203854a1380a72ac6ea3b7914cb3ca73d7758a4` |
| automotive | `7aaf79da6b0e042e6e2c1f2edbf8e8949c992511db7a148ea59886530f2da402` |
| home | `b278fb0b686a82f4ea41335004161842a82fee49bc87d17e42687a7da2e26eed` |
| maphost | `1a28b2c035c9e36259d015c0ae0e7161755d1d9cb06219a3a05f841485e40622` |

日志与汇总在本机未跟踪的 `build/sync-0.2.12/`；原工作备份在 `D:\WorkSpace\CarPlay\analysis\diplay-0.2.12-sync-backup-20261005`。保留隔离工作树以供审阅，不强制清理其中复制的原有工作。

## 设备验证与回退

未安装车机、连接 iPhone、执行硬件音道测试或签署发布包。旧版车测记录不能代替 0.1.10 验收。建议停车状态下验证：吉利两种接入模式的方控播放／暂停／切歌；媒体与导航音道、播报中音量调整及结束恢复；通话与 Siri；图标替换／恢复与设置保存；歌曲封面切换；USB／无线／同一局域网连接与重连；旋转、分屏和新昼夜／分辨率设置。新方控辅助保持关闭，除非专门测试。

同步前提交完整保留。如已共享主线需要撤回，应对本轮 merge 按第一父创建 revert，不重写历史；用户未提交工作始终独立保留。
