# USB-CarPlay

用于 USB CarPlay 盒子配套 APK 的独立维护目录。与上层 CarBridge 的纯软件实现分开构建。

正式修改基线固定为 `baselines/CarPlay-USBBox-F25-MultiTouch-Slim.apk`（7,545,236 字节，约 7.55 MB）。基线已包含双指缩放、F25 播放/暂停修正及纯填充精简。本次在此基础上增加自动／F25 直连／MediaBridge 桥接三种媒体模式。

| 内容 | 位置 |
| --- | --- |
| 安装包 | `releases/` |
| 精简基线、回退包 | `baselines/` |
| 新增 USB APK 媒体接入源码 | `src/` |
| 原 APK 编译接口声明；不装入 APK | `stubs/` |
| 精确接线、构建、签名、全量比对 | `scripts/`、`tools/` |
| USB 接入运行测试 | `tests/`、`test-project/` |
| MediaBridge 2.3.9 历史源码快照（不再作为构建入口） | `mediabridge/` |
| 以往补丁源码、原始验证材料 | `history/` |
| 修改、使用、重建与验证说明 | `docs/` |

请先看 [安装与使用](docs/USAGE.md)、[修改记录与边界](docs/CHANGES.md)、[构建与维护](docs/BUILDING.md) 和 [验证记录](docs/VALIDATION.md)。

2026-10-03 修复封面/歌词接入、增量歌曲混合和断连占用，见 [本次修复](docs/METADATA_HANDOFF_2026-10-03.md)。MediaBridge 正式源码在 `D:\CarSoft\MediaBridgeApp\MediaBridge-src` 的 `main`，可设置 `MEDIABRIDGE_ROOT` 指向其他克隆；构建和测试脚本直接检查该仓库。

已对齐 CarBridge 的“缺失歌手时的标题格式”和“直连在线封面与歌词”，默认分别为“保留原始信息”和关闭。入口：原设置列表中的三项媒体设置。当前 USB 安装包为 `releases/CarPlay-USBBox-Slim-Controls-R2.apk`，配套 MediaBridge 2.3.12；实现与验证见 [本轮控制和歌词修复](docs/CONTROLS_R2_2026-10-03.md)，场景分析见 [配置适用性分析](docs/CARBRIDGE_SETTINGS_APPLICABILITY.md)。

三模式只接管车机侧的媒体注册与发布。盒子协议、USB 收发、音视频、触控编码及现有命令发送函数保持基线实现。新媒体按键调用原有 F25 控制链。

当前新增三模式已完成本地构建与测试；车机实测状态见验证记录。无需把临时反编译工程或整套 JADX/JDK/SDK 塞入本目录。外部工具有固定版本与哈希，准备脚本可下载或导入本机现有文件。
