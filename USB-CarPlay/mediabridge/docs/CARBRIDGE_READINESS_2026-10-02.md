# CarBridge 0.1.2 配套就绪诊断

MediaBridge 2.3.2-carbridge-26100203，正式与手机 Debug 使用同一 main 源码。

用户 08:41 日志显示 Android 13 / Xiaomi Mi 10，手机调试后端 REGISTERED，但新版本两个进程中没有 Notification listener connected。授权状态不能代替系统实际绑定；此前 CarBridge AUTO 的等待静音由该条件解释。用户已确认通用直连有声；直连根据既定协作规则不在 MediaBridge 小窗发布 CarBridge 的歌曲。

`CarBridgeCompanionService.POLICY` 增加可选 `unavailable` 原因字段，区分 DISABLED / NOTIFICATION_ACCESS / LISTENER_DISCONNECTED / BACKEND_NOT_READY。原 ready 判定、来源排除、让位和播放许可不变；仅诊断内容变化不递增 policy revision。策略变更记录监听、后端、权限及总状态，不输出曲目或认证内容。

配套 CarBridge 显示具体修复提示，并将实际配对包写入诊断。没有伪造 onListenerConnected、自动改写系统授权、通过私有通道绕过媒体监听，也没有在直连时恢复双路发布。手机恢复流程：重新开关新版 Debug 的通知使用权，保持手机调试及桥接启用，输入显示已连接后使用 CarBridge AUTO / BRIDGE；此流程待设备验证。

用户后续已确认上述操作后“有声音且显示歌曲”，即这台 Mi 10 的手机桥接恢复路径已验证。新增版本的提示和 CarBridge 转屏功能仍需升级后单独验收。

新增回归验证：授权由关闭变为开启但系统尚未绑定时，ready 仍为 false，原因变为 LISTENER_DISCONNECTED，所有权 revision 不因原因变化而变化；CarBridge 仍不得申请播放。构建结果见配套交付记录。

本机验证完成：158 项测试，零失败/错误/跳过；正式 lint 0 错误 / 16 告警，Debug 0 / 14，两种 APK 构建成功。构建提交 `58a57aaae4062efee480fb45a82ea5759b3859bc`，配套 CarBridge 0.1.2 构建提交 `ec996195c1addd0c562ca4f1e71190d34f91ea5d`，完整证据位于 `D:\WorkSpace\CarPlay\deliverables\CarBridge-0.1.2\evidence`。本地提交，未推送或安装到设备。
