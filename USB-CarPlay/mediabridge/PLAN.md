# MediaBridge 自维护版实施计划

版本：0.7  
日期：2026-09-25  
目标项目目录：`D:\车机软件\MediaBridgeApp\MediaBridge-src`  
需求基线：`D:\车机软件\MediaBridgeApp\docs\媒体桥接应用-SPEC.md` v0.5  
状态：标准 MediaCenter、F25 三条服务发现分支、播放器监听、歌词、受限封面 provider、收藏保护与单后端协调状态机已完成本地实现和自动化验证；T01–T24 的目标车验收仍未完成。F25 已不再是代码占位，但必须保持“代码完成、实车待验证”标识。当前修复与证据见 `docs/修复记录-2026-09-26.md`。  

## 0. 当前实现快照

已落地：D 盘本地 JDK/SDK/Gradle 工具链、同包名 release 构建、通知监听与播放器名称解析、标准 ECARX MediaCenter Binder 注册/元数据/方控回传、F25 配置入口、在线歌词候选源与缓存、±3000ms 即时微调、收藏能力降级、受限封面缓存 provider、开机/断链/注册重试入口、通知授权撤销提示、固定/自动/忽略播放器选择、手动重连和诊断日志导出。

必须上车确认后才能宣称完成：博越 L 的 Binder 权限是否允许当前签名注册、标准模式和 F25 模式的车机卡片/仪表/HUD 实际可见性、各播放器收藏行为、无 USB 盒 F25 闭环、原车是否接受传入的封面 URI/全文歌词。工程本地编译成功不替代这些实测。

## 1. 项目目标和实施原则

在 `MediaBridge-src` 中建立一个可以由 Android Studio、Gradle、Git 长期维护的 Android 项目。V1 面向当前这台博越 L（Android 11 定制系统），优先使用包名 `com.netease.cloudmusic.iot` 和当前修改版相同的签名进行覆盖升级。

应用保留旧 MediaBridge 的核心桥接行为：

- 从已授权的 `NotificationListenerService` 获取第三方播放器的 MediaSession。
- 选择当前播放器，读取歌名、歌手、专辑、封面、状态、进度和收藏状态。
- 经 ECARX 媒体中心向原车媒体区域发送信息。
- 把原车上一曲、下一曲、播放、暂停和收藏操作转发给当前播放器。
- 获取并同步歌词，支持 -3000～+3000ms 的即时微调。
- 提供旧 MediaBridge 标准通道与 F25 兼容通道。

实施遵循以下原则：

1. **以实测可用的旧 MediaBridge 行为为兼容基线。** 方法名、类结构可以重写，业务结果、协议字段、调用顺序和默认值不得随意变化。
2. **参考实现不等于整包复制。** 反编译 Java 可能包含结构错误、重复逻辑和编译伪影；应根据可观察行为重新实现。厂商 Binder 协议则必须逐项核对 descriptor、transaction 和 Parcel 顺序。
3. **先让标准通道完整工作，再接 F25。** 未完成标准闭环前，不并行混入第二套 SDK，以免无法判断故障来源。
4. **所有必要改变均记录理由。** 本文第 7 节列出允许偏离旧行为的项目；未列出的行为默认先保持，再依据测试决定是否优化。
5. **原始材料只读。** 根目录 APK、`sources`、`resources`、`Cplay/sources`、`Cplay/resources` 和 `diagnostics` 不做修改；所有新代码、测试和项目文档均进入 `MediaBridge-src`。
6. **ADB 是开发工具，不是运行依赖。** 正常使用必须在关闭 ADB、断网或冷启动后仍按规格工作。
7. **不绕过商业激活或复制敏感凭据。** Cplay 仅用于理解 F25 的公开行为和协议路径；USB 盒、激活、AppKey、VIN 和业务代码不迁入。

## 2. 输入材料和证据优先级

### 2.1 当前材料

| 材料 | 位置 | 用途 | 处理方式 |
| --- | --- | --- | --- |
| MediaBridge APK | `..\com.geely.auto.music.MediaBridgeApp - 副本.apk` | 证书、Manifest、DEX、资源和覆盖升级基线 | 只读，先确认它究竟是原版还是当前修改版 |
| Cplay APK | `..\CarPlay - 副本.apk` | F25 参考 APK | 只读 |
| MediaBridge 反编译代码 | `..\sources`、`..\resources` | 兼容行为和协议参考 | 只读 |
| Cplay 反编译代码 | `..\Cplay\sources`、`..\Cplay\resources` | F25 通道参考 | 只读 |
| APK 诊断 | `..\diagnostics` | 既有证书/Manifest 分析 | 重新核对，不代替签名私钥检查 |
| 完整排查记录 | `D:\Downloads\解决权限狗跨用户安装.md` | 实车现象和历史命令输出 | 用户报告和其他 AI 推断分开记录 |
| 需求规格 | `..\docs\媒体桥接应用-SPEC.md` | 功能、边界和验收的唯一需求基线 | 变更需同步更新版本 |

### 2.2 证据优先级

发生冲突时按以下顺序判断：

1. 同一构建在目标博越 L 上的可重复实测和原始日志。
2. APK 中实际的 Manifest、签名、DEX/Binder 协议及运行时 dump。
3. 反编译源码中的明确控制流。
4. Android 官方 API 文档。
5. 导出对话中的分析性结论。

任何仅由反编译类名或接口存在推断出的能力，标为“接口存在，未实车验证”，不能写成“功能已支持”。

## 3. 目标工程结构

