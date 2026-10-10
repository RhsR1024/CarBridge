# CarBridge 0.2.16 — DiPlay 0.2.16 同步

日期：2026-10-10。目标为上游 0.2.16 公开预览版；未进行新版车机／iPhone 实测或签名正式发布。

## 基线与版本

| 项目 | 记录 |
| --- | --- |
| 同步前 CarBridge | `3a25d24773c27573c4600887cf470ff6b9e273f8`，main，工作区干净 |
| 原上游基线 | `v0.2.15 / b940efe81ebe6fe930caac71e19d9624723e6b7f` |
| 本轮目标 | `v0.2.16 / bac419695ca26b7e67d204a279333ca52132c2cb`；不跟随移动 main |
| 集成分支 | `sync/diplay-0.2.16`；本报告所在双亲 merge 保留上述 CarBridge 与上游提交 |
| 版本 | mobile／automotive `0.2.16 / 112`，mobile debug 为 `0.2.16-debug` |
| 身份 | mobile `io.github.rhsr1024.carbridge`，debug `.debug`，automotive `.automotive`；最低 Android 9 / API 28 不变 |
| 回退 | `backup/pre-diplay-0.2.16-20261010`；共享后按 merge 第一父 revert，不重写历史 |
| 协作 | 未修改外部 MediaBridge 或 USB-CarPlay；协作协议仍为 1.0 |

上游两标签间有 343 个文件变更、49,071 行增加、791 行删除；大部分新增行来自内置 Concentus 软件 Opus。吸收麦克风编码回退、USB NCM／较小读取重试、热点地址恢复、Android 17 本地网络权限、VPN 授权失败处理、WPA3、视频低延迟选项和积压恢复、设置搜索及仪表布局。新增实验功能仍默认关闭。

## 定制保护

- **音乐与方控**：继续由 CarBridgeMediaRuntime 管理单一 MediaSession、明确 PLAY／PAUSE、ECARX／MediaBridge 路由和音乐互斥；上游 CarPlayMediaKeys 的另一套会话／封面监听不覆盖本项目。
- **音道与导航音量**：原媒体／导航偏好键、旧配置迁移、GuidanceActivity、吉利导航临时焦点、前台 volumeControlStream 绑定与结束恢复保留。上游 NavigationPlayback 仅提供新 opt-in 导航方控所需的播放提示，不取代吉利现有音量逻辑。
- **所有音乐启动路径**：首次播放、补缓冲恢复、尾音仍通过 MusicOutputGate；新增导航提示与氛围灯 renderer 同时组合，释放时关闭门控 observer、导航提示 token 和 ambient renderer。译码后先归还 codec buffer 再写 AudioTrack 的修复保留。
- **BYD 隔离**：灯光 sink／支持检测／设置变更和蓝牙暂停入口检查所选车型。已保存的 BYD 开关不能在吉利／通用配置下启动灯光 I/O 或暂停蓝牙；灯光设置卡仅向 BYD 展示。蓝牙 journal 恢复允许在车型切换后执行，防止先前的暂停遗留。电话扬声器路由同样要求 BYD。
- **播放状态复用**：ambient 的 owner 与播放状态由原 runtime 快照更新；陈旧 owner 不能覆盖新 owner。不增加音乐所有权或 Android 焦点申请。
- **上游 ambient 行为**：用户明确启用后，暂停／断线维持最低亮度；关闭开关尝试恢复原车灯光。本轮保留该设计，默认关闭且限定 BYD，未将其误改成断线自动关闭。
- **图标与歌曲资源**：Geely 默认图标、用户替换／预览／恢复入口保持；升级不清空图标或音道。Now Playing 按连接和曲目代次隔离封面，不采用跨曲目旧图保留。
- **方向与身份**：默认固定横屏、自动旋转默认不重连、可选旋转重连保留；更新源继续指向 CarBridge，并在 SHA-256 校验后核对 APK 包名。versionCode 递增为 112，不复制上游 35。

USB-CarPlay、ecarx、车机路由、协作协议、Now Playing 核心、PlaybackPolicy／MusicOutputGate、GuidanceActivity 与同步前一致。AudioChannelMapping 仅增加上游 BYD 通话扬声器选择函数，原媒体／导航映射保持。

Concentus 来源与许可证保留；仅统一新增文件的换行、行尾空白和缩进空白，已与上游逐文件做同等规范化比较，未改变代码或许可文本内容。

## 2.5K 与大字体

0.2.15 已有“设置 → 显示 → 界面大小”：自动、100%、125%、150%、200%。自动模式根据屏幕的 dp 与密度缩放，不能仅凭 2.5K 像素分辨率保证固定字号；保存的 100% 选择不因升级被覆盖。此控制用于 CarBridge 自身界面；CarPlay 的文字／图标由独立大小设置决定。

