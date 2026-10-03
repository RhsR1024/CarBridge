# USB CarPlay 元数据与来源切换修复

本次客户端源码位于本目录 `../src/`。配套 MediaBridge 从 `D:\CarSoft\MediaBridgeApp\MediaBridge-src` 的 main 构建，版本 `2.3.11-usb-metadata-26100304`；纯软 CarBridge 为 `0.1.7`。不再修改 `USB-CarPlay/mediabridge/` 的历史快照。

## 实车证据与原因

用户提供 `mediabridge-probe-20261003-181847-269.log.txt` 和 `DiPlay-20261003-182058-263.txt`。原始日志保留在用户下载目录，不将包含歌曲/设备信息的全文提交到仓库。

- MediaBridge 日志 18:09:08 显示正常歌名/歌手，18:09:11 变成歌名与歌手相同；18:14:12–18:14:22 出现旧歌名与下一首歌手/歌名混合。旧 USB 增量合并会保留之前的 title，并无条件更新 artist。上游也存在歌词借用歌曲字段的迹象，不能假定所有异常字段都能互换还原。
- USB 元数据日志的 60 次记录均为 `duration=0 lookupAllowed=false`，没有原生封面和内嵌歌词。旧新增模块只接了文字和状态，未接原 APK `v0.d.A(byte[])` 封面回调、`MediaLyrics` 与时间字段；MediaBridge 对受管理来源要求正时长，因此在线补全也被挡住。
- 切换后的 CarBridge 有音乐输入，但日志连续记录 `writtenFrames=0`、媒体门控未获资格；握手期间没有成功的 RoutePolicy。USB 断连后仍用非空连接代次保持协作，MediaBridge 的单客户端席位未正确释放。拒绝 HELLO 的 ERROR 不带 server，旧 CarBridge 先检查 server，吞掉真实占用原因，随后只报告超时。

## 实现

### 只读媒体观察

新增第五处原方法接线：在原 `v0.d.A(byte[])` 返回之前调用 `UsbMediaBridge.artwork`。其上游已有 dashboard COVER 事件，未改动 USB 收包、事件分发和原函数中的指令。

封面最多复制 4 MiB，单解码线程、队列容量 1，限制解码尺寸并缩采样至最长边不超过 256；本地异步结果只有连接代次、曲目代次和身份都相符才发布。换曲、断连清除旧图。

`MediaLyrics` 发布到标准 MediaSession 歌词字段：LRC 按原时间戳解析，纯文本作为盒子当前歌词行转交，不制造时间戳。字符串有长度限制。原 JSON 对象只读，原解析分发先执行。

`BoxClock` 根据连续前进采样与单调时钟确认秒/毫秒单位。未确认时保留未知时长/进度，未知进度速度设为 0，不让接收端凭空推算。新曲清时间数据，新连接重新确认；没有增加 SEEK 或盒子定位命令。

### 换曲和控制

artist 等于已知 title 的明显错位增量不覆盖已确认歌手。只有 artist 变化而没有新歌名时，清除可能混合的字段与旧歌词/封面，等待新身份；非空媒体 ID 使过渡期仍保留控制入口。新歌名不继承上首歌手/专辑。同一消息里的完整 title+artist 可识别同名翻唱，避免永久等待。

无法从任意已损坏的输入恢复正确歌名。若 iPhone 音乐应用的“车载蓝牙歌词”持续把歌词和歌名写入 title/artist，仍应关闭该功能进行对照。不会猜测交换字段或把某个网络搜索结果当作真实曲目。

### 断连交接

读取既有 `BoxInterface.f.P` 确认手机断开后，使用旧连接身份发送 CLOSE，停止 PING/绑定，释放车机直连入口；无手机时不占用 MediaBridge 席位。新连接重新协商。

CarBridge 在校验 UID 和 instance 后接收握手前 ERROR，显示真实占用来源；超时清空 peer 后本轮不继续发送 HELLO。MediaBridge 保持“一次一个活跃客户端”，没有强抢仍连接手机的另一个 CarPlay。

## 配套 MediaBridge

- 原有空控制会话也从 `selectManaged` 中排除；真实媒体 ID 的待补全会话仍可用。
- USB 未知时长允许在线检索，但歌名/歌手必须完整、不能相同，并要求归一化后精确且唯一匹配。保留 Live/版本词；有可信时长时要求误差不超过 3 秒。多个候选匹配则不选。
- 原生封面、原生歌词优先。在线封面支持网易/QQ；在线歌词使用设置中已启用的网易/QQ/LrcLib。酷我/Zvuk 尚无 USB 严格匹配器时跳过并记日志，不绕过用户开关。
- 无媒体会话的 Toast 缩短为“未检测到 CarPlay 媒体会话”，详细失败原因写日志。系统 Toast 外框由 ROM 决定。

## 不变的边界

固定精简基线：7,545,236 字节，SHA-256 `10dc853023f48cf0a11f44f99a6da1dd64cb08010550e0b1a72e8030cf94d796`。

三模式均不改 USB 收发、音视频、触控编码、多点开关、原 F25 按键映射及焦点函数。元数据只是读取副本。模式切换、断连和资格回执不生成手机 PLAY/PAUSE。明确用户按键继续一次性调用原有 F25 回调。

盒子原始封面事件没有曲目 ID；本次能拒绝本地解码迟到，不能识别盒子已把旧图片标作新歌曲事件的情况。未知进度时无法保证在线 LRC 逐句同步，盒子实时歌词行不依赖本地进度。上述边界须在车机上验证，不能用本地测试宣称实车通过。

## 重建与验收

运行 `scripts/run_tests.ps1`；再依次运行 `scripts/build.py`、`scripts/sign_usb.py`、`scripts/verify.py`。纯软 CarBridge 和 MediaBridge 通过上层 `scripts/Build-PairedRelease.ps1` 构建。验证产物放在 `docs/evidence/`，安装包放在 `releases/`，不要覆盖精简基线。

实车检查：连续切歌（含同名翻唱）、封面与歌词、暂停后等待再播放、断开 USB 手机连接后切换纯软 CarBridge、三种模式往返、双指缩放。安装前后沿用同一签名覆盖，不需要卸载清数据。