第一阶段创建单 application module，避免在协议未稳定时过早拆成大量 Gradle module；使用包级边界保持解耦。协议成熟后再决定是否拆模块。

```text
MediaBridge-src/
├── README.md
├── PLAN.md
├── CHANGELOG.md
├── settings.gradle.kts
├── build.gradle.kts
├── gradle.properties
├── gradlew / gradlew.bat
├── gradle/
│   ├── wrapper/
│   └── libs.versions.toml
├── app/
│   ├── build.gradle.kts
│   ├── proguard-rules.pro
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── aidl/                         # 仅经核验必须使用的厂商接口
│       │   ├── java/
│       │   │   ├── com/geely/auto/music/   # 需要保持全限定名的兼容组件
│       │   │   └── app/mediabridge/
│       │   │       ├── app/
│       │   │       ├── ui/
│       │   │       ├── session/
│       │   │       ├── player/
│       │   │       ├── model/
│       │   │       ├── bridge/
│       │   │       │   ├── api/
│       │   │       │   ├── legacy/
│       │   │       │   └── f25/
│       │   │       ├── lyrics/
│       │   │       ├── favorite/
│       │   │       ├── artwork/
│       │   │       ├── settings/
│       │   │       ├── diagnostics/
│       │   │       └── compat/
│       │   └── res/
│       ├── test/
│       └── androidTest/
├── protocol-fixtures/                       # Binder/Parcel 结构和测试夹具，不含凭据
├── scripts/                                 # APK 审计和可重复验证脚本
└── docs/
    ├── COMPATIBILITY_MATRIX.md
    ├── PROTOCOL_NOTES.md
    ├── TEST_LOG.md
    ├── BUILDING.md
    ├── SIGNING_AND_ROLLBACK.md
    └── decisions/                           # 每项有意改变旧行为的 ADR
```

### 3.1 身份和兼容组件

- `applicationId = "com.netease.cloudmusic.iot"`。
- 不声明 `android:sharedUserId`。
- 新的内部 namespace 可独立，但通知监听组件尽量保留 `com.geely.auto.music.MediaListenerService`，避免覆盖升级后原通知授权仍指向旧类名。
- 若 `UniversalBridgeService`、BootReceiver 或 Artwork provider 的全限定名会影响外部绑定、旧 PendingIntent 或设置迁移，也保留兼容入口，由入口委托新实现。
- 应用显示名明确为媒体桥接，不伪装成网易云官方应用。
- Provider authority 默认保持 `com.netease.cloudmusic.iot.artwork`，确认旧车机是否依赖后再决定是否更名；不得公开任意文件。

## 4. 统一领域模型和后端边界

先定义与厂商 SDK 无关的数据模型，旧标准通道和 F25 通道都只能通过这些模型与业务层交互。

### 4.1 核心模型

```kotlin
data class PlayerId(
    val userId: Int,
    val packageName: String,
)

data class TrackSnapshot(
    val generation: Long,
    val sessionId: String,
    val trackId: String?,
    val player: PlayerId,
    val appLabel: String?,
    val title: String?,
    val artist: String?,
    val album: String?,
    val durationMs: Long?,
    val position: PlaybackPosition,
    val state: PlaybackStatus,
    val artwork: ArtworkRef?,
    val favorite: FavoriteState,
    val lyrics: LyricsState,
    val launchIntent: PendingIntent?,
)

enum class FavoriteState {
    FAVORITED, NOT_FAVORITED, UNKNOWN, UNSUPPORTED
}

enum class FavoriteWriteCapability {
    UNSUPPORTED, SET_WITHOUT_CONFIRMATION, SET_AND_OBSERVE
}
```

### 4.2 后端接口

```kotlin
interface CarBridgeBackend {
    val id: BackendId
    val state: StateFlow<BackendState>
    val capabilities: StateFlow<BackendCapabilities>
    val controls: Flow<CarControlEvent>

    suspend fun connect(generation: Long): BackendResult
    suspend fun registerClient(generation: Long): BackendResult
    suspend fun publishSnapshot(snapshot: TrackSnapshot): BackendResult
    suspend fun publishProgress(update: ProgressUpdate): BackendResult
    suspend fun publishLyric(update: LyricUpdate): BackendResult
    suspend fun publishCapabilities(capabilities: SourceCapabilities): BackendResult
    suspend fun unregister(generation: Long)
    suspend fun close(generation: Long)
}
```

约束：

- 后端不负责选择播放器、不查询歌词、不维护 UI 设置。
- 后端不得创建自己的无限重试循环；重试由唯一 `BridgeCoordinator` 调度，底层只报告结构化阶段和错误。
- 所有异步结果携带 `generation`。模式或连接代次改变后，旧结果无条件丢弃。
- 连接成功、初始化成功、注册成功分为三个状态；任何一个都不能代表完整可用。
- 最新状态快照可以重发；上一曲、下一曲、收藏等一次性命令永不自动重放。

## 5. 旧 MediaBridge 行为迁移表

