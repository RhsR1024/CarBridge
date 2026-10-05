# CarBridge 0.1 实现与维护边界

0.1.10 修订（2026-10-05）：同步 DiPlay v0.2.12 稳定标签 `22d2aac`。保留 CarBridgeMediaRuntime、ECARX／MediaBridge 路由、音乐门控、吉利导航焦点和音量目标、默认图标与自定义图标。新增方控缩放／摇杆保持默认关闭；BYD 新输出仍受车型隔离。上游“下一首封面待传输时沿用上一首封面”不适用于本项目按曲目代次防串图机制，不引入第二套媒体会话。连接、画面、昼夜模式与诊断增强见 [本轮报告](UPSTREAM_SYNC_0.2.12.md)。

0.1.5 修订：同步 DiPlay v0.2.10 及 main 4556313 的诊断说明。保留 CarBridgeMediaRuntime 和按连接／曲目代次处理的原生封面通道；上游可选封面回调与原通道互斥。通话接入平台回声处理，音乐门控及吉利导航音量逻辑保持。详见 [本轮报告](UPSTREAM_SYNC_0.2.10.md)。

0.1.4 修订：同步 DiPlay v0.2.9，保留 0.1.3 吉利音频处理和协作协议。画面与触摸共用按会话协商尺寸计算的 CarPlayVideoLayout；默认旋转不重连，可选旋转稳定后重连与上游摄像头窗口策略并存。新增导航摘要供小组件使用，BYD 仪表歌曲仍按车型隔离。详见 [同步报告](UPSTREAM_SYNC_0.2.9.md)。

0.1.3 修订：下文“导航保留原有覆盖路径”仅对非吉利配置保持。吉利新增 GuidanceActivity 与 AudioFocusCoordinator 导航临时焦点，并经 CarPlayHostActivity 的前台绑定跟随音量目标；不更改音乐 PlaybackPolicy 或协作协议。详细行为和可观测边界见 RELEASE_0.1.3.md。

日期：2026-10-02；配套 MediaBridge 2.3.0-carbridge-26100201；协作协议 1.0。

## 代码落点

| 功能 | 实现 |
| --- | --- |
| iPhone 歌曲数据 | shared/nowplaying/NowPlayingParser、NowPlayingStore、CarPlayNowPlaying |
| 原生封面 | iAP2 session 12 → Iap2FileTransferReceiver → 有界图片解码 → CarBridgeArtworkProvider |
| Android 媒体会话与通知 | CarBridgeMediaRuntime、CarPlayMediaKeys、DiPlaySessionService |
| 音乐互斥 | PlaybackPolicy + MusicOutputGate；AndroidMediaSink 只在音乐通道执行门控 |
| 方式协商 | CarBridgeRouteManager；对端 CarBridgeCompanionService |
| 直连车机 | EcarxMediaRoute + 独立 ecarx Android library |
| 直连资源 | DirectMusicResources；LRCLIB 歌词、iTunes 封面；SynchronizedLyrics 时间轴 |
| 车型与图标 | CarBridgeSettings、BydOutputSettings、AirPlayPersistence 默认资源入口 |

内部包名保持上游命名。mobile 应用 ID 为 `io.github.rhsr1024.carbridge`；automotive 为独立的 `io.github.rhsr1024.carbridge.automotive`，仅检查可构建性，不作为本次车机 APK。原版 DiPlay 的配置、授权和忽略条目不会自动继承。

## 协作协议实际接口

使用显式 Service + Android Messenger，双方收包后校验 UID、应用包名及签名。信任条件是与当前应用同签名，或匹配本次授权测试签名的公开 SHA-256 指纹；没有任意信任开关。后续更换发布签名时应同步更新双方信任配置，不能仅改一个 APK。

协议文件为两仓库中内容相同的 `io/github/rhsr1024/interop/BridgeProtocol.java`。Envelope 包含 major/minor、CarBridge instance、MediaBridge server、connection、epoch 和 policy revision。消息最多 24 个基本类型字段，每个字符串最多 512 字符；元数据通过标准 MediaSession，图片不装进该消息。

| 消息 | 意义 |
| --- | --- |
| HELLO / POLICY | 签名验证、实例及策略快照 |
| PREPARE / PREPARED | 关闭旧输入/发布，等待串行车机工作队列中已进入的调用完成 |
| COMMIT / READY | 提交当前代次的 BRIDGE 或 DIRECT；只在此后开放新路由 |
| PLAY / GRANT | 新主动播放意图，可在真实状态为 PAUSED 时申请，不伪造 PLAYING |
| PAUSE / YIELD | 撤销旧播放资格；暂停保留明确 PLAY 恢复入口 |
| COMMAND | 带 ID 的明确媒体命令；重复 ID 去重，快速不同 ID 连按保留 |
| PING / PONG / CLOSE | 每秒检查、3.5 秒失联停用、显式退出 |

