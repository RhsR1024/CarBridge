# CarBridge 详细实施计划

日期：2026-10-01；实施更新：2026-10-02。逐项本机证据及实车待验证边界见 [实施状态](IMPLEMENTATION_STATUS.md)；原验收复选项保留作为最终实车验收清单。所有实现均由主工作线程直接执行，不使用子代理。需求依据：[规格](CARBRIDGE_SPEC.md)；接口依据：[协作协议](MEDIABRIDGE_INTEROP.md)；结果依据：[验证方案](CARBRIDGE_VALIDATION.md)。

## 1. 交付顺序与门槛

```text
P00 仓库/文档
  → P01 身份与构建 → P02 协议/车机探针
  → P03 状态模型 → P05 元数据 → P06 MediaSession
  → P07 音乐仲裁 → P08 输出门控

P01/P03 → P04 协议契约 ↔ MB01/MB02
P02/P03/P07 → P09 ECARX → P10 统一命令
P04/P06/P08/P09/P10 + MB01–MB05 → P11 模式交接
P05 → P12 封面 → P13 歌词
P01 → P14 车型隔离 → P15 Geely 按钮
全部必需项 → P16 集成/车机验证 → P17 发布与同步演练
```

图是依赖关系，允许独立工作顺序调整，不表示派生多个工作会话。音乐互斥不能只改焦点回调就结束；完整功能也不能停在文字元数据而跳过封面/歌词任务。

| 阶段 | 完成门槛 |
| --- | --- |
| G0 基线 | P00 完成；有稳定来源和可维护文档 |
| G1 可构建/可观测 | P01/P02；普通源码构建通过；车测包认证输入可用；关键协议与 ECARX 探针有结果 |
| G2 共同基础 | P03/P05–P08；真实歌曲/状态可发布，互斥与恢复状态机通过测试 |
| G3 两种模式 | P04/P09–P11、MB01–MB05；同车桥接/直连都能方控、切换无重复 |
| G4 完整卡片与品牌 | P12–P15；可匹配测试曲目的封面/歌词通过，两种模式和 Geely 按钮验证 |
| G5 可发布 | P16/P17；问题清单处理完、认证/签名/升级/上游同步回归有证据 |

G1 探针失败不能伪装成后续通过：字段未核实则先推进不依赖该字段的部分；ECARX 自身包名注册失败则记录原因、修复适配，不能借用参考 APK 包名绕过。原生封面未通时可推进在线补全，但不能把“支持原生封面”勾为完成。

## 2. CarBridge 任务

### P00 — 仓库与规格基线（已完成）

- [x] 克隆到 `D:\WorkSpace\CarPlay\CarBridge`；保留 origin 并新增 upstream。
- [x] 获取 upstream 与标签，固定 DiPlay 0.2.8 的提交；不修改运行代码。
- [x] 建立规格、协议、任务、验证、源码证据、同步流程和台账；保存用户图片参考。
- 完成证据：仓库 refs、上述文档与本次文档检查；不以本项代表任何车机功能完成。

### P01 — 应用身份与可复现构建

- [ ] 固定应用 ID（规划 `io.github.rhsr1024.carbridge`）和调试后缀 `.debug`，显示名 CarBridge；根项目名可改为 CarBridge，内部 namespace 先保留。
- [ ] 检查所有 authority、显式组件、包可见性、通知、PendingIntent、deep link、自动启动、资源名和测试常量；杜绝与 DiPlay 并存冲突。
- [ ] 定义 CarBridge 独立 versionCode/版本名称及发布签名；认证资产继续从显式外部输入提供，不提交私钥或更改上游认证守卫。
- [ ] 记录 JDK/SDK/NDK；先运行上游要求的源码构建，再验证单独的 standalone 车测构建条件。
- 文件：`settings.gradle.kts`、`mobile/build.gradle.kts`、相关 manifests/strings、构建配置；评估 `automotive/build.gradle.kts` 的身份但不误用其 APK。
- 依赖：P00。交付：构建说明、身份映射与版本表、可验证的源码 APK；车测 APK 须满足认证前提。
- 验收：V01、V22；与原版 DiPlay 可共存，实际安装包名与文档一致。

