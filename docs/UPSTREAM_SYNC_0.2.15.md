# CarBridge 0.2.15 — DiPlay 0.2.15 同步

日期：2026-10-09。本轮同步本地源码；未推送或发布，未执行新版实车验收。

## 基线

| 项目 | 记录 |
| --- | --- |
| 同步前 CarBridge | `133f4353ed355849265ff3b7420e0495a0216668`，main，工作区干净 |
| 已集成上游 | `v0.2.12 / 22d2aacedcadc4ec1aef0d74b161b05321f708a7` |
| 本轮目标 | `v0.2.15 / b940efe81ebe6fe930caac71e19d9624723e6b7f`；不跟随移动 main |
| 集成分支 | `sync/diplay-0.2.15`；本报告随双亲 merge 提交记录 |
| CarBridge 版本 | mobile / automotive `0.2.15 / 111`；mobile debug 为 `0.2.15-debug`（显示版本与上游对齐；内部版本码继续递增） |
| 应用身份 | `io.github.rhsr1024.carbridge`；debug `.debug`，automotive `.automotive` |
| 回退基线 | 本地 `backup/pre-diplay-0.2.15-20261009` 保留同步前提交 |
| MediaBridge | 未修改外部仓库；协作协议仍为 1.0 |

上游 0.2.12 → 0.2.15 共 414 个文件变更、35,046 行增加、1,749 行删除。吸收 0.2.13–0.2.15 的无线／USB 连接修复、旧 Android 兼容、蓝牙重连唤醒、音频恢复、视频 surface 生命周期与 pacing、主视频解码提示、设置分类和搜索、亮暗外观、界面大小、语言／关于、BYD 通话与仪表功能。上游仍属于公开预览版。

## 定制保留与合并

17 个文本冲突文件逐块处理。没有用上游整套媒体控制覆盖本项目。

- **吉利方控与单一路由**：CarPlayMediaKeys 继续委托 CarBridgeMediaRuntime，保留 ECARX／MediaBridge 的所有权交接、明确 PLAY／PAUSE、去重和先后主动播放策略；不引入第二套 MediaSession 或流启动即抢焦点的路径。
- **音道与音量**：媒体／导航设置键与 AudioChannelMapping 不变。保留 Geely GuidanceActivity、导航临时焦点、前台 volumeControlStream 绑定、暂停／销毁解绑以及导航结束恢复。AndroidMediaSink 同时接入上游 AudioFocusRequestCompat 和新音频／视频参数，通话与 Siri 优先级保留。
- **音乐门控与预缓冲**：采用上游残余 PCM 计入恢复阈值的修复，预缓冲启动、尾音和恢复播放都经过 MusicOutputGate。失去播放资格仍静音并丢弃过时缓冲。通话／提示获得焦点本身不能恢复已挂起的音乐。
- **旧 Android**：shared 采用上游 API 25 与 native 平台兼容代码；CarBridge 音乐焦点同步使用兼容请求，保留延迟焦点等待和陈旧回调检查。媒体通知对 API 25 使用无 channel 的 Builder；诊断不再直接读取 API 29 的 AudioTrack.audioAttributes。ECARX SDK 仍要求 API 28，因此 mobile／automotive 保留原 minSdk 28（Android 9+），不使用 overrideLibrary 强行忽略要求。
- **图标与歌曲资源**：Geely 原图／默认图标、自定义图标保存、预览、替换和恢复入口保留，升级不清空。仍按连接和曲目代次处理封面，不跨曲目沿用旧图，也不引入上游独立媒体封面缓存。
- **新设置分类**：车机与媒体接入位于“车辆”，方向和原旋转重连偏好位于“显示”。音乐互斥继续作用于现有策略并实时通知 runtime；不暴露只控制上游第二套媒体焦点的临时静音开关。音道设置仍在“音频”。
- **旋转**：默认固定横屏、原自动旋转不重连／可选重连策略保留。用户启用新的双方向画布功能时切换自动方向；会话已提供双方向 view areas 时不再触发旧旋转重连。
- **车型隔离**：新增 BYD 通话输出和电话键 opt-in 继续要求 BYD profile；保存的 BYD 开关不能影响吉利。吉利首次启动不自动打开 DiLink 引导。蓝牙音频、回声消除、平滑视频和 buffered music 等新实验项默认关闭。
- **更新身份**：手动更新查询 `RhsR1024/CarBridge` 发布源，下载后校验 SHA-256 和当前包名。调试／automotive 包不会接受不同包名的正式 mobile APK。

USB-CarPlay、ecarx、CarBridgeRouteManager、EcarxMediaRoute、协作协议、Now Playing、PlaybackPolicy／MusicOutputGate、AudioChannelMapping 和 GuidanceActivity 的核心文件与同步前逐文件一致。MediaBridge 外部项目未修改。源码分发和许可说明保留。

## 自动化验证

