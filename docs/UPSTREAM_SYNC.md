# DiPlay 上游同步与 CarBridge 维护策略

建立日期：2026-10-01。当前基线和每次同步记录见 [同步台账](UPSTREAM_SYNC_LOG.md)。目标是在保留吉利双模式、音乐互斥和歌曲卡片功能的同时，持续吸收 DiPlay 的连接、协议、音频与界面修复。

## 1. 仓库关系

| remote | 地址 | 用途 |
| --- | --- | --- |
| origin | `git@github.com:RhsR1024/CarBridge.git` | 本项目 fork |
| upstream | `git@github.com:shihabal3amri/DiPlay.git` | 原项目代码与标签 |

保留原始提交历史，不把 fork 变成源码压缩包导入的新仓库。默认 `main` 为 CarBridge 可集成主线，特性用短期分支，upstream 同步使用 `sync/diplay-<已验证标签或短提交>` 分支。远端配置是本地 Git 配置，不会随普通代码提交传播；新克隆按上表设置。

当前已完成 fetch，`v0.2.8`、origin/main、upstream/main 与本地 HEAD 均为 `f2d06951b4e8114dbb62f551c12a32a845a3042f`。未来以实际获取的 ref 为准，不能把此记录当成永久最新状态。

## 2. 降低冲突的结构规则

1. 保留现有 `shared/common/mobile/automotive` 层次及内部 namespace；外部 applicationId、显示名、签名和分发版本独立。
2. 新增的 Now Playing、播放仲裁、ECARX、车型配置与跨应用协作优先放在独立包/模块；原有大型 Activity/Controller 只加入清晰的接口调用。
3. 保留 BYD 实现和测试，通过 VehicleProfile 控制；不对整个仓库批量删 BYD、替换字符串或重新排版。
4. 新增 Geely 图标，使用默认资源选择器，不直接破坏 BYD 可选资源；OEM 显示名称和认证身份分开。
5. 把协议解析、状态逻辑与 Android/ECARX I/O 分开，使上游修复可以合并而不重写车辆适配。
6. 每次提交只处理一个主题；品牌/包名、元数据、焦点、ECARX、协作协议和图片分开提交。通用修复尽量与 CarBridge 专属部分分离。
7. 记录第三方 SDK/解析库来源和许可证，保留 DiPlay、DiAuto 以及既有第三方 notices；重命名项目不改变原代码许可。

## 3. 每次同步流程

### 3.1 准备

- 工作区必须干净，先提交/保存本项目工作；不自动 stash 用户改动或 reset 清理。
- 获取 upstream 和标签，核对版本说明、目标提交及真实差异；优先稳定标签，不盲跟一个移动中的 main。
- 记录当前 CarBridge 提交、已集成上游基线、目标上游提交、配套 MediaBridge 版本。目标 ref 明确后才创建集成分支。
- 若以后存在 `.codegraph/`，定位理解代码先用 CodeGraph；没有则不自行索引。

下面为维护流程示例，不是本轮执行记录；`$syncTarget` 必须填入实际审阅过的标签或完整提交，`$syncBranch` 使用本次确定的分支名：

```powershell
git status --short
git fetch upstream --tags
git log --oneline --decorate main..upstream/main
git rev-parse $syncTarget
git switch -c $syncBranch main
git merge --no-ff --no-commit $syncTarget
```

不得把未设置的变量直接复制运行。上游如强制移动标签，记录异常并核实来源，不自动以 force 覆盖本地可信标签。

### 3.2 合并与冲突处理

- 保留 merge 提交记录，逐块比较上游意图与本项目需求；不对大文件整体选择 ours/theirs。
- 若上游已实现等效通用能力，优先使用其实现并保留 CarBridge 扩展接口，删除确实重复的适配时同时回归。
- 同步元数据/媒体控制时检查明示 PLAY/PAUSE、先后播放和单一路由要求；同步音频时特别检查绕过门控的新播放路径。
- 清点新增设置默认值、权限、后台服务、SDK/targetSdk/NDK 变化，不只解决文本冲突。
- 更新文档中的现状/源码位置与基线，不能保留旧版缺口描述却已合并新实现。

### 3.3 验证与完成