### P02 — 协议与目标车机能力探针

- [ ] 捕获脱敏的 iAP2 Now Playing 样例：播放、暂停、切歌、seek、同名曲、无专辑、断连；核实字段 ID、长度、类型、单位、清空语义和曲目标识。
- [ ] 验证 wired/wireless 两条接收路径，不能只在一种连接上解析。
- [ ] 记录原生封面传输标识和 session 12 分片格式；若未下发，区分订阅、协议支持与播放器行为。
- [ ] 以本项目包名做最小 ECARX 注册/状态/回调探针；记录 SDK/EAS 服务、注册 API、来源类型、requestPlay 和读取 owner 的返回语义。
- [ ] 捕获目标车按一下 NEXT/PLAY/PAUSE 时 Android 与 ECARX 的事件关系；验证手机侧主动播放是否可与旧 PLAYING 区分。
- 文件：`Iap2Messages.kt`、`Iap2LinkEngine.kt`、wired/wireless clients、`CarPlayController.kt` 的诊断钩子；新增探针说明与脱敏 fixture。
- 依赖：P01。交付：字段映射证据表、SDK 能力表、按键样例、限制清单。无真实样例的字段标待验证，不猜协议。
- 验收：V02、V10、V24；只有探针不算正式适配完成。

### P03 — 统一状态与生命周期

- [ ] 新增 `NowPlayingSnapshot/Store`，区分连接、曲目代次、状态 revision；串行处理增量更新。
- [ ] 定义未知/缺失/显式清空、同名曲、停止/暂停/断连语义；时钟可注入。
- [ ] 定义进度基准、速率、暂停冻结和 seek 更新；异步资源必须校验曲目和连接。
- 文件：新增 `shared/.../nowplaying/`；保留/迁移 `media/CarPlayPlaybackStatus.kt` 的现有调用兼容层。
- 依赖：P00；具体字段由 P02 提供。交付：不可变快照、状态归并器及 fixture 测试。
- 验收：V03、V04、V18；测试断言增量不丢字段、换曲不串资源、断连不复活。

### P04 — 协作契约与 CarBridge 客户端

- [ ] 固定 v1 方法/数据结构、能力与拒绝码、签名信任和调试配对策略。
- [ ] 新增显式 Binder 客户端、身份验证、连接实例、超时/死亡处理与串行事件调度。
- [ ] 建立两端共享的协议 fixture/合同测试，协议源采用独立小目录或受控生成流程，记录版本哈希。
- [ ] 将就绪与忽略策略分开；不直接操作对端私有配置。
- 文件：新增 `common/.../interop/`、契约目录、manifest queries/权限；配套 MB01。
- 依赖：P01/P03，接口设计与 MB01 同步。交付：可在假后端上双向运行的协议。
- 验收：V07、V08、V19、V23。

### P05 — Now Playing 解析与发布源

- [ ] 依 P02 字段表补充必要订阅，解析标题/歌手/专辑/时长/进度/状态/资源标识。
- [ ] 从 wired/wireless 控制帧进入统一 Store，替换仅布尔监听的限制；导航订阅继续保留。
- [ ] 限制帧大小，处理截断/未知字段与顺序变化，出错不终止 CarPlay。
- 文件：`shared/.../iap2/message/Iap2Messages.kt`、`media/CarPlayPlaybackStatus.kt`、`orchestration/CarPlayController.kt`、transport clients、P03 包。
- 依赖：P02/P03。交付：真实协议驱动的状态，不用静态“DiPlay”通知凑信息。
- 验收：V02–V04、V18；录制样例和实机内容一致。

