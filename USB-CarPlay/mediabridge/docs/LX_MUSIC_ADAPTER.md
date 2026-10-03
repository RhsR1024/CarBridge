# LX Music 1.9.1 适配

MediaBridge 使用 Android 标准心形评分接口发送收藏和取消收藏。LX Music 1.9.1 原版没有向 MediaSession 声明该能力，也没有处理外部评分事件，因此必须给 LX 源码应用本仓库中的适配补丁。

```powershell
git apply D:\CarSoft\MediaBridgeApp\MediaBridge-src\patches\lx-music-mobile-v1.9.1-mediabridge.patch
```

补丁完成四件事：

- 向 TrackPlayer 声明 `SetRating` 和 `Heart` 类型；
- 收到收藏命令时直接加入 LX 的“我的收藏”默认列表，取消收藏时直接移除；
- 将当前歌曲是否位于“我的收藏”写回 MediaSession；
- 收藏完成后立即刷新元数据，让 MediaBridge 能确认操作结果。

手机验证封面时，需要在 LX 设置中打开“通知栏显示图片”。MediaBridge 的手机调试包会把 LX/酷我提供的网络封面下载并转换为本地 `content://` 地址，避免 Android `ImageView` 无法直接显示网络 URI。

本地构建生成的 LX Release APK 使用调试证书，仅供验证。若手机上已有不同签名的 LX，Android 不允许直接覆盖安装；请先备份 LX 的本地数据和歌单，再自行决定是否卸载旧版进行验证。