| 新模块 | 旧实现参考 | 默认保持的行为 | 必要修正 |
| --- | --- | --- | --- |
| SessionRepository | `MediaListenerService.updateActiveSession/findBestController/registerAllControllerCallbacks` | 通过通知监听获取 sessions；排除自身/native/忽略包；播放态和最近活跃优先；token 变化重绑 | 拆成过滤、评分、稳定选择；避免反编译重复分支 |
| PlayerNameResolver | `MainActivity.getAppLabel`、`MediaListenerService.getAppName` | PackageManager label；失败回退包名 | 缓存带 user/package/version/locale；记录失败原因；验证包可见性 |
| MetadataMapper | `MediaListenerService` 的 metadata/status/position 提取 | 保持字段来源与空值回退 | 统一单位、曲目标识、旧异步结果校验 |
| ArtworkRepository | `ArtworkHelper`、`CoverFallback`、`ArtworkContentProvider` | URI → Bitmap → 通知缓存 → 默认封面顺序 | 限制缓存、只读 URI 授权、避免重复落盘 |
| LegacyBackend | `MediaCenterAPI`、`UniversalBridgeService.performRegistration` | EAS 优先/直连 fallback；registerInMusic/registerMusic；source type 6；能力声明；初始暂停态 | 后端化、结构化错误、补齐断开重连，不吞异常 |
| BridgeCoordinator | `UniversalBridgeService` | 有限重试、Binder death、焦点检查、注册后重放 `lastMetadataIntent` | 由最新 `TrackSnapshot` 代替 Intent；重试耗尽后继续低频恢复 |
| PlayerController | `MusicClientImpl`、`MediaListenerService.sendMediaCommand` | next/previous/play/pause 显式语义和播放器适配 | 防止旧 session 控制、旧代次事件、重复事件 |
| FavoriteController | `handleCollect`、AppMediaFeatures、通知 PendingIntent | 优先可靠适配；源端状态最终校正 | UI 改“显示收藏”；不支持隐藏/提示；未知不伪报成功 |
| LyricsRepository | `LyricsManager` 和五个 adapter | 200ms debounce、切歌取消、正/负缓存、来源顺序 | 超时/取消/失败缓存过期/隐私提示；track key 更稳健 |
| LyricsTimeline | `LrcParser`、`getCurrentLine` | 当前行按播放位置 + 基础提前量计算 | 用户值扩为 ±3000；调整当前曲立即重算；不篡改真实进度 |
| SettingsRepository | `SettingsManager`、`mb_ui_prefs` | 默认歌词/收藏开启、忽略包、历史包、微调值 | schema 版本、幂等迁移、模式分别保存微调 |
| Boot/Lifecycle | `BootReceiver`、前台 Service | 开机恢复和常驻状态 | 遵循用户启用状态；分清 force-stop、未解锁、授权撤销 |

### 5.1 必须保留的标准通道协议顺序

首个可运行实现应按旧版顺序完成：

1. 连接 EAS Framework；无法取得主 Binder 时按旧逻辑进入直接服务 fallback。
2. 初始化成功后注册 MusicClient，优先 `registerInMusic(packageName, client)`，按已核验逻辑 fallback 到 `registerMusic(client)`。
3. 注册成功后声明 source type 6、MediaCenter 能力 `[0,2,3]`、收藏类型 `[0,3,4]`，设置当前 source type 6。
4. 发布初始暂停快照，再发布最新真实 `TrackSnapshot`。
5. 播放状态变为播放时，按旧逻辑判断当前 focus，并在必要时请求 `requestPlay`。
6. Binder 死亡、注册 token 失效或 IPC 抛错后，使旧 token 失效，再完整重连/注册。
7. 注册恢复后只重发最新快照，不重放历史按键或收藏命令。

数组、source type、注册顺序和 focus 语义先保持；只有实车日志证明不适合时，才以 ADR 记录后调整。

## 6. F25 参考和实现边界

### 6.1 可以参考的部分

- `Cplay/sources/com/zqsdk/OooOo00.java`：MediaCenterAPI 初始化、注册 MusicClient、请求播放、发布状态的高层顺序。
- `Cplay/sources/com/zqsdk/C0151OooOo0.java`：下一曲 87、上一曲 88、快退 89、快进 90、播放 126、暂停 127 等回调映射。新实现仍以语义事件为主，不把播放和暂停都降成 toggle 85。
- `Cplay/sources/com/ecarx/eas/sdk/mediacenter/MediaCenterProxy.java`：依据服务端 EAS 支持选择 OpenAPI/EAS，及通过 service pool 获取 `mediacenter` 服务的路径。
- `Cplay/sources/com/ecarx/eas/framework/sdk/common/internal/EASFrameworkApiClient.java`：EAS/OpenAPI 两个服务入口、binding died、disconnect、用户解锁等待和重连框架。
- F25 SDK 中已有进度、当前歌词、播放信息和收藏相关接口，可作为协议存在性的证据。

### 6.2 禁止迁移的部分

- USB 盒子、CarPlay 投屏、设备枚举和 `USB_DEVICE_ATTACHED` 业务。
- AppId/AppKey、激活逻辑、VIN、账号、网络验证或混淆字符串中的敏感值。
- 硬编码启动其他应用的 Intent、固定封面和样例文案。
- 把显式 play/pause 统一变成 toggle 的旧行为。
- 未知来源的整个 SDK 包或重复全限定名类整包复制。

### 6.3 F25 必须先做的 spike

在完整 `F25Backend` 前创建一个仅开发构建可进入的验证页，逐步记录：

1. 不插 USB 盒子时能否绑定 EAS/OpenAPI 服务。
2. 目标服务实际使用哪个 action、component、descriptor 和版本分支。
3. `getService(pid, uid, packageName, "mediacenter")` 是否返回 Binder，是否按包名/签名拒绝。
4. 能否用当前包名注册 MusicClient 并取得有效 token。
5. 初始元数据是否出现在原车媒体栏。
6. 上一曲、下一曲、显式播放、显式暂停是否回调一次且语义正确。
7. 进度、当前歌词、全文歌词和收藏能力逐项是否被本车采用。
8. Service disconnect、Binder death、重复 init 后能否清理并恢复。

