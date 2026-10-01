# CarBridge 与 MediaBridge 协作协议

版本：实现 v1.0；更新：2026-10-02。双方已实现显式 Messenger 协作通道；具体消息、边界和来源见 [实现记录](IMPLEMENTATION_ARCHITECTURE.md)。以下保留完整设计约束，建议逻辑方法由实现消息组合完成。依赖 [规格](CARBRIDGE_SPEC.md) 的 R02–R04、R09–R12、R16–R17；跨仓库任务见 [计划](CARBRIDGE_IMPLEMENTATION_PLAN.md)。

## 1. 职责与通道

CarBridge 是自身模式偏好的决策方，MediaBridge 是自己的忽略名单、桥接授权和后端状态的事实来源。车机仍决定实际媒体源所有权，任何一端都不能仅因偏好设置就宣称获得播放资格。

MediaBridge 已新增显式绑定的 `CarBridgeCompanionService`，使用基于 Binder 的 Messenger 双向消息。CarBridge 持有路由协调器，绑定明确的组件；不读写另一应用的私有 SharedPreferences，不使用无保护的隐式广播传递控制权。

| 数据 | 路径 |
| --- | --- |
| 曲目/状态/进度 | 标准 Android MediaSession，作为主数据通道 |
| 封面 | MediaMetadata + 受控内容 URI；单独验证读取授权 |
| 已有可用的结构化歌词 | 小型描述与内容 URI；版本化能力协商；不能塞大段数据进每个回调 |
| 策略/能力/就绪 | Companion Binder 快照与变更回调 |
| 模式交接/暂停让位/主动播放资格 | Companion Binder，带完整代次和确认 |
| 车机媒体命令 | 新配套使用带 ID 的协作命令进入统一路由；旧版保留 MediaSession 控制路径 |

新版对 CarBridge 发出一条媒体命令时选择 Binder 或 MediaController 其中一条，不两条都发。标准媒体会话仍供系统和其他合法控制器使用。

## 2. 身份、版本和最小权限

- 用显式组件与 Android 包可见性声明发现对端；规划包名 CarBridge `io.github.rhsr1024.carbridge`，MediaBridge 当前为 `com.mediabridge.app`。调试变体作为单独配对对象。
- 校验 Binder 调用 UID 对应包名和信任签名指纹；指纹来自项目自己的发布配置，不能凭空填写。双方可能由不同证书签名，不能只设同签名 permission 后假设可连接。
- 首次信任以预置受信发布指纹或本机清晰的配对操作完成；升级校验签名 lineage。不能把任意相同显示名的应用视为对端。
- 只暴露 CarBridge 自身的策略结果、必要的来源状态和控制能力，不导出整个忽略名单、任意文件或全量媒体记录。
- 每次连接协商 `protocolMajor/minor`、能力集合、对端版本；不兼容 major 进入旧版/不兼容流程，不部分启用所有权协议。
- URI 只读、有范围限制，连接/曲目失效后撤销适当授权；不接受任意 URL/path 来让服务代读文件。

## 3. 消息模型

以下是实现需要覆盖的逻辑字段和方法，不是已存在的 AIDL 定义。P04 与 MB01 冻结可编译契约后，两端测试同一组协议样例。

### 3.1 公共标识

| 字段 | 用途 |
| --- | --- |
| `carBridgeInstanceId`、`mediaBridgeInstanceId` | 每次进程启动生成新随机值，拒绝进程重启前的回调 |
| `connectionId` | 当前 CarPlay 连接；重连变化，不复用旧播放资格 |
| `routeEpoch` | CarBridge 协调器在实例内递增的路线代次 |
| `policyRevision` | MediaBridge 忽略/启用/配对策略的版本 |
| `requestId` / `commandId` | 请求与确认关联、幂等去重；重试沿用同一 ID |
| `intentId`、`intentRevision` | 播放意图与先后顺序，区别于路由代次 |
| `trackGeneration`、`snapshotRevision` | 资源、控制对象与状态关联 |
| `observedElapsedMs` | 本机单调时钟，跨进程只在同次设备启动中比较 |

标识组成复合键，不能只用从 1 重新开始的整数 epoch。重复请求返回已知结果；过期请求明确拒绝；参数过大/越界拒绝，不进入播放器线程。

### 3.2 能力与事实快照

`hello()` 返回版本与实例；`getPolicy(carBridgePackage)` 返回 `ignored/enabled/trusted/policyRevision`；`getReadiness()` 返回监听授权与连接、车机后端连接和注册、路由准备情况、最近错误。

建议能力位：`ROUTE_HANDOVER_V1`、`PLAY_INTENT_V1`、`SOURCE_YIELD_V1`、`COMMAND_ID_V1`、`CONTENT_URI_V1`、`LYRICS_DESCRIPTOR_V1`。自动模式至少要求前三项，不能靠“对端版本号较新”猜支持。

`VehicleState` 区分 `known ownerPackage` 与 `unknown`；不把空字符串当成无人持有来源。`sourceEventRevision` 在 MediaBridge 内单调递增，用于让位顺序和迟到回调排除。

