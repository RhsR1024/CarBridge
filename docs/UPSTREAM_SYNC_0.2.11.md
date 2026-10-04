# CarBridge 0.1.9 — DiPlay 0.2.11 同步

日期：2026-10-04。本轮继续同步原项目 main，保留 CarBridge 定制。自动化检查与设备验收分别记录。

## 不可变基线

| 项目 | 记录 |
| --- | --- |
| 同步前 CarBridge | `bd9bef0cef9cad949fa2fe24d793bf37fda73d42`（0.1.8） |
| 已集成上游 | `v0.2.10 / 45563135a3d05a17315a84f57434fdb5dfa43a5f` |
| 新稳定标签 | `v0.2.11 / 6014025c653c4dae88d319ce446e0bf1ddb658ea`；比标签多网站与发布文档提交 |
| 本轮目标 main | `abe750f79dfb7ee4be613e36f2952a43778b3a04` |
| 集成分支 | `sync/diplay-0.2.11`；采用双亲合并，合并提交 `ef446e305406acddae43bf0f1d9044cf710e08b7`，另见台账 |
| 版本 | mobile / automotive 均为 `0.1.9 / 109`，mobile debug 为 `0.1.9-debug` |
| 应用身份 | `io.github.rhsr1024.carbridge`，debug 后缀 `.debug`，automotive 后缀 `.automotive` |
| MediaBridge | 本轮未修改；协作协议仍为 1.0 |

上游差异为 148 个文件、10,288 行增加、536 行删除。包括 Wi-Fi Direct 首选信道、可移动自定义转向卡片、2/3/4 指设置手势、可选旧版车辆数据只读探测（含探测并发与电池发布修正）、无线位置与车辆数据移至运行时 Wi-Fi 链路、可选车机热点自动开启、Android TV 遥控器支持、增量标题保留歌手与元数据变更发布、Android 9 音频属性修正、编解码器失败释放、稳定尺寸重连、本应用 VPN 范围限制，以及有界无线／媒体／昼夜／进程退出诊断。

## 合并与定制保留

十五个文本冲突文件：CHANGELOG、AndroidManifest、CarPlayHostActivity、CarPlayMediaKeys、DiPlayActivity、六份语言资源、mobile 构建、BydOutputSettings、AndroidMediaSink、CarPlayController。

- **方控、媒体会话与双模式**：CarPlayMediaKeys 继续交给 CarBridgeMediaRuntime；保留 ECARX、直连／MediaBridge 路由、显式 PLAY/PAUSE、命令校验、音乐互斥与歌曲资源补全。新增上游 Android TV 遥控键（CarPlayRemoteKeys／AndroidTvInputMode）接入 dispatchKeyEvent，触摸返回与绝对旋钮语义不变；语音键 Siri 处理保持 BYD 车型门控。CarPlayMediaCallback 保留 mapKey 注入。
- **歌曲与元数据**：CarBridgeMediaRuntime 的按曲目键发布策略不变，等效实现上游 #162；新增 `metadataChanged` 辅助函数承载上游回归测试。
- **音量与通话**：AndroidMediaSink 保留音乐门控（MusicOutputGate）、导航期间音量目标与结束恢复、resetMusicAfterLoss；接入上游 Android 9 音频属性兼容、编解码器启动失败释放与诊断阶段标记，未改音乐渲染路径。
- **无线连接与位置**：CarPlayController 保留 csm／隧道／陈旧运行守卫，接入上游 `diagnostics.controlProgress`；按上游 #157 从蓝牙引导链路移除 locationRequest／vehicleStatusProvider（位置与车辆数据在运行时 Wi-Fi 链路，隧道调用保留 provider）。按车型隔离启动 BYD 输出的定制不变。
- **图标与车型**：BydOutputSettings 采用上游 `navigationAvailable` 命名并保留 CarBridgeSettings.isByd 门控；AndroidManifest 查询列表保留 MediaBridge／ECARX 包并新增 `com.byd.carsettings`；图标资源与 Geely/BYD 隔离未变。
- **旋转与重连**：CarPlayHostActivity 保留 followDisplay 采纳最新尺寸与旋转保留画布策略，接入上游 maybeStartCarPlay 就绪检查与稳定尺寸重连；onDestroy 同时清理上游转向卡片监听。
- **界面与滚动**：DiPlayActivity 采用上游 rootScroll／pendingScrollY 滚动恢复（替代 CarBridge 0.1.8 的等效实现，行为一致且覆盖停止窗口场景）；home 页热点启动提示采用上游结构并保留 CarBridge 未命名 SSID 处理；companion 常量与 CHANGELOG（保留 CarBridge 条目并收录上游 0.2.11 说明）合并。
- **字符串**：六份语言文件按块合并——保留 CarBridge 品牌描述、采用上游去除逐项 “· needs ADB” 的新标题与全部新增字符串（新增字符串品牌统一为 CarBridge）；无重复键。
- **构建**：mobile / automotive 0.1.9 / 109；包名与签名策略不变。