每一步记录为：接口存在 / 调用成功 / Binder 返回成功 / 原车可见 / 实际控制成功。前一步失败时，不用后一步的成功假象掩盖。

## 7. 允许偏离旧逻辑的项目

下表是本项目已知需要改变旧实现的地方。除此之外的行为，先保持兼容。

| 改动 | 理由 | 新行为 | 回归保护 |
| --- | --- | --- | --- |
| 删除 shared UID 和系统自授权 | 当前车机签名不兼容，修改版普通 UID 已实测成功 | 使用用户开启通知访问；不反射修改安全设置 | 通知授权、会话发现和标准桥接实车测试 |
| 删除模拟息屏/自动息屏/AVAS | 与纯媒体桥接无关且涉及亮度/车辆控制 | 不迁移 UI、Service、权限和启动调用 | APK 静态审计无残留 |
| “显示点赞”改为“显示收藏” | 功能实际是收藏/取消收藏，旧名称不准确 | 所有用户文案使用“显示收藏”；旧 key 仅用于迁移 | UI 快照和设置迁移测试 |
| 收藏不再无条件乐观成功 | 旧版立即回写可能和播放器真实状态不一致 | 不支持时隐藏或提示；未知显示未确认；源端状态最终确认 | 支持/不支持/无反馈/输出端不支持四类测试 |
| 歌词微调扩为 ±3000ms | 用户明确要求 | `实际位置 + 通道基础补偿 + 用户微调`；当前歌曲立即重算 | 边界、方向、暂停/拖动/重启测试 |
| 应用名称增加可诊断回退 | 普通 UID 环境下旧版只显示包名 | label → 有效缓存 → 包名，并说明失败原因 | user/package/version/locale 缓存测试和实车查询 |
| 重试不在 10 次后永久停止 | 冷启动故障仅重启 App 即恢复，旧版部分路径只清状态 | 快速退避后 60 秒低频恢复；明确拒绝则暂停 | fake service 晚 120 秒 ready，Binder death 测试 |
| 单一状态机统一重试 | 旧多层 Handler 容易形成遗漏或竞态 | BridgeCoordinator 串行转换，后端只报告事件 | generation 和重复触发单测 |
| F25 后端化 | 用户要求可配置切换 | 与标准通道共享快照和控制接口 | 双模式切换 10 次，无重复注册/控制 |
| 包可见性最小化 | `QUERY_ALL_PACKAGES` 尚未证明必需 | 先 queries 和诊断，自用变体再试 broad visibility | APK 权限审计和标签对照 |

每项进一步的行为变化必须新增 `docs/decisions/ADR-xxxx.md`，包含：旧行为、问题证据、选择方案、未选方案、兼容影响、回退方法和测试。

## 8. 分阶段实施计划

### P0 — 取证、工程和覆盖升级闸门

目标：在写核心逻辑前确认“我们正在覆盖哪个 APK、用什么签名、哪些协议必须保持”。

#### P0.1 建立可重复工具链

- 创建 Gradle Kotlin DSL 工程、Wrapper 和 version catalog。
- 检测本机 JDK、Android SDK、Build Tools 和可用 compileSdk；选择版本后固定在工程文档，不在计划阶段凭空指定不存在的版本。
- `minSdk=28`、`targetSdk=34` 先与参考 APK 对齐；compileSdk 选择已安装且可在 Windows/macOS/CI 重现的版本。
- 创建 debug/release 配置。release 签名从本机安全配置读取，不把密钥或密码提交到项目。
- 添加 `./gradlew test lint assembleDebug` 的基础验证。

交付：Gradle 工程、`docs/BUILDING.md`、版本锁定说明。  
完成条件：Windows 和 macOS 至少各有一套可执行构建说明；空壳 APK 可安装到非目标测试环境，Manifest 不含 shared UID。

#### P0.2 APK 身份和签名审计

- 对两个根目录 APK 计算 SHA-256、读取 package/version/min/target、证书摘要、Manifest 组件和权限。
- 明确 MediaBridge APK 是原版还是当前可覆盖的修改版；不要依据文件名“副本”推断。
- 从车机读取当前已安装包的 `codePath`、version、签名摘要、user 安装状态和组件列表，与本地 APK 对照。
- 确认是否持有当前修改版签名私钥或可重复使用的签名工具。只有证书相同不代表持有私钥。
- 备份当前可用 APK、版本、证书摘要和设置截图；设计回退流程。

阻断条件：不能用同一签名产生可覆盖 APK 时，停止“覆盖升级”发布路线并向用户报告；不得默认卸载当前可用版本。  
交付：`docs/SIGNING_AND_ROLLBACK.md`、`docs/COMPATIBILITY_MATRIX.md` 的身份章节。

#### P0.3 Manifest 和协议清单

- 建表记录旧 MainActivity、MediaListenerService、UniversalBridgeService、BootReceiver、Artwork provider、intent-filter、meta-data、authority 和 SharedPreferences 文件。
- 提取两套 ECARX 接口的 descriptor、transaction、Parcel 顺序、服务 action/component/module 名称和版本选择条件。
- 对同名接口做结构 diff，决定共享一套、适配两套或仅保留最小自有 Binder 实现；禁止同包同类重复进入 APK。
- 记录 source type 6、能力数组、收藏类型、注册顺序和初始数据字段。

