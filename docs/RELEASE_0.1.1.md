# CarBridge 0.1.1 手机反馈修复

日期：2026-10-02。CarBridge 功能代码提交 `2a41059`，配套构建脚本提交 `24a5013`，版本 0.1.1 / 101；使用原测试签名，可按系统正常更新流程覆盖 CarBridge 0.1.0。配套 MediaBridge 升级为 **2.3.1-carbridge-26100202 / 26100202**，功能提交 `5dc7eb3`，正式与手机 Debug 来自同一份 main 源码；协议仍为 1.0。

## 用户五项反馈

| 反馈 | 处理与范围 |
| --- | --- |
| 热点提示出现空引号 | 名称为空时使用“车机热点未开启”；连接确认框同步修正，非空名称仍显示 |
| 默认 60 帧和高效视频 | 未保存设置时默认 60 fps、HEVC 开启；已保存的 30 fps/H.264 保留，升级后可手动调整 |
| 不需要旧版手动直连 | 删除 UI、偏好读取和兼容绕过；已发现的 MediaBridge 不支持协作时提示同时更新 |
| 手机 CarPlay 无声 | 修复缺少 `.dev` 包可见性声明；已配对路线就绪后补发当前封面只读授权；保留无车机服务时的播放门控 |
| 转屏就重连 | 默认固定横屏，可选固定竖屏/自动；本地尺寸变化保留 CarPlay 已协商画布和连接，以等比缩放和对应触摸坐标适配 |

自动旋转时，当前连接保持最初的画面比例，比例不同会出现留边；不会把横屏视频拉伸成竖屏。留边触摸不发给 iPhone；画面内拖动和抬起使用同一映射。改变视频格式、分辨率等并显式应用设置仍沿用原有重连流程。

## 本次日志结论

用户日志 `mediabridge-probe-20261002-072132-492.log.txt` 开头记录：Android 13 / API 33，小米 Mi 10；MediaBridge **2.1.0-phone-debug-26092812-dev / 26092812**；手机调试台已连接；ECARX 三个相关服务均不可用。截图中 CarBridge 选择“吉利 / ECARX”与“自动”，状态为“ECARX 服务不可用”。

07:16:07 的封面异常显示调用应用是 **com.netease.cloudmusic.iot.dev**，读取 CarBridge 私有 Provider 被拒绝。它是历史包名，不是配套的 `com.mediabridge.app.dev`。日志已包含 CarBridge 的图片 content URI，不能据读取失败判断手机没有提供歌曲资源；也不能据该 URI 宣称小窗展示已通过。

CarBridge 0.1.0 未声明新版 `.dev` 包可见性，在 Android 11+ 也可能忽略正确安装的手机调试版，本补丁已修复。历史包不在协作/授权范围，不能通过开放整个 Provider 或信任旧包来规避版本混用。

用户随后确认：CarBridge 0.1.0 改“通用 Android”并重连后已有声音。这说明当时标准 Android 音频路径能够工作，符合没有 ECARX 的手机环境；该用户实测不算 0.1.1 的设备验收，也不证明车辆 SDK 已通过。

后续 `mediabridge-probe-20261002-073921-446.log.txt` 已是新版 MediaBridge 2.3.0-carbridge-26100201-dev，不应继续把该份日志的问题归因于历史包。日志 3017 行含 2083 条重复封面候选记录、85 次 sameTrack=false 资源解析、46 次图片缓存完成；末尾 title 为歌词、artist 为 CarPlay 已连接、时长缺失。

用户确认 iPhone 酷狗开启“车载蓝牙歌词”，关闭后歌曲信息恢复正常。这与上游将歌词写入媒体标题、接收端按标题识别换曲的链路相符。本次保持正常标题/切歌处理，不通过冻结旧标题来吞掉真实换曲。

MediaBridge 2.3.1 增加防护：CarBridge 元数据仅取 MediaSession，不把连接通知当歌手/图片；缺歌名、歌手或正时长时不发起封面/歌词搜索，也不使用可能错误的歌词缓存，原始图片和内嵌同步歌词保留。CarBridge 在线封面需匹配歌名、歌手及 ±3 秒时长，不能直接采用第一条搜索结果。其他播放器的通知与搜索策略不变。新增去重的标题来源诊断，曲目文本按导出选项脱敏。

## 收藏联动边界

截图中的爱心属于 iPhone 绘制的 CarPlay 页面，手动触摸可操作。当前 CarBridge 没有收藏状态解析、评级元数据或可确认的远程收藏指令，MediaBridge 因此不能与 iPhone 酷狗账号双向同步。未支持命令明确反馈不支持；没有实现假收藏或固定坐标点击。具体证据与后续方案见 [收藏可行性](CARBRIDGE_FAVORITES_FEASIBILITY.md)。