## 上游测试适配

上游 0.2.11 新增测试按 CarBridge 车型隔离语义运行：

- 五个 BYD 测试类（BydVehicleFieldStoreTest、BydSettingsReconnectTest、BydVehicleDataSettingsTest、CarHotspotSetupTest、BydAdbSettingsUiTest）在 setup 中显式选择 BYD 档位。
- CarPlayHostDisplaySizeTest 在 Robolectric 环境使用 GENERIC 档位以避开厂商服务绑定；为 mock 控制器补上 now-playing 存根；同尺寸旋转用例改为 CarBridge 旋转保留策略下会产生 pending 的非旋转场景（`aPendingResizeResumesAfterTeardown`）。

## 自动化验证

| 检查 | 结果 |
| --- | --- |
| shared 单元测试 | 537 通过，0 失败／错误／跳过 |
| common 单元测试 | 291 通过，0 失败／错误／跳过 |
| home 单元测试 | 4 通过，0 失败／错误／跳过 |
| mobile lint debug | 0 错误，18 个警告 |
| mobile lint release | 0 错误，4 个警告 |
| home lint debug | 0 错误，5 个警告 |
| maphost lint debug | 0 错误，2 个警告 |
| 四模块 debug 构建 | mobile、automotive、home、maphost 均成功 |
| 定制与资源核对 | 582 个受保护文件中仅 `automotive/build.gradle.kts` 版本号与 `docs/UPSTREAM_SYNC*.md` 同步文档为簿记性修改，其余逐字节未变 |
| public tree 检查 | 因 USB-CarPlay 已跟踪的分发产物（基线／发布 APK 与公开证书共 9 项）报错，属合并前既有状态，非本轮引入 |

完整构建 `BUILD SUCCESSFUL in 4m 3s`，411 个任务，退出码 0；home 测试另以 `--rerun` 复跑通过。共 **832 项单元测试通过**。lint 保留既有警告，不宣称零警告。

四个 APK 已核对包名与版本，均无运行时认证资产：

| 模块 | SHA-256 |
| --- | --- |
| mobile | `202f4987881d7b895efc72d7024d3b53f1b29a7aa9ca2f96b34e24cc1852e8b4` |
| automotive | `956aedf5068abfa42d74e29122e086bccbdccf57debfe272b4862f92d1f916fe` |
| samples/home | `b278fb0b686a82f4ea41335004161842a82fee49bc87d17e42687a7da2e26eed` |
| samples/maphost | `1a28b2c035c9e36259d015c0ae0e7161755d1d9cb06219a3a05f841485e40622` |

复现命令：

```powershell
python scripts/check_public_tree.py
.\gradlew.bat :shared:testDebugUnitTest :common:testDebugUnitTest :home:testDebugUnitTest :mobile:lintDebug :mobile:lintRelease :home:lintDebug :maphost:lintDebug :mobile:assembleDebug :automotive:assembleDebug :home:assembleDebug :maphost:assembleDebug --no-configuration-cache --console=plain
```

工具链：JDK 25.0.3+9、Android SDK 37。本地日志及检查结果位于未跟踪的 `build/sync-0.2.11/`。

## 设备验证与回退

本轮没有安装车机、连接 iPhone 或发布签名正式包。此前 [0.1.3 车辆记录](VEHICLE_VERIFIED_0.1.3.md) 不代表 0.1.9 已经实车验收。源码 debug APK 不含运行时认证资产，不作为独立连接 iPhone 的车测交付包。

车测重点：两种模式下方控与语音键、歌曲与封面切换、音乐让位、导航播报音量目标及结束恢复、通话回声、USB／无线连接与重连、Wi-Fi Direct 首选信道、Android TV 遥控（如有）、转向卡片与设置手势、车型档位切换后 BYD 开关的隐藏与保留。

同步前提交保留。已共享主线如需撤回，按本轮 merge 的第一父创建 revert，不重写历史。
