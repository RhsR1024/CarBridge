# 兼容性与能力矩阵

`代码完成` 代表本地源码、夹具和自动化测试已经覆盖；`待实车` 代表不能用 IPC 返回成功替代车机实际显示或控制；`外部待验证` 代表依赖目标车或真实网络服务。

| 项目 | 当前实现 | 状态 |
| --- | --- | --- |
| 标准后端生命周期 | LegacyBackend，阶段时限、generation gate、无限退避 | 目标车 MediaCenter 2.5.0 实车通过 |
| F25 服务获取 | OpenAPI service-pool、EAS main binder、EAS service-directory 三分支 | 代码完成，待实车 |
| 元数据/状态/进度 | source type 6、快照、进度和 EAS `updatePlayState` | 目标车/酷我实车通过 |
| 播放控制 | 播放、暂停、停止、上一曲、下一曲、快进、快退、seek | 目标车标准方控实车通过 |
| 歌词 | LRC 多时间戳/offset、五类来源、缓存、按模式微调 | 目标车/酷我歌词显示通过 |
| 收藏 | 方向动作、曲目/session/输入版本确认、未知状态降级 | 代码完成，播放器矩阵待实车 |
| 封面 | 原始 URI 透传、Bitmap/通知图标/在线源回退、小图规范化、同曲质量保护 | 26092709 目标车/酷我实车通过 |
| 手机调试封面 | Debug 模式下载网络封面并转换为本地 URI；LX 优先使用完整专辑图 | 代码与自动化测试完成，酷我/LX 待手机复验 |
| LX 收藏适配 | 标准 Heart rating 直达“我的收藏”，并回传确认状态 | LX 1.9.1 补丁与安装包完成，待手机复验 |
| 自动恢复 | 2/5/10/20/30/60 秒退避、连接/初始化/注册独立时限 | 代码完成，长时实车待验证 |
| 热切换 | 单后端、独立 30 秒切换截止、失败回退原模式和原微调 | 代码完成，双模式实车待验证 |
| 权限与日志 | 最小权限、授权三态、有限日志导出和脱敏 | 目标车 Downloads 导出通过；其他 ROM 待验证 |

## 应用身份

- applicationId：`com.netease.cloudmusic.iot`
- 显示名：`MediaBridge`
- 通知监听兼容类：`com.geely.auto.music.MediaListenerService`
- artwork authority：`com.netease.cloudmusic.iot.artwork`
- 不使用 shared UID；不请求 MDC 系统权限、息屏、AVAS、悬浮窗或修改系统设置权限。
