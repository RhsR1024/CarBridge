# 验证记录

最后更新：2026-09-28（Asia/Shanghai）

## 当前版本

- 当前实车确认正常的功能基线包：`D:\CarSoft\MediaBridgeApp\MediaBridge-2.0.2-v18-cover-26092802.apk`，versionName `2.0.2-v18-cover-26092802`，versionCode `26092802`；从 `26092718` 单独派生，仅调整酷狗在线完整封面输出。Git 固定标签为 `v26092802-verified-four-player-baseline`。`26092718` 原包及其哈希也继续保留。
- package `com.netease.cloudmusic.iot`
- min/target SDK `28/34`
- 测试套件 9 个，共 94 项，失败 0，错误 0，跳过 0
- `testDebugUnitTest`、`lintDebug`、`assembleRelease`：均为 `BUILD SUCCESSFUL`
- 用户实车确认当前版四播放器现有桥接功能正常，包括之前失效的方控与小窗按钮；酷我、汽水、LX 保持原图策略，酷狗在线找到完整图后不再缩成内接小方块。50dp 留白和字体调整继续保留。播放列表与推荐歌单尚未实现，不属于本版已验证能力。
- 签名使用本机 `C:\Users\public.DESKTOP-IQJOR7V\.android\debug.keystore`，证书 SHA-256 为 `A539C794675FC8AD7FAC6E78604C066E15C98C3C65F62CC34895FA3FC65A23C7`。

测试套件明细：AndroidRegression 44、Backoff 1、BridgeCoordinator 15、FavoriteActionPolicy 4、LrcParser 6、LyricsHttp 3、LyricsSourceFixture 6、ProtocolRegression 9、ReviewRegression 6。

## 自动化覆盖

- 标准 MediaCenter 的 golden parcel、11 个 transaction、EAS service-directory 和 OpenAPI/service-pool 发现。
- VR 通道声明、音乐意图观察器、`QMusicResult`、多媒体列表 Parcelable 线序，以及可选探针失败不影响主桥接状态。
- F25 三条本地发现分支、共享 client 注册和失败分类。
- 连接/初始化/注册分阶段时限、2/5/10/20/30/60 秒退避、generation gate、旧回调丢弃和停止后不自动重启。
- 收藏方向、未知状态、重复点击、旧曲目请求、输入版本确认和 3 秒超时。
- LRC 多时间戳、offset、BOM/CRLF、边界大小；五个歌词来源固定响应、Zvuk 令牌刷新和 Cookie 保留。
- 封面缩放像素采样哈希、URI/provider 路径拒绝、缓存清空失效和进程重启后的持久日志/缓存边界。
- 开机恢复、通知授权三态、播放器名称版本缓存、网络默认关闭和设置迁移。

## APK 审计

构建产物：本机 Debug 证书签名的 `MediaBridge-2.0.2-Probe-26092711-VR专用协议探针.apk`，SHA-256 为 `5B94BB5A01D5864387DFCFFFFF14A11D51C40ED2C42084A00F60FF6AFDB6E54A`。使用 `scripts/audit-apk.ps1` 检查 package、versionCode、SDK、七项权限 allowlist（存储写入权限仅限 Android 9 及以下）、禁止权限/能力、通知监听兼容类、封面 authority 和签名证书，结果为 `PASS`。

2026-09-27 14:00：V3 artwork alignment candidate `MediaBridge-2.0.2-Probe-26092712-V3-artwork-aligned.apk`，versionCode `26092712`，SHA-256 `F132EA386E288AA027CFF692B79BCD34628020FAB65C7A10EA76A383C1389CE0`。变更为 `ART_URI/ART` 优先并补充 `metadata_candidates` 诊断事件。`testDebugUnitTest`、`lintDebug`、`assembleRelease` 通过；APK v2 签名通过，证书 SHA-256 为 `A539C794675FC8AD7FAC6E78604C066E15C98C3C65F62CC34895FA3FC65A23C7`。该包尚未完成酷狗概念版和汽水音乐的实车复测。