### P06 — Android MediaSession 与通知

- [ ] `CarPlayMediaKeys` 使用统一快照设置 metadata/playback state/actions/position，保持明确 PLAY/PAUSE。
- [ ] 暂停可恢复、后台有效、断开释放；移除用音频流启停覆盖手机真实状态的逻辑。
- [ ] `DiPlaySessionService` 通知展示实际歌曲和会话；无曲目时显示连接状态。
- [ ] URI/位图输出设大小预算和读取授权；为后续原生封面保留接口。
- 文件：`common/.../CarPlayMediaKeys.kt`、`DiPlaySessionService.kt`、Controller 与 P03 Store 的接线。
- 依赖：P03/P05。交付：MediaBridge 能读取歌名、歌手、状态与进度的标准会话。
- 验收：V05、V06、V18；通知、手机、会话三方状态相符。

### P07 — 统一音乐仲裁器

- [ ] 定义主动播放、失效意图、临时恢复令牌与车辆来源 revision，实现独立状态机。
- [ ] 合并 `CarPlayMediaKeys` 与 `AudioFocusCoordinator` 的音乐焦点职责，区分注册、route ready、车机 grant 与 Android focus grant。
- [ ] 连接/初始 PLAYING/重复状态不抢播；其他音乐后播放即让位；获准回调不得复活旧意图。
- [ ] 优先级按用途修正，通话高于音乐，导航/Siri 单独策略；可配置音乐互斥并统一开关语义。
- 文件：新增 `shared/.../playback/` 与 Android 适配；现有两处 focus 逻辑只通过该接口协作。
- 依赖：P03；事件来源由 P02/P05 校准。交付：状态转移表、可注入时钟和平台 focus 的实现。
- 验收：V12–V15、V19；通过真实状态转移测试，覆盖焦点拒绝/延迟/旧回调。

### P08 — 音乐输出门控与手机暂停

- [ ] 覆盖首次 start、补缓冲 restart、tail/drain 的 `AudioTrack.play`；未经许可不能输出。
- [ ] 失焦时及时静音/暂停音乐 track，清理过期缓冲并发明确 PAUSE；收到旧 PLAYING 不再抢回。
- [ ] 接收音乐与输出音乐分开；保持 CarPlay 连接及导航/通话通道，恢复不播放旧尾音。
- [ ] 通话结束仅恢复有效原意图；关闭互斥、运行中修改设置、重连时状态清晰。
- 文件：`shared/.../media/AndroidMediaSink.kt`、`MediaAudioBuffer.kt`（按实际需要）、P07、Controller 命令接口。
- 依赖：P07/P05。交付：实际音频受仲裁状态控制，统一焦点开关。
- 验收：V12–V15、V18；必须听音验证，不以 metadata PAUSED 代替静音证据。

### P09 — Geely/ECARX 直连适配

- [ ] 基于 P02 已验证能力封装 SDK 连接、注册、失效、来源/元数据/进度发布与注销。
- [ ] 使用实际应用包名与自己的启动 Intent，明确资源读取方；不复制 QQ 占位或 USB 盒子控制包。
- [ ] requestPlay 仅处理有效意图；注册后待机，失去车辆来源及时通知仲裁器。
- [ ] 封装可能阻塞的 SDK 调用，独立监控超时；撤销后的在途请求影响必须纳入交接。
- [ ] SDK 来源和许可证审查，优先复用已理解的合法适配层，不能把整包反编译代码直接纳入仓库。
- 文件：新增 vehicle/ecarx 适配（独立包或 Android library），必要的依赖/manifests、内容 Provider。
- 依赖：P01/P02/P03/P07。交付：不安装 MediaBridge 时可工作的正式直连后端。
- 验收：V10、V11、V16、V19；注册、回调和真实 iPhone 行为都要有证据。

### P10 — 统一方控与去重

