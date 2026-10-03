# 2.3.4 车机返回播放器规格与实现

日期：2026-10-02。基线为已由用户实车验证的 MediaBridge 2.3.3 + CarBridge 0.1.3。本轮 MediaBridge 版本为 2.3.4-carbridge-26100205；CarBridge Android 功能代码不变，继续使用原已验证 APK。

## 行为规格

1. 设置新增“切回时续播”，默认开启。在车机明确选择 MediaBridge 来源时，尝试对上次实际播放的桥接播放器发送 PLAY。当前播放器的常规播放/暂停/切歌能力保持；关闭设置后，停用空状态或让位状态下的续播回退。
2. 仅持久保存上次播放器包名，不持久保存“正在播放”、车机所有权、曲目进度、会话令牌或 PendingIntent。连接期间保留原会话标识用于多个会话的准确选择；进程重启后只在同包存在唯一有效会话时自动控制。
3. 上次播放器被忽略、与固定播放器设置冲突、通知监听不可用、后端未运行、旧代次/旧输入事件均不续播。CarBridge 必须仍连接且通过协作协议获得 BRIDGE 资格，PLAY 只发一次协作命令，不另发 MediaController 命令。
4. 不在开机、服务注册、元数据刷新、轮询当前车机来源或自动重连时恢复播放。注册未完成的回调无效；应用自身 requestPlay 周边 1.5 秒的恢复回声被抑制。同会话 1.5 秒内重复恢复事件去重。
5. 不主动取消原生播放器让位屏障，不伪造旧曲目为 PLAYING；由播放器真实播放/焦点变化驱动既有选择规则。发送后只观察结果，不循环重试。后来的用户选择仍按原互斥规则优先。
6. 会话已结束或同包多会话无法确定原会话时，只尝试打开对应播放器，不延迟排队补发 PLAY。系统禁止后台启动或播放器不支持续播时记录日志，由用户进入播放器恢复；不承诺已被杀掉的应用恢复到原歌曲进度。
7. 车机音乐信息的 launchIntent/playerIntent 始终指向专用 VehiclePlayerActivity。它动态打开当前有效播放器，否则打开上次有效播放器；优先使用同包创建的会话 Activity PendingIntent，再使用播放器专用入口/启动入口；不可用则进入设置并提示。
8. VehiclePlayerActivity 不导出，只通过本应用提供的不可变 PendingIntent 或内部显式调用进入。应用列表的 MAIN/LAUNCHER 始终保留在 MainActivity，不根据不可靠的调用者猜测改变入口。
9. 若车机绕过所提供 PendingIntent，直接按包名启动 launcher，则仍进入设置，不冒险把应用列表入口一起重定向。通过 VEHICLE_ENTRY / SETTINGS_ENTRY 日志识别，待实车确认是否需要针对 ROM 另作适配。

## 实现任务

| 任务 | 实现 |
| --- | --- |
| 保存恢复提示 | LastPlayerStore；只在有效已播放快照发布时记忆 |
| 明确来源选择恢复 | LegacyMusicClient → MediaListenerService.resumeLastPlayer；资格检查、会话选择、去重、结果观察 |
| 注册及请求回声保护 | LegacyBackend 标记回调就绪与自身播放请求抑制窗口 |
| 卡片进入播放器 | VehiclePlayerActivity；LegacyMusicClient 空卡片及正常卡片共用专用 PendingIntent |
| 保留设置入口 | MainActivity 原 launcher 不变，新增开关及入口日志 |
| 验证 | VehicleResumeTest 与既有 SourcePriorityIntegrationTest / AndroidRegressionTest 等 |

## 实车验收

- 覆盖安装正式包，保留 CarBridge 0.1.3。升级后先播放一次需要桥接的播放器，以建立新版本的恢复提示。
- 第三方播放 → 原生 QQ/网易云 → 在车机选择 MediaBridge：应恢复原播放器；检查音频互斥、歌曲信息及连续快速切源。
- CarBridge 桥接播放 → 原生音乐 → MediaBridge：应通过 CarBridge 协作恢复；CarBridge 切为直连、断开 iPhone 或被忽略后不能强制恢复桥接。
- 正常卡片和空卡片点击“进入应用”应进入播放器；应用列表点击 MediaBridge 应进入设置。记录两种入口的诊断事件。
- 禁用“切回时续播”、固定播放器、忽略上次播放器、播放器被杀、通知授权撤销、服务重连、开机、注册时均验证无意外播放。
- 出问题后导出 MediaBridge 详细日志；CarBridge 相关问题同时导出其日志。关注 VEHICLE_RESUME、VEHICLE_ENTRY、SETTINGS_ENTRY。发送命令成功不等于播放器已确认。

本机验证结果与 APK 签名/哈希见交付目录 manifest.json 和 evidence；新功能仍需实车验收，不能沿用 2.3.3 的车测结论。

## 本机构建验证

- APK 功能源码提交：`f8fa48a5655a60234b48cfb28c5582f571e7cfa9`。
- 170 项单元/回归测试通过（含 12 项新增续播测试），失败和错误均为 0。
- release/debug 构建通过；Android Lint 均为 0 错误，分别 16/14 项警告。
- 使用现有 Android 测试证书签名，正式版及手机调试版分别交付；CarBridge 保留原已验证 0.1.3 安装包。
- 本轮尚未实车验证来源选择回调、卡片入口及续播行为，请按上方验收步骤测试。
