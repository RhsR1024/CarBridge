# ECARX MediaCenter 协议记录

本文记录从本地 MediaBridge/Cplay 反编译材料核对并已写入当前实现的事实。IPC 返回成功仍不等于中控、仪表或 HUD 已显示。

## 标准路径

- Support action：`com.ecarx.eas.core.intent.action.SUPPORT_SERVICE`
- EAS 组件：`ecarx.xsf.mediacenter/.MediaCenterService`
- Support descriptor：`com.ecarx.eas.framework.sdk.IEASFrameworkSuppportService`
- MediaCenter descriptor：`ecarx.xsf.mediacenter.IMediaCenterSvc`
- source type：6
- 注册优先 `registerInMusic(packageName, client)`，不支持时回退 `registerMusic(client)`；权限拒绝不伪装成普通协议不支持。

当前夹具覆盖的 transaction：`registerMusic=1`、`unregister=4`、`requestPlay=5`、`updateMusicPlaybackState=6`、`updateMediaSourceTypeList=7`、`updateCurrentSourceType=8`、`updateCurrentProgress=10`、`updateCurrentLyric=14`、`declareSupportCollectTypes=15`、`registerInMusic=19`、`declareMediaCenterCapability=30`。

## F25 路径

当前 F25 初始化按以下顺序尝试，并且每条路径都要求拿到真实 MediaCenter binder 后才进入注册：

1. `com.ecarx.sdk.openapi` 的 `OpenAPIService` service-pool，调用 `getService(pid, uid, package, "mediacenter")`。
2. `com.ecarx.sdk.openapi` 的 EAS Framework service，先取 main binder，再从消息返回值读取 MediaCenter binder。
3. EAS service-directory 的 `getService("mediacenter")`，保留 EAS 返回的 descriptor/version 信息用于诊断。

F25 复用同一个 `IMusicClient` 协议实现，控制码和回调映射遵循 Cplay 参考：next=87、previous=88、forward=90、rewind=89、play=126、pause=127。126/127 是显式播放和暂停，不能折叠为 toggle。

## 收藏与歌词

收藏只有在源端声明可写且当前状态已知时才发布为可用；LIKE/UNLIKE 等方向动作按目标状态选择，未知 toggle 不会被猜测。请求携带 package、session、track identity 和输入版本，源端没有在 3 秒内反馈时进入 UNKNOWN。

歌词来源包括 NetEase、QQ、酷我、LRC Library 和 Zvuk。在线请求默认关闭；用户明确同意后才会请求网络。Zvuk 的令牌刷新仅对同一请求生效，保留 Cookie，并拒绝跨主机/POST 重定向。

## 安全边界

没有证据表明 MediaCenter 必需 Cplay 的 AppKey、USB 盒激活或商业凭据，这些内容未迁入工程。`protocol-fixtures` 只保存非敏感常量。