主项目 **2,002 项测试全部通过，0 失败／错误／跳过**。日志及汇总在本机未跟踪目录 `build/sync-0.2.15/`。

| 检查 | 最终结果 |
| --- | --- |
| shared 单元测试 | 1,037 通过 |
| common 单元测试 | 961 通过 |
| home 单元测试 | 4 通过 |
| mobile lint debug / release | 0 错误，19 / 4 警告 |
| home / maphost lint debug | 0 错误，5 / 2 警告 |
| mobile / automotive / home / maphost debug 构建 | 全部成功 |
| 资源、合并与文档 | 无冲突标记、重复资源键或新文档断链；git diff --check 通过 |
| public tree | 因同步前已跟踪的 21 个 USB 分发 APK／公开证书失败；本轮没有增加 |

最终源码完整命令在 `build-verified.log` 为 `BUILD SUCCESSFUL in 6m 20s`，411 个任务、退出码 0。随后按用户要求仅对齐 mobile／automotive 显示版本并重新执行完整命令，`build-aligned.log` 为 `BUILD SUCCESSFUL in 17s`，411 个任务、退出码 0；下文 APK 元数据与哈希以对齐后的产物为准。

验证使用 JDK 25.0.3+9、Gradle 9.5、Android SDK 37、NDK 28.2.13676358。本机使用 `E:\DevTools\MediaBridgeTooling`，`CARBRIDGE_TEST_SHELL=C:\Program Files\Git\bin\bash.exe`；首次离线缺失缓存记录保留，在线加载依赖后继续验证。USB 盒子源码不在本轮变更范围。

```powershell
.\gradlew.bat :shared:testDebugUnitTest :common:testDebugUnitTest :home:testDebugUnitTest `
  :mobile:lintDebug :mobile:lintRelease :home:lintDebug :maphost:lintDebug `
  :mobile:assembleDebug :automotive:assembleDebug :home:assembleDebug :maphost:assembleDebug `
  --offline --no-configuration-cache --console=plain
```

测试适配：BYD 专属仪表、通话与旧语音按键用例显式选择 BYD；通用 host 生命周期使用 GENERIC，mock controller 提供本项目 Now Playing 实例；音频反射 fixture 补充门控和导航参数。媒体焦点测试通过真实 CarBridgeMediaRuntime 的门控验证 API 25／28／33 上连接不抢播、立即授予、拒绝、duck、临时／永久失焦和旧请求隔离。Windows AtomicFile 测试模拟 Android 的原子覆盖 rename，USB 授权命令由本机 Git Bash 读取原样临时脚本执行，避免 Windows argv 丢失内嵌引号；安装权限测试隔离 Android FileProvider 路径与 Windows 分隔符。没有跳过失败用例或放宽生产车型门控。

新增兼容保护验证新 BYD 开关不能越过车型、新实验能力默认关闭、车机媒体设置只有一个分类、外观／平滑视频设置不覆盖音道或自定义图标。

## 源码验证 APK

以下均为本机构建的 debug 产物，ZIP 已确认不含运行时认证身份。mobile 与 automotive 包名和实际版本核对通过；mobile 包含 arm64-v8a、armeabi-v7a、x86_64 三种 Speex echo 库。

| 模块 | SHA-256 |
| --- | --- |
| mobile | `096aa9c2c96d8c94899bb4d9825379914f091dbd2247e67c2560b83d85c6f921` |
| automotive | `0480d53fdcbf44613172089fc2004a8de0fa07355ab17768ea704c3d2bb1a894` |
| samples/home | `b278fb0b686a82f4ea41335004161842a82fee49bc87d17e42687a7da2e26eed` |
| samples/maphost | `1a28b2c035c9e36259d015c0ae0e7161755d1d9cb06219a3a05f841485e40622` |

## 版本对齐规则

本轮按用户要求将 CarBridge 显示版本与上游对齐为 **0.2.15**，内部版本码保留单调递增的 **111**，不复制上游较小的 34。规则写入根 [AGENTS.md](../AGENTS.md#carbridge-版本对齐规则) 和 [维护策略 §5](UPSTREAM_SYNC.md#5-构建认证签名与版本)：后续同步的显示版本等于已合并验证的上游版本；同基线定制修复通过内部版本码和提交区分；历史版本记录不回改。

## 实车验收与回退

本次 debug 构建用于源码验证。没有安装车机、连接 iPhone、执行实际声道和方向盘测试或签署新版正式包；旧版本车测不能代表此版本。需要停车验证：直连／桥接两种模式的播放、暂停和切歌；媒体／导航音道和播报中音量调节、播报结束恢复；通话／Siri；图标替换和恢复；曲目／封面切换；USB、无线与重连；横竖屏／分屏。新实验功能保持关闭，除非单独验收。

同步前提交及备份分支完整保留。已共享合并需撤回时按 merge 第一父创建 revert，不重写 main 历史。