- [ ] Android、ECARX、MediaBridge Binder 与界面输入进入同一命令路由。
- [ ] 按模式限制输入来源；明确 PLAY/PAUSE、切换、NEXT/PREVIOUS 分开；视频控制优先级保留。
- [ ] 根据 P02 双重事件样例去重，重复 requestId 幂等，快速真实双击不丢失。
- [ ] 记录命令接受/提交/拒绝及后续观测；不把发送成功当作手机执行确认。
- 文件：`CarPlayMediaKeys.kt`、`airplay/CarPlayMediaButton.kt`、`CarPlayController.kt`、新增 playback 路由；MB03。
- 依赖：P04/P06/P07/P09。交付：两条车机通道共用可靠控制。
- 验收：V06、V11、V17、V24。

### P11 — 模式协调与设置界面

- [ ] 实现三种模式及就绪/忽略决策表，串行 prepare/quiesce/commit/ready 状态机。
- [ ] 接入 MB02 的排除规则和 MB03/MB04 的命令、资格、让位；资格转移不生成新主动播放。
- [ ] 新版明确直连不要求用户再改忽略名单；明确桥接遇忽略冲突显示可理解的原因。
- [ ] 旧版使用手动配对说明；自动不支持时明确提示；暂时不可达不等于已卸载。
- [ ] 显示实际路线、外部来源让位、失败原因与诊断入口；不让普通界面出现 Binder/epoch 等术语。
- 文件：新增 mode coordinator、设置组件/持久化、HostActivity 少量接线、interop、vehicle。
- 依赖：P04/P06/P08/P09/P10、MB01–MB05。交付：完整双模式产品流程。
- 验收：V07–V09、V16、V19、V23；模式切换 20 次无双路申请或重复切歌。

### P12 — 原生封面与在线补全

- [ ] 扩展 link engine 事件分发以传递文件 session，再贯通 wired/wireless；按 P02 协议重组、关联并解码图片。
- [ ] 加入长度/像素/并发/超时限额、取消和缓存；旧曲目图片回调不能覆盖新曲目。
- [ ] 建立 content URI Provider 和必要读授权，验证 MB 进程与 ECARX 进程分别可读。
- [ ] 桥接使用 MB 资源策略；直连提供独立在线补全/缓存；离线/无匹配用明确占位。
- 文件：`Iap2LinkEngine.kt`、clients、Controller、新增 artwork 包与 Provider；MB05。
- 依赖：P02/P03/P05/P06/P09。交付：卡片真实图片及资源来源状态，原生与在线能力分项记录。
- 验收：V20、V21；原生链路未验证不能标支持，在线不得用错封面掩盖。

### P13 — 歌词与时间同步

- [ ] 桥接提供准确元数据/位置给 MB 现有 LyricsManager；若有原生歌词增加标准化资源描述。
- [ ] 直连封装可独立运行的歌词查找/LRC 解析/缓存与当前行发布，记录复用来源和许可。
- [ ] 同名、现场版、纯音乐、无时轴、断网、seek、暂停和切歌正确降级；保留用户偏移设置。
- 文件：新增 lyrics 资源模块、P03/P06/ECARX 适配；MB05 小窗验证/必要兼容调整。
- 依赖：P05/P06/P09/P12 的资源生命周期接口。交付：可匹配测试曲目的同步歌词，不承诺所有 iPhone 应用原生输出。
- 验收：V21、V04；暂停不滚动、跳转对齐、失败不残留上一首。

### P14 — 车型配置与 BYD 隔离

- [ ] 定义 VehicleProfile 和相关能力，将 Geely、通用、BYD 分开；吉利为此项目默认。
- [ ] BYD 专有键/toggle 兼容、HUD/仪表/导航、车辆读取等受 profile 控制，保留现有实现与测试。
- [ ] 不将 BYD 车辆信号当作 Geely 驻车证据；视频安全约束保留。
- 文件：新增 vehicle/profile、HostActivity/Controller 的有限入口、`hud/Byd*` 调用点、设置。
- 依赖：P01；P09 通过该接口接线。交付：可明确选择的车型能力，无全局 BYD 删除。
- 验收：V25；Geely 不启动 BYD 路径，BYD 回归测试仍通过。

