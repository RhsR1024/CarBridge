# Upstream 同步与发布台账

## 2026-10-01 — 初始化 CarBridge 工作仓库

| 字段 | 记录 |
| --- | --- |
| CarBridge 分支 | main |
| CarBridge/origin 提交 | f2d06951b4e8114dbb62f551c12a32a845a3042f |
| upstream 标签 | v0.2.8 |
| upstream/main 提交 | f2d06951b4e8114dbb62f551c12a32a845a3042f |
| 左右差异 | HEAD...upstream/main = 0 / 0 |
| 操作 | 克隆 origin，配置 upstream 并 fetch tags；无额外 merge |
| 配套 MB 分析基线 | ece9746c4e33a2134d64d10f2df8a3ca9b3bba5e |
| 协作协议 | v1 设计稿，尚未实现 |
| 本地变更 | 新增 docs 下 CarBridge 文档与参考图；尚未提交/推送 |
| 构建/车测 | 未执行；本轮仅文档范围 |
| 文档检查 | 9 份 Markdown、57 个本地链接、18 项需求覆盖、26 项用例引用及 2 张原图校验通过；原有 tracked 文件未修改 |
| 状态 | P00 完成；功能实现未开始 |

## 后续记录模板

每次追加一节，保留历史：

```text
日期 / 同步任务
同步前 CarBridge 提交：
原 upstream 基线：
目标 upstream 标签与完整提交：
集成分支与合并提交：
上游主要变化：
冲突文件、处理依据与回归：
CarBridge 专属行为保留/调整：
MediaBridge 兼容版本与协议版本：
构建环境、自动测试、车测报告：
APK 包名/版本/签名摘要/哈希（如构建发布）：
剩余问题、回退方案：
最终状态：候选 / 已合入 / 已发布 / 未通过
```

同步步骤见 [UPSTREAM_SYNC.md](UPSTREAM_SYNC.md)。不能只记录“已同步最新”，必须保留不可变提交及验证依据。

## 2026-10-02 — 双模式实现与本机交付

| 字段 | 记录 |
| --- | --- |
| 同步前基线 | f2d06951b4e8114dbb62f551c12a32a845a3042f |
| upstream/main / v0.2.8 | f2d06951b4e8114dbb62f551c12a32a845a3042f |
| 演练 | 再次 fetch upstream --tags；功能提交前 HEAD...upstream/main 左右 0/0；merge-tree --write-tree 检查通过 |
| 演练 tree | 7945164c7d94a7126583c3750d1616f1265d5a6f |
| 上游变更/冲突 | 没有新增上游提交可合并，无冲突；未宣称已测试未来冲突 |
| CarBridge 功能提交 | 128e50f；a77b9dc43bd261ac54e42634d9d24db76558574e |
| 配套 MB 提交 | a6e7801e236bcfeefe300694580696be50bd5114 |
| 配套版本 | CarBridge 0.1.0 / 100；MediaBridge 2.3.0-carbridge-26100201 / 26100201；协议 1.0 |
| 专属行为 | 新包身份、真实 Now Playing、歌曲资源、音乐仲裁、ECARX/MB 协商、车型隔离与 Geely 图标 |
| 自动化 | 535 项通过，release lint 0 错误；源码/认证构建隔离、签名和最终资产检查通过 |
| 发布状态 | 本地车测候选；源码已分主题提交，未推送；未执行实车安装或升级验收 |
| 回退 | 保留原版 DiPlay 和历史 MB 安装包/配置；优先保持应用数据，不把卸载重装视为覆盖回归 |
| 完整证据 | LOCAL_VALIDATION_2026-10-02.md、IMPLEMENTATION_STATUS.md、RELEASE_GUIDE_0.1.0.md |

上游接线冲突关注 shared 的 Controller/transport/AndroidMediaSink，以及 common 的 MediaKeys/SessionService/Activity/Persistence。先检查新协议和音乐门控接口，再保留上游新增逻辑；不整文件覆盖。下一次有真实 upstream 新提交时另开同步记录并重复相应构建与设备回归。

## 2026-10-02 — 同步 DiPlay v0.2.9，CarBridge 0.1.4

