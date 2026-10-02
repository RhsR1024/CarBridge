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
