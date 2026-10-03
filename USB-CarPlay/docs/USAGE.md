# 安装与使用

## 安装包

- `releases/CarPlay-USBBox-Slim-MediaOptions.apk`：当前正式版，包含三模式、封面/歌词与断连修复，以及对齐 CarBridge 的两项媒体选项。
- `releases/MediaBridge-2.3.11-usb-metadata-26100304.apk`：当前配套 MediaBridge，从正式仓库 main 构建。
- `releases/CarPlay-USBBox-Slim-MediaModes-MetadataFix.apk`、`CarPlay-USBBox-Slim-MediaModes.apk`、`MediaBridge-2.3.9-usbbox.apk`：历史交付包。
- `baselines/CarPlay-USBBox-F25-MultiTouch-Slim.apk`：用户指定的纯精简基线，也是回退包。

USB APK 保持原包名 `com.flyme.auto.energy`、版本字段和原签名，方便覆盖安装及回退。通过设置中新增的“媒体接入”入口辨认本版。不要卸载后重装来验证覆盖功能，以免丢失原设置。

MediaBridge 已有 CarBridge 的媒体桥接机制。本次配套版补充 USB 包身份、原签名验证和 USB 能力声明；旧版不一定能识别 USB APK，不能仅凭已经支持纯软 CarBridge 判断兼容。

## 设置入口

在 USB CarPlay 自身设置页顶部点“媒体接入”，选择以下模式；“接入状态”显示具体等待原因。

| 模式 | 行为 |
| --- | --- |
| 自动（默认） | 未安装 MediaBridge，或在 MediaBridge 中关闭桥接、忽略 CarPlay时，走 F25 直连；已开启桥接且未忽略时，等待 MediaBridge 就绪后走桥接。 |
| F25 直连 | 使用 APK 内原有 F25 SDK 和原控制回调；安装了配套 MediaBridge 时先协商排除重复桥接。 |
| MediaBridge 桥接 | 通过标准媒体会话提供歌曲信息，通过协作协议接收媒体按键。未安装、未授权、被忽略或尚未就绪时显示等待原因。 |

桥接模式需安装并打开配套 MediaBridge，开启桥接和通知使用权，并取消对 CarPlay 的忽略。MediaBridge 当前只能维护一个协作客户端；纯软 CarBridge 活跃时，先退出它的协作连接再使用 USB 版本。

MediaBridge 最低支持 Android 9，三模式也在 Android 9 及以上启用。更旧系统继续执行基线的 F25 初始化，不显示新增入口。

## 媒体选项

入口：USB CarPlay 设置 → 媒体接入 → 媒体选项。

- **缺失歌手时的标题格式**：默认“保留原始信息”；可选“歌手 - 歌曲名”“歌曲名 - 歌手”。只在没有独立歌手且标题符合格式时拆分输出，已有歌手不拆，原始数据不改写。修改后重新连接手机生效，直连与桥接均使用该输出。
- **直连在线封面与歌词**：默认关闭。开启后，在 F25 直连或自动模式实际走直连时使用 LrcLib 查歌词、iTunes 查封面。需要车机联网、完整歌名/歌手及可信正时长，并严格核对匹配结果；未知时长先等待。原生数据优先，关闭后仍可用原生内容和已有缓存。切到桥接后使用 MediaBridge 的资源设置。

在线开关即时刷新资源，不重新协商媒体模式、不触发播放，也不向 USB 盒子发送新指令。封面和歌词显示在车机媒体小窗，实际展示取决于车机 SDK 和小窗能力，不改变手机 CarPlay 投屏画面。详见 [实现与验证](MEDIA_OPTIONS_2026-10-03.md)。

## 控制与显示

歌曲标题、歌手、专辑和播放状态来自盒子已有的歌曲数据。暂停后保留媒体入口，因此小窗和方控仍可发送播放。直连和桥接的明确媒体命令都沿用原控制链。

切换、授权变化、心跳超时和让出车机媒体通道不主动发送 USB 播放/暂停，也不门控 USB 音频。USB 原有自动播放、音频焦点等策略依旧按基线运行，因此其他车机软件引发的系统行为仍须实车确认。

当前修复版读取盒子已有封面与 `MediaLyrics` 字段；完整 LRC 或实时歌词行可经直连或 MediaBridge 发布。连续进度采样确认秒/毫秒后才发布时长和进度，不增加拖动定位。开启 MediaBridge 在线检索后，缺少时长的 USB 歌曲也可按完整歌名和歌手进行唯一精确匹配；封面使用网易/QQ，歌词使用已启用的网易/QQ/LrcLib。原始数据缺失且没有唯一匹配时保留空白。详见 [修复边界](METADATA_HANDOFF_2026-10-03.md)。

从 USB 切换纯软 CarBridge 时，先断开 USB CarPlay 的手机连接或退出 USB 应用。修复版在观测到断连后会释放协作席位。仅切换前台但两个 CarPlay 都仍连接手机时，不会强制抢走另一个的活跃连接。

## 回退

覆盖安装 `baselines` 中的纯精简版即可回到此前功能。保留原应用设置。配套 MediaBridge 仍会把 USB 包作为受协作管理的来源；基线没有协作入口，因此回退后使用其原 F25 直连。回退后不依靠 MediaBridge 控制这个基线包。
