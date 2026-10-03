# 构建与维护

## 环境

Windows、JDK 17、Android SDK platform 34/build-tools 35.0.0、Gradle 8.9、Python 3。设置 `JAVA_HOME`、`ANDROID_SDK_ROOT`，必要时设置 `GRADLE_BIN` 和 `GRADLE_USER_HOME`。脚本默认兼容本机 `D:\CarSoft\MediaBridgeApp\tooling` 下的已安装工具。

后续 USB 功能修改从本目录的固定精简基线进行，编辑 `src/`，新增接线只在 `scripts/build.py` 中做精确变换。`stubs/` 仅用于编译声明和测试，严禁将其类装入 USB APK。`BridgeDexTool` 和验证脚本会拒绝意外新增/覆盖原类。

## 一次性准备

在本目录运行：

```powershell
python -m venv .venv
.venv\Scripts\python.exe -m pip install -r scripts/requirements.txt
.venv\Scripts\python.exe scripts/prepare_toolchain.py
```

准备脚本下载固定 JADX 1.5.6 和与原 APK 相同的公开 AOSP platform 示例签名材料，按 SHA-256 校验后放入被忽略的 `_tooling/`。也可用 `--jadx`、`--key`、`--cert` 指定本机已有文件，离线导入。SDK/JDK/Gradle 使用正常安装的工具链，不重复存入仓库。

该 AOSP key 是原包使用的公开示例材料，不是车机厂商的私密生产密钥。MediaBridge 的本机 keystore 是另一套签名，不包含在目录中。

## USB APK 构建、签名、比对

```powershell
.venv\Scripts\python.exe scripts/build.py
.venv\Scripts\python.exe scripts/sign_usb.py
.venv\Scripts\python.exe scripts/verify.py _build/CarPlay-USBBox-Slim-MediaModes.apk
```

流程：验证基线哈希 → 编译新增类 → 以原 minSdk 16 进行 D8 转换 → 反汇编指定四类并精准接线 → 合并 DEX → 保留其余全部原 ZIP payload → zipalign → 原签名签署 → 最终 APK 全方法/ABI/资源比对。

新增三模式在 Android 9+ 启用；低版本保留原 F25 初始化。编译并未抬高原 APK 清单中的最低版本。

输出在 `_build/`，审核验证后再复制到 `releases/`。不要覆盖 `baselines/`；它是当前正式维护链的固定起点。

## 测试与配套 MediaBridge

```powershell
powershell -ExecutionPolicy Bypass -File scripts/run_tests.ps1
```

USB 测试用独立 Android 测试工程，避免其原厂 SDK 编译替身与 MediaBridge 自有 SDK 实现冲突。测试原控制回调时，以记录输入按键的替身作为边界；实际原链路另由 DEX 全量比对和历史字节码测试验证。

`mediabridge/` 仅保留 2.3.9 历史源码快照，不再参与当前构建。正式源码在 `D:\CarSoft\MediaBridgeApp\MediaBridge-src` 的 `main`；其他机器通过 `MEDIABRIDGE_ROOT` 指向它的克隆。脚本检查正式仓库协议并运行其测试。未提供签名变量时生成未签名 release。使用与当前配套版相同的本机签名，须通过环境变量提供 `MEDIABRIDGE_STORE_FILE`、`MEDIABRIDGE_STORE_PASSWORD`、`MEDIABRIDGE_KEY_ALIAS`、`MEDIABRIDGE_KEY_PASSWORD`。不要把口令或 keystore 写进仓库。

USB 端与 MediaBridge 端的 `BridgeProtocol.java` 必须完全相同；构建脚本会检查。当前 USB 信任原公开 AOSP 证书；配套 MediaBridge 信任指纹见验证记录。更换配套签名时须更新双方信任策略、重跑身份验证测试，并明确安装兼容性。

## 历史补丁

`history/usb-multitouch`、`history/usb-f25`、`history/usb-size` 保存此前实际使用的修改脚本和验证材料，原脚本的工作目录路径按当时布局保留，供审计和定位，不是当前构建入口。以后修改使用上面的独立构建流程即可，基线已经包含全部历史修正。

如需从最初 APK 重新追溯，最初文件 SHA-256 为 `14659850cd85dda1b1f156ca65438d2beb866de6d7c6101408a62866c4dc1e6d`；历史顺序为多点开关 → F25 修复 → 删除填充。无需把原先含 23.4 MB 填充的大包重复纳入维护目录。
