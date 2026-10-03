# 2.3.8 播放器跳转分支与生命周期诊断

版本：`2.3.8-entry-probe-26100301` / `26100301`。

## 问题与范围

2026-10-03 用户报告播放洛雪时，点车机媒体小窗放大偶发停留在全屏媒体画面，底部控件被车机导航栏遮住，随后多次点击正常。11:47:36.177 和 11:48:01/05/09/15 均记录到 MainActivity 打开洛雪的请求，但原日志位于实际发送之前，不能判定打开结果或区分启动分支。

MediaBridge 正式版没有这类全屏歌曲展示页面。MainActivity/VehiclePlayerActivity 是透明路由入口，真正的应用兜底是 SettingsActivity。照片中页面的具体所属窗口尚未采集，因此本版不修改其按钮布局，也不把该画面认定为 MediaBridge 自绘兜底。需要系统窗口记录确认它属于车机媒体界面还是播放器。

本版仅增加观察记录，保持原有会话入口优先、普通应用入口回退和设置兜底。没有增加延迟、重试、播放命令或权限，也没有改变目标选择及 Activity 导出配置。

## 新日志

- `VEHICLE_ENTRY`：保留原来的入口/目标请求记录。
- `VEHICLE_LAUNCH`：目标来源（当前快照、上次播放器、内部请求）；会话入口是否存在、创建包和资格；发送前、返回或异常；普通应用启动 Intent 的 action/component/flags；进入设置的原因。
- `VEHICLE_ENTRY_LIFECYCLE`：create/start/restart/resume/pause/stop/destroy/new_intent/window_focus/finish_requested。各记录包含进程、入口类、实例、任务、是否任务根、是否正在结束、窗口焦点、当前分支、最后阶段及单调时钟耗时，可将同一次跳转串起来。
- `SETTINGS_ENTRY`：保留原有记录，补充进程和窗口状态，便于识别真正进入了设置兜底。

`session_send_returned` / `launcher_start_returned` / `settings_start_returned` 均标明 `foreground=unverified`，只表示调用正常返回。不能据此宣称外部播放器已经成为前台。入口在 onCreate 中立即 finish 时，可能没有 start/resume，这本身不代表失败。

Android 11 沿用原来的会话入口兼容策略；低于 API 31 的公开接口不能查询 PendingIntent 是否 Activity，日志为 `type=unknown_pre31`，不会伪报已验证类型。

日志只输出有限长度标识符和异常类型，不序列化 URI、Intent extras、PendingIntent 本体或异常消息。调用者/referrer 只是线索，不参与授权或跳转决策。new_intent 保存本次 Intent 供后续生命周期记录，避免沿用旧 referrer，不重复拉起播放器。

## 实车复核

1. 覆盖安装本版，播放洛雪并确认媒体小窗显示歌曲。
2. 点击放大；如复现，记下时间，返回后再正常点击一两次并导出日志。
3. 对比同一实例的 lifecycle 和 launch：未进入入口、会话发送返回、会话异常后普通入口、普通入口异常及设置兜底可分别识别。
4. 如果失败和成功都显示启动调用正常返回，仍需同时采集系统 ActivityTaskManager/WindowManager 日志或窗口快照，判断外部播放器是否被系统阻止、未恢复或被另一个窗口覆盖。本应用无跨应用前台确认权限，本版没有新增此类权限。

本地测试可验证分支、异常回退与日志内容，不能替代目标车的窗口行为验证。