交付：`docs/PROTOCOL_NOTES.md`、`protocol-fixtures`。  
完成条件：每个将迁入的厂商接口有来源、用途、调用方和测试；敏感凭据清单为空。

#### P0.4 覆盖升级探针

- 空壳版本不能直接覆盖并破坏可用 App。先使用测试构建验证签名和 Manifest，再进入有回退准备的同包测试。
- 保持通知监听兼容类名，验证覆盖后系统授权记录是否仍指向有效组件。
- 验证旧 SharedPreferences 可读，迁移只读预览不写回。

完成条件：确认覆盖安装成功，或明确阻断原因；任何失败都不通过卸载来掩盖。

### P1 — 设置、状态页和通知授权

目标：先建立可观察、可迁移的壳，再接入媒体与车机协议。

#### P1.1 设置和迁移

- 新建版本化 `SettingsRepository`。
- 读取旧 `media_bridge_settings`：`lyrics_enabled`、`show_like_button`、`blacklist`、`seen_packages`、默认文本等实际仍有用途的键。
- 读取 `mb_ui_prefs/lyric_tune_ms`；旧值钳制到 -3000～+3000 并写入标准模式配置。
- 新 UI 名称为“显示收藏”，旧键仍用于一次性迁移。
- 不迁移 `auto_screen_off_self`、AVAS 或息屏状态。
- 迁移使用 schema version，重复执行结果相同；成功前不删除旧数据。

#### P1.2 状态页

- 展示：桥接启用状态、实际模式、目标/待重启模式、通知授权、Listener 连接、当前播放器、当前歌曲、车机连接阶段和最近错误。
- 通知访问从未开启时显示“尚未开启”；曾开启后撤销显示“通知访问已关闭”。
- 提供“去开启”和“重新检查”；查询失败显示未知，不伪报撤销。
- 授权恢复但 Listener 未绑定时显示“已授权，正在恢复监听”。
- 授权撤销后旧歌曲标记失效、控制禁用、歌词/收藏操作清空。

#### P1.3 清单最小化

- 先声明 INTERNET、ACCESS_NETWORK_STATE、RECEIVE_BOOT_COMPLETED、合适的前台服务能力以及 NotificationListener 组件。
- 不声明 shared UID、WRITE_SECURE_SETTINGS、系统悬浮窗、WRITE_SETTINGS、CAR_*、跨用户和广泛存储权限。
- ECARX 权限只在明确的调用失败证据要求时单独评审，不能批量加回。

测试：迁移单测、授权状态 UI 测试、组件可实例化测试、最终 Manifest 权限快照。  
完成条件：即使没有授权，App 也能稳定启动、清楚解释状态且不自改系统设置。

### P2 — 播放器发现、选择和应用名称

目标：不接车机输出，先把第三方播放器输入做正确。

#### P2.1 NotificationListener 和 MediaSession

- 保留兼容组件 `com.geely.auto.music.MediaListenerService`，内部委托 `SessionRepository`。
- 仅在 `onListenerConnected` 后注册 active sessions listener；断开时释放 callback，并按公开 API 条件请求 rebind。
- 通过自身已授权组件调用 `getActiveSessions`；捕获并分类 SecurityException、空列表和查询异常。
- 通知 token 只作为补充；无 MediaSession token 的通知不宣称可控制。

#### P2.2 选择算法

- 保留旧版：自身/native/忽略过滤、正在播放优先、最近活跃稳定选择、暂停时保持上次有效目标。
- 将过滤结果和分数写入诊断，便于判断“发现了但被忽略”。
- 支持自动、固定播放器和恢复自动；固定包无会话时等待，不偷切到其他 App。
- 包名身份严格包含 user；`cn.toside.music.mobile` 和 `mobileo` 不合并。

#### P2.3 应用名称

- 在应用进程内查询 ApplicationInfo/label，不用 shell 结果代替。
- label 成功后按 user/package/version/locale 缓存；安装/替换/语言变化时失效。
- 失败时显示包名并保留异常类别。
- 先通过 `<queries>` 验证；仍失败时制作仅自用的 visibility 实验变体测试 `QUERY_ALL_PACKAGES`。验证有效后再决定 release 是否包含。

测试：多 session 排序、playing/paused 切换、固定/忽略、token 更换、跨 user 不混用、label 缓存与失败。  
实车门：酷我、实际安装的星海/星河 Music 和一个新标准 MediaSession App 均能出现；名称失败有明确原因。  
完成条件：输入状态稳定，不接 ECARX 时也可在状态页正确显示和控制所选播放器。

### P3 — 标准 LegacyBackend 最小闭环

目标：使用旧 MediaBridge 已在本车成功的路径完成第一条端到端闭环。

#### P3.1 协议实现

- 只迁入 P0 核验过的 EAS/MediaCenter Binder 接口。
- 保持旧版 EAS 优先、直接绑定 fallback 和主 Binder 获取方式。
- 保持注册与能力声明顺序（第 5.1 节）。
- 所有 Binder 调用包装为结构化结果：阶段、错误种类、原异常、可否重试。
- 连接类支持幂等 close、unbind、unlinkToDeath、token 清理和重复 init 防护。

#### P3.2 元数据和控制

- 发布 title/artist/album/artwork/duration/status/source/appName/package/launch/playerIntent/uuid/listId。
- 用真实播放器 label 作为 appName；桥接包身份字段保持协议要求，二者不得混淆。
- 接收车机 next/previous/play/pause，转发到接收事件时的目标 controller。
- 保持显式 play/pause，不退化成 toggle。

