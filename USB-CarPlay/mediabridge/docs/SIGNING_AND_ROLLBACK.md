# 签名、升级与回退

## 已知本地样本

| 样本 | SHA-256 | 签名证书 SHA-256 | 结论 |
| --- | --- | --- | --- |
| 根目录 `com.geely.auto.music.MediaBridgeApp - 副本.apk` | `40F1D7DDD2C4620877ECD14EAA67C79C9C5A3557FADA403AC4AAE7DDD29503B0` | `C8A2E9BCCF597C2FB6DC66BEE293FC13F2FC47EC77BC6B2B0D52C11F51192AB8` | 本地参考 APK；仓库没有已证明匹配的私钥 |
| 旧诊断目录中的修改包 | 以原文件复核为准 | `A40DA80A…` | 与上一个样本也不是同一签名 |
| 本工程历史本地测试包 | 每次构建重新记录 | `50E0F1A99A029CCD9A843333D5BB07BD0099415B3532DF6BB7ED2017DC64D822` | 仅本机自签名测试身份 |
| 1.8 本轮 Android Debug 签名测试包 | `9F88E7F298D0A26E6D940D10A9F28BFABD88B6B667190E07509694C182E88B36` | `A539C794675FC8AD7FAC6E78604C066E15C98C3C65F62CC34895FA3FC65A23C7` | 只供受控测试；不能覆盖前述任一签名的同包 APK |
| 1.9 UI 与收藏兼容测试包 | `E93F7A8DBCB524994BBB7F1851A98D6504FDCC9605DBDE766350831E3D14F61A` | `A539C794675FC8AD7FAC6E78604C066E15C98C3C65F62CC34895FA3FC65A23C7` | 与本工程 1.8 最终测试包同签名、versionCode 30；是否能覆盖车机当前包须先核对证书 |
| 1.10 Review Fixes 测试包 | `A52FD1A66F488C70507832D0EB5EE7B442278B4713920F6E03EA0FFE9B3DAAF8` | `A539C794675FC8AD7FAC6E78604C066E15C98C3C65F62CC34895FA3FC65A23C7` | versionCode 31；本机 Android Debug 证书，仅供受控测试 |
| 26092716 圆形封面与 UI 测试包 | `2F036F8A09BC675F5D2F9ACED5A2857E8153662FF10E69D69F6F68413AC270A7` | `A539C794675FC8AD7FAC6E78604C066E15C98C3C65F62CC34895FA3FC65A23C7` | versionCode 26092716；本机 Android Debug 证书，仅供受控实车测试 |
| 26092717 稳定来源与完整缩放测试包 | `5B1452AE8F09EC336600CB45E7E11E9820FE38B01E19725AEACEE0D8D5EC3262` | `A539C794675FC8AD7FAC6E78604C066E15C98C3C65F62CC34895FA3FC65A23C7` | versionCode 26092717；本机 Android Debug 证书，仅供受控实车测试 |
| 26092718 智能封面适配测试包 | `F2F605C9FD2F84A93E8E4DD24F3ACFE9D1DE7329DB08798D14959AD12212FDA9` | `A539C794675FC8AD7FAC6E78604C066E15C98C3C65F62CC34895FA3FC65A23C7` | versionCode 26092718；本机 Android Debug 证书，仅供受控实车测试 |

证书相同不等于持有私钥；当前没有证据证明本地 release key 能覆盖车机已安装包。`app-release-unsigned.apk` 是未签名构建中间产物，不是可安装包。测试签名会因机器而变，后续正式交付必须换成受控且持续保存的签名密钥。

当前 Windows 工作站使用的 Android Debug 测试证书文件为 `C:\Users\public.DESKTOP-IQJOR7V\.android\debug.keystore`，证书 SHA-256 为 `A539C794675FC8AD7FAC6E78604C066E15C98C3C65F62CC34895FA3FC65A23C7`。仓库只记录文件位置和公开证书摘要，不记录别名或口令；该测试身份不能覆盖原版 APK。

## 本次安装路线决定

2026-09-25 用户明确允许卸载原版并全新安装本版本。因此 G1 不再阻断本次自用测试包交付，但这属于范围变更，不代表完成了 SPEC T19 的同签名覆盖验收。卸载前仍应备份当前 APK、版本和设置截图；卸载后原版数据及通知访问授权会丢失，新版本必须重新授权。

## 安装前必须采集

```text
adb shell dumpsys package com.netease.cloudmusic.iot
adb shell pm path com.netease.cloudmusic.iot
adb shell cmd package list packages -U | findstr com.netease.cloudmusic.iot
adb pull <codePath> backup-current.apk
apksigner verify --print-certs backup-current.apk
```

同时记录车机用户、versionCode/versionName、通知访问组件、设置截图和 APK SHA-256。不得根据文件名推断安装身份。

## 升级规则

1. 仅在新 APK 与车机当前安装包证书完全相同且 versionCode 合法时执行覆盖安装。
2. 覆盖后立刻核对 `com.geely.auto.music.MediaListenerService` 仍存在、通知访问仍指向该组件、设置迁移幂等、标准模式可注册。
3. 签名不一致时停止；不得用“先卸载”掩盖 G1，也不得擅自清除用户数据和授权。

## 回退规则

发布前保留从车机导出的可用 APK。Android 通常拒绝降低 versionCode；正式发布应另备“更高 versionCode、上一稳定代码”的 rollback build，并用同一正式密钥签名。若唯一回退路径需要卸载并丢失数据/通知授权，必须先单独获得用户确认。

当前“卸载本版本并重新安装备份原版”的回退尚未在目标车实测；T19/G10 仍不能标记通过。
