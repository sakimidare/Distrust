# Distrust Android

原生 Android 校园 VPN 客户端，包名：`idont.trust.atrust`。

## 设计目标

Distrust 提供两种互斥运行方式：

1. **本地代理模式（默认）**：不占用 Android `VpnService`，在
   `127.0.0.1` 暴露 SOCKS5/HTTP，供 Clash、Mihomo 或抓包软件作为上游代理。
2. **系统 VPN 模式**：由 Distrust 独占 `VpnService`，直接对设备流量进行校园网分流。

本地代理模式是 Android 上实现“aTrust + 梯子”和“aTrust + 抓包”的主要方式。

## 当前里程碑

- [x] Kotlin/Compose/Material 3 原生工程
- [x] 包名 `idont.trust.atrust`
- [x] 动态配色、深色模式和基础自适应布局
- [x] Android Keystore 加密凭据存储
- [x] DataStore 配置存储与配置校验
- [x] `VpnService` 授权、前台服务和 TUN 生命周期骨架
- [x] 本地代理前台服务边界
- [x] 连接状态、脱敏运行日志和通知停止操作
- [x] SOCKS5/HTTP 端口配置和 Mihomo 配置片段导出
- [x] 可选加载本地 `zju-connect.aar` 的 EasyConnect PoC 桥接
- [ ] Go mobile session/callback API
- [ ] aTrust 密码认证
- [ ] SOCKS5 与 HTTP 实际监听
- [ ] 图形验证码、短信、TOTP、RADIUS、CAS/OAuth2
- [ ] 动态路由、DNS、Fake IP 与服务端资源策略
- [ ] 多配置管理、导入导出和自动重连
- [ ] 内置 PCAP/PCAPNG 抓包

## 工具链

使用当前 Android Studio 稳定版能够支持的最新兼容组合：

- Android Gradle Plugin 9.3.1
- Gradle 9.5.0
- JDK 21
- compileSdk / targetSdk 37
- Kotlin 2.4.20
- Compose BOM 2026.08.00

不要单独把 AGP 升到 9.4，除非 Android Studio 已升级到明确支持 AGP 9.4 的版本。

## 构建

```bash
cd android
./gradlew assembleDebug
```

APK 输出位置：

```text
android/app/build/outputs/apk/debug/app-debug.apk
```

## Go 核心

将从固定上游提交构建的 AAR 放到：

```text
android/app/libs/zju-connect.aar
```

没有 AAR 时，应用 UI 仍可构建和运行，但连接会明确显示“未安装核心”，不会伪造成功状态。
当前兼容桥只声明上游真实具备的 EasyConnect VPN 能力；aTrust 和本地代理必须等新的移动
API 落地后才会对用户启用。

计划中的移动核心接口：

```text
NewSession(configJson, callbacks) -> sessionId
Prepare(sessionId) -> negotiatedConfig
SubmitAuth(sessionId, challengeId, response)
StartTun(sessionId, tunFd)
StartProxy(sessionId, socksAddress, httpAddress)
Stop(sessionId)
```

`negotiatedConfig` 必须返回客户端地址、MTU、路由、DNS 和服务端资源，禁止在生产实现中
硬编码学校网段或 DNS。
