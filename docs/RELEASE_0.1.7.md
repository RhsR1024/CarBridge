# CarBridge 0.1.7 与 USB CarPlay 配套修复

CarBridge 修复 MediaBridge 握手前拒绝消息被 server ID 检查丢弃的问题。另一 CarPlay 尚未退出时显示真实占用原因；超时清空连接后不再继续向空端点发送握手而掩盖原错误。既有车机播放资格和音频门控策略保留，连接退出后正常重新协商。

USB CarPlay 从用户固定的 7.55 MB 基线重建，修复断开手机后仍占用协作席位、增量歌曲混合，接入盒子原生封面、歌词与经采样确认的进度。具体实现、USB 不变边界和实车验收见 [USB 修复记录](../USB-CarPlay/docs/METADATA_HANDOFF_2026-10-03.md)。

配套 MediaBridge `2.3.11-usb-metadata-26100304` 来自其独立仓库 main，支持 USB 未知时长的严格补全，排除原空控制会话并缩短失效会话提示。三个应用需要使用本次配套构建测试切换；仅更新 MediaBridge 无法修复旧 USB 客户端持续占位。

当前版本仍需车机实测。本地构建、单元测试、Lint 与 APK 差异校验不代表特定车机 ROM、盒子固件和 iPhone 音乐应用的行为已全部验证。发布 APK 使用现有授权的外部运行认证资产和本机配套签名；资产原文件与签名私钥不进入 Git。
