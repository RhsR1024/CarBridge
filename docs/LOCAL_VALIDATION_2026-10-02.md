# 本机验证与交付报告

日期：2026-10-02。结论：配套源码实现、本机自动化与两个测试签名 APK 交付完成；车辆和 iPhone 上的最终功能验收尚未执行。

## 固定源码与交付

| 项目 | 值 |
| --- | --- |
| CarBridge 基线 | f2d06951b4e8114dbb62f551c12a32a845a3042f，DiPlay v0.2.8 |
| CarBridge 功能提交 | 128e50f；a77b9dc43bd261ac54e42634d9d24db76558574e |
| MediaBridge 基线 | ece9746c4e33a2134d64d10f2df8a3ca9b3bba5e |
| MediaBridge 功能提交 | a6e7801e236bcfeefe300694580696be50bd5114 |
| 协作协议 | Messenger 1.0，双方 BridgeProtocol.java 字节相同 |
| 协议源码 SHA-256 | 941a29b81f6bd6237964de1e7686a51b76166197ff172886a080af40bff91be8 |
| 输出目录 | D:\WorkSpace\CarPlay\deliverables\CarBridge-0.1.0 |

APK 内嵌 version-control-info 已核对为表中功能提交；其后仅整理文档，不改变应用代码。两个仓库保留本地提交和上游历史，未推送。

| APK | 字节 | SHA-256 |
| --- | ---: | --- |
| CarBridge-0.1.0.apk | 33196101 | 5ab314ed4f49e3fd703db928459765c304b7ac128deedf2fa7f213d749c60a07 |
| MediaBridge-2.3.0-carbridge-26100201.apk | 609715 | 4444de2a6195c2bc4496e33b8b8e1312c348a3086b12eb3c38d8635e1d310c82 |

CarBridge 为 `io.github.rhsr1024.carbridge`，0.1.0 / 100，minSdk 28 / targetSdk 37；MediaBridge 为 `com.mediabridge.app`，2.3.0-carbridge-26100201 / 26100201，minSdk 28 / targetSdk 34。最终 manifest/包信息确认两个包均非 debuggable，CarBridge 图片 Provider authority 使用新应用 ID，MediaBridge 包含协作服务。

两包 APK v2 签名通过。公开测试证书 SHA-256：`A539C794675FC8AD7FAC6E78604C066E15C98C3C65F62CC34895FA3FC65A23C7`。与已核对的历史 MediaBridge 26092810 APK 相同；车机当前安装包未检查。

用户提供的 `D:\Downloads\DiPlay-0.2.8.apk` SHA-256：`9b36a0866608244e422053d6027706b4672d8f2f4a8be9eb7e612411ffb6bf48`。最终 CarBridge 包中两个 offline-mfi 认证资产与它逐字节相同。私钥保存在指定 tooling 外部输入目录，未进入 Git；APK 均不包含 Android 签名 keystore。普通源码 debug 包在未设置认证输入时构建成功，ZIP 检查确认无认证资产。

## 本机测试和构建结果

| 检查 | 结果 |
| --- | --- |
| CarBridge shared:testDebugUnitTest | PASS：307 项，0 失败/错误/跳过，79 个报告 |
| CarBridge common:testDebugUnitTest | PASS：78 项，0 失败/错误/跳过，14 个报告 |
| MediaBridge app:testDebugUnitTest | PASS：150 项，0 失败/错误/跳过，16 个报告 |
| 合计 | **535 项通过**；包含原有回归与新增协议、仲裁、资源、车型测试 |
| CarBridge mobile:lintRelease | PASS：0 错误、4 警告 |
| MediaBridge app:lintRelease | PASS：0 错误、16 警告 |
| 普通源码 mobile:lintDebug / assembleDebug | PASS：未配置认证输入；lint 0 错误、18 警告 |
| mobile:verifyStandaloneAuthentication / assembleRelease | PASS：显式认证资源和本机测试签名 |
| automotive:assembleDebug | PASS：检查独立变体可构建；未作为此次安装包交付 |
| Build-PairedRelease.ps1 | PASS：完整执行两端测试/lint/打包、协议检查、复制和签名校验 |
| 最终 APK 身份/认证/签名/哈希 | PASS；最终来源提交在 APK 中可追溯 |
| 在线资源接口探针 | PASS：本工作站以实际参数/User-Agent 请求 LRCLIB 和 iTunes，均 HTTP 200；有同步歌词字段和封面 URL，未输出歌词内容 |

release lint 的 CarBridge 4 项是 localeConfig 的 API 提示、跨模块布尔资源引用及上游 CarPlay TLS 信任实现的两项警告。新增在线补全使用标准 HTTPS 校验。MediaBridge 16 项主要为既有 SDK/图标资源和 vendor 导出接口提示；保留完整 lint 报告，不以关闭检查掩盖问题。源码 debug 的额外警告包含测试及调试范围，不等同 release 新增错误。

新增重点覆盖：增量缺失与清空、同名曲与旧资源、超限/损坏图片、只读 Provider、LRC/匹配、失焦和临时恢复、重复与初始 PLAYING、旧进程/连接/epoch/ACK、重复命令 ID、未确认释放、在途车机工作完成栅栏、PAUSED 显式申请及迟到 grant。

这些是单元、Robolectric 和假后端检查，没有进行 Android 设备上的双进程联调、真实 AudioTrack 听音或目标 ECARX 注册。

