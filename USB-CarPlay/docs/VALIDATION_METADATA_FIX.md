# 元数据与交接修复验证（2026-10-03）

## 发布产物

| 文件 | 字节数 | SHA-256 |
| --- | ---: | --- |
| `CarPlay-USBBox-Slim-MediaModes-MetadataFix.apk` | 7,565,716 | `9c7524393e2a894abd6df0bf4779ae9fdade8fa326fec6244036b41af2bf7a3f` |
| `MediaBridge-2.3.11-usb-metadata-26100304.apk` | 637,463 | `841830637ac720e19bbb93c1aeb951c2eea910b9e319018fdc0a62bb875d08d3` |
| `CarBridge-0.1.7.apk`（上层 deliverables） | 37,686,521 | `e5ae73d9a5cfcab5c03a3ffd0871a13e4c9afe264f44730fb61359848e9db763` |

USB APK 保留原包名、versionCode `2112232222`、versionName `2025.08.07.1852` 和签名证书 `C8A2E9BC…F51192AB8`。新文件名用于区分修复版；不覆盖历史发布包。CarBridge 与 MediaBridge 均使用原配套本机测试签名 `A539C794…65A23C7`，签名校验通过。CarBridge 的两份授权运行认证资产与外部输入逐字节一致，原文件未纳入 Git。

## 本地验证

- USB 模块：16 项测试通过。
- CarBridge common：160 项；shared：421 项，全部通过。
- MediaBridge：204 项，全部通过。
- 总计 801 项，零失败、零跳过。CarBridge mobile 与 MediaBridge 的 Release Lint 和签名发布构建通过。
- 三端 `BridgeProtocol.java` 字节相同；APK 包名和版本复核通过。
- USB 原方法 37,724 个，仅预期五处接线不同，其余 37,719 个指令不变；四个尾部 hook 删除新增调用后原指令完全一致。
- 所有原类字段、继承关系和方法签名保留；原 USB/焦点/触控/方控方法不变，多点开关保持开启；资源、清单、原生库及其他非 DEX payload 逐字节相同。

机器可读证据：[全量 DEX/资源验证](evidence/metadata-fix-verification.json)、[测试与发布摘要](evidence/metadata-fix-release.json)。旧首版证据保留，不覆盖其历史记录。

初次并发校验曾遇到同一 MediaBridge 测试输出目录被占用；结束重叠构建后，完整配套脚本重新运行成功。最终结果以上述成功构建为准。

## 未完成的外部验证

尚未连接用户车机/iPhone/USB 盒子实测。需要验证盒子实际封面供给、歌词字段和时间单位采样、连续切歌、暂停后播放、USB 断连后纯软 CarBridge 有声，以及原双指缩放和三模式切换。若上游继续将歌词塞入歌曲身份字段，本地修复不能保证还原；未找到唯一在线候选时留空。
