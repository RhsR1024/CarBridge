# 安装与使用

## 安装包

- `releases/CarPlay-USBBox-Slim-MediaModes.apk`：精简基线加完整媒体三模式。
- `releases/MediaBridge-2.3.9-usbbox.apk`：识别 USB CarPlay 包身份的配套 MediaBridge。
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

## 控制与显示

歌曲标题、歌手、专辑和播放状态来自盒子已有的歌曲数据。暂停后保留媒体入口，因此小窗和方控仍可发送播放。直连和桥接的明确媒体命令都沿用原控制链。

切换、授权变化、心跳超时和让出车机媒体通道不主动发送 USB 播放/暂停，也不门控 USB 音频。USB 原有自动播放、音频焦点等策略依旧按基线运行，因此其他车机软件引发的系统行为仍须实车确认。

本版没有确认盒子时间字段的单位及封面数据关联，所以不伪造总时长、进度或封面，不提供拖动定位。歌曲文字与常规播放控制可以桥接；在线封面、歌词查找因缺少可信时长可能不启用。原 APK 内已有的歌词等分发逻辑不受影响。

## 回退

覆盖安装 `baselines` 中的纯精简版即可回到此前功能。保留原应用设置。配套 MediaBridge 仍会把 USB 包作为受协作管理的来源；基线没有协作入口，因此回退后使用其原 F25 直连。回退后不依靠 MediaBridge 控制这个基线包。
