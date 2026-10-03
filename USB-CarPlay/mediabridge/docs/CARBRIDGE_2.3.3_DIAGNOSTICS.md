# 2.3.3 CarBridge 配套诊断

日期：2026-10-02；versionCode 26100204；正式和手机 Debug 使用同一 main 源码及既有测试签名。配套 CarBridge 0.1.3，协作协议仍为 1.0。

本轮只在 CarBridge 歌词路径增加诊断并提升版本：记录请求编号、信息是否足够、在线开关、来源列表、跳过或匹配结果、最终来源与歌词长度。保留原匹配/缓存/互斥/路由和普通播放器行为。歌曲详细字段仍由既有导出选项控制，不记录歌词全文。

CarBridge 负责导航用途、临时焦点和前台音量跟随；MediaBridge 不额外申请导航焦点。实车先测试直连再测试实际桥接就绪，异常后及时导出两个应用日志。若状态为直连或已让位，MediaBridge 不负责该曲目的输出及歌词查询。

交付证据见 D:/WorkSpace/CarPlay/deliverables/CarBridge-0.1.3/evidence 与 manifest.json。设备验证由用户完成。