MediaBridge 永久识别两个 CarBridge 包名（正式和 debug），进程重启默认排除；内存中的路线授权不持久恢复。明确直连使用临时排除，不修改用户的忽略名单。服务入口再次检查排除状态，阻止已排队的旧媒体快照跨过交接。

交接 5 秒未完成只进入等待/停止状态，不获得对方权限。CarBridge 的直连注销必须获得远端正常返回；失败保持未知退出状态。MediaBridge 对 CarBridge 停发时以原有串行 Lane 的完成栅栏确认旧调用已结束，保留对其他音乐软件的服务。模式变更期间播放资格可能被撤销；需要主动播放恢复，不能把重连和元数据回声当成新意图。

0.1.1 根据用户要求移除了旧版手动兼容和确认开关。已发现的 MediaBridge 不支持协作时提示同时更新，所有模式都不得以旧确认偏好绕过。正式包和 `.dev` 调试包均显式声明包可见性；封面只读授权在存入缓存与路线就绪时补发，以覆盖安装/重连后的现有曲目。

## 音乐与资源

注册、连接、初始 PLAYING 和自动换曲不申请音乐所有权。明确播放及暂停确认后的手机 PLAYING 变化才能创建新意图。Android 焦点和车机资格都获准才开音乐门；失焦先静音、清空旧缓冲，再向 iPhone 发明确 PAUSE。临时恢复只复用仍有效的意图。通话和 Siri 使用上游独立音频用途，导航保留原有覆盖路径。

原生字段依据 xcertplay 已实现协议路径核对：信息组 0 的 title=1、duration=4、album=6、artist=12、artwork transfer id=26；播放组 1 的 status=0、elapsed=1、queue index=2、queue count=3、app name=7。时长和进度按 U32 毫秒处理。没有虚构原生歌词字段或歌曲 ID。真实 iPhone 下发行为仍需车测。

封面限制 4 MiB、16M 像素，缩采样到最长边不超过 1024；文件接收最多 4 路、30 秒超时，缓存最多 24 张。连接、曲目代次和 transfer ID 共同防止迟到图片覆盖。跨应用只发只读 content URI。支持 PNG、JPEG、GIF、WebP；其他编码清晰降级。

直连在线补全默认关闭，可在设置启用。在线匹配要求标题和歌手规范化后相等，时长相差不超过 3 秒；封面有专辑时还校验专辑。版本标记（如 Live）不被删除。资源按曲目缓存，拒绝错配比强行补图优先。歌词支持 LRC、多时间标记、offset、暂停和 seek；纯文本不伪造同步滚动。桥接模式不运行 CarBridge 在线查询，复用 MediaBridge 的原有资源配置和小窗。

普通应用不能强制暂停所有不遵守 Android 焦点的第三方软件。本实现保证自己的音乐让位，并通过车机来源协调目标软件。实车需确认各播放器的实际声音、曲目状态和来源回调。

## 上游与来源

DiPlay 当前基线：`22d2aacedcadc4ec1aef0d74b161b05321f708a7`（v0.2.12 稳定标签，见本轮报告）；原始 fork 基线为 `f2d06951b4e8114dbb62f551c12a32a845a3042f`（v0.2.8）。新增代码集中在独立包及 ecarx 模块，上游大文件只保留必要接线。BYD 功能未删除，入口受车型控制，原固件与接收器校验继续保留。

文件传输实现参考 `shilapi/xcertplay` 的 `17c92439413638dfd1d7f91d7e1c2e7358398762`：Iap2LinkEngine、Iap2LinkChannel、Iap2CsmChannel、Iap2Session 的 session 12 增量及 FileTransferReceiver。保留 GPL-3.0 来源，并补充资源上限、曲目关联和生命周期约束。

ecarx 模块来自本地 MediaBridge-src 的受维护 Binder ABI 和 MediaCenterAPI，基线 `ece9746c4e33a2134d64d10f2df8a3ca9b3bba5e`。仅提取 `com/ecarx`、`ecarx` ABI、SDK 连接层及小型有序队列；重新提供独立日志和普通 bindService 支持。未复制参考 APK 的 USB 控制数值、QQ 启动 Intent 或其他应用身份。原有反编译来源注释保留，外部 SDK ABI 的权利归属不因提取改变；对外分发仍需随源码保留原项目 notices。

Geely 图标来自用户提供的 480×480 参考图，裁切范围 x=53..428、y=148..271，仅保留车标，转换为黑色，在 256×256 白底画布中央按比例缩放为 220×73。原图、生成结果和加工说明均保留；仅用作用户要求的返回车辆桌面图标。