当前测试证书摘要为本机 Android Debug 证书 `A539C794675FC8AD7FAC6E78604C066E15C98C3C65F62CC34895FA3FC65A23C7`。它不等于原版 APK 证书，不能直接覆盖原版安装。

### 2026-09-27 17:26：V3 Bitmap/Provider 对齐包

- 依据附件原版 V3 APK 与 `mediabridge-probe-20260927-164806-428.log.txt`，恢复 `ArtworkContentProvider exported=true`、小图不放大、内嵌 Bitmap 优先于远程 URI，以及后到 Bitmap 可替换未知尺寸透传 URI。
- 播放器映射：`cn.toside.music.mobile` 为 LX Music，`com.luna.music.car` 为汽水音乐；当前待修复对象是汽水音乐与酷狗概念版的 Bitmap/provider 路径。
- `AndroidRegressionTest`：35 项，失败 0。
- 完整 `testDebugUnitTest`：9 个套件、85 项，失败 0、错误 0、跳过 0。
- `lintDebug`、`assembleDebug`、使用指定 Debug 证书的 `assembleRelease`：均为 `BUILD SUCCESSFUL`。
- `scripts/audit-apk.ps1`：`PASS`；合并清单确认 provider `exported=true`、authority 正确、允许 URI grant。
- 测试包：`app/build/outputs/apk/release/app-release.apk`，package `com.netease.cloudmusic.iot`，versionName/versionCode `2.0.2-V3-baseline-26092714` / `26092714`。
- APK SHA-256：`147514874E495648B345989693CF704C2EF35A93AA4A0B90966FFC99305952B2`。
- 签名证书：`C:\Users\public.DESKTOP-IQJOR7V\.android\debug.keystore`，证书 SHA-256 `A539C794675FC8AD7FAC6E78604C066E15C98C3C65F62CC34895FA3FC65A23C7`，v2 签名验证通过。
- 仍需实车复测四个播放器；本条只声明本机代码、构建、清单、签名和 APK 审计通过，不提前声明汽水/酷狗封面已修复。

### 2026-09-27：酷我封面清晰度修复包

- 实车复测确认四个播放器均可桥接并显示封面；新增问题为酷我小窗封面模糊，以及酷狗圆形小窗裁切较多。
- 日志显示酷我同时提供 HTTP(S) 原图 URI 和 `70x70` / `129x129` 内嵌缩略图。此前的无条件 Bitmap 优先会把缩略图缓存后交给车机放大；现在仅当竞争 Bitmap 的短边小于 `256` 像素时优先保留 HTTP(S) 原图 URI，`256` 像素及以上 Bitmap 仍沿用本地 provider 路径。
- 增加两项回归：`129x129 + HTTPS` 必须保留远程原图；`70x70 + 无远程 URI` 仍必须使用内嵌 Bitmap，避免破坏只有小图的播放器。
- 酷狗截图中，同一封面在播放器主界面较完整、车机圆形小窗被中心裁切，且原版 V3 表现一致。桥接层不知道目标控件形状；强制缩图加边会影响主界面、仪表/HUD 等所有消费者，因此本版不修改酷狗原图。
- `AndroidRegressionTest`：37 项，失败 0；完整 `testDebugUnitTest`：9 个套件、87 项，失败 0、错误 0、跳过 0。
- `lintDebug`、使用指定 Debug 证书的 `assembleRelease`：均为 `BUILD SUCCESSFUL`。
- `scripts/audit-apk.ps1`：package、versionCode、SDK、权限 allowlist、禁止能力、listener、artwork authority 和签名全部 `PASS`。
- APK：`app/build/outputs/apk/release/app-release.apk`，versionName/versionCode `2.0.2-V3-artwork-quality-26092715` / `26092715`。
- APK SHA-256：`AF3776ADD52F042B8C2E540216E1E43905E24EC7E0D3E3D1BC79D0FC88361D79`。
- 签名证书：`C:\Users\public.DESKTOP-IQJOR7V\.android\debug.keystore`，证书 SHA-256 `A539C794675FC8AD7FAC6E78604C066E15C98C3C65F62CC34895FA3FC65A23C7`，APK Signature Scheme v2 验证通过。
- 仍需在目标车复测酷我的清晰度；本机自动化只能确认候选选择和产物身份，不能替代屏幕观感验收。

