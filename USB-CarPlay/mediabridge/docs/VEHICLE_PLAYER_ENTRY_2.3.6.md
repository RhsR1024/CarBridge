# 2.3.6 播放器入口与独立设置图标

## 背景与目标

FX11-A2 的媒体小窗放大按钮直接启动 MediaBridge MainActivity，之后可能只把该任务恢复到前台；它没有执行已提供的播放器 PendingIntent。2026-10-02 15:17 日志显示同一设置实例反复 resume，没有 new_intent。不能依赖旧 referrer 猜测当前调用者。

用户同意让普通 MediaBridge 图标与小窗放大共用播放器入口，另增“MediaBridge 设置”图标。

## 行为及实现

- 保留 MainActivity 组件名，兼容车机缓存的旧入口；改为短暂路由页面，打开目标后立即 finish，不留下可被恢复的设置页面。
- 原设置界面完整迁移到 SettingsActivity，独立 launcher 图标、独立 taskAffinity。通知入口及目标不可用时的回退均显式进入设置任务。
- 普通包启动通过 MAIN/INFO 优先解析 MainActivity；两个 launcher 中 MainActivity 保持较高优先级，SettingsActivity 标注“MediaBridge 设置”。
- 当前有效快照优先于上次播放器，播放和暂停状态都可打开。无当前快照时尝试记忆的播放器；被忽略、固定播放器冲突、未获桥接资格的 CarBridge 不作为有效目标。
- 优先发送同包创建的媒体会话 Activity PendingIntent，否则使用播放器启动入口。目标卸载、入口不可用或无目标时提示并进入设置，不在路由页循环。
- 本路径不发送 PLAY，不请求音频焦点、不更改媒体快照。音乐应用自身打开界面后的行为仍由该应用决定。
- 非导出的 VehiclePlayerActivity 继续处理媒体 PendingIntent；导出的 MainActivity 不接受外部 resumePackage 来指定目标。
- CarBridge 0.1.3 的媒体会话入口指向 singleTask 的 CarPlayHostActivity，连接期间优先恢复该画面。本轮不修改或重新构建 CarBridge。

## 验证任务

- 自动测试覆盖暂停时打开当前播放器且未发播放命令、无快照打开上次播放器、目标不存在回退设置且结束路由页、两个桌面入口及设置独立任务、原有切源续播测试。
- 实车覆盖安装后：确认两个图标；播放及暂停时点小窗放大，均打开播放器；从独立设置图标进入设置后再点放大，仍打开播放器；切回续播继续正常；CarBridge 连接时验证恢复 CarPlay 画面。
- 部分车机桌面按包去重可能只展示一个图标。APK 声明两个入口，实际桌面展示仍待实车验证；前台服务通知同时保留设置入口。
- 诊断日志保留 VEHICLE_ENTRY（MainActivity/VehiclePlayerActivity）及 SETTINGS_ENTRY。

具体构建提交、测试数量、签名及校验值见交付目录 manifest.json 与 evidence。本轮功能需实车验证。
