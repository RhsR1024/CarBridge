# MediaBridge 2.3.4 续播配套说明

2026-10-02：已验证基线 CarBridge 0.1.3 / MediaBridge 2.3.3 已推送 origin/main，验证范围见 VEHICLE_VERIFIED_0.1.3.md。

新功能在 MediaBridge 实现：明确选择车机 MediaBridge 来源时尝试恢复上次实际桥接的播放器；卡片“进入应用”使用专用播放器跳转入口，应用列表保持设置入口。保留自动/显式桥接/直连三种模式。CarBridge 无需修改 Android 功能或安装新版本，继续使用已验证的 0.1.3 APK。

CarBridge 恢复只走既有协作 COMMAND PLAY，保持媒体门控及新播放意图校验。处于直连、已断连、被忽略或协作未就绪时不可根据持久包名强行恢复。两应用协议仍为 1.0。

配套规格、实现和实车清单位于 MediaBridge 仓库 docs/VEHICLE_RETURN_2.3.4.md。此次不是 CarBridge 音频策略的再次修改。
