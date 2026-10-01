# Distrust

Distrust 是一个校园 VPN 客户端项目，主力是**原生 Android 应用**，包名为
`idont.trust.atrust`，使用 Kotlin、Jetpack Compose、Material 3 与 `VpnService`
构建，核心为自维护的
[DistrustCore](https://github.com/sakimidare/DistrustCore)（基于
[Mythologyli/zju-connect](https://github.com/Mythologyli/zju-connect)）。

项目同时保留上游 [EZ4Connect](https://github.com/chenx-dust/EZ4Connect) 的桌面代码，
作为功能与兼容性参照，并复用同一套核心。

> Android 版本仍处于早期开发阶段；认证流程与真机兼容性仍在逐步完善。

## 运行模式

Distrust 提供两种互斥的运行方式：

1. **本地代理模式（默认）**：不占用 Android `VpnService`，在 `127.0.0.1` 暴露
   SOCKS5/HTTP，供 Clash、Mihomo 或抓包软件作为上游代理。这是 Android 上实现
   「aTrust + 代理 / 抓包」的主要方式。
2. **系统 VPN 模式**：由 Distrust 独占 `VpnService`，直接对设备流量进行校园网分流。

## 主要能力

- Kotlin / Compose / Material 3 原生工程，动态配色、深色模式与自适应布局
- Navigation Compose 顶层导航、返回栈与状态恢复
- 配置向导：协议、服务器、认证方式发现、凭据、运行模式
- 直接读取 aTrust 服务器公开的认证方式列表
- 认证：aTrust 密码、EasyConnect、短信、TOTP、RADIUS、文本验证码、内嵌 SSO WebView
  （在 3xx 导航加载前自动截获回调 URL）
- Android Keystore 加密凭据、DataStore 配置存储
- `VpnService` 授权与前台服务、本地代理前台服务
- SOCKS5/HTTP 实际监听，Mihomo 配置片段导出
- 全局日志同时输出 Logcat 与 UI，敏感字段脱敏
- 基于核心的多来源 DNS、服务端资源分流与节点优选

仍在完善：自动重连、内置抓包等。

## 仓库结构

| 路径 | 说明 |
| --- | --- |
| [`android/`](android/) | 原生 Android 客户端，开发状态与构建方式见 [`android/README.md`](android/README.md) |
| [`android/core`](https://github.com/sakimidare/DistrustCore) | 核心子模块 **DistrustCore** |
| 仓库根目录 | 上游 EZ4Connect 桌面客户端（Qt6），保留作参照并复用本仓库核心 |

## Android 客户端

### 构建

```bash
git submodule update --init --recursive
cd android
./gradlew assembleDebug
```

APK 输出：

```text
android/app/build/outputs/apk/debug/app-debug.apk
```

`preBuild` 会先由固定源码生成 `android/core/build/distrust-core.aar` 再嵌入；已有 AAR 时
可用 `-PskipGoCore` 跳过。没有 AAR 时 UI 仍可构建运行，但会明确显示「未安装核心」，
不会伪造成功状态。

### 工具链

- Android Gradle Plugin 9.3.1、Gradle 9.5.0、JDK 21
- compileSdk / targetSdk 37、Kotlin 2.4.20、Compose BOM 2026.08.00
- Go 1.26+ 与 gomobile（用于内嵌核心）

## 核心（DistrustCore）

核心在上游 zju-connect 基础上，针对**校外访问校园网**增强了服务端资源感知的 DNS 解析：

- 多来源并发解析：策略 DNS、`policy-secondary`（经隧道查询第二个策略 DNS）、系统 DNS、历史成功地址、自定义 DNS；
- 命中服务端下发 IP Resource 的地址优先，避免解析到公网 WAF 地址；
- `policy-secondary` 修复了校外**无法解析校园域名**的问题。部分学校（如 SEU）的第一个策略 DNS 校外不可直达，只有经隧道查询第二个策略 DNS 才能得到正确地址；
- 桌面 CLI（`main.go`）与 Android 端使用同一套策略。

构建与同步上游说明见 [DistrustCore](https://github.com/sakimidare/DistrustCore)。

## 桌面客户端（上游 EZ4Connect）

仓库根目录保留了上游 EZ4Connect 桌面客户端（Qt6），作为功能与兼容性参照，与本仓库核心配套使用。

- 上游项目：<https://github.com/chenx-dust/EZ4Connect>
- 该客户端沿用上游的「改进的 ZJU-Connect 图形界面」，绿色版分发：Windows 解压后双击
  `EZ4Connect.exe`，macOS 为 dmg，Linux 为 AppImage。
- 本仓库产出的包内核心为 `zju-connect v1.3.1-distrust`（含上述 DNS 修复）。如需自行替换，
  将编译好的 `zju-connect`（Windows 为 `zju-connect.exe`）放到 `EZ4Connect` 同目录即可。
- 桌面上游的使用方式、路线图与架构说明以其仓库文档为准（架构见
  [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md)）。

## 许可证

本项目遵循 [GNU General Public License Version 3](LICENSE) 开源。

## 致谢

- [Mythologyli/zju-connect](https://github.com/Mythologyli/zju-connect)
- [chenx-dust/EZ4Connect](https://github.com/chenx-dust/EZ4Connect)
- [Mythologyli/ZJU-Connect-for-Windows](https://github.com/Mythologyli/ZJU-Connect-for-Windows)

> 欢迎加入 HITSZ 开源技术协会 [@hitszosa](https://github.com/hitszosa)