本轮用真实 Android native graphics 渲染 2560×1440 自动缩放、紧凑／宽屏和阿拉伯 RTL，大字体系数 1.4；同时断言 Activity 的 fontScale、scaledDensity/density 和阿拉伯资源确实生效。检查发现上游密度 context override 使用默认 Configuration，会将系统大字体重置为 1 倍；本项目将该 override 的 fontScale 留为未设置，保留驾驶员原选择，并验证自动、150%、200% 三种密度缩放。针对窄窗口大字体问题，按钮在正常最小高度上按内容增高，避免第二行被裁切；紧凑大字体 header 的搜索框独占一行，避免文字仅剩一个字母。普通字体保持既有控制高度。

图像为源码测试渲染，缺少认证资产的提示属于测试环境；不是实车截图。完整 12 张输出在本机 `build/sync-0.2.16/screenshots/`，代表图随本报告保存：

- [2560×1440 自动界面](assets/sync-0.2.16/2560x1440-auto-en-overview.png)
- [紧凑大字体音道设置](assets/sync-0.2.16/compact-en-audio.png)
- [宽屏大字体车辆设置](assets/sync-0.2.16/full-en-vehicle.png)
- [阿拉伯 RTL 大字体](assets/sync-0.2.16/compact-ar-overview.png)

## 自动化验证

**2,293 项测试通过，0 失败／错误／跳过**。日志和 APK 元数据位于本机未跟踪目录 `build/sync-0.2.16/`。

| 检查 | 结果 |
| --- | --- |
| shared | 1,249 项通过 |
| common | 1,040 项通过 |
| home | 4 项通过 |
| mobile lint debug／release | 0 错误，19／4 警告 |
| home／maphost lint debug | 0 错误，5／2 警告 |
| mobile／automotive／home／maphost debug | 全部构建成功 |
| 大字体、原生渲染与 RTL | 14 项专项通过；12 张界面图，4 张代表图逐张查看 |
| 合并／文档 | 无冲突标记；diff --check 与本轮文档链接检查通过 |

完整源码命令在 `build-final.log` 为 `BUILD SUCCESSFUL in 9m 58s`，413 个任务、退出码 0。随后将 native 图像 fixture 的 applicationInfo 设置为正式 mobile 同样的 supportsRtl 标志，并断言 Arabic 根视图确实为 RTL；不改变生产代码。`rtl-final.log` 对两类 14 项测试和 mobile 两项 lint 再验证通过，`BUILD SUCCESSFUL in 28s`、退出码 0。全量 common XML 保存在 `full-results/common/`，用后续两类专项结果更新对应文件；汇总不重复计数，未过滤掉其他用例。

中间失败记录保留；已修复音频 constructor fixture、BYD 测试车型选择、单一互斥开关测试、图像测试宽屏 Back 导航和真实系统字号环境。最终没有忽略失败或跳过测试。

```powershell
.\gradlew.bat :shared:testDebugUnitTest :common:testDebugUnitTest :home:testDebugUnitTest `
  :mobile:lintDebug :mobile:lintRelease :home:lintDebug :maphost:lintDebug `
  :mobile:assembleDebug :automotive:assembleDebug :home:assembleDebug :maphost:assembleDebug `
  --offline --no-configuration-cache --console=plain
```

工具链为 JDK 25.0.3+9、Gradle 9.5、SDK 37、NDK 28.2.13676358；本机 Git Bash 执行既有 USB 授权脚本测试。新增 BYD 生命周期测试显式选择 BYD，保留生产车型限制。AudioRenderer 反射 fixture 同时传入门控、导航、ambient 和电话参数；上游独立 auto-yield UI 测试改为本项目单一音乐互斥开关的活动会话／重连提示保护。没有跳过失败用例或弱化生产策略。

public-tree 检查仍因同步前已跟踪的 21 个 USB 分发 APK／公开证书失败，本轮没有增加。这与单元、lint 和源码构建结果分别记录。

## 源码验证 APK

mobile 为 `io.github.rhsr1024.carbridge.debug / 0.2.16-debug / 112`；automotive 为 `io.github.rhsr1024.carbridge.automotive / 0.2.16 / 112`。ZIP 检查不含运行时认证身份，两者均带 arm64-v8a、armeabi-v7a、x86_64 的 Speex echo 库。以下为源码 debug 产物，不是签名正式车测包。

| 模块 | SHA-256 |
| --- | --- |
| mobile | `7c36bd2a49edfd502d6ef505d05b5ec61f2deb16d1f3bdec8567543e67eed2a8` |
| automotive | `b83c09709e9b7b9732dae59e0a24e6ef11175381ddc0a4a505264ddb2bdc013a` |
| home | `b278fb0b686a82f4ea41335004161842a82fee49bc87d17e42687a7da2e26eed` |
| maphost | `1a28b2c035c9e36259d015c0ae0e7161755d1d9cb06219a3a05f841485e40622` |

## 设备验收

没有安装本轮 APK 到车机、连接 iPhone 或执行真实方控／声音测试。停车验证应覆盖直连／桥接、播放暂停与切歌、媒体／导航音道和播报中音量调节、结束恢复、通话／Siri、图标替换恢复、横竖屏／分屏及 USB／无线重连。上游默认视频积压策略本轮有变化，尤其需要目标 ROM 反馈。BYD 新实验功能保持关闭，除非单独验收。