#### P3.3 最小恢复

- Binder death、service disconnected、binding died、null binding 和 token invalid 均上报协调器。
- 注册恢复后发送最新快照。
- 初始阶段仍可采用接近旧版的重试节奏，完整退避在 P4 统一。

实车门：酷我播放时原车显示歌名/歌手/状态；连续 20 次 next/previous 只各执行一次；重复 pause 不触发 play。  
完成条件：标准模式在不授予 UID1000/MDC/OPENAPI 高权限的情况下达到当前修改版已验证的核心效果。

### P4 — 唯一连接状态机和可靠恢复

目标：解决冷启动或依赖晚启动时人工强停重开的现象，同时保留旧版已有重试优势。

#### P4.1 BridgeCoordinator

- 串行维护 STOPPED、WAITING_FOR_USER、CONNECTING、INITIALIZING、REGISTERING、REGISTERED、RETRY_WAIT、ACCESS_DENIED、INCOMPATIBLE。
- 同时只有一个连接尝试和一个重试 timer。
- 默认退避 2、5、10、20、30、60 秒，此后 60 秒带小抖动；明确权限/身份拒绝停止无意义重试。
- service ready、用户解锁、有效 session 恢复和手动重连可以触发即时尝试，但要合并去重。
- 连接、初始化、注册分别设置观察超时；不能无限创建卡住的 Binder 线程。

#### P4.2 最新快照

- `TrackSnapshotStore` 始终只保存最新有效快照和代次，取代旧 `lastMetadataIntent`。
- 注册成功后完整重发最新快照；进度按当前时间基准重算。
- 不保存、不重放 next/previous/favorite 命令。

#### P4.3 生命周期

- Boot、user unlocked、Listener connected、手动启动调用同一幂等入口。
- force-stop 后不承诺绕过系统自行启动；用户重开后必须恢复。
- 页面退出不停止桥接；用户明确停止后不自动重启。
- 不把固定 30 秒开机延时当作恢复方案；延时仅能作为 ROM 兼容参数。

测试：fake service 分别延迟 5/30/120 秒 ready；每种断链；旧代次回调晚到；重复触发 100 次无多重 timer。  
实车门：正常冷启动和休眠唤醒各至少 5 次，失败能够自行恢复或给出准确阶段，不靠重复授权。  
完成条件：当前记录中“强停重开后恢复”的场景由应用自身状态机恢复。

### P5 — 封面、歌词和即时 ±3000ms 微调

目标：恢复旧歌词/封面能力，并按新规格澄清微调行为。

#### P5.1 封面

- 保持旧的来源优先级，但统一进入 `ArtworkRepository`。
- 内存 + 有界磁盘缓存；曲目变化取消旧任务。
- Provider 仅允许已生成封面，使用 grant URI permission，防目录穿越。
- 验证原车是否接受固定 authority 和 URI；失败时记录厂商限制。

#### P5.2 歌词来源

- 按旧顺序迁移并逐个测试 NetEase、QQ、Kuwo、LrcLib、Zvuk adapter。
- 统一网络 client、超时、User-Agent、取消、错误分类和限流；不迁入账户凭据。
- 先播放器可靠歌词，再成功缓存，再在线来源。
- track key 至少结合规范化 title/artist/duration；异步结果必须匹配当前 track generation。
- 负缓存有过期时间，提供手动重新获取；旧版永久 negative cache 不照搬。

#### P5.3 时间轴和微调

- 保持标准模式基础补偿 +1000ms，F25 初始也用 +1000ms 但标记待校准。
- 用户值 -3000～+3000ms，100ms 步长，默认 0。
- 公式：`samplePosition = playbackPosition + backendBaseOffset + userTune`。
- “歌词慢了点＋（提前）”；“歌词快了点－（延后）”。
- 修改后立即重算当前歌曲当前行，不重新下载、不切歌、不重启、不改真实进度；暂停也立即按暂停位置重算。
- 全文 LRC 和当前行通道分别验证，避免车机自行按全文时间轴定位时重复补偿。

测试：LRC 解析、同名歌曲 key、切歌旧结果、-3000/0/+3000、边界连点、暂停/拖动、重启持久化。  
实车门：当前歌曲调整后可见结果更新；显示延迟另记，不把本地重算当作车机已显示。  
完成条件：歌词失败不会中断元数据/方控，关闭歌词立即清除原车旧歌词。

### P6 — “显示收藏”能力

目标：保留旧版收藏通路，修正文案和失败反馈，不影响基础桥接。

#### P6.1 能力模型

收藏显示取三者交集：

```text
用户开启“显示收藏”
        ∩
播放器 FavoriteWriteCapability
        ∩
当前 CarBridgeBackend 收藏能力
```

- 已知任一端不支持：不向原车声明收藏能力，原车入口应隐藏；应用设置保留并说明具体哪一端不支持。
- 只有触发后才能得知不支持：拒绝伪报成功，有限频提示“当前播放器暂不支持收藏”，在状态页保留原因。
- 状态为 FAVORITED、NOT_FAVORITED、UNKNOWN、UNSUPPORTED；UNKNOWN 不显示成未收藏。

#### P6.2 播放器适配

- 先重建 AppMediaFeatures 接口；按来源验证 notification PendingIntent、Rating heart、已知 custom action。
- Spotify、YandexMusic、YandexNavi、Zvuk 的旧适配作为测试参考；不因类存在就宣称本车播放器支持。
- 酷我和实际星海/星河 Music 版本分别检查 actions、metadata、通知按钮和实际收藏变化。
- `setFavorite(true/false)` 与 `toggleFavorite()` 分开；状态未知且只有 toggle 时不能可靠执行“设为收藏”。

