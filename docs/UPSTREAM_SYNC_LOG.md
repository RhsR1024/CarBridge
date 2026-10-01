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