运行上游要求的单元、lint 和目标构建，以及本项目受影响的状态机/合同测试；再按下表做车辆回归。将结果写入台账和测试报告，完成后形成集成提交，再按届时工作授权合入主线或创建 PR。推送/发布不属于自动 fetch 的附带动作。

不通过时保留明确失败记录，在集成分支修复或放弃本轮候选，不把失败合并推到稳定主线。已经共享的错误合并通过正常 revert/修复提交处理，不重写共享 main 历史。

## 4. 冲突热点与回归清单

| 上游变更区域 | 必须复核的 CarBridge 行为 | 验收 |
| --- | --- | --- |
| Iap2Messages、LinkEngine、wired/wireless clients | 订阅、增量字段、文件传输、曲目/连接代次和内存上限 | V02/V03/V18/V20 |
| AndroidMediaSink、缓冲逻辑 | 所有 play/restart 路径受门控，拒绝焦点不漏音，通话与导航独立 | V12–V15 |
| CarPlayMediaKeys、MediaButton、Controller | 明确 PLAY/PAUSE、BYD toggle 隔离、命令去重和视频优先级 | V06/V11/V17/V24/V25 |
| HostActivity、生命周期/通知 | 后台连接、设置生效、重连不抢播、OEM 预览一致 | V05/V14/V18/V26 |
| AirPlayInfoPlist/Config、requestUI | Geely 资源与标签，HOME 和视频请求分流，协议身份不被品牌覆盖 | V26/V25 |
| HUD/BYD/车辆状态 | Profile 隔离，Geely 不启动 BYD 服务，驻车安全限制未失效 | V25 |
| manifests/Gradle/包名/targetSdk | 独立身份、Provider 读取、监听/后台权限、签名与认证输入 | V01/V20/V22/V23 |
| MediaBridge 同步或协议变更 | 两端 major 能力匹配、忽略策略、让位、旧版回退 | V07–V09/V16/V19/V23 |

重大音频/协议改动执行完整车机矩阵；纯文档/图标变更运行相应的有限检查，不无理由重复全部测试。

## 5. 构建、认证、签名与版本

构建基础遵循 [BUILD.md](BUILD.md)：JDK 25、Android SDK 37、NDK 28.2.13676358。Java source/target 11 不代表 Gradle 应使用 JDK 11。`shared` 使用 `shared/build.gradle`，不是 `.kts`。

源码/CI 构建可不带认证身份。车测需要 `DIPLAY_AUTH_ASSETS_DIR` 中规定的 `offline-mfi/identity.pk8` 和 `offline-mfi/certificate.p7b`；缺失即保留失败条件，不能从无关安装或用户隐私目录搜索复制凭证。认证输入不进入 Git，Android 发布 keystore 同样外置。

CarBridge 使用自己的稳定发布签名，不能期待其 APK 覆盖更新 DiPlay 的签名/包名。首次发布固定包名与签名后不随 upstream 版本更改。调试和正式包使用独立身份时，忽略名单、IPC 配对和授权分别设置。

建议版本形式 `0.1.0` 起步并单独记录 `upstreamBase=v0.2.8`；versionCode 在本项目内严格递增，不直接套用上游值。本轮固定 CarBridge 0.1.0 / versionCode 100，配套 MediaBridge 2.3.0-carbridge-26100201。不能同时沿用“DiPlay 0.2.8 已发布”与“CarBridge 所有功能已完成”的描述。

每次发布记录：CarBridge 提交/版本/包名/APK 哈希、上游 tag+提交、协议 major/minor、兼容 MediaBridge 版本、构建依赖、目标车机报告、已知资源/语音限制。SDK/协议 source type 等兼容参数有变化也要记录。

## 6. 跨仓库版本管理

协作契约不从对端 main 动态加载。若采用共享源码或发布库，固定版本/提交并有合同样例哈希；若两端复制小型契约，CI 校验语义和 fixture 一致性，避免两边各自修改。

MediaBridge 可继续服务其他应用，不为支持 CarBridge 改成专用附属应用。重大协议改动升级 major 并保留明确旧版流程；未发布的配套改动不得在 CarBridge 发布说明中写成默认已可用。

同步工作按任务手动执行；本文件没有设置自动拉取、定时构建或自动发布。