### 2026-09-27：圆形封面适配与 UI 调整包

- 新增通用“圆形封面适配”开关，默认开启，不按播放器包名区分；关闭后严格沿用播放器原图和现有车机裁切效果。
- 开启时将完整封面等比例缩入圆形内接正方形，边长为画布的 `1/√2`（约 `70.7%`，每侧安全边距约 `14.6%`），外围使用暗化柔化的原图延展背景，不使用白色或透明边。
- 小图仍不放大：酷狗 `240x240` 输入输出仍为 `240x240`，只是主体缩入安全区。远程 HTTP(S) 原图会先照常透传，后台成功取得原图后再替换为适配后的 provider URI；下载失败则保留原始 URI，不回退为低清缩略图。
- 开关切换会更新持久设置、使 artwork repository 缓存代际失效，并主动刷新当前媒体会话；缓存键包含开关状态，避免开关前后的图片互相复用。
- UI：页面外层左右留白设为 `50dp`；字体基准统一增加 `2sp`，因此 “MediaBridge” 标题相对旧版增加 `2sp`；通知未授权弹窗的标题、正文和两个按钮相对系统默认字号各增加 `5sp`。
- 新增回归覆盖默认开启/手动关闭，以及安全区输出尺寸、`1/√2` 几何边界和不透明背景。`AndroidRegressionTest` 39 项；完整 9 个套件共 89 项，失败 0、错误 0、跳过 0。
- `testDebugUnitTest`、`lintDebug`、使用指定 Debug 证书的 `assembleRelease` 均为 `BUILD SUCCESSFUL`；`scripts/audit-apk.ps1` 全部 `PASS`。
- APK：`app/build/outputs/apk/release/app-release.apk`，versionName/versionCode `2.0.2-round-artwork-ui-26092716` / `26092716`。
- APK SHA-256：`2F036F8A09BC675F5D2F9ACED5A2857E8153662FF10E69D69F6F68413AC270A7`。
- 签名证书：`C:\Users\public.DESKTOP-IQJOR7V\.android\debug.keystore`，证书 SHA-256 `A539C794675FC8AD7FAC6E78604C066E15C98C3C65F62CC34895FA3FC65A23C7`，APK Signature Scheme v2 验证通过。
- 实车需对比开关开启/关闭时四个播放器的小窗与大卡片，尤其确认酷我清晰度、酷狗主体完整度以及 50dp 留白在目标分辨率下是否合适。

### 2026-09-27：稳定封面来源与完整缩放包