### 3.3 交接接口

建议逻辑方法：`prepareRoute(target, epoch, policyRevision)`、`quiescePrevious(epoch)`、`previousQuiesced(epoch, fenceResult)`、`commitRoute(epoch)`、`routeReady(epoch)`、`revokeRoute(epoch, reason)`、`getRouteState()`、`onPolicyChanged()`。

每个 ACK 返回完整关联 ID、实际结果、拒绝原因及最新 revision。任何 “ready” 必须对应本轮，不接受上一轮成功缓存。状态原因包含 `IGNORED`、`LISTENER_UNAVAILABLE`、`BACKEND_UNAVAILABLE`、`VERSION_MISMATCH`、`OWNER_UNKNOWN`、`RELEASE_UNCONFIRMED`、`NEWER_INTENT` 等。

## 4. 路由状态机

路由状态与音乐播放状态分开，获得桥接路线并不立即播放。

| 状态 | 允许行为 |
| --- | --- |
| `UNNEGOTIATED` | 可以采集手机信息；不发布冲突的车机信息/接管方控 |
| `PREPARING` | 新路线检测依赖、缓存快照；不开启车机控制/播放申请 |
| `QUIESCING` | 旧路线关闭输入，停定时发布，作废排队任务，等待在途调用归并 |
| `BRIDGE_READY` | MediaBridge 是 CarBridge 的唯一车机出口；CarBridge 直连端停用 |
| `DIRECT_READY` | CarBridge 是自身车机出口；MediaBridge 对该包有效排除 |
| `SUSPENDED` | 车机已切到其他来源，路由可保留但不能继续申请播放或处理陈旧方控 |
| `FAILED_SAFE` | 无法确认交接/对端状态；保持关闭冲突路径并显示原因 |

`QUIESCING` 的完成标准不只是布尔值变 false：包括控制回调禁用、定时器取消、待执行发布和 requestPlay 失效、在途 Binder 调用已结束或底层通道明确失效。已经进入远程服务的调用不能靠本地递增 epoch 撤回；存在这种调用时不能发送释放完成 ACK。

### 4.1 桥接 → 直连

1. 由忽略策略变化或明确直连设置创建新 epoch；标记 CarBridge 路由切换中。
2. MediaBridge 立即对该包停止新增输入、播放申请、歌曲/歌词/进度发布，拒绝旧命令；完成旧调用收敛。
3. 其余播放器仍可按正常策略使用 MediaBridge；不为退出 CarBridge 而破坏全局监听服务。
4. MediaBridge 返回 `previousQuiesced`，记录该包需协议授权后才能重新桥接。
5. CarBridge 初始化/核验自身 ECARX 客户端，完成路由提交；注册只进入待播放。
6. 若旧播放意图仍有效且车辆期间未切源，转移资格；否则保持暂停/待播放。车机申请只在资格检查后发生。

### 4.2 直连 → 桥接

1. MediaBridge 确认未忽略、监听与后端就绪；CarBridge 创建新 epoch。
2. CarBridge 直连端关闭控制/发布，注销或停用自己的 SDK token，确认在途调用已收敛。
3. MediaBridge 获得新路线授权、关联实际 CarBridge MediaSession，加载同一快照并 ACK。
4. 提交后才允许 MediaBridge 代表该会话申请播放；有效旧意图可以转移，元数据本身不能触发新的优先级。

新所有者未准备好时不撤销旧所有者可以避免不必要中断；但一旦进入停止阶段，失败后的回滚也要走新一轮协商，不能两端各自恢复。

## 5. 忽略名单与重启后的规则

自动模式：`ignored=true` 要求直连；`ignored=false` 且就绪要求桥接。明确直连可通过运行时 `effectiveBypass` 让新版 MediaBridge 排除 CarBridge，不修改用户持久忽略名单。明确桥接尊重持久忽略，遇冲突显示修复动作。

MediaBridge 应持久记录“该受信包使用协作协议，未经本轮握手不得自动桥接”的标记。这样服务重启后即使忽略名单为 false，也不会先按普通播放器接管 CarBridge。记录不是旧 lease 的恢复授权；重启必须重新握手。

CarBridge 的新包名应纳入配套版本的协作识别流程。第一次发现未经握手的该包先等待配对，不先桥接再决定直连。旧 DiPlay 保持普通播放器处理，不能误用专门针对其他 CarPlay 包的“空会话”规则。

## 6. 播放资格与互斥

### 6.1 消除先发 PLAYING 才能申请焦点的循环

当前 MediaBridge `LegacyBackend.requestPlayFocus()` 依赖已缓存 PLAYING 快照；新协作流程不能要求 CarBridge 在获准输出前发布虚假 PLAYING。