## 手机与车机配置

| 用途 | CarBridge | MediaBridge |
| --- | --- | --- |
| 手机单独测试 CarPlay 声音 | 通用 Android + 自动；主动播放 | 可不运行桥接 |
| 手机测试配套桥接 | 自动；CarBridge 不在忽略名单 | 安装本目录 2.3.1 `.dev`，开启手机调试模式、桥接与通知访问 |
| 吉利车机通常使用 | 吉利 / ECARX + 自动 | 正式配套版就绪、取消忽略 CarBridge |
| 吉利车机直连 | 吉利 / ECARX + ECARX 直连，或自动并忽略 | 新版通过协议临时排除或遵循忽略策略 |

本目录新手机 Debug 包不会覆盖历史 `com.netease.cloudmusic.iot.dev`，因为包名不同。安装后打开新版本并重新授予其通知访问权限；测试时停用历史桥接服务，避免两个应用同时控制。没有代用户卸载应用、清除数据或安装到设备。

单纯“自动”不等于已经获得播放资格。手机独立出声用通用 Android；手机桥接则要求新版调试服务就绪。车机保留吉利配置，不为消除错误提示而默认绕过 ECARX 音乐资格。

iPhone 酷狗关闭“车载蓝牙歌词”；切一首歌重新发布元数据，必要时重连 CarPlay。CarPlay 画面本身的歌词/收藏与该蓝牙标题功能不同，不需要为了接收真实歌名而关闭正常的 CarPlay 操作。

## 本机验证

- `shared:testDebugUnitTest`：307 项通过；`common:testDebugUnitTest`：85 项通过，均 0 失败/错误/跳过。
- 新增 7 项覆盖：视频默认值与已保存兼容选项、方向偏好/非法值、两种比例的等比布局/触摸、图片只读重新授权、通用 Android 独立播放许可、发现 `.dev` 且旧确认不能绕过。
- `mobile:lintRelease`：0 错误、4 项原有告警；`automotive:assembleDebug` 通过。
- 最终配套脚本完成两端构建/协议/签名检查；MediaBridge **157** 项测试通过（原 150 + 本次 7），0 失败/错误/跳过。合计 **549** 项测试。
- MediaBridge 正式 lint：0 错误、16 告警；手机 Debug lint：0 错误、14 告警，均为既有告警数量。正式与手机 Debug APK 均构建成功。
- 原始日志：tooling/tmp/carbridge-0.1.1-check.log、mediabridge-26100202-check.log、carbridge-0.1.1-final-paired.log；最终交付复制到 evidence。
- 签名证书：`A539C794675FC8AD7FAC6E78604C066E15C98C3C65F62CC34895FA3FC65A23C7`；认证资产继续从用户授权的原版 APK 外部输入，未提交 Git。
- 最终 APK 核验：CarBridge 101 / io.github.rhsr1024.carbridge，MB 26100202 / com.mediabridge.app 与 com.mediabridge.app.dev；三个包签名一致。CarBridge 包内两份认证资产与用户授权的 DiPlay-0.2.8.apk 逐字节相同；最终 manifest 已含 .dev 包可见性及主界面/CarPlay 界面的默认横屏声明。
- 正式包内 Git 来源分别为 CarBridge 24a5013、MediaBridge 5dc7eb3；Debug 不嵌入 version-control-info，来源由同一轮构建和源码提交记录关联。构建时仅文档有未提交更新，后续文档提交不改变编译代码；源码归档包含最终维护文档。

本机未安装 0.1.1 到手机/车机。更新后需验证固定方向、自动旋转音画连续/触摸位置、切回前台与后台、60fps/HEVC 解码、手机桥接及真正 ECARX 声音/方控/互斥。保留原有 V01–V26 的车测边界。

配套脚本选项 IncludePhoneDebug 可同时生成手机调试 APK；无此选项保持两包发布。2026-10-02 构建前重新获取 MediaBridge origin/main，远程基线仍为 ece9746，本地 main 包含配套协作与本补丁；没有待合入的远程 main 提交，也没有推送操作。

## 交付

目录：`D:\WorkSpace\CarPlay\deliverables\CarBridge-0.1.1`。包含 CarBridge 0.1.1、配套 MediaBridge 正式版、手机 Debug 版、README、源码和检查证据。APK 哈希以该目录 `SHA256SUMS.txt` / `manifest.json` 为准。

功能已本地提交，未推送。旧 0.1.0 交付目录保留，供版本对照；不要将历史手机调试版视作本次配套包。