| 字段 | 记录 |
| --- | --- |
| 同步前 CarBridge | 575a9b46d7f887dc7e79faf6a6639328d565ab8c（0.1.3） |
| 原上游基线 | v0.2.8 / f2d06951b4e8114dbb62f551c12a32a845a3042f |
| upstream/main 与标签 | v0.2.9 / 18429e737228e8d75d9b6c850af89dcca2f591b6 |
| 分支 / 合并提交 | sync/diplay-0.2.9 / 61c9771dd7f7d202bfff3074013f72ac8ff8af6b；保留两个父提交 |
| 版本 | CarBridge mobile / automotive 0.1.4 / 104，包名保持 |
| 上游主要变化 | 浮动与嵌入地图、导航小组件、启动器示例、仪表歌曲、昼夜模式、摄像头窗口、0–20 音道、乌克兰语、GPS 方向修复 |
| 六处冲突 | common Manifest、HostActivity、mobile 构建、settings、CarPlayTouchMapper、CarPlayController；按行为合并 |
| 定制保留 | ECARX 方控、MB 双模式、元数据、音乐互斥、吉利导航焦点与音量目标、Geely 图标；默认旋转不重连及可选旋转重连保留 |
| 隔离／兼容 | BYD 仪表歌曲按车型开关；旧导航设置兼容；统一会话画布与触摸尺寸；新翻译保留 CarBridge 品牌 |
| MediaBridge / 协议 | 未修改外部仓库；协议 1.0；此前实车组合见 VEHICLE_VERIFIED_0.1.3.md |
| 自动化 | shared 341 + common 129 + home 4 = 474 项通过，0 失败／跳过；四项 lint 0 错误；四模块 debug 构建成功；public tree 和资产检查通过 |
| 设备范围 | 本次未安装车机或进行新一轮 iPhone／目标 ROM 验收；旧 0.1.3 车测不能代替新版回归 |
| 状态 | 本地同步验证通过并合入 main；未推送、未发布正式或独立车测包 |
| 详细报告／产物哈希 | [UPSTREAM_SYNC_0.2.9.md](UPSTREAM_SYNC_0.2.9.md) |
| 回退 | 保留同步前提交；共享主线需要撤回时对合并提交按第一父创建 revert |


## 2026-10-03 — 同步 DiPlay v0.2.10 后 main，CarBridge 0.1.5

| 字段 | 记录 |
| --- | --- |
| 同步前 CarBridge | 997162f180575ea807bd78ee4bc7c4db01802c75（0.1.4） |
| 原上游基线 | v0.2.9 / 18429e737228e8d75d9b6c850af89dcca2f591b6 |
| 目标标签 | v0.2.10 / 3e43e25c55921bdf5149f5f92851acf202ed353a |
| 目标 main | 45563135a3d05a17315a84f57434fdb5dfa43a5f；标签后增加诊断报告说明 |
| 分支 / 双亲合并提交 | sync/diplay-0.2.10 / 38bfb717063cd4a5da4f8762528df0c7857fd406 |
| 版本 | mobile / automotive 0.1.5 / 105；包名保持 |
| 主要变化 | USBMUX 与热点验证、AirPlay 端口回退、Wi-Fi Direct、通话回声处理、BYD 电池、地图镜像、视频与诊断 |
| 冲突 | 十个文件，按行为处理；原媒体会话和按曲目代次的封面处理优先保留 |
| 定制 | 吉利方控、ECARX/MB 双模式、歌曲、音乐互斥、导航音量目标、图标、旋转策略保持；137 个受保护文件及图片未变 |
| 协作 | 未改 MediaBridge；协议 1.0 |
| 自动化 | shared 421 + common 159 + home 4 = 584 项通过，无失败／错误／跳过；四项 lint 零错误；四模块 debug 构建成功 |
| 构建 | BUILD SUCCESSFUL in 8m 49s；411 个任务执行；源码 APK 无运行时认证资产 |
| 设备范围 | 未安装车机、连接 iPhone 或发布签名正式包；需要导航音量、通话及连接实车回归 |
| 状态 | 源码同步验证通过；集成主线，远端推送状态以 Git refs 为准 |
| 报告 | [UPSTREAM_SYNC_0.2.10.md](UPSTREAM_SYNC_0.2.10.md) |
| 回退 | 对上述合并提交按第一父创建 revert，保留共享历史 |


## 2026-10-04 — 同步 DiPlay v0.2.11 后 main，CarBridge 0.1.9