新增 `requestPlayback(intent, routeEpoch, snapshotRevision)`：MediaBridge 验证路由、策略、意图新鲜度后，可使用“待播放意图 + 已知快照”申请车机播放资格，独立于 PCM 是否已经输出。结果通过 `playbackGrant/reject` 返回；CarBridge 再协调 Android 音乐焦点，只有两边允许且意图未被覆盖才开门输出。若另一项失败，撤销临时资格并发布真实暂停/等待状态，不冒充正在播放。

直连同样使用显式意图完成申请。获得路由、注册成功、收到歌曲信息、心跳续约都不能自动调用 requestPlay。两端要抑制自己的状态回声再次生成新意图。

### 6.2 让位通知

MediaBridge 发现车辆来源已切换或选中了新的本地播放器后，先在本地作废旧 CarBridge 输入/输出，再发送 `yieldPlayback(reason, sourceEventRevision, replacedIntentId)`。CarBridge 验证代次后立即关闭音乐门、丢弃过期音乐、向 iPhone 发明确 PAUSE，回报 `locallyGated` 和后续手机状态。

“已停止本地声音”“PAUSE 已发送”“iPhone 已报告暂停”是三个独立结果。最后一项超时不允许重新开门。重复让位幂等，不无限重复发暂停。若消息在传输中出现新的主动播放，按意图与来源 revision 裁决，旧让位不得暂停新的曲目意图。

MediaBridge 已有原生来源让位规则继续适用；必须补上向 CarBridge 的通知，不能只停自己的小窗和控制。CarBridge 永久失焦也通知对端撤销旧播放资格，避免两端各自重试。

### 6.3 命令关联

`executeMediaCommand` 包含 `commandId`、动作、route epoch、connection、可选目标曲目代次以及来源。每个命令只走一个出口；返回 `accepted/queued/rejected`，观察回报另发。明确 PLAY/PAUSE 不折成 toggle。

桥接专用 Binder 命令由受信 MediaBridge 发出；直连模式拒绝其陈旧 MediaController 控制。系统通知/耳机控制仍可作为合法本地输入进入统一路由，并在可取得 controller identity 时区分来源。没有可靠关联 ID 的重复回调通过实际采样设计去重，不能预设所有短间隔同键都是重复。

## 7. 超时、死亡与失败恢复

建议初始参数：握手 3 秒，交接 5 秒；活跃路由心跳 1 秒，失联 3 秒先关闭路线。参数集中管理并经车机调整；定时任务不能与可能阻塞的 SDK 调用共用唯一执行线程。

这些期限是“何时停止等待/主动停用”的界限，不是“超时即拥有对方权限”的凭证。lease 到期只有在旧方确实关闭且确认无在途操作时才支持转移。

| 故障 | 行为 |
| --- | --- |
| Binder 断开或对端崩溃 | 本方关闭相关命令/发布；重新绑定并核验；Binder death 本身不证明 ECARX 已清理旧注册 |
| 对端无响应但进程仍存在 | `FAILED_SAFE`，不直接启用另一出口；提示正在等待释放 |
| ECARX 服务重启 | 作废 token 和所有旧 ACK，重新探测；初始状态不得成为新主动播放 |
| Android 监听授权丢失 | 撤销桥接 readiness；自动模式等待退出确认，不能静默假定已直连 |
| 超时后迟到 ACK/命令 | 丢弃，记录旧 epoch；不能把已失败路线复活 |
| 两端同时启动/同时改设置 | CarBridge 串行决策；policyRevision 不一致重新准备，不并行提交 |
| SDK 调用卡住且不能确定撤销 | 保持失败态，等待明确的后端失效/清理证据；不做无限申请或强制结束其他应用 |

对端明确已卸载且没有残留所有权，或停止完成 ACK 加上后端核验，可作为恢复依据；空 owner/一次查询失败不算退出证据。

## 8. 旧版兼容

旧 MediaBridge 没有此协议，也无法读取 CarBridge 的明确直连偏好。必须在 UI 说明当前属于手动兼容：

| 选择 | CarBridge | 旧 MediaBridge |
| --- | --- | --- |
| 桥接 | 明确桥接，直连禁用；标准 MediaSession | 从忽略名单移除实际 CarBridge 包名、启用桥接及监听权限 |
| 直连 | 明确直连；首次显示配置检查 | 把实际 CarBridge 包名加入忽略名单，或停止桥接服务 |
| 自动 | 提示需要升级配套版本或选择手动方式 | 不支持自动接管协商 |

“我已配置忽略”是用户的手动配置事实，不是代码能验证对端私有设置的证据。启动直连前仍核对实际车机来源与重复回调；如果发现旧 MediaBridge 仍抢占，关闭冲突路线并给出原因。手动兼容不具备新版全部失联与意图保障，应在版本兼容表中明确。

## 9. 合同验收

必须有两端都运行的协议回放测试：乱序/重复 ACK、策略同时变化、旧进程回调、在途 requestPlay、心跳过期、重启后未握手、明确直连但未勾忽略、明确桥接且已忽略、旧版组合。测试断言是唯一可发布/可接受命令的路线和真实音乐门状态，不能只断言枚举值改变。
