# CarBridge 实施状态

日期：2026-10-02。P00–P17、MB01–MB05 的源码实现与可在本机完成的检查已完成，交付两个测试签名 APK。实际车辆、iPhone、ECARX 服务和覆盖安装相关验收均为 **NOT RUN**，不视为整车验收通过。实施全程由主线程直接执行。

配套版本：CarBridge 0.1.0 / 100；MediaBridge 2.3.0-carbridge-26100201 / 26100201；协作协议 1.0。

## 逐项实施证据

“本机完成”表示源码和本地检查完成；原计划未勾选的混合验收项继续保留，直到补齐实车证据。

| 任务 | 本机完成的实现与证据 | 仍需设备验证 |
| --- | --- | --- |
| P00 仓库/规格 | origin/upstream、固定 v0.2.8 基线、规格/协议/计划/验收文档和参考图 | 无 |
| P01 身份/构建 | mobile 独立 ID、0.1.0、debug 后缀；automotive 独立身份；正式包签名/包名/认证校验；普通源码构建保留认证守卫 | 同装、安装授权与覆盖升级 |
| P02 协议/车机探针 | 核对上游 Now Playing 字段与 session 12；两条传输路径接入；命令/模式/焦点诊断可导出；ECARX 自身包名注册代码 | 真实脱敏帧、车辆 SDK 返回及 Android/ECARX 双事件采样；没有伪造实车 fixture |
| P03 状态 | NowPlayingSnapshot/Store：连接/曲目/revision、增量清空、进度、旧资源隔离；Store 测试 | 手机同名曲与快速切歌 |
| P04 协作 | Messenger 双向协议、UID/签名、代次、超时；两仓库协议字节相同；双方合同测试 | 真机双进程、系统杀进程 |
| P05 解析 | CarPlayNowPlaying、NowPlayingParser；有线/无线控制帧进 Store；截断和未知字段测试 | 各 iOS/播放器实际字段 |
| P06 媒体会话 | CarBridgeMediaRuntime/CarPlayMediaKeys/通知接入真实快照；明确命令、暂停恢复、只读封面 URI | 小窗/手机/通知一致、后台控制 |
| P07 音乐仲裁 | PlaybackPolicy：有效主动播放、旧意图撤销、临时恢复；状态机测试 | 目标 ROM 焦点与后播放优先 |
| P08 输出门控 | MusicOutputGate + AndroidMediaSink；失焦静音、清缓冲、手机 PAUSE；play/rebuffer/tail 门控 | 听音、导航/通话/Siri 与特殊播放器 |
| P09 ECARX | 独立 ecarx 模块与 EcarxMediaRoute；自身身份、来源 6、SDK 有序 I/O 与释放确认；release 构建 | 实际服务准入、回调、来源和 URI 读取 |
| P10 方控 | Android/ECARX/协作/界面统一路由；明确 PLAY/PAUSE、命令 ID 去重；客户端合同测试 | 无关联 ID 的两类方控事件关系，快速连按 |
| P11 模式 | 自动/桥接/直连、prepare/commit/ready、心跳；未知释放不打开第二出口；旧版手动选项与实际路线显示 | 20 次双模式切换、服务重启 |
| P12 封面 | session 12、有界接收/解码、只读 Provider、原生优先、直连在线缓存；恶意路径/字节/超限/旧图测试 | 原生传输、跨进程读取、线上实际命中 |
| P13 歌词 | 直连 LRCLIB + LRC 时间轴/缓存/offset；桥接交给 MB 原资源链；匹配/时间轴测试 | 实际曲库、seek/暂停/小窗歌词滚动 |
| P14 车型 | Geely 默认、Generic/BYD；BYD 各入口隔离并保留实现；BYD 回归与偏好测试 | BYD 设备回归、各车型驻车状态 |
| P15 Geely | 纯黑标志白底，统一默认/预览/协议资源，保留自定义；已查看最终图片、默认值测试 | iPhone OEM 缓存刷新、HOME 后台音频 |
| P16 诊断/回归 | 有界 512 条媒体/路由记录并接入诊断导出；535 项自动测试通过、release lint 无错误 | V01–V26 的整车部分及现场证据 |
| P17 交付/同步 | 配套测试签名 APK、构建脚本、源代码提交、来源说明；fetch/merge-tree 演练与台账 | 已安装版本升级、配置保留；没有代替实车发布验收 |
| MB01 协作服务 | CarBridgeCompanionService、显式组件、身份/版本校验；CarBridgeCompanionTest | 真机服务绑定及授权 |
| MB02 路由隔离 | 受控包重启默认排除；忽略/临时直连、旧快照二次过滤、队列完成栅栏；实际路线由 CarBridge 设置显示，MB 忽略名单不被临时改写 | 切换时真实后端无残留发布 |
| MB03 方控/意图 | 新协作路径单发 Messenger；PAUSED 可显式申请、ID 幂等；BridgeCoordinatorTest | 方控到 iPhone 的完整实际响应 |
| MB04 让位 | 外部车机源/其他本地播放器主动播放发带意图的 YIELD；旧 grant 丢弃、暂停手机 | 车机回调/实际声音互斥 |
| MB05 卡片 | 标准元数据/位置/URI 接入现有封面与歌词链；避免双方在线补全同时运行；原有回归通过 | 完整真实小窗、歌词与图片读权限 |

## 源码和结果

- CarBridge 功能提交：128e50f（协议/状态/音乐门控）、a77b9dc（应用/双模式/车型/资源）。
- MediaBridge 功能提交：a6e7801。文档提交见各仓库 Git 历史；未推送。
- [本机验证与交付报告](LOCAL_VALIDATION_2026-10-02.md)：测试、工具、签名、APK 哈希、设备边界。
- [实现与维护边界](IMPLEMENTATION_ARCHITECTURE.md)：实际模块和协议接口。
- [安装与车测说明](RELEASE_GUIDE_0.1.0.md)：默认配置、切换方式、车测次序。
- [上游同步记录](UPSTREAM_SYNC_LOG.md)：不可变来源和本次同步演练。

本机检查无法证明目标车辆接受新包名 ECARX 注册、手机实际下发封面、第三方音乐遵守焦点，或实车声音无重叠。认证来自用户指定 APK，仅外部输入及本次车测包包含，未提交私钥。未安装、卸载车机应用或推送远端。
