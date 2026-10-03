# 2.3.5 小窗放大入口诊断

用户提供的 2026-10-02 15:04:43 日志确认两次切源续播酷我成功（observedPlaying=true）。这不能替代其他播放器及 CarBridge 的续播验收。

本版只补充诊断，不改变放大按钮路由、播放策略、组件导出或桌面入口。放大跳转播放器的问题尚未修复。

- SETTINGS_ENTRY：记录 create/resume/pause/new_intent、实例与任务标识、启动 action/flags/component、callingPackage 和 android-app referrer 的包名。不输出任意 URI、Intent extras 或凭据。
- VEHICLE_ENTRY_QUERY：记录车机是否读取 getLaunchIntent/getPlayerIntent、调用 UID 和返回 PendingIntent 是否存在。每个 Binder 前 20 次及随后每 100 次记录，避免轮询淹没日志。读取入口不代表使用了入口；缓存过的入口可能不再被读取。
- VEHICLE_RESUME：拆出原来统一标记 listener_unavailable 的各项前置状态，避免把无上次播放器误判为通知监听断开。
- 这些调用者信息只是诊断线索，不能视为可信身份，也不用于自动重定向。

## 实车操作

1. 覆盖安装车机正式 APK，保留 CarBridge 0.1.3。
2. 播放第三方音乐并确认小窗显示歌曲，点击小窗右上角放大按钮，记下时间。
3. 返回桌面，从应用列表打开 MediaBridge，记下时间。
4. 再返回小窗点击放大，覆盖已有设置页面被重新带到前台的场景。
5. 导出诊断日志，附上上述操作的大致时间。

通过三组记录比较车机和应用列表的启动方式。如果两者不可区分，需要保留独立设置入口或采用其他车机协议适配，不能凭猜测重定向所有 launcher 启动。