- 实车截图确认 26092716 有两个质量问题：酷我同一首歌会在一两秒内切换封面；酷狗部分歌曲把已经局部化的候选缩成方块，未恢复完整原图。
- 根因一：`ART_URI` 与 `ALBUM_ART_URI`、`ART` 与 `ALBUM_ART` 过去分别选择，可能把不同元数据家族的 URI/Bitmap 混合。现在候选按家族成对选择，并在诊断日志增加 `selectedSource=art|album`。
- 酷狗概念版 `com.kugou.android.lite` 与酷我 `cn.kuwo.kwmusiccar` 优先完整 `ALBUM_ART_URI/ALBUM_ART` 家族；其他播放器继续保持 V3 的 `ART_URI/ART` 优先。
- 酷狗开启圆形适配时，即便完整专辑候选同时带远程 URI，也优先处理配对 Bitmap：完整原图只做等比例 `fitCenter` 缩小到安全区，前景路径没有 center-crop；外围中心裁剪仅用于生成柔化背景。
- 酷我若只先给出 `70x70` / `129x129` 缩略图且尚无 URI，会等待最多 `1600ms` 让高清 URI 到达；高清远程 URI 一旦发布就保持透传，不再后台下载后用另一个 provider URI 二次替换。若超时仍无 URI，才使用缩略图，避免永久无封面。
- 新增三项回归：酷狗/酷我始终保持 album URI 与 album Bitmap 配对；酷我低清图等待高清 URI；酷狗圆形适配在远程 URI 旁仍使用完整 album Bitmap。
- `AndroidRegressionTest` 42 项；完整 9 个套件共 92 项，失败 0、错误 0、跳过 0。
- `testDebugUnitTest`、`lintDebug`、使用指定 Debug 证书的 `assembleRelease` 均为 `BUILD SUCCESSFUL`；`scripts/audit-apk.ps1` 全部 `PASS`。
- APK：`app/build/outputs/apk/release/app-release.apk`，versionName/versionCode `2.0.2-stable-cover-fit-26092717` / `26092717`。
- APK SHA-256：`5B1452AE8F09EC336600CB45E7E11E9820FE38B01E19725AEACEE0D8D5EC3262`。
- 签名证书：`C:\Users\public.DESKTOP-IQJOR7V\.android\debug.keystore`，证书 SHA-256 `A539C794675FC8AD7FAC6E78604C066E15C98C3C65F62CC34895FA3FC65A23C7`，APK Signature Scheme v2 验证通过。
- 仍需实车用截图中的酷狗歌曲复测完整度，并观察酷我切歌后两秒内是否保持同一张封面。

### 2026-09-27 19:56：按来源智能适配包

- 复核 `mediabridge-probe-20260927-193929-458.log.txt`：酷狗概念版始终只提供 `ALBUM_ART=240x240`、无 artwork URI；适配开启和关闭时输入与输出尺寸都保持 `240x240`。因此“图片像素过大”不是根因，不能用宽高阈值判断是否需要缩图。汽水音乐提供正常的 `426x426`，此前方形套圆形是通用安全区算法误处理造成的。
- 原“圆形封面适配”改名为“智能封面适配”，设置键保持不变以兼容已有安装。开启后目前只处理已确认的酷狗异常来源；汽水、酷我、LX Music 以及其他未命中策略的播放器继续原图透传。
- 酷狗存在 HTTP(S) 完整封面 URI 时直接使用完整 URI，不再让 `240x240` 内嵌图压过它。只有“酷狗 + 无 URI + 有内嵌 Bitmap + 在线检索开启”时，才先按标题/歌手查完整封面；查询成功后把完整图等比缩入圆形安全区，失败才使用酷狗内嵌图。等待期间不先发布局部图，避免同曲先局部、后完整的二次跳图。
- 新增策略回归，确认智能适配只命中酷狗的内嵌-only 场景；新增汽水回归，确认即使设置开启，`426x426` 原图的角像素也不被背景合成改写；更新酷狗远程 URI 回归，确认完整 URI 优先。
- 针对性 `AndroidRegressionTest` 44 项；完整 `testDebugUnitTest` 为 9 个套件、94 项，失败 0、错误 0、跳过 0。
- `testDebugUnitTest`、`lintDebug`、指定 Debug 证书 `assembleRelease`：全部 `BUILD SUCCESSFUL`。
- `scripts/audit-apk.ps1`：package、versionCode、SDK、权限 allowlist、禁止能力、listener、artwork authority 和签名全部 `PASS`。
- APK：`app/build/outputs/apk/release/app-release.apk`，versionName/versionCode `2.0.2-smart-cover-26092718` / `26092718`，大小 `422264` 字节。
- APK SHA-256：`F2F605C9FD2F84A93E8E4DD24F3ACFE9D1DE7329DB08798D14959AD12212FDA9`。
- 测试证书：`C:\Users\public.DESKTOP-IQJOR7V\.android\debug.keystore`，证书 SHA-256 `A539C794675FC8AD7FAC6E78604C066E15C98C3C65F62CC34895FA3FC65A23C7`，APK Signature Scheme v2 验证通过。
- 仍需实车确认酷狗在线补全在车机网络环境可用；若补全失败，日志会出现 `kugou_complete_cover_lookup state=fallback_embedded`，此时无法从播放器已裁掉的像素中恢复完整画面。

