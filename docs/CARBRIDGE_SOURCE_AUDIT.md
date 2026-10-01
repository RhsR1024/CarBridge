# CarBridge 基线源码核对记录

日期：2026-10-01；CarBridge HEAD：`f2d06951b4e8114d...`（完整值见 [入口](CARBRIDGE_README.md)）。本文件区分源码事实、用户实测和待验证假设，供后续改造及 upstream 合并复核。

## 1. 已确认的关键事实

以下链接指向仓库当前文件；行号对应 DiPlay 0.2.8 基线，未来可能移动。合并后按符号定位，不依赖行号永远不变。

| 事实 | 来源 | 对方案的影响 |
| --- | --- | --- |
| mobile 版本 0.2.8/code 27，包名 `com.shihab.diplay`；namespace 不同 | [mobile/build.gradle.kts](../mobile/build.gradle.kts#L11) | 必须规划自己的安装身份；不能混淆 namespace 与包名 |
| 已订阅 Now Playing，group 0 参数 `[1,4,6,12,26]`、group 1 `[0,1,7]` | [Iap2Messages.subscriptions](../shared/src/main/java/com/shilapi/xcertplay/iap2/message/Iap2Messages.kt#L278) | 有协议入口，不需要从画面 OCR 抓歌名；各字段语义还须核实 |
| 目前只从 0x5001 的 group 1/status 0 读播放布尔变化 | [CarPlayPlaybackStatus.accept](../shared/src/main/java/com/shilapi/xcertplay/media/CarPlayPlaybackStatus.kt#L16) | 收到协议不等于已解析歌曲元数据 |
| Controller 仅转发布尔播放状态给监听者 | [CarPlayController](../shared/src/main/java/com/shilapi/xcertplay/orchestration/CarPlayController.kt#L501) | 需要完整统一快照接口 |
| MediaSession 由音频启停设置 PLAYING/PAUSED，位置未知，无 setMetadata | [CarPlayMediaKeys.updateLocked](../common/src/main/java/com/shilapi/xcertplay/CarPlayMediaKeys.kt#L75) | MediaBridge 无法从该会话获取歌名、歌手、封面和准确进度 |
| 手机 playing=true 会尝试重新取得焦点，但不更新完整媒体状态 | [CarPlayMediaKeys.onIphonePlaying](../common/src/main/java/com/shilapi/xcertplay/CarPlayMediaKeys.kt#L60) | 需防止旧播放状态造成反抢 |
| 通知主要为固定连接文案 | [DiPlaySessionService](../common/src/main/java/com/shilapi/xcertplay/DiPlaySessionService.kt#L29) | 不能指望通知兜底提供歌曲信息 |
| link engine 声明 FILE_TRANSFER_SESSION_ID=12，但输入事件只向上传递控制 session | [Iap2LinkEngine 输入分发](../shared/src/main/java/com/shilapi/xcertplay/transport/Iap2LinkEngine.kt#L430)、[session 声明](../shared/src/main/java/com/shilapi/xcertplay/transport/Iap2LinkEngine.kt#L576) | 原生封面需要文件通道和关联工作，不止 setMetadata |
| sink 失焦回调只在 duck/gain 时调音量，明确保持永久/临时失焦后继续音频 | [AudioFocusCoordinator](../shared/src/main/java/com/shilapi/xcertplay/media/AndroidMediaSink.kt#L46) | 用户报告的音乐并播存在直接代码依据 |
| focus 请求结果被记录，没有作为输出许可；同通道已有 request 时直接返回 | [refreshRequest](../shared/src/main/java/com/shilapi/xcertplay/media/AndroidMediaSink.kt#L70) | 焦点拒绝/丢失后不能仅靠重新开启开关解决 |
| renderer 请求焦点后继续运行，startPlayback 直接 track.play，补缓冲也可重启 | [renderer](../shared/src/main/java/com/shilapi/xcertplay/media/AndroidMediaSink.kt#L757)、[startPlayback](../shared/src/main/java/com/shilapi/xcertplay/media/AndroidMediaSink.kt#L1189) | 所有音乐输出路径必须受同一门控 |
| MediaKeys 自己也请求音乐焦点，永久失焦只更新 focusHeld | [CarPlayMediaKeys.start](../common/src/main/java/com/shilapi/xcertplay/CarPlayMediaKeys.kt#L89) | 必须合并职责，避免两份焦点状态相互打架 |
| sink 焦点开关在创建时传入 | [CarPlayHostActivity](../common/src/main/java/com/shilapi/xcertplay/CarPlayHostActivity.kt#L2961) | 当前修改开关不一定影响已有 sink；新设置需定义热更新/重连语义 |
| 现有方控是 MediaSession/KeyEvent → sendMediaButton → AirPlay HID | [CarPlayMediaKeys](../common/src/main/java/com/shilapi/xcertplay/CarPlayMediaKeys.kt#L141)、[AirPlaySession.sendMedia](../shared/src/main/java/com/shilapi/xcertplay/airplay/AirPlaySession.kt#L223) | 可复用控制出口，但没有原生 ECARX 注册入口 |
| BYD 353 和硬件 PLAY/PAUSE 合并 toggle 是明确车型兼容规则 | [CarPlayMediaButton](../shared/src/main/java/com/shilapi/xcertplay/airplay/CarPlayMediaButton.kt#L16) | Geely 的明确播放/暂停不能照搬此规则 |
| OEM 配置含 label 和 imageData | [AirPlayInfoPlist](../shared/src/main/java/com/shilapi/xcertplay/airplay/AirPlayInfoPlist.kt#L59)、[AirPlayPersistence](../common/src/main/java/com/shilapi/xcertplay/AirPlayPersistence.kt#L81) | Geely 图标/标签可以通过现有入口替换 |
| 实际默认图为 ic_car_home；预览 fallback 为 placeholder_icon | [图标加载/预览](../common/src/main/java/com/shilapi/xcertplay/CarPlayHostActivity.kt#L2802) | 需要统一预览与发送资源，不能只改一张图 |
| OEM 请求已用 ACTION_MAIN + CATEGORY_HOME 返回桌面 | [onHostUiRequested](../shared/src/main/java/com/shilapi/xcertplay/orchestration/CarPlayController.kt#L285) | 保留点击行为即可，不需要重写返回桌面协议 |

源码范围没有发现完整 ECARX/F25 客户端、歌曲元数据发布、封面重组或歌词解析实现。此为本次基线结论，未来上游增量需要重新评估。

## 2. MediaBridge 现状与日志边界

本地源码：`D:\CarSoft\MediaBridgeApp\MediaBridge-src`，提交 `ece9746c4e33a2134d64d10f2df8a3ca9b3bba5e`。

| 事实 | 源码位置（相对该仓库） |
| --- | --- |
| 当前运行工厂选择 PhoneDebug 或 Legacy，枚举 F25 存在不代表当前可选运行 | `app/src/main/java/com/geely/auto/music/UniversalBridgeService.java`，factory 与 configuredMode（约 36/125 行） |
| 使用实际 context 包名注册，API 兼容 registerInMusic/registerMusic | `LegacyBackend.java:98`、`com/ecarx/eas/sdk/mediacenter/MediaCenterAPI.java`（位于 app/src/main/java） |
| suspendOutput 关闭输入、计时发布并作废排队输出；其他 owner 到来即让位 | `app/src/main/java/com/geely/auto/music/LegacyBackend.java:173` |
| yieldToVehicleSource 清输入和快照、建立恢复屏障，没有暂停 DiPlay | `app/src/main/java/com/geely/auto/music/MediaListenerService.java:1295` |
| 新播放恢复受状态边沿和来源屏障约束，不是任意重复 PLAYING 即恢复 | 同文件 `updateRecord`（约 707 行） |
| 歌曲从 MediaController 元数据读取，空元数据没有真实歌名/歌手 | 同文件 `read` 相关快照读取（约 748 行） |
| 已有 ArtworkRepository 和 LyricsManager，空标题不具备正常在线歌词匹配条件 | `app/src/main/java/com/geely/auto/music/ArtworkRepository.java`、`lyrics/LyricsManager.java` |
| 专门的空 CarPlay 会话保护针对 com.flyme.auto.energy，不针对 DiPlay | `app/src/main/java/com/geely/auto/music/MediaListenerService.java` 的相关包名判断 |

原始日志：`C:\Users\public.DESKTOP-IQJOR7V\Downloads\mediabridge-probe-20261001-211013-070.log.txt`。不把原始日志整体提交仓库。

日志头显示版本 `2.2.0-carplay-guard-26100101`，Android 11，实际输入/输出通道为 MediaBridge 标准；结束摘要是已让位 QQ 音乐、标准通道已注册、最后 NEXT 已接收。日志第 4513 行在 20:46:22 选择 `com.shihab.diplay`，第 4517 行记录输出包 `com.mediabridge.app`、输入包 `com.shihab.diplay`。

这些日志可以证明桥接选中过 DiPlay，也有其他来源接管；`Control received` 只证明 MediaBridge 收到命令。没有完整的 DiPlay HID 与 iPhone 状态关联，就不能断言每次切歌失败的唯一原因都是焦点。当前源码缺口足以解释元数据空白与可能并播，但具体失败次数和手机是否执行仍需双端联调日志。

## 3. F25 参考 APK

- 原件：`D:\WorkSpace\CarPlay\CarPlay - 副本.apk`。
- 包名：`com.flyme.auto.energy`；版本 `2025.08.07.1852` / `2112232222`。
- SHA-256：`14659850CD85DDA1B1F156CA65438D2BEB866DE6D7C6101408A62866C4DC1E6D`。
- 可读分析：`D:\WorkSpace\CarPlay\analysis\F25与DiPlay-0.2.8方控对比.md`；解码文件 `analysis\zqsdk-decoded\`，原始字节码核验 `analysis\f25-bytecode.txt`。
- 反编译整体有还原错误；关键 F25 分支/回调已按字节码核对，不能推断其全部源码可重建或直接编译。

已核实 F25 分支选 `com.ecarx.eas.sdk.mediacenter.MusicClient`，初始化后以实际包名 `registerMusic`，调用 requestPlay、来源类型 6 和播放状态发布。下一曲/上一曲/播放/暂停回调分别传 87/88/126/127，后续盒子适配却把明确 PLAY/PAUSE 合为 toggle；CarBridge 不沿用这个折叠。

最终发送到盒子的 203/204/205 不是 iPhone HID 编码。CarBridge 可在 ECARX 回调后复用现有 sendMediaButton/AirPlay HID，无需该 USB 盒子。参考代码中的 QQ 音乐硬编码 Intent/假 URI 也不是封面实现。

用户已实测参考 APK 的 F25 可控制该车上的 CarPlay。它证明路径值得适配，不保证 CarBridge 新包名/签名自动获准；MediaBridge 以自身包名已工作提供了进一步依据，仍须 P02/P09 目标车验证。

## 4. 当前不能承诺的事项

1. 未经字段样例核对，不能给订阅里的每个数字编造歌名/封面/歌词含义。
2. iPhone 原生封面是否发送、传输格式、与曲目的关联需要验证；原生歌词更不能从手机界面存在就推断。
3. 普通 Android 11 应用没有“强制暂停任何软件”的通用权限；音乐互斥必须同时约束本项目输出、焦点及车辆来源。
4. 暂停/播放状态不总能表达所有手机点击动作；新意图识别有信息边界，需要探针与明确恢复入口。
5. F25 媒体回调不能证明吉利语音键/Siri 通道同样可用。
6. 当前源码构建与可连 iPhone 的 standalone 构建条件不同，不能只编译普通 debug 就作为可用车测包交付。

上述问题已经纳入计划和验证，不作为省略用户功能目标的理由。