| 字段 | 记录 |
| --- | --- |
| 同步前 CarBridge | bd9bef0cef9cad949fa2fe24d793bf37fda73d42（0.1.8） |
| 原上游基线 | v0.2.10 / 45563135a3d05a17315a84f57434fdb5dfa43a5f |
| 目标标签 | v0.2.11 / 6014025c653c4dae88d319ce446e0bf1ddb658ea；比标签多网站与发布文档提交 |
| 目标 main | abe750f79dfb7ee4be613e36f2952a43778b3a04 |
| 分支 / 双亲合并提交 | sync/diplay-0.2.11 / ef446e305406acddae43bf0f1d9044cf710e08b7 |
| 版本 | mobile / automotive 0.1.9 / 109；包名保持 |
| 主要变化 | Wi-Fi Direct 首选信道、可移动自定义转向卡片、设置手势指数量、旧版车辆数据只读探测与并发修正、无线位置／车辆数据移至运行时 Wi-Fi 链路、可选车机热点自动开启、Android TV 遥控、元数据变更发布、Android 9 音频修正、稳定尺寸重连、本应用 VPN 范围与有界诊断 |
| 冲突 | 十五个文件，按行为处理；CarPlayMediaKeys 保持 CarBridgeMediaRuntime 委托，DiPlayActivity 采用上游滚动恢复，AndroidMediaSink／CarPlayController 保留音乐门控、导航音量目标与陈旧运行守卫 |
| 定制 | 吉利方控与语音键门控、ECARX／MB 双模式、歌曲与音乐互斥、导航音量目标、图标、旋转保留策略保持；582 个受保护文件中仅版本与同步文档为簿记性修改 |
| 上游测试适配 | 五个 BYD 测试类按 BYD 档位运行；CarPlayHostDisplaySizeTest 使用 GENERIC 档位避开厂商绑定，并适配同尺寸旋转用例与 mock 存根 |
| 协作 | 未改 MediaBridge；协议 1.0 |
| 自动化 | shared 537 + common 291 + home 4 = 832 项通过，无失败／错误／跳过；四项 lint 零错误；四模块 debug 构建成功 |
| 构建 | BUILD SUCCESSFUL in 4m 3s；411 个任务；home 以 --rerun 复跑通过；源码 APK 无运行时认证资产 |
| 设备范围 | 未安装车机、连接 iPhone 或发布签名正式包；需要方控、音量、连接与旋转实车回归 |
| 状态 | 源码同步验证通过；集成主线，远端推送状态以 Git refs 为准 |
| 报告 | [UPSTREAM_SYNC_0.2.11.md](UPSTREAM_SYNC_0.2.11.md) |
| 回退 | 对上述合并提交按第一父创建 revert，保留共享历史 |


## 2026-10-05 — 同步 DiPlay v0.2.12，CarBridge 0.1.10

| 字段 | 记录 |
| --- | --- |
| 同步前 CarBridge | e54215a1db794a9e334ab20011ba6646f9866f6c（0.1.9） |
| 原上游基线 | abe750f79dfb7ee4be613e36f2952a43778b3a04（v0.2.11 后 main） |
| 目标标签 | v0.2.12 / 22d2aacedcadc4ec1aef0d74b161b05321f708a7；不跟随标签后的 main |
| 分支 / 双亲合并 | sync/diplay-0.2.12；本记录所在 merge，父提交为上述 CarBridge 与目标标签 |
| 版本 | mobile / automotive 0.1.10 / 110；包名保持 |
| 主要变化 | 同一局域网无线、热点就绪／重试、扫描恢复、USB 辅助、分屏／分辨率、昼夜／环境光／画面调节、可选方控缩放／摇杆、BYD 仪表恢复、诊断导出 |
| 冲突与定制 | 十四个文件按行为合并；保留 ECARX 方控、MB 双模式、音乐互斥、媒体／导航音道与音量目标、图标、旋转策略；不采用上游跨曲目沿用旧封面 |
| 原有工作 | 隔离工作树验证；七个未提交文件逐字节保留并排除在同步提交外，原有 R3–R9 未跟踪 APK 不动 |
| 测试适配 | BYD 专属用例显式选车型，保留生产隔离；新增六项 CarBridge 兼容保护测试 |
| 自动化 | shared 700 + common 552 + home 4 = 1,256 项，USB 32 项，均无失败／错误／跳过；四项 lint 0 错误，四模块 debug 构建成功 |
| 构建 | 主构建 4m 6s，411 个任务，退出码 0；源码 APK 无运行时认证身份 |
| 既有检查问题 | public tree 因原有已跟踪 USB APK／公开证书 9 项失败，本轮未引入 |
| 协作与设备范围 | MediaBridge 未修改，协议 1.0；未实车测试、未推送、未发布 |
| 状态 | 本机同步验证通过；双亲合并后快进本地 main，原有工作仍未提交 |
| 完整报告与产物哈希 | [UPSTREAM_SYNC_0.2.12.md](UPSTREAM_SYNC_0.2.12.md) |
| 回退 | 保留前基线；共享后按 merge 第一父创建 revert，不重写历史 |