### P15 — Geely OEM 返回按钮

- [ ] 从 docs 参考图提取纯黑标志，去除中英文，保持比例，放入方形白底画布并留边；保存处理说明和导出资产。
- [ ] 新增 Geely 默认图标和 `Geely` 标签，通过 profile 决定，用户自定义优先。
- [ ] 设置预览和实际 `oemIcons` 输出用同一函数；“恢复车型默认”与新装默认一致。
- [ ] 点击仍走 HOME，验证后台音频与再次进入 CarPlay；提示必要重连，不改认证身份。
- 文件：`AirPlayPersistence.kt`、`CarPlayHostActivity.kt` 图标加载/预览、`common/src/main/res/raw/`、设置资源；一般不需改 HOME 协议行为。
- 依赖：P14。交付：实际 CarPlay 页面显示 Geely 和纯车标的截图。
- 验收：V26；仅放入 assets 参考图不代表按钮修改完成。

### P16 — 集成、诊断与目标车机回归

- [ ] 统一诊断字段和导出；敏感字段脱敏，音频原始数据默认不记录。
- [ ] 执行完整 V01–V26，固定车机 ROM、iOS、播放器、两个 APK 版本和签名。
- [ ] 记录实际声音、歌曲卡片、命令链及模式迁移；处理失败后只重测受影响项和必要回归。
- [ ] 对每项未知给出实测结论或明确限制；有双播、重复方控、串图或旧状态反抢则不得通过。
- 文件：诊断基础设施、测试目录与新增结果报告；需要实际车机和 iPhone。
- 依赖：全部功能任务。交付：测试报告与未解决清单，不把模拟器结果称为车机通过。
- 验收：验证文件的完整完成定义。

### P17 — 发布和上游同步演练

- [ ] 固定 CarBridge/MediaBridge 配套版本与协议 major，执行已安装版本升级和数据保留测试。
- [ ] 执行一次集成分支 fetch/merge 检查和冲突清单演练，维护基线台账。
- [ ] 完成署名/许可、外部 SDK 来源、签名/认证输入、构建哈希与发布说明。
- [ ] 分功能提交，保留上游历史；发布/推送按届时用户任务执行，本计划不是发布授权。
- 依赖：P16。交付：可复现构建与升级路径、同步记录、已知限制。
- 验收：V01、V22、V25，配套版本可追溯。

## 3. MediaBridge 配套任务（另一仓库，必须同时排期）

以下任务在 `D:\CarSoft\MediaBridgeApp\MediaBridge-src` 实施。其根目录存在 `.codegraph/`，定位代码先用 CodeGraph，再针对缺失内容读取。配套实现已修改该仓库，结果见实施状态。

