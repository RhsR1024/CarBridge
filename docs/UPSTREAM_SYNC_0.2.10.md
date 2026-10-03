# CarBridge 0.1.5 — DiPlay 0.2.10 同步

日期：2026-10-03。本轮继续同步原项目 main，保留 CarBridge 定制。自动化检查与设备验收分别记录。

## 不可变基线

| 项目 | 记录 |
| --- | --- |
| 同步前 CarBridge | `997162f180575ea807bd78ee4bc7c4db01802c75`（0.1.4） |
| 已集成上游 | `v0.2.9 / 18429e737228e8d75d9b6c850af89dcca2f591b6` |
| 新稳定标签 | `v0.2.10 / 3e43e25c55921bdf5149f5f92851acf202ed353a` |
| 本轮目标 main | `45563135a3d05a17315a84f57434fdb5dfa43a5f`；比标签多一项征集诊断报告的文档提交 |
| 集成分支 | `sync/diplay-0.2.10`；采用双亲合并，准确合并提交随后记录在台账 |
| 版本 | mobile / automotive 均为 `0.1.5 / 105`，mobile debug 为 `0.1.5-debug` |
| 应用身份 | `io.github.rhsr1024.carbridge`，debug 后缀 `.debug`，automotive 后缀 `.automotive` |
| MediaBridge | 本轮未修改；协作协议仍为 1.0 |

上游差异为 84 个文件、4,136 行增加、348 行删除。包括 USBMUX 填充解析、USB 不依赖未完成的热点设置、AirPlay 占用端口回退、Wi-Fi Direct 兼容、通话平台回声处理、BYD 电池协议识别、地图镜像开关、视频播放器及跳转、乌克兰语和有界后台诊断日志。

## 合并与定制保留

十个文本冲突文件：CarPlayMediaKeys、DiPlayActivity、五份语言资源（默认、阿拉伯语、西班牙语、俄语、简体中文）、mobile 构建、Iap2LinkChannel 和 Iap2LinkEngine。

- **方控、媒体会话与双模式**：CarPlayMediaKeys 继续交给 CarBridgeMediaRuntime。保留 ECARX、直连／MediaBridge 路由、显式 PLAY/PAUSE、命令校验、音乐互斥和歌曲资源补全。没有接入上游收到音乐流／PLAYING 即重新申请焦点的独立媒体会话。
- **歌曲封面**：保留现有按连接、曲目代次和传输 ID 校验的接收器、4 MiB／超时限制、有界图片解码队列、URI 发布和跨应用只读授权。底层采用上游通用 Session 事件，保留原发送接口。默认原始 session-12 数据只交给 CarBridge 接收器；只有显式传入回调时才采用上游接收器。新增两项真实链路工作线程测试，防止一份封面被两套接收器确认。上游独立媒体会话的 bitmap 元数据测试不适用于本项目 URI 发布实现，保留本项目测试与其余上游回归。
- **音量与通话**：吉利 GuidanceActivity、AudioFocusCoordinator、导航期间前台音量目标及结束恢复保持。AndroidMediaSink 接入上游仅针对 telephony 的通信模式、回声／降噪处理及模式恢复，未改音乐渲染门控；新增 MODIFY_AUDIO_SETTINGS 权限。硬件效果需要车测确认。
- **图标与车型**：已有图片资源无改动；Geely 默认资源和 BYD 隔离保持。新增乌克兰语地图说明使用 CarBridge 品牌，上游署名和许可不变。
- **视频／地图**：接入 Media3 1.11.1、进度条和十秒跳转，以及地图镜像开关。原视频按键优先入口保留，视频仍要求有效驻车状态；没有新增吉利挡位适配或宣称吉利视频已验收。
- **诊断与连接**：新增启动、蓝牙、USB、麦克风诊断，同时保留 CarBridge 媒体／路由事件。默认旋转不重连及可选稳定后重连策略保持。

## 自动化验证

| 检查 | 结果 |
| --- | --- |
| shared 单元测试 | 421 通过，0 失败／错误／跳过 |
| common 单元测试 | 159 通过，0 失败／错误／跳过 |
| home 单元测试 | 4 通过，0 失败／错误／跳过 |
| mobile lint debug | 0 错误，18 个警告 |
| mobile lint release | 0 错误，4 个警告 |
| home lint debug | 0 错误，5 个警告 |
| maphost lint debug | 0 错误，2 个警告 |
| 四模块 debug 构建 | mobile、automotive、home、maphost 均成功 |
| 定制与资源核对 | 137 个受保护源文件及图片未变；差异检查和 public tree 检查通过 |
| 文档本地链接 | 50 个通过 |

共 **584 项单元测试通过**。重点覆盖音乐门控、导航焦点、方控、双模式路由、USBMUX、通话模式恢复和新封面链路。lint 保留既有警告，不宣称零警告。

四个 APK 已核对包名与版本，均无运行时认证资产：

| 模块 | SHA-256 |
| --- | --- |
| mobile | `899cb4905218ea9e863a8eb22e187b3eba26ff9077f2797f20921a95e3b13632` |
| automotive | `582cce930db26ccf9a9ab8f59a8404cced2bb7441b9de3d68cf2cef9a85cbd48` |
| samples/home | `b278fb0b686a82f4ea41335004161842a82fee49bc87d17e42687a7da2e26eed` |
| samples/maphost | `1a28b2c035c9e36259d015c0ae0e7161755d1d9cb06219a3a05f841485e40622` |


复现命令：

```powershell
python scripts/check_public_tree.py
.\gradlew.bat :shared:testDebugUnitTest :common:testDebugUnitTest :home:testDebugUnitTest :mobile:lintDebug :mobile:lintRelease :home:lintDebug :maphost:lintDebug :mobile:assembleDebug :automotive:assembleDebug :home:assembleDebug :maphost:assembleDebug --no-configuration-cache --console=plain
```

工具链：JDK 25.0.3+9、Android SDK 37、NDK 28.2.13676358。本地日志及检查结果位于未跟踪的 `build/sync-0.2.10/`。

## 设备验证与回退

本轮没有安装车机、连接 iPhone 或发布签名正式包。此前 [0.1.3 车辆记录](VEHICLE_VERIFIED_0.1.3.md) 不代表 0.1.5 已经实车验收。源码 debug APK 不含运行时认证资产，不作为独立连接 iPhone 的车测交付包；原有 mobile debug 桌面测试名称继续保留。

车测重点：两种模式下方控、歌曲和封面切换、音乐让位、导航播报时音量目标及结束恢复、通话期间回声与挂断后的音量模式、USB／无线连接与重连、图标和旋转／倒车窗口往返。上游也未为 0.2.10 提供新一轮实车验收。

同步前提交保留。已共享主线如需撤回，按本轮 merge 的第一父创建 revert，不重写历史。
