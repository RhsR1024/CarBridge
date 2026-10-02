# 0.1.3 实车验证基线

2026-10-02 用户确认：CarBridge 0.1.3 + MediaBridge 2.3.3-carbridge-26100204 在吉利 FX11-A2 / Android 11 上，导航播报时滚轮调整导航音量，直连方控和 MediaBridge 切换接管可用；目前手动测试未发现阻塞使用的问题。

证据：DiPlay-20261002-121901-081.txt、mediabridge-probe-20261002-121729-993.log.txt。日志记录导航 usage=12 / stream=11 / focus granted=1，播报结束释放；媒体 stream=3。导出时 AUTO 正在桥接、有声，郭顶 / 我们俩已正确拆分，部分曲目的在线歌词及缓存可用。

这不是所有场景完全验收：部分歌词源未收录或返回 HTTP 错误；中间存在 iPhone 把歌词文字作为标题的输入；切回车机 MediaBridge 来源续播、空状态进入应用仍待改进。未将所有播放器、后台通话或故障恢复标为已验证。

APK 构建提交 e0471a36522b03c7dddd68e8ea3c04964affec82；交付与自动测试见 RELEASE_0.1.3.md。这是下一轮续播改造前的已实测基线。
