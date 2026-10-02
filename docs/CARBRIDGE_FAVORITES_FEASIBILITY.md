# CarPlay 收藏双向同步可行性

日期：2026-10-02。当前结论：CarPlay 画面里的收藏可由用户触摸操作，但 CarBridge 0.1.1 / MediaBridge 2.3.1 尚不支持把该状态与 MediaBridge 双向同步。本项是能力核对与后续适配设计，不是已实现能力。

## 截图与代码能证明什么

用户提供的 iPhone 酷狗 CarPlay 播放页在进度条下方有爱心按钮。这说明 iPhone 当前页面有收藏操作；不能仅据截图推断每个音乐 App 都开放同一种远程收藏协议。

- `common/src/main/java/com/shilapi/xcertplay/CarPlayHostActivity.kt` 用 TextureView 显示解码画面，触摸经 CarPlayTouchMapper 转成坐标并调用 controller.sendTouch。画面里的爱心不是 Android 原生 View，也不是本项目画出的按钮。
- `shared/src/main/java/com/shilapi/xcertplay/nowplaying/NowPlayingParser.kt` 当前解析歌曲、歌手、专辑、时长、封面 ID、播放状态、进度、队列位置及应用名；没有收藏状态。
- `common/src/main/java/com/shilapi/xcertplay/CarPlayMediaKeys.kt` 的 CarPlayMediaCallback 仅实现播放、暂停、上一首、下一首；没有 onSetRating。
- CarBridgeMediaRuntime 发布的 MediaMetadata 没有 USER_RATING，PlaybackState 没有 ACTION_SET_RATING；CarBridgeRouteManager 的协作命令也没有收藏。
- iAP2 目录虽登记 0x5003 / SetNowPlayingInfo 名称，但没有据此建立收藏字段、序列化、成功确认或酷狗行为证据。消息名称不能作为可用收藏接口的证明。

因此两个方向目前都缺链路：iPhone 收藏后没有结构化状态回传；MediaBridge 点收藏后也没有已验证命令可交给 iPhone。MediaBridge 2.3.1 对该未支持命令明确反馈不支持，不本地改成红心或伪报成功。

## 推荐适配路线

1. 在明确测试歌曲、iOS、音乐 App 版本的条件下，对用户手动收藏/取消收藏前后进行受控协议采样。比较 Now Playing 等已有通道的字段 ID、长度和值变化，默认不记录认证、音频和设备身份。分清“喜欢”“加入资料库”“加入歌单”，不能把评分数值直接当收藏。
2. 只有确认原生状态与可调用操作后，才为该能力补充精确字段解析/编码和 fixture；未知值保持 UNKNOWN。画面有爱心而无协议字段时，记录为仅 UI 可操作。
3. CarBridge 快照增加收藏三态、读写能力及来源；命令使用明确 desired=true/false，携带 connection、trackGeneration、requestId、routeEpoch。真实切歌或路由变化使旧操作失效；重复请求不能导致反向切换。
4. MediaBridge 只在能力成立时显示可写收藏。发命令后显示等待，收到 iPhone 对应曲目的确认才更新。不能把命令已发当成账号收藏成功。
5. ECARX 直连使用同一状态与命令入口；协作协议按兼容规则升级，两个 APK 配套测试。至少覆盖前后台、不同画布、连切、失联、取消收藏及 iPhone 端主动变更。

协议不支持时，可选择建立两应用共享的本地收藏记录，但必须叫“本地收藏”，不能冒充已同步酷狗账号。本轮没有实现这类替代语义。

## 不采用固定坐标点击作为正式方案

自动点击画面爱心受当前页面、横竖屏、画布尺寸和音乐 App 布局影响；后台并不一定停留在该页面。单次点击通常是切换动作，无法可靠区分收藏与取消收藏；仅识别红心像素也无法提供跨 App、跨主题和跨曲目的可靠账号确认。因此现有按钮保留正常手动操作，不用盲点屏幕绕过能力确认。

本地代码只能确认当前缺少接口，不能证明 Apple 或所有音乐 App 永远不支持。下一阶段是否能做真实双向同步，取决于协议证据与实机验证。