#### P6.3 反馈

- 可以短暂显示“正在确认”，但只有源端反馈后才确定最终状态。
- 3 秒无反馈进入 UNKNOWN/未确认，不无限重试。
- 模式切换、关闭“显示收藏”或失去 session 时撤销旧能力和状态。

测试：播放器支持/不支持/无反馈、后端不支持、开关关闭、模式切换、重复点击。  
完成条件：收藏支持时真实播放器状态改变；其他情况隐藏或给出准确提示，绝不伪报成功。

### P7 — F25Backend 和模式切换

目标：在标准模式稳定后增加第二通道，不破坏共享播放器/歌词/收藏逻辑。

#### P7.1 完成 F25 spike

- 按第 6.3 节逐项执行，记录到 `docs/TEST_LOG.md`。
- 已完成本地 OpenAPI、EAS main binder、service-directory 三分支和 descriptor 夹具；目标车无 USB 盒注册仍待实测。
- 只迁入验证需要的 service pool/EAS/OpenAPI/Binder 接口，不迁入 AppKey、激活或商业凭据。

#### P7.2 后端实现

- 已实现与 LegacyBackend 共享协议的 `F25Backend`。
- 将 Cplay key callback 转为统一语义事件；126=PLAY、127=PAUSE，不能都变成 TOGGLE。
- 补齐 Cplay 高层实现没填写的 album/duration/artwork/lyrics/favorite 字段；每项由能力协商控制。
- 已增加幂等 unregister/close、死亡监听和重复 init 防护。

#### P7.3 热切换事务

1. 写入目标模式，显示正在切换，暂停接收旧通道控制。
2. 注销并关闭旧后端，取消它的 callback/timer，递增 generation。
3. 新后端连接和注册。
4. 成功后发送最新快照，更新当前生效模式。
5. 30 秒快速窗口失败时恢复旧模式；失败目标只写诊断。

若 SDK 单例无法可靠释放：保存“待重启模式”，保持当前模式实际运行；应用完整重启后才尝试新模式。UI 必须同时显示目标、实际、待重启状态。

本地已覆盖单后端、旧 callback、目标失败回退和重启边界；双向切换各 10 次、调用成功与原车可见仍需实车记录。  
完成条件：同一时刻只有一个后端注册和转发；无重复控制；F25 的“调用成功”和“原车可见”均有实车记录。

### P8 — 开机、诊断、发布和回退

#### P8.1 开机与前台运行

- 仅用户启用桥接时恢复；未解锁等待。
- 选择符合实际用途的前台服务策略，Android 11 实车验证；不播放静音音频保活。
- 常驻通知显示桥接状态；通知权限和通知访问分别处理。

#### P8.2 诊断

- 有限循环日志，默认 2MB × 3。
- 记录版本、用户、模式、session、label 来源、过滤原因、连接阶段、generation、Binder descriptor、注册、最后快照和控制结果。
- 不默认记录通知正文、完整歌词、账号和长期听歌历史。
- 通过系统文件选择器导出，默认去除敏感数据。

#### P8.3 最终审计

- 包名、签名、versionCode、min/target、组件、authority、meta-data。
- 无 shared UID、息屏/AVAS 类和权限、无商业凭据、无错误硬编码跳转。
- 所有内部组件默认不导出；系统要求导出的组件有正确绑定权限。
- Windows/macOS 干净环境执行 test/lint/assemble。
- 记录 APK SHA-256、签名摘要、ROM、播放器版本和测试矩阵。

#### P8.4 发布与回退

- 先生成可回退的同签名旧版本备份及说明。
- 小范围安装前导出设置和授权状态。
- 覆盖安装，不默认卸载；安装后立刻核对通知授权组件、设置迁移和标准模式。
- 回退必须实测；Android 通常不允许直接安装较低 versionCode，必要时准备版本号更高但代码为上一稳定版的 rollback build。
- 任何需要卸载并导致数据/授权丢失的回退都必须由用户单独确认。

## 9. 测试策略

### 9.1 JVM 单元测试

- 会话过滤、评分、最近活跃稳定性和固定/忽略规则。
- TrackSnapshot 相等性、曲目 generation、位置外推。
- App label 缓存失效。
- 重试退避、状态机合法转换、重复事件合并。
- F25/Legacy callback 到统一控制语义的映射。
- 收藏四态、写能力和源端确认。
- LRC 解析、offset、±3000、暂停/拖动/切歌。
- 旧设置到新 schema 的幂等迁移。

### 9.2 Android 仪器测试

- NotificationListener 授权查询和生命周期适配层。
- PackageManager label、queries 与异常回退。
- Service、receiver 和兼容全限定名可实例化。
- Artwork provider URI 只读授权和路径安全。
- SharedPreferences 旧文件读取/迁移。
- 前台服务通知和用户停止行为。

### 9.3 Fake Binder/协议测试

- descriptor、transaction 编号、Parcel 字段顺序的 golden tests。
- bind success/failure/null/death/disconnect。
- init ready 早到/晚到/永不到。
- register null/token invalid/IPC exception。
- unregister/close 幂等。
- F25 service pool 返回不同服务版本。

### 9.4 APK 静态测试

每个候选发布 APK 自动检查：