## 实车验收

### 2026-09-28：酷狗收藏反馈误报修复（待实车复测）

- 依据 `C:\Users\public.DESKTOP-IQJOR7V\Downloads\mediabridge-probe-20260928-171455-093.log.txt`：酷狗在 17:01:25.378 收到 `rating-attempt desired=true`；17:01:26.597 封面链路报告 `sameTrack=false`，紧接着桥接提示“歌曲已切换”。同类情况在 17:00:32、17:00:55 也出现；用户确认实际收藏成功且画面未换歌。日志没有记录逐字段元数据，故只能确定严格曲目键发生变化，不能断言变化的是媒体 ID、专辑还是时长。
- 收藏等待期间的换歌判断现在先核对包名/会话，再核对可见的曲名与歌手；仅严格曲目键变化、可见曲名与歌手未变时继续等待播放器反馈，不把封面缓存用的严格键变化当作必然换歌。两者缺失时仍退回严格键，真正切歌或切换播放器仍中止旧请求。
- 对已发出的收藏操作，超时或切歌只提示“收藏结果无法确认”，不再暗示操作失败；只有操作无法发送或抛异常时才提示失败。酷狗是否回传了收藏状态、是否最终显示确认，仍需实车复测。
- 主线合并提交 `e7348a8` 之后单独修改；版本 `26092810`。`testDebugUnitTest` 9 套件 101 项、失败/错误均 0；`lintDebug` 与测试证书签名的 `assembleRelease` 成功；APK 审计通过。APK SHA-256 `4D01866A79A996458F7D9F0F03C0672C52CC2B3FD47D737B495D7F102EE7E567`。
- 构建工具：`D:\CarSoft\MediaBridgeApp\tooling`；证书：`C:\Users\public.DESKTOP-IQJOR7V\.android\debug.keystore`。上述为本机验证，未代替目标车复测。

### 2026-09-28：收藏忽略名单合入 main（待实车）

- 从 `probe/iou-native-source-260928` 合入收藏忽略名单等试验改动，保留 main 已有的蓝色启动图标、封面辅助逻辑和资源；合并冲突仅为版本号，合并版设为 `26092809`。
- `testDebugUnitTest`、`lintDebug`、签名 `assembleRelease` 均成功，APK 审计通过；合并 APK SHA-256 `DC9ADF72388D7276CBCBD21DA7AE71B241CE4633695287B59C69AAA251505DAB`。
- 构建工具：`D:\CarSoft\MediaBridgeApp\tooling`；测试证书：`C:\Users\public.DESKTOP-IQJOR7V\.android\debug.keystore`。本机验证不等于实车验证。

### 2026-09-28：收藏忽略名单与通用红心试验版（待实车）

- 基于改包网易云收藏成功、偶发 3 秒未确认的实车日志，改为默认显示所有桥接播放器红心，新增独立“收藏忽略名单”（默认空，勾选后仅隐藏该应用红心）；等待确认或收藏状态未知不再撤销入口。点击后仍需播放器回传，失败或未确认会提示，不能将点击等同于收藏成功。
- 设置页移除了“探测改包网易云”的临时按钮，通用诊断日志导出保留。具体行为、风险与车上逐项验证见 [收藏忽略名单试验](收藏忽略名单试验-2026-09-28.md)。
- `testDebugUnitTest`：9 套件、99 项，失败/错误 0；`lintDebug`、测试证书签名的 `assembleRelease`、APK 审计均通过。versionCode `26092808`，APK SHA-256 `765C7C15FD28BCC5BC957B769419A0419BA665335F47ED36AD875FAC7F81C475`；本节不是实车通过记录。

### 2026-09-28：改包网易云收藏红心试验版（待实车）