| 任务 | 内容与文件落点 | 依赖 | 交付/验收 |
| --- | --- | --- | --- |
| MB01 协作服务 | 新增显式 CompanionService、契约、受信 UID/签名校验、包可见性、版本协商；manifest 注册；不导出全量偏好 | P01/P04 契约 | V07/V23；双方合同测试与调试配对 |
| MB02 策略与路由隔离 | 从实际 settings 输出该包 ignored/enabled；`MediaListenerService` 选择、固定源、暂停兜底均尊重协作授权；持久“必须握手”标记；`LegacyBackend` 在途发布/requestPlay 收敛与 ACK；UI 展示临时直连排除 | MB01 | V07–V09/V19；重启不误桥接；不影响其他播放器 |
| MB03 方控与播放意图 | `LegacyMusicClient` → `MediaListenerService` → 协作命令只走一次；新增待播放意图申请流程，拆除协作路径对预先 PLAYING 的循环依赖；保留普通播放器原逻辑 | MB01/MB02/P07/P10 | V06/V11/V12/V17；明确 PLAY/PAUSE、ID 幂等与真实播放确认 |
| MB04 让位通知 | `LegacyBackend.onVehicleFocusChanged`、`MediaListenerService.yieldToVehicleSource` 与新的本地播放器优先事件发带代次 yield；停止旧输出，再通知 CarBridge 暂停；区分路由退出与正常切源 | MB02/MB03/P08 | V12–V16/V19；小窗让位与 iPhone 音乐停止同步 |
| MB05 卡片和兼容 | `ArtworkRepository`、`lyrics/LyricsManager`、`PlayerSnapshot` 校验元数据、URI 授权、进度和歌词；避免双端在线请求；更新状态提示及回归 | P05/P06/P12/P13、MB02 | V05/V20/V21/V23；完整卡片与旧版/无资源降级 |

不直接把 MediaBridge 整体搬入 CarBridge。可提取独立的协议或资源解析库，但依赖、许可证、两端版本和测试必须明确；两应用的 UI 和播放来源选择仍各自负责。

## 4. 需求覆盖表

| 需求 | 任务 | 验收 |
| --- | --- | --- |
| R01 | P00/P01/P17 | V01/V22 |
| R02 | P04/P11/MB01/MB02 | V07/V08/V09 |
| R03 | P04/P11/MB01/MB02 | V07/V08/V19 |
| R04 | P11/MB05 | V09/V23 |
| R05 | P02/P03/P05 | V02/V03/V04 |
| R06 | P06/MB05 | V05/V06/V18 |
| R07 | P12/MB05 | V20/V21 |
| R08 | P13/MB05 | V04/V21 |
| R09 | P09/P10/MB03 | V06/V10/V11/V17/V24 |
| R10 | P07/P08/MB03 | V12/V13/V14 |
| R11 | P07/P08/MB04 | V12/V13/V16/V19 |
| R12 | P07/P08 | V15 |
| R13 | P02/P09 | V10/V11/V16 |
| R14 | P14 | V25 |
| R15 | P15 | V26 |
| R16 | P03/P04/P08/P11/MB02/MB04 | V18/V19 |
| R17 | P02/P10/P16 | V17/V24 |
| R18 | P01/P16/P17 | V01/V22/V25 |

## 5. 更新规则与风险台账

每个任务追加：状态、实现提交、依赖的两端版本、测试结果、车机证据、剩余限制。只有所有复选项和验收满足才标完成。不得因时间不足把原生资源未知、SDK 注册失败或旧版双开风险写成“已支持”。

| 风险/未知 | 负责任务 | 处理 |
| --- | --- | --- |
| iAP2 字段与封面传输尚未有完整样例 | P02/P05/P12 | 先验证原始事实；在线补全与原生能力分别验收 |
| 手机主动播放与迟到状态难区分 | P02/P07 | 暂停确认/意图代次；无法判定时保留明确恢复入口 |
| 新包名 SDK 准入和 URI 读取 | P02/P09/P12 | 自身身份车测；不冒用参考 APK 身份 |
| 某些车机音乐不服从标准焦点 | P08/MB04/P16 | 用车辆来源协作验证；记录具体不兼容应用，不承诺任意应用强制暂停 |
| SDK 在途调用无法取消 | P09/P11/MB02 | 输出收敛确认；未确认不能自动开启第二出口 |
| 在线资源未匹配/断网 | P12/P13 | 缓存、匹配质量、可见降级，禁止串歌 |
| 认证输入和测试签名已显式配置 | P01/P17 | 保留上游守卫；源码包不含认证，车测包资产与用户原版 APK 相同；实际握手仍须手机验证 |
| 上游继续修改音频/控制大文件 | P14/P17 | 小接口接线，分主题提交，按同步清单逐次核验 |
