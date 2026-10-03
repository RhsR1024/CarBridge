# USB CarPlay 媒体选项对齐（2026-10-03）

当前 R2 版本见 [控制、设置和歌词更新](CONTROLS_R2_2026-10-03.md)：设置移回原列表，USB 标题格式即时生效，歌词改为三级兜底。下方保留此前版本的分析与历史验证，不作为当前安装入口或时长门槛说明。

在固定 7,545,236 字节精简基线上，保留双指缩放、现有 F25 修正、三种媒体模式及封面/歌词/交接修复，增加与 CarBridge 一致的两个选项。所有实现、编译声明和测试在本目录；本次扩展未再改动 MediaBridge 或纯软 CarBridge 生产源码。

## 使用与默认值

设置 → 媒体接入 → 媒体选项：

| 选项 | 默认值 | 生效规则 |
| --- | --- | --- |
| 缺失歌手时的标题格式 | 保留原始信息 | 可选“歌手 - 歌曲名”“歌曲名 - 歌手”；重新连接手机后应用，直连/桥接均使用解析后的显示副本 |
| 直连在线封面与歌词 | 关闭 | F25 直连和自动模式实际走直连时生效；开关即时刷新资源，桥接使用 MediaBridge 设置 |

已有独立歌手字段时不拆分标题。拆分只接受长度不超过 512、无换行、恰好一个两侧带空格/制表符的横线分隔。原始标题、歌手、媒体 ID 和曲目代次不改写，避免拆出的歌手干扰后续增量合并。格式选择和解析原因写入 Android 诊断日志及桥接元数据。

在线功能需要车机联网，使用 LrcLib `/api/get` 和 iTunes 搜索。与 CarBridge 一样要求完整歌名、歌手及可信正时长；归一化身份匹配、版本文字保留，时长误差不超过 3 秒，封面还在有专辑信息时核对专辑。未知时长不查找、不猜测。盒子原生封面和歌词不受此门槛限制。桥接模式可由 MediaBridge 使用此前实现的未知时长唯一精确匹配。

## 实现与维护位置

| 源码 | 责任 |
| --- | --- |
| `CombinedTitleMetadata.java` | Java 移植 CarBridge 同名 Kotlin 的标题格式解析 |
| `DirectSnapshot.java` | 不可变显示快照及可信进度推算，保留原媒体 ID |
| `SynchronizedLyrics.java` | LRC 多时间戳、offset、稳定排序和当前行选择 |
| `DirectMusicResources.java` | 直连查询、缓存、取消及迟到结果隔离 |
| `DirectArtworkCache.java` | 图片验证、缩放、有界 PNG 缓存和单文件读权限 |
| `F25Direct.java` | 原生/在线资源优先级、SDK 封面与歌词输出、500 ms 进度/歌词刷新 |
| `BridgeSettings.java` | 滚动选项页、独立偏好持久化 |
| `UsbMediaBridge.java`、`TrackState.java` | 原始数据只读观察、显示副本和资源时间传递 |

参考 CarBridge：`shared/.../nowplaying/CombinedTitleMetadata.kt`、`SynchronizedLyrics.kt`、`common/.../media/DirectMusicResources.kt`。USB 采用 Java 和原 APK 已有 FileProvider，以保持原清单及资源不变。

封面 URI 为 `content://<包名>.fileprovider/cache_path/usbbox-artwork/<sha256>.png`。使用原未导出的 FileProvider 和 `cache_path` 配置；只授予配套 MediaBridge、ECARX 媒体包单文件读权限，淘汰图片时撤销。图片缓存最多 24 张；下载最多 4 MiB，验证 PNG/JPEG/GIF/WebP 魔数和像素尺寸，最长边缩至不超过 1024。在线封面只允许 HTTPS 的 `.mzstatic.com` 主机；禁止重定向、限制响应大小，连接及读取各有 5 秒超时。

资源缓存最多 100 项，未找到的结果缓存一小时。在线关闭后仍允许读取已有缓存。原生图、原生歌词优先；在线纯文本不伪造同步时间轴，原生实时歌词行可以直接发布。换曲、模式退出、断连通过任务取消和代次检查拒绝旧结果。

## 控制边界

资源变化只刷新媒体展示。在线开关不触发媒体模式重新协商、USB PLAY/PAUSE 或车机 requestPlay。F25 原有六种控制回调保持委托原实现；周期刷新只提交进度和歌词，完整元数据去重发布。

与上一封面修复版一样，原 37,724 方法中只改五处媒体接线，其余 37,719 方法指令不变。USB 收发、盒子命令、音视频、原焦点/方控、触控和双指开关均保持精简基线。所有原字段、签名、继承、非 DEX/签名资源、清单和原生库不变；测试 SDK 声明不进入 APK。

## 本地验证与交付

- USB：27 项测试，0 失败、0 错误、0 跳过；`lintDebug` 0 错误。保留 8 项非阻断警告，涉及依赖版本、最低 API 检查、应用级 Context 持有及不改资源前提下的中文界面文本。
- 新覆盖：标题默认值与拆分边界、重连生效与原 ID 保留、F25 原生图/歌词/进度且无隐式播放、默认关闭与未知时长无网络、严格匹配和缓存复用、FileProvider 实际读取、非法图/URI 拒绝、换曲/关闭拒绝迟到回调。
- 此前未改动的 CarBridge common 160、shared 421、MediaBridge 204 项均已通过；本次只重跑受影响的 USB，共计已验证 812 项。CarBridge/MediaBridge 的 release lint 与签名构建沿用上一修复验证。
- USB 全量 DEX/ABI、ZIP payload、原签名及对齐检查通过，新增 83 类；证据见 `evidence/media-options-verification.json`、`evidence/media-options-release.json` 和 `evidence/media-options-tests/`。

当前 USB 包：`releases/CarPlay-USBBox-Slim-MediaOptions.apk`，7,578,004 字节，比精简基线增加 32,768 字节；SHA-256：`5a7ddcb8130645c6d4a98721b7becb46b3dcc5263f92b3f62cb826e557df01d0`。配套包仍为 MediaBridge 2.3.11（26100304）及纯软 CarBridge 0.1.7；旧安装包和历史证据保留，不覆盖。

尚未在用户车机实测本次新增功能。重点验证 F25 小窗是否接受图片读权限和歌词接口、车机联网查询，以及 USB 断连后切到纯软 CarBridge 的声音；本地测试不保证资源服务一定有匹配歌曲，也不能替代实际盒子/车机验证。出现问题可覆盖安装固定精简基线回退。