- 用户已确认 MT 管理器改包重签的官网下载网易云 `.iou` 可桥接方控，但小窗无红心；旧诊断日志记录该会话 `PlaybackState.actions=0`，故无法由现有安全能力判断直接放行收藏。
- `26092807` 仅对 `.iou` 在已知收藏状态时试开红心，点击后可在无声明动作时试发 `setRating()`，仍等待播放器回传，不伪造成功；增加评分、会话自定义动作、通知操作和桥接判断的诊断日志。详细操作与限制见 [改包网易云收藏红心试验](改包网易云收藏红心试验-2026-09-28.md)。
- `testDebugUnitTest`（9 套件、96 项）、`lintDebug`、测试证书签名的 `assembleRelease` 成功；APK 审计通过。APK SHA-256 `123889D45C59E42A544C0807431107D98FBF66DE3726913F0C86FE61D39BAFDA`。尚无车上收藏实测结果。

### 2026-09-28：官网下载网易云车机版独立包名试验（待实车）

- 原版官网下载 APK 与 MediaBridge 同为 `com.netease.cloudmusic.iot`，签名不同，不能并存。仅把官网下载版清单的精确包名改为 `.iov` 并用测试证书 v1/v2/v3 重签；其余非清单/签名 ZIP 项一致。配套 MediaBridge `26092806` 探针同时检查 `.iou`/`.iov`，不改变现有控制与封面路径。
- 静态检查确认官网下载版声明 `MediaBrowserService`，但实车 MediaSession、封面、歌词、控制及是否能登录尚无证据。两个 APK 哈希、安装顺序、排查步骤与限制见 [官网网易云车机版改包桥接试验](官网网易云车机版改包桥接试验-2026-09-28.md)。
- MediaBridge 本机验证：9 套件、96 项测试通过，`lintDebug`、签名构建和 APK 审计通过。当前电脑无已连接车机，本节不作桥接成功结论。

### 2026-09-28：改包网易云只读探针分支

- 从 `v26092802-verified-four-player-baseline` 单独创建 `probe/iou-native-source-260928`；不把未验证的 `.iou` 适配改入四播放器基线。探测范围、车机操作步骤与判读限制见 [改包网易云探针](改包网易云探针-2026-09-28.md)。
- `testDebugUnitTest`：9 套件、96 项，失败/错误 0；`lintDebug` 和测试证书签名的 `assembleRelease` 成功。APK 审计通过，SHA-256 `6F8A97B8B89A52D693EC1C02BCCE0A3873569032838B437231E7D72D667CBC16`，versionCode `26092805`。
- 尚无车机运行日志；本条仅为本机构建验证，不声称 `.iou` 已桥接或四播放器已在该探针包上实车复测。

### 2026-09-28：以实测正常的 26092718 重建封面修正版

- 唯一基线为提交 `0b1be44` / versionCode `26092718` / APK SHA-256 `F2F605C9FD2F84A93E8E4DD24F3ACFE9D1DE7329DB08798D14959AD12212FDA9`。用户确认这一版四播放器的桥接、方控、小窗按钮、封面与歌词正常；后续版本不作为本次基线。
- 只改酷狗**在线检索成功**后的完整封面输出：保留检索到的整幅方图，不再额外合成“内接小方块 + 模糊圆形背景”；车机小窗仍会施加自己的圆形遮罩。检索失败或只有酷狗 `240x240` 内嵌图时仍沿用 26092718 的回退路径，无法从已裁掉的内嵌图恢复像素。酷我、汽水、LX 封面策略不变。
- 未移植 26092801 的合成播放列表、浏览器/队列探针或任何播放控制改动。26092801 日志在 `updateMusicPlaybackState` 报 `queueId == null`，之后 `RETRYABLE_FAILURE active=none`；这与用户所见方控及小窗播放/暂停/切歌/收藏大多无效相符。异常根因与车机内部队列关联的判断仍需在车上证实，本版先恢复已验收控制基线。
- `testDebugUnitTest`：9 套件、94 项，失败/错误/跳过均为 0；`lintDebug`、指定 Debug 证书 `assembleRelease` 均成功；`scripts/audit-apk.ps1` 对包名、版本、SDK、权限、provider、v2 签名与证书检查均为 `PASS`。
- versionName/versionCode：`2.0.2-v18-cover-26092802` / `26092802`；APK SHA-256：`CCF453067E659AE224A7BC3131B399E0724E6A78FAEB01EC0D731ABA5E613D9E`；大小 `422344` 字节。证书 `C:\Users\public.DESKTOP-IQJOR7V\.android\debug.keystore`，证书 SHA-256 `A539C794675FC8AD7FAC6E78604C066E15C98C3C65F62CC34895FA3FC65A23C7`。构建工具位于 `D:\CarSoft\MediaBridgeApp\tooling`。
- 用户随后反馈本 APK 实车验证“正常了”，确认这次恢复可用；它是截至目前现有功能验证最完整的一版。此反馈未附新的逐项测试日志，因此保留上述自动化验证与用户实测的区别。后续实验失败时应以固定标签 `v26092802-verified-four-player-baseline` 和本节 APK SHA-256 为代码、安装包双重回退依据，不要以 26092801 列表探针版回退。部分在线搜索封面仍可能与酷狗播放器内置封面不同。
- 播放列表与推荐结论、旧探针日志证据见 [播放列表探针分析](播放列表探针分析-2026-09-28.md)。本版不宣称列表功能已实现。