在线探针样本为 Rick Astley 的 Never Gonna Give You Up；LRCLIB 返回 214 秒与同步歌词字段（请求时长 213 秒，在匹配容差内），iTunes 返回 10 项及封面地址。该检查仅证明本工作站当次网络和接口结构可用，不能代替车机网络、歌曲匹配率、图片下载/授权和小窗渲染。结果保存在 `tooling/tmp/carbridge-online-probe.json`。

## 环境与证据位置

工具均位于 `D:\CarSoft\MediaBridgeApp\tooling`：CarBridge 使用 Gradle wrapper 9.5.0、JDK 25.0.3+9、Android SDK 37、Build Tools 36/37、NDK 28.2.13676358；MediaBridge 使用 Gradle 8.9、JDK 17.0.16+8、SDK 34。构建脚本将缓存与临时目录指向 tooling 并保存/恢复环境。

原始构建日志位于 tooling/tmp：

- `carbridge-release-check2.log`：共享/common 测试、release 与 automotive 构建。
- `carbridge-final-diagnostics.log`：最终诊断接线后 common 测试、release lint/打包。
- `mediabridge-release-final.log`：MediaBridge 最终功能源码检查。
- `carbridge-source-only-final.log`：不含认证的源码构建与 debug lint。
- `paired-release-final.log`：已提交源码的配套脚本最终输出；两包内嵌提交与上表一致。

JUnit XML 位于各模块 `build/test-results/testDebugUnitTest`；lint 位于 `mobile/build/reports` 和 MediaBridge `app/build/reports`。交付目录另保存 `validation-evidence.zip` 和源代码快照；源码快照不含认证、签名或构建缓存。

## V01–V26 覆盖和未执行部分

以下所有整车操作均为 NOT RUN；“本机覆盖”只说明相应层次的检查通过。

| 验证编号 | 本机覆盖 | 设备部分 |
| --- | --- | --- |
| V01 | 构建脚本、源码包/车测包、应用身份/authority | 同装启动与系统授权 NOT RUN |
| V02 | 已核对字段实现、构造帧/截断解析、两接收路径接线 | 原始手机帧与实测对照 NOT RUN |
| V03 | Store 增量/同名曲/旧资源测试 | 连切录屏 NOT RUN |
| V04 | 单调时钟进度、暂停、seek/LRC 测试 | ≤1 秒目标误差 NOT RUN |
| V05 | MediaSession/通知与 MB 原资源链接线 | 小窗及 1 秒更新时间 NOT RUN |
| V06 | 命令明确语义和路由测试 | 前后台手机执行 NOT RUN |
| V07 | handshake 代次与路线授权测试 | 自动模式切换 20 次 NOT RUN |
| V08 | 临时排除、持久忽略隔离和模式分支 | 设置/车机状态联调 NOT RUN |
| V09 | 旧版/未安装/不兼容分支与提示 | 旧版混搭设备验证 NOT RUN |
| V10 | ECARX 自身身份和 SDK 适配构建 | 服务准入/真实 callback NOT RUN |
| V11 | 统一明确命令、发送/观测诊断 | 直连前后台执行 NOT RUN |
| V12 | 主动意图、让位、输出门控测试 | 双向抢占 20 轮和 300ms 听音目标 NOT RUN |
| V13 | 失败/延迟/旧 focus、缓冲门控覆盖 | 真实 PCM 与尾音 NOT RUN |
| V14 | 连接/重复 PLAYING 不抢播测试 | 手机重连/后台状态 NOT RUN |
| V15 | 用途优先级和恢复意图测试 | 电话/Siri/导航声音 NOT RUN |
| V16 | 车辆 YIELD/旧意图失效接线与测试 | 收音机/外部源切换 NOT RUN |
| V17 | 命令 ID 幂等、旧代次拒绝测试 | 无 ID 双路径采样和快速方控 NOT RUN |
| V18 | 生命周期/陈旧资源和连接隔离测试 | 真机反复连接/前后台 NOT RUN |
| V19 | 在途栅栏、迟到 grant/ACK、未确认释放测试 | 杀进程/SDK 断开/真实阻塞 NOT RUN |
| V20 | 文件传输/图片上限/损坏/Provider 负面测试 | 原生封面和跨应用 URI 读取 NOT RUN |
| V21 | 匹配约束、LRC/缓存生命周期测试 | 在线服务命中/两模式歌词卡片 NOT RUN |
| V22 | 版本/签名/独立 ID 和静态配置隔离 | 升级/数据保留 NOT RUN |
| V23 | UID/签名、malformed payload、版本负面测试 | 真机 IPC 权限/旧版组合 NOT RUN |
| V24 | 输入来源和命令诊断、原媒体键映射 | 方控、音量和语音分别采样 NOT RUN |
| V25 | Geely/BYD/Generic 偏好与原 BYD 测试 | BYD 实物和驻车约束 NOT RUN |
| V26 | 纯黑图标视觉检查、默认/自定义/恢复测试 | iPhone 图标缓存与 HOME 后台 NOT RUN |

无真实事件关联 ID 时没有加入盲目的时间窗口去重，以免吞掉用户快速两次切歌。原生封面与手机歌词可用性、新包名 ECARX 准入、特殊播放器焦点行为不能凭本机通过宣称已实车支持。上游原有逻辑和回归测试保留，但“未影响所有设备既有功能”仍需上述实车回归。