## 2026-10-09 — 同步 DiPlay v0.2.15，CarBridge 显示版本对齐

| 字段 | 记录 |
| --- | --- |
| 同步前 CarBridge | 133f4353ed355849265ff3b7420e0495a0216668；工作区干净 |
| 原上游基线 | v0.2.12 / 22d2aacedcadc4ec1aef0d74b161b05321f708a7 |
| 目标标签 | v0.2.15 / b940efe81ebe6fe930caac71e19d9624723e6b7f；不跟随移动 main |
| 分支 / 双亲合并 | sync/diplay-0.2.15；本记录随 merge，父提交为上述 CarBridge 与上游目标 |
| 显示版本 / 内部版本码 | mobile／automotive 0.2.15 / 111；debug 保留 -debug |
| 新维护规则 | 按用户要求 versionName 对齐已同步上游，versionCode 保持本项目递增；写入 AGENTS.md 和 UPSTREAM_SYNC.md §5 |
| 定制保护 | 吉利方控、直连／MediaBridge、音乐门控、媒体／导航音道和音量目标、自定义图标、歌曲资源和旋转策略保留；新增 BYD 通话功能继续车型隔离 |
| 核心未变更 | USB-CarPlay、ecarx、车机路由、协作协议、Now Playing、PlaybackPolicy／MusicOutputGate、音道映射与 GuidanceActivity |
| 兼容性 | ECARX SDK 要求 API 28，mobile 最低 Android 9 保持；共享层吸收旧 Android 兼容实现 |
| 自动化 | shared 1,037 + common 961 + home 4 = 2,002 项通过，无失败／错误／跳过；四项 lint 0 错误；四模块 debug 构建成功 |
| 既有检查问题 | public tree 因同步前已跟踪的 21 个 USB 分发 APK／公开证书失败，本轮没有增加 |
| 协作与设备范围 | MediaBridge 未修改，协议 1.0；未实车测试、未推送或发布 |
| 回退 | backup/pre-diplay-0.2.15-20261009；共享后按 merge 第一父创建 revert |
| 完整报告 | [UPSTREAM_SYNC_0.2.15.md](UPSTREAM_SYNC_0.2.15.md) |

## 2026-10-10 — 同步 DiPlay v0.2.16

| 字段 | 记录 |
| --- | --- |
| 同步前 CarBridge | 3a25d24773c27573c4600887cf470ff6b9e273f8；工作区干净 |
| 原上游基线 | v0.2.15 / b940efe81ebe6fe930caac71e19d9624723e6b7f |
| 目标标签 | v0.2.16 / bac419695ca26b7e67d204a279333ca52132c2cb；公开预览版，不跟随移动 main |
| 分支 / 双亲合并 | sync/diplay-0.2.16；本记录随 merge，父提交为上述 CarBridge 和上游目标 |
| 版本 | mobile／automotive 0.2.16 / 112；独立包名与 API 28 最低版本保持 |
| 定制 | 吉利方控、双模式、音乐互斥、媒体／导航音道与音量恢复、图标、封面代次、旋转策略保留 |
| 新增隔离 | BYD 氛围灯和蓝牙暂停限制车型；ambient 播放状态复用原 runtime；导航方控为默认关闭的 opt-in |
| 大字体 | 修复密度覆盖重置系统字号、窄窗口按钮裁切和搜索框过窄；原生渲染核验含 2560×1440 和阿拉伯 RTL |
| 自动化 | shared 1,249 + common 1,040 + home 4 = 2,293 项通过；四项 lint 0 错误，四模块 debug 构建成功；后续 RTL／大字体 14 项专项通过 |
| 既有问题 | public-tree 因先前已跟踪的 21 个 USB APK／公开证书失败，本轮未增加 |
| 协作与设备范围 | MediaBridge、USB-CarPlay、协作协议未变；未进行新版实车验收或正式发布 |
| 回退 | backup/pre-diplay-0.2.16-20261010；共享后按第一父 revert |
| 完整报告 | [UPSTREAM_SYNC_0.2.16.md](UPSTREAM_SYNC_0.2.16.md) |