2026-09-27，用户在目标车、ECARX MediaCenter 2.5.0、酷我音乐车机版 7.6.6.21 环境确认 26092709 的标准桥接、方向盘媒体控制、封面和歌词均正常。封面故障的日志证据、失败尝试和最终决策见 [封面故障调试记录](封面故障调试记录-2026-09-27.md)。

## 仍需外部验收

### 2026-09-30：独立包名、来源目录与原生优先（待实车）

- 分支 `codex/native-priority-independent-package`，版本 `2.2.0-native-priority-26093001` / `26093001`，正式安装包名 `com.mediabridge.app`。实现和安装边界见 [ADR-0002](decisions/ADR-0002-independent-source-priority.md)。
- 页面“应用忽略名单”决定播放器让位；“收藏忽略名单”独立。加入来源目录合并登记、车机焦点让位、旧输入失效、第三方新播放恢复和让位期间控制阻断。此次按钮调查不改动原有按钮能力声明，见 [小窗按钮上报对比](小窗按钮上报对比-2026-09-30.md)。
- `testDebugUnitTest`：13 套件、126 项，失败/错误/跳过均 0。包含新增目录合并和来源优先测试；覆盖忽略应用缓冲、多原生会话、监听重建、旧播放不抢回、第三方新播放恢复与控制阻断。
- `lintDebug`、`assembleRelease` 成功；Lint 为 0 errors/fatals、12 warnings。`git diff --check` 通过。未因文档补充重复构建。
- APK：`D:/CarSoft/MediaBridgeApp/artifacts/native-priority-26093001/MediaBridge-2.2.0-native-priority-26093001.apk`，468610 字节；SHA-256 `DA2D17C68ED7EF5DD58CC905537B6D461D939386B4E06A1EB42A468F8CFC8310`。
- release APK 使用本机 Android Debug 测试证书签名，证书 SHA-256 `A539C794675FC8AD7FAC6E78604C066E15C98C3C65F62CC34895FA3FC65A23C7`。APK v3 签名、对齐及身份/权限审计通过；minSdk 28，targetSdk 34，非 debuggable。
- 工具来自 `D:/CarSoft/MediaBridgeApp/tooling`，离线构建；APK 同目录保存 `build.log`、`test-results.json`、`apk-audit.txt`。
- 以上均为本机验证。尚未安装到目标车，目录显示、独立包按钮布局、原生/第三方交替、车机方控和官方网易云共存仍需实车确认。旧包设置和通知访问不会自动迁移。

上述结果只覆盖当前目标车的标准 MediaCenter/酷我组合。其他播放器收藏反馈、仪表/HUD 差异、无 USB 盒 F25 闭环、冷启动/休眠、长时断网及其他车型仍需分别验证。当前环境没有干净 macOS 主机，因此 T23 的 macOS 构建未宣称通过；真实在线服务也不以固定夹具测试替代可用性保证。
