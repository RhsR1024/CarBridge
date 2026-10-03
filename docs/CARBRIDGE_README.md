# CarBridge 开发文档

当前源码为 **CarBridge 0.1.5 / versionCode 105**，上游基线已同步至 **DiPlay v0.2.10**（main `4556313`，包含 v0.2.10 后的诊断说明更新）。吉利方控、双模式、音乐互斥、导航焦点与音量目标、定制图标继续保留，详情见 [0.2.10 同步与验证](UPSTREAM_SYNC_0.2.10.md)。收藏同步限制见 [能力核对](CARBRIDGE_FAVORITES_FEASIBILITY.md)。

更新：2026-10-03。CarBridge 是本 fork，DiPlay 专指原项目。此前 0.1.3 的实车结果见 [车测记录](VEHICLE_VERIFIED_0.1.3.md)；0.1.5 本次执行源码自动化回归，尚未执行新一轮车机验收。下表保留首轮 0.1.0 交付历史，不代表当前版本。

## 首轮交付与基线（0.1.0 历史记录）

| 项目 | 记录 |
| --- | --- |
| 本地工作目录 | D:\WorkSpace\CarPlay\CarBridge |
| origin | git@github.com:RhsR1024/CarBridge.git |
| upstream | git@github.com:shihabal3amri/DiPlay.git |
| DiPlay 基线 | v0.2.8，f2d06951b4e8114dbb62f551c12a32a845a3042f |
| CarBridge 身份 | io.github.rhsr1024.carbridge，0.1.0 / 100 |
| 配套 MediaBridge | com.mediabridge.app，2.3.0-carbridge-26100201 / 26100201 |
| MediaBridge 工作目录 | D:\CarSoft\MediaBridgeApp\MediaBridge-src |
| MediaBridge 基线 | ece9746c4e33a2134d64d10f2df8a3ca9b3bba5e |
| APK 目录 | D:\WorkSpace\CarPlay\deliverables\CarBridge-0.1.0 |
| 已验证 | 535 项自动化测试；两端 release lint 无错误；构建、签名、认证资产、身份与协议一致性 |
| 未执行 | 车机安装/升级、iPhone 和目标 ROM 联调、远端推送 |

## 阅读入口

| 文档 | 用途 |
| --- | --- |
| [安装与车测说明](RELEASE_GUIDE_0.1.0.md) | 两包安装、自动/桥接/直连配置和实车验证 |
| [实施状态](IMPLEMENTATION_STATUS.md) | P00–P17、MB01–MB05 的逐项证据和设备边界 |
| [本机验证报告](LOCAL_VALIDATION_2026-10-02.md) | 测试、工具链、签名、哈希 |
| [产品与技术规格](CARBRIDGE_SPEC.md) | 稳定需求 R01–R18 |
| [详细实施计划](CARBRIDGE_IMPLEMENTATION_PLAN.md) | 工作分解、依赖和最终验收 |
| [协作协议](MEDIABRIDGE_INTEROP.md) | 所有权、命令、异常恢复约束 |
| [实现与维护边界](IMPLEMENTATION_ARCHITECTURE.md) | 实际消息、模块、资源策略和来源 |
| [验证方案](CARBRIDGE_VALIDATION.md) | V01–V26 设备用例与证据格式 |
| [原始源码核对记录](CARBRIDGE_SOURCE_AUDIT.md) | 改造前 DiPlay / F25 / MediaBridge 事实 |
| [上游同步流程](UPSTREAM_SYNC.md) / [台账](UPSTREAM_SYNC_LOG.md) | 保留上游历史并同步新增功能与修复 |
| [图片参考](assets/carbridge/README.md) | 用户提供的原图 |

默认车型 Geely，默认自动路由和音乐互斥开启。自动模式跟随新版 MediaBridge 的忽略策略；明确直连无需再改忽略名单，明确桥接遇持久忽略会提示冲突。只有有效主动播放才获得音乐资格；连接和旧状态不抢播。BYD 功能按车型隔离保留。

歌曲文字和状态来自 iPhone；原生封面路径已实现，实际可用性取决于手机播放器和协议输出。直连在线补全默认关闭，桥接使用 MediaBridge 原有资源策略。不保证任意播放器都能获得原生歌词/封面。

原有 [BUILD.md](BUILD.md)、[TESTING.md](TESTING.md)、[THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md) 保留。认证和签名通过外部输入，配套构建脚本为 [Build-PairedRelease.ps1](../scripts/Build-PairedRelease.ps1)。后续修改模式/协议时同步更新规格、计划、测试与配套版本。
