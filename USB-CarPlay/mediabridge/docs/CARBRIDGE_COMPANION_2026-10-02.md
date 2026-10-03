# MediaBridge 与 CarBridge 配套说明

当前补丁为 MediaBridge 2.3.1 / CarBridge 0.1.1，见 [歌曲元数据补丁](CARBRIDGE_METADATA_2026-10-02.md)。以下保留首轮实施基线。

日期：2026-10-02；MediaBridge 2.3.0-carbridge-26100201 / 26100201；CarBridge 0.1.0 / 100；协作协议 1.0。

## 行为变化

CarBridge 发布真实 Android MediaSession 元数据、进度和图片 URI；MediaBridge 沿用现有歌曲卡片、封面和歌词链路。新增协作服务处理这个来源的接管、方控、主动播放及让位，普通播放器继续使用原有控制路径。

CarBridge 自动模式跟随 MediaBridge 忽略名单；明确直连临时排除 CarBridge，不修改持久名单；明确桥接遇忽略冲突需取消忽略。CarBridge 包在每次进程启动时默认排除，握手完成才恢复桥接；服务入口再次过滤已排队的旧快照。实际路线显示在 CarBridge 设置中，不把临时排除伪装成用户忽略配置。

新的受控来源仅通过 Messenger 发一次命令，避免再发送 MediaController 命令。真实 PAUSED 状态也可申请主动播放，无需伪造 PLAYING。旧代次、重复 ID、失效播放许可、未确认完成的后台队列均被隔离；普通播放器原有策略保留。新的本地播放器或外部车机来源接管时，通知 CarBridge 撤销旧播放意图并暂停手机。

## 实现与验证

- 功能提交：`a6e7801e236bcfeefe300694580696be50bd5114`；基线：`ece9746c4e33a2134d64d10f2df8a3ca9b3bba5e`。
- 核心文件：CarBridgeCompanionService、MediaListenerService、UniversalBridgeService、BridgeCoordinator、LegacyBackend、BridgeProtocol。
- 新服务显式绑定，验证 UID、包名、同签名或预置公开证书指纹；协作消息有类型/长度限制。
- 两端 BridgeProtocol.java 字节一致，SHA-256：`941a29b81f6bd6237964de1e7686a51b76166197ff172886a080af40bff91be8`。
- `testDebugUnitTest`：150 项通过，0 失败/错误/跳过；包括旧版回归和新增身份/代次/释放栅栏/主动播放测试。
- `lintRelease`：0 错误、16 警告；`assembleRelease` 成功。未在车机执行安装、更新、接口联调或听音验收。
- 测试签名 SHA-256：`A539C794675FC8AD7FAC6E78604C066E15C98C3C65F62CC34895FA3FC65A23C7`，与核对过的历史 26092810 APK 相同；不代表已核实当前车机安装包。

## 交付与配置

两个 APK 和哈希位于 `D:\WorkSpace\CarPlay\deliverables\CarBridge-0.1.0`。先更新本应用，再安装 CarBridge；CarBridge 默认“吉利 / ECARX”“自动”和音乐互斥开启。若希望通过忽略名单控制模式，请操作新包 **CarBridge** 的条目，原 DiPlay 条目不适用。

桥接时资源和小窗沿用本应用配置；直连时本应用不再显示或控制 CarBridge 来源，由 CarBridge 对接车机媒体接口。仍可桥接其他音乐软件。需要实测歌曲/URI/歌词、方控单次和连按、音频互斥、车辆 SDK 重启和两种路线反复切换。

详细规格/计划/状态/安装步骤保存在 `D:\WorkSpace\CarPlay\CarBridge\docs`，入口 `CARBRIDGE_README.md`。可复用 `CarBridge\scripts\Build-PairedRelease.ps1` 构建配套版本；保留本仓库 [BUILDING.md](BUILDING.md)、[SIGNING_AND_ROLLBACK.md](SIGNING_AND_ROLLBACK.md) 的正常构建和升级规则。