- applicationId、versionCode、证书摘要。
- 不含 `sharedUserId`。
- 通知监听组件和 binding permission 正确。
- provider authority 和 exported/grantUriPermissions 正确。
- 不含息屏/AVAS 组件及对应权限。
- 权限清单符合最小基线。
- 不含 Cplay AppKey、VIN、激活、USB 业务字符串和无关 native 库。

### 9.5 博越 L 实车矩阵

每个测试记录：APK SHA、模式、ROM/MediaCenter/EAS 版本、播放器包和版本、结果、日志时间段。

| 类别 | 场景 |
| --- | --- |
| 安装 | 同包覆盖、设置迁移、通知授权保留、回退 |
| 输入 | 酷我、星海/星河实际包、新标准 MediaSession App、多播放器 |
| 名称 | label 成功、缓存、包不可见、包变体、语言变化 |
| 标准通道 | 元数据、封面、进度、原车来源、方控、断链 |
| F25 | 无 USB 注册、元数据、方控、歌词、收藏、断链 |
| 切换 | 热切、失败回退、待重启生效、切换中按键 |
| 歌词 | 在线/离线/无歌词、快速切歌、±3000、暂停、拖动 |
| 收藏 | 支持、不支持、状态未知、后端不支持、关闭开关 |
| 生命周期 | 页面退出、用户停止、force-stop 后手动打开、冷启动、休眠唤醒 |
| 权限 | 首次未授权、运行中撤销、恢复、授权存在但 Listener 未绑定 |
| 稳定性 | 关闭 ADB、断网、持续运行 2 小时、日志上限 |

完整验收编号和预期以 SPEC 第 14 节 T01–T24 为准，本计划不得降低它们。

## 10. 工作包依赖和建议提交顺序

```text
P0 工程/身份/协议
 ├─> P1 设置/授权/UI
 │    └─> P2 MediaSession输入
 │          └─> P3 Legacy标准闭环
 │                └─> P4 可靠恢复
 │                      ├─> P5 封面/歌词
 │                      ├─> P6 显示收藏
 │                      └─> P7 F25与切换
 └────────────────────────────> P8 发布/回退
```

建议每个可回退提交只包含一个主题：

1. `build: scaffold reproducible Android project`
2. `docs: record package signing and protocol baseline`
3. `feat: add settings migration and permission status UI`
4. `feat: discover and select media sessions`
5. `feat: add legacy ECARX backend minimal loop`
6. `feat: add bridge coordinator and recovery`
7. `feat: add artwork and lyric pipeline`
8. `feat: expose capability-aware favorite control`
9. `feat: add verified F25 backend`
10. `feat: add transactional backend switching`
11. `release: finish boot diagnostics migration and rollback`

不要把反编译 SDK 整包导入、业务功能和大规模格式化混在同一提交；每个协议提交都应附对应 fake Binder 或 golden test。

## 11. 风险闸门

| 闸门 | 必须回答的问题 | 不通过时的处理 |
| --- | --- | --- |
| G1 签名 | 是否拥有当前修改版同一签名的可用私钥/签名流程？ | 停止同包覆盖，不卸载；与用户重新选择身份路线 |
| G2 组件兼容 | 覆盖后通知授权是否指向实际存在的 Listener？ | 保留兼容类名/alias，修复后再继续 |
| G3 标准注册 | 最小权限普通 UID 能否按新工程注册标准通道？ | 对比旧修改版 Manifest/协议，定位具体差异，不批量加权 |
| G4 包可见性 | queries 是否足以取得播放器标签？ | 自用变体测试 broad visibility，再据证据决定 |
| G5 F25 无盒 | 无 USB 盒能否完成注册和方控闭环？ | 标为实验不可用，标准模式照常交付，不伪报完成 |
| G6 热切换 | 两套 SDK 是否可安全注销和重建？ | 使用明确的“待重启生效”方案 |
| G7 收藏 | 播放器和车机端是否都能可靠读写收藏？ | 隐藏入口或点击后合理提示，不影响方控 |
| G8 歌词展示 | 车机使用全文时间轴还是当前行？ | 分通道适配，避免重复 offset |
| G9 Provider | 车机能否读取受限封面 URI？ | 调整专用 provider 协议，不能开放任意文件 |
| G10 回退 | 覆盖失败或新版本异常时如何恢复且不误删数据？ | 发布前必须准备已验证的同签名 rollback build |

## 12. 完成定义

V1 只有同时满足以下条件才视为完成：

- 在 `MediaBridge-src` 可重复构建，不依赖反编译目录作为编译输入，也没有本机绝对路径。
- 可按已确认签名策略覆盖安装，设置和通知组件完成兼容迁移，有实测回退方式。
- 标准模式达到当前修改版已有的媒体信息和方控效果，并能自动恢复已知冷启动故障模型。
- F25 模式完成无 USB 盒实车闭环；若未完成，则 V1 必须明确标注该模式实验性，不能把标准模式发布等同于双模式完成。
- 第三方播放器显示应用名称；失败时显示包名和可诊断原因。
- “显示收藏”名称统一；不支持时隐藏或给出合理提示，不伪报成功。
- 歌词 -3000～+3000ms 当前歌曲即时生效，界面明确“慢点＋、快点－”，不改变真实播放进度。
- 通知访问撤销时主界面持续提示并提供恢复入口，旧会话不可继续控制。
- 模拟息屏、自动息屏和 AVAS 代码/组件/权限全部不存在于发布 APK。
- SPEC T01–T24、静态安全检查和目标车机回归均有记录；所有未通过项均明确为阻断、已知限制或后续版本任务。
