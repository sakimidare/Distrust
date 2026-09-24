# EZ4Connect 项目调研及 Android 迁移可行性报告

**调研日期：** 2026 年 9 月 24 日  
**调研对象：** EZ4Connect（本地提交 `c56e1673efa1c3c41f18c51092a05d9e7d8298a0`，最新标签 `v1.9.1`）  
**报告结论：** 有条件可行，建议“原生 Android 客户端 + Go 核心 AAR + Android VpnService”，不建议直接移植 Qt 桌面应用。

---

## 1. 执行摘要

EZ4Connect 是面向 EasyConnect/aTrust 校园 VPN 场景的跨平台桌面图形客户端。项目本身不实现 VPN 协议，而是负责配置管理、认证交互、连接状态、日志、系统代理和更新检查，并通过 `QProcess` 启动上游 `zju-connect` Go 核心。当前支持 Windows、macOS、Linux，代码采用 Qt 6/C++17 和轻量分层架构。

Android 迁移在技术上可行，但不能理解为“把 Qt 界面交叉编译成 APK”。Android 没有桌面式 sudo、系统托盘和可自由修改的全局系统代理；后台运行、VPN 授权、TUN 获取和网络绕行均由 Android 框架约束。因此需要将桌面的“外部核心进程 + 系统提权 + 系统代理”模式改造成“应用内 Go 核心 + `VpnService` + 前台服务通知”。

上游 `zju-connect` 已提供 `gomobile bind` 生成 Android AAR 的构建流程、Android 专用 TUN 栈和一个历史 Android 原型，这显著降低了基础版风险。但现有移动 API 只支持基础 EasyConnect 用户名/密码登录，接口仅有 `Login`、`Logout`、`StartStack`，尚未覆盖 aTrust、CAS/OAuth2、短信/RADIUS/TOTP、图形验证码、证书、动态路由/DNS、细粒度配置、结构化错误和完整生命周期。因此：

- **EasyConnect 基础 MVP：可行性高。** 可复用现有 AAR 验证链路，预计 7～11 人周。
- **达到 EZ4Connect 桌面版主要能力：可行性中等。** 需要先扩展 Go 移动 API，预计 19～31 人周。
- **100% 逐项照搬桌面功能：不合理。** 系统代理、托盘、sudo、任意额外命令行参数等功能应在 Android 上删除、替换或重新定义。

建议先进行 2～3 人周的技术验证门禁，只有在真机上完成 EasyConnect/aTrust 登录、动态分流、DNS、断网重连和后台保活后，再进入正式产品开发。

## 2. 调研范围与方法

本次调研包括：

1. 阅读项目 README、架构文档、CMake 配置、核心领域模型、连接会话、进程适配、认证协调器、配置存储、系统代理和 CI。
2. 统计项目规模及测试资产，并在当前 Linux 环境执行完整桌面构建和测试。
3. 检查上游 `Mythologyli/ZJU-Connect` 的 Android 移动绑定、Android TUN 实现和构建工作流。
4. 检查历史项目 `Mythologyli/ZJUConnectForAndroid` 的 `VpnService` 原型。
5. 对照 Android 官方 VPN 开发模型，分析权限、服务生命周期、路由、DNS、网络回环和发布约束。

本报告是代码与架构层面的可行性评估，未连接真实 VPN 服务端进行协议联调，也未覆盖所有厂商 ROM 的后台策略。因此总体结论置信度为“中等”，生产排期必须保留真机与真实网络验证缓冲。

## 3. 项目现状

### 3.1 项目定位

EZ4Connect 是 `zju-connect` 的桌面 GUI 和运行编排层，典型工作流为：

1. 用户选择配置并输入账号、密码或其他认证信息。
2. UI 将 INI 配置映射为强类型 `ConnectionProfile`。
3. `CoreCommandBuilder` 将配置转换为命令行参数和敏感环境变量。
4. `ZjuConnectProcess` 通过 `QProcess` 启动外部 `zju-connect`。
5. 程序解析核心标准输出中的交互提示，弹出验证码、短信、TOTP、RADIUS、SSO 等窗口并回写标准输入。
6. 连接建立后可启用本地 SOCKS5/HTTP 代理、桌面系统代理或 TUN。

这意味着 Android 迁移的协议能力主要取决于上游 Go 核心能否以库方式暴露，而不是取决于现有 C++ UI 能否编译。

### 3.2 技术栈与规模

| 项目 | 当前情况 |
|---|---|
| 语言与标准 | C++17 |
| GUI | Qt 6 Widgets、Qt WebEngineWidgets、QSS |
| 构建 | CMake 3.12+ |
| 核心集成 | 外部 `zju-connect` 可执行文件 + `QProcess` |
| 持久化 | `QSettings` INI、多配置文件 |
| 网络 | Qt Network；VPN 协议由 Go 核心承担 |
| 桌面平台 | Windows x64/arm64、macOS x64/arm64、Linux x64/arm64 |
| 许可证 | EZ4Connect：GPL-3.0；上游核心：AGPL-3.0 |
| 版本状态 | 最新标签 `v1.9.1`；当前提交日期 2026-09-08 |
| 仓库历史 | 392 个提交 |
| 代码规模 | 约 15,179 行 C++/头文件/UI，约 196 个文件，仓库约 16 MB |
| 自动化测试 | 15 个 CTest 测试目标 |

### 3.3 架构评价

项目采用如下分层：

```text
presentation -> application -> core
       |              ^
       v              |
infrastructure -------+
```

优点：

- `ConnectionProfile` 已将凭据、端点、DNS、代理、隧道、行为和调试选项结构化。
- `ConnectionSession` 与具体 `QProcess` 实现解耦，包含连接、停止、重连和错误状态机。
- `CoreProcess`、`SystemProxyBackend`、`ProfileBackend` 均有端口抽象，便于测试。
- Coordinator 已从 `MainWindow` 中拆出连接和认证流程，迁移需求可据此整理。
- 参数构建、输出解析、配置迁移、配置文件管理均有单元测试。

限制：

- `core/` 模型仍直接依赖 Qt 类型（如 `QString`），不能直接作为纯 C++ 跨平台领域库复用到 Kotlin。
- 认证流程依赖解析英文控制台文本，接口脆弱；上游提示文本变化会影响客户端。
- 桌面 UI 和配置项较多，部分选项本质上是命令行核心的直接透传，不适合移动产品。
- 平台层包含大量 Windows API、`networksetup`、`gsettings`/KDE 命令、sudo 和系统托盘假设。

### 3.4 主要功能

| 功能域 | 当前能力 |
|---|---|
| 协议 | EasyConnect、aTrust |
| 认证 | 用户名/密码、p12/pfx 证书、图形验证码、短信、TOTP、RADIUS 动态码、CAS/OAuth2 SSO |
| 连接 | 启停、自动重连、连接状态、核心错误映射、自动选线、保活 |
| 网络 | SOCKS5、HTTP、Shadowsocks、直连代理、TUN、路由、DNS 劫持、Fake IP、网口绑定 |
| 高级 | TCP/UDP 端口转发、自定义 DNS、强制代理域名、额外核心参数 |
| 配置 | 多配置、新建/切换/重命名/删除、导入/导出、配置迁移 |
| 桌面集成 | 系统代理、托盘、开机启动、静默启动、通知 |
| 运维 | UI/核心更新检查、日志、PCAP、TLS key log、设备授信管理 |

### 3.5 构建与质量验证

本次使用 Release 配置执行：

```text
cmake -DBUILD_TESTING=ON -DCMAKE_BUILD_TYPE=Release ...
cmake --build ...
ctest --output-on-failure
```

结果为主程序构建成功，15/15 测试通过，总测试运行时间约 0.17 秒。说明当前提交至少在本调研环境中具备良好的基础可构建性，且纯逻辑边界已有一定回归保护。

不足是测试主要集中在单元和轻量 UI 边界，尚未看到真实核心二进制、真实 VPN 服务端、长时间连接、丢网切网、DNS 泄漏或系统级路由的端到端测试。Android 项目应补足这些测试层次。

## 4. Android 平台差异

| 桌面实现 | Android 对应方式 | 迁移判断 |
|---|---|---|
| `QProcess` 启动核心 | Go AAR/JNI 进程内调用 | 必须重构，不建议执行随包二进制 |
| sudo/管理员权限创建 TUN | `VpnService.prepare()` + `Builder.establish()` | 可替代，用户需首次授权 |
| Wintun/Linux/macOS TUN | Android 提供 TUN 文件描述符 | 可替代，需把 FD 交给 Go 栈 |
| 修改全局系统代理 | `VpnService` 路由设备流量 | 功能语义应替换，普通应用不能照搬桌面方式 |
| 系统托盘 | 前台服务常驻通知、快捷设置/应用入口 | 必须重做 |
| 开机自启动/静默启动 | Always-on VPN、受限的启动策略 | 不能简单等价；应优先支持系统 Always-on |
| Qt WebEngine SSO | Custom Tabs 或 Android WebView/深链回调 | 需要重做，优先 Custom Tabs |
| QSettings INI | DataStore/Room + Android Keystore | 可迁移，敏感信息必须单独保护 |
| Qt Widgets/QSS | Jetpack Compose + Material 3 | 建议重写，不建议桌面布局直搬 |
| 外部文件路径 | Storage Access Framework | 证书导入、日志导出需适配 URI 权限 |
| 自动更新二进制 | 应用商店/F-Droid/GitHub Release | 核心应随 APK/AAB 版本发布，避免运行时替换原生库 |

Android 官方 VPN 模型要求应用继承 `VpnService`，通过系统授权创建本地 TUN，使用前台服务和不可随意移除的活动通知，并对隧道自身连接执行网络绕行以防流量回环。同一用户/工作资料通常只能有一个活动 VPN，这也意味着 EZ4Connect 无法像桌面版那样与另一个 TUN VPN 工具随意并行。

## 5. 可复用资产与关键缺口

### 5.1 可复用资产

上游 `zju-connect` 当前仓库包含以下 Android 基础设施：

- `mobile/mobile_android.go`：可由 gomobile 导出 AAR。
- `.github/workflows/build-android.yml`：使用 Go 1.25、gomobile、Android API 24 和 NDK 生成 `zju-connect.aar`。
- `stack/tun/stack_android.go`：接收 Android TUN 文件描述符，在 TUN 和 VPN L3 连接之间转发 IPv4 TCP/UDP 包。
- 历史 `ZJUConnectForAndroid` 原型：已证明 Java `VpnService` 可调用 `Mobile.login()` 和 `Mobile.startStack(fd)` 建立基础 EasyConnect 通道。

这些资产使“协议完全重写”的必要性大幅降低，是本项目 Android 可行性的最重要正面因素。

### 5.2 当前移动 API 的能力边界

现有移动 API 实际只有：

```text
Login(server, username, password) -> client IP 字符串
DebugLogin(server, username, password) -> client IP 字符串
Logout()
StartStack(tunFd)
```

主要缺口如下：

1. **仅创建 `easyconnect.Client`，没有 aTrust 移动入口。**
2. **没有认证事件回调。** 无法可靠承载验证码、短信、TOTP、RADIUS、CAS/OAuth2 和证书交互。
3. **没有结构化错误。** 登录失败只返回空字符串，无法区分密码错误、服务不可达、认证过期等情况。
4. **没有动态配置输出。** 客户端拿不到服务端下发路由、DNS、MTU 和资源策略。
5. **Android 原型硬编码** `10.0.0.0/8`、`10.10.0.21` 和 MTU 1400，不能覆盖不同学校和 aTrust 策略。
6. **没有网络保护接口。** Android 官方要求隧道底层 socket 绕过 VPN，现有 AAR 没有暴露 `VpnService.protect()` 回调或 Android `Network` 绑定能力，存在回环和切网风险。
7. **全局单例和阻塞式栈。** 不利于可靠取消、重复连接、并发状态读取和服务重建。
8. **Android 专用栈功能较窄。** 当前仅转发 IPv4 TCP/UDP，`AddRoute`、DNS resolver 和 IP pool 在 Android 实现中为空操作。

### 5.3 历史 Android 原型的参考价值

历史 Android 仓库最后提交于 2024-07-28，版本 `0.1.0`，可作为 PoC 参考，但不适合作为生产代码基线：

- UI 仅有服务器固定为 ZJU、用户名和密码。
- 密码明文保存于 `SharedPreferences`。
- `VpnService` 没有在服务内部正确转为前台服务。
- 使用已弃用的 `LocalBroadcastManager`。
- 路由和 DNS 硬编码，缺少 aTrust 及复杂认证。
- 服务重启、授权撤销、网络切换、错误恢复和资源释放不完整。
- AAR 作为手工文件依赖，版本追踪和供应链管理不足。

因此应复用“技术路径”，而不是直接续写其应用代码。

## 6. 迁移方案对比

| 方案 | 描述 | 优点 | 主要问题 | 建议 |
|---|---|---|---|---|
| A. Qt 直接移植 | Qt for Android 继续使用 C++/Widgets | 表面代码复用较多 | WebEngineWidgets、QProcess、系统托盘、平台命令均不适配；桌面 UI 体验差；最终仍需 Java/Kotlin `VpnService` 桥接 | 不推荐 |
| B. APK 内运行 CLI | 打包 Android 可执行核心并由应用启动 | 可沿用命令行协议 | Android 进程、SELinux、后台和 ABI 管理复杂；交互仍依赖文本；TUN FD 与 socket 保护难处理 | 不推荐 |
| C. 原生 Android + Go AAR | Kotlin/Compose UI，Go 核心以库方式嵌入 | 符合平台模型；已有 AAR/TUN 基础；便于生命周期和权限管理 | 必须扩展移动 API，处理 AGPL 和双语言调试 | 推荐 |
| D. Kotlin 重写协议 | 全部协议和数据平面重写 | Android 代码单一 | 协议复杂、风险和维护成本最高，与上游演进脱节 | 不推荐 |

推荐方案 C，并将 Android 项目作为独立应用仓库或本仓库中的独立 Gradle 工程维护。不要让 Android 工程依赖桌面 Qt/CMake 构建，也不要试图在一套 UI 代码中兼容桌面和移动端。

## 7. 推荐目标架构

```text
Jetpack Compose UI
    |
ViewModel / Use Cases / StateFlow
    |
ConnectionRepository -------- ProfileRepository
    |                               |
EZ4VpnService                  DataStore / Room
    |                               |
GoCoreBridge (AAR)             Android Keystore
    |
zju-connect mobile API
    |
Android TUN FD <-> VPN protocol connection
```

### 7.1 Android 应用层

- Kotlin + Jetpack Compose + Material 3。
- 单向数据流：UI 只订阅 `StateFlow<ConnectionState>`，连接状态由服务统一持有。
- Activity 只负责 VPN 授权、配置和导航，不持有真实连接。
- 认证请求使用显式事件通道，由 UI 提交结构化响应，避免解析日志文本。

### 7.2 VPN 服务层

- `EZ4VpnService : VpnService` 是唯一连接所有者。
- 启动后立即进入前台并显示持续通知，通知提供“断开”和“打开应用”操作。
- 调用 `VpnService.prepare()` 获取授权；`Builder` 动态配置地址、路由、DNS、MTU 和应用范围。
- 正确处理 `onStartCommand()`、`onRevoke()`、进程重建、重复启动和幂等停止。
- 监听默认网络变化，向 Go 核心提供底层 `Network` 或 socket protect 能力，并在 Wi-Fi/蜂窝切换后重连。

### 7.3 Go 移动桥接层

建议把现有四个函数升级为会话式 API：

```text
NewSession(configJson, callbacks) -> sessionId
Prepare(sessionId) -> negotiatedConfig
SubmitAuth(sessionId, challengeId, response)
Start(sessionId, tunFd)
Stop(sessionId)
GetState(sessionId) -> structured state
```

其中 `negotiatedConfig` 至少应返回客户端地址、前缀、MTU、IPv4/IPv6 路由、DNS、分流资源和推荐模式；回调至少应包含状态、日志、结构化错误和认证挑战。底层拨号必须支持由 Android 提供网络绕行能力。

### 7.4 数据与安全

- 非敏感配置使用 Proto DataStore；配置数量和关系复杂时可使用 Room。
- 密码、TOTP 密钥和证书密码使用 Android Keystore 保护的加密存储，不进入日志、Intent 或普通备份。
- 证书通过 Storage Access Framework 导入到应用私有目录，并谨慎持有 URI 权限。
- 默认关闭 PCAP 和 TLS key log；开启时必须二次确认、限制文件权限，并提供显著敏感数据提示。
- 日志进行账号、手机号、SID、token、URL 查询参数等脱敏。

## 8. 功能迁移矩阵

| 桌面功能 | Android 目标 | 难度 | 建议阶段 |
|---|---|---:|---|
| EasyConnect 用户名/密码 | Go AAR + `VpnService` | 中 | MVP |
| 基础分流 TUN | 动态 `Builder.addRoute()` | 中 | MVP |
| 连接/断开/状态 | 前台服务 + StateFlow | 中 | MVP |
| 多配置 | DataStore/Room | 低 | MVP |
| 自动重连 | 服务状态机 + 网络回调 | 中 | MVP |
| 日志查看/导出 | 应用内日志 + SAF | 低 | MVP |
| DNS | 动态服务端 DNS + 本地策略 | 高 | MVP 后 |
| 图形验证码/短信/TOTP/RADIUS | Go 认证事件回调 + Compose 对话框 | 高 | 第二阶段 |
| EasyConnect 证书 | SAF + Go API | 中高 | 第二阶段 |
| aTrust 密码登录 | 新增 aTrust mobile session | 高 | 第二/三阶段 |
| CAS/OAuth2 SSO | Custom Tabs/深链 + Go 回调 | 高 | 第三阶段 |
| Fake IP/域名分流 | 扩展 Android 栈和 DNS | 很高 | 第三阶段 |
| Always-on VPN | 系统能力适配 | 中 | 稳定版 |
| 每应用 VPN | allowed/disallowed app 列表 | 中 | 可选 |
| SOCKS5/HTTP 本地代理 | 仅保留有明确移动需求的模式 | 中 | 可选 |
| Shadowsocks/直连代理组合 | 需重新评估与其他 Android VPN 冲突 | 高 | 延后 |
| TCP/UDP 端口转发 | 移动场景价值有限 | 中高 | 延后/删除 |
| 系统代理设置 | 由 `VpnService` 替代 | — | 删除 |
| 系统托盘 | 前台通知替代 | — | 替换 |
| sudo | VPN 授权替代 | — | 删除 |
| 任意额外 CLI 参数 | 强类型配置替代 | — | 删除 |
| UI/核心独立更新 | APK/AAB 整体发布 | — | 替换 |

## 9. 关键风险

### 9.1 aTrust 与复杂认证：高风险

桌面版的重要差异化能力是 aTrust，但上游 Android mobile 包当前只实例化 EasyConnect 客户端。aTrust 的会话缓存、登录域、密码/短信/CAS/OAuth2、RADIUS、图形验证码、SID 刷新和设备授信均需以库 API 重新设计。若上游不接受这些移动接口，Android 项目将长期维护核心分叉。

**缓解措施：** 先向上游提交最小、通用、平台无关的 session/callback API 设计；PoC 必须包含目标学校真实 aTrust 登录，而不能只验证 ZJU EasyConnect。

### 9.2 路由与 DNS 正确性：高风险

历史原型固定使用 `10.0.0.0/8` 和 `10.10.0.21`，而桌面核心已支持服务端策略、域名资源、自动 DNS、备用 DNS、DNS 劫持和 Fake IP。Android `VpnService.Builder` 的路由在 TUN 建立前配置，策略变化通常要求重建 TUN。

**缓解措施：** Go 核心在建立 TUN 前返回协商结果；定义策略变化后的可控重建流程；增加 DNS 泄漏、IPv4/IPv6、分流命中和多学校配置测试。

### 9.3 VPN 回环和网络切换：高风险

隧道自己的 socket 若进入同一 VPN，会形成递归。Android 官方建议使用 `VpnService.protect()`，同时移动设备会频繁在 Wi-Fi 和蜂窝网络间切换。

**缓解措施：** 在移动桥接中引入 Android 提供的 socket 保护/底层网络绑定接口；在飞行模式、锁屏、切网、Captive Portal 和弱网条件下进行长时间测试。

### 9.4 后台存活与厂商 ROM：中高风险

Android 8+ 对后台服务有限制，VPN 必须以前台服务运行；不同厂商还可能额外限制后台和自启动。

**缓解措施：** 严格按 `VpnService` 生命周期实现，提供持续通知和系统 Always-on 指引，不使用不透明的保活技巧；建立主流 ROM 真机矩阵。

### 9.5 安全与隐私：高风险

桌面路线图已注明尚未使用密钥链，当前设置可包含密码和 TOTP；历史 Android 原型还将密码明文放入 `SharedPreferences`。VPN 应用可接触设备流量，安全要求高于普通工具。

**缓解措施：** 将安全存储、日志脱敏、备份排除、依赖审计、证书校验和隐私说明列为 MVP 的完成条件，而非发布前补丁。

### 9.6 许可证与分发：中高风险

EZ4Connect 为 GPL-3.0，上游 Go 核心为 AGPL-3.0。将 AAR 静态/动态集成到同一 APK 后，整体分发义务需按 AGPL 兼容边界审查；应用商店发布还需要满足 VPN 服务相关声明和隐私要求。

**缓解措施：** 发布前进行许可证审查；保留完整对应源代码、构建脚本、修改说明和许可证文本；不要把闭源 SDK 无审查地链接到受 AGPL 约束的组合程序。

### 9.7 上游接口稳定性：中风险

桌面版通过 CLI 参数和输出文本与核心耦合，Android 则会直接依赖 Go 导出 API。若 API 没有版本化，核心升级可能导致应用不可用。

**缓解措施：** AAR 使用固定版本或提交哈希；定义 `ApiVersion`、能力查询和兼容策略；Android CI 中从源码可重复构建 AAR。

## 10. 工作量与里程碑

以下估算以 1 名 Android 主程、1 名 Go/网络工程师和兼职测试/设计为基准，不包含未知服务端兼容问题。

| 阶段 | 主要交付 | 估算 |
|---|---|---:|
| P0 技术验证 | AAR 可重复构建；真机 TUN；EasyConnect/aTrust 各至少一个目标环境；socket protect；动态路由/DNS验证 | 2～3 人周 |
| P1 Android 基础 | Compose 工程、导航、配置、Keystore、VPN 授权、前台通知、状态机 | 3～5 人周 |
| P2 EasyConnect MVP | 登录、分流、DNS、重连、错误、日志、基础多配置、真机回归 | 4～6 人周 |
| P3 认证与配置扩展 | 验证码、短信、TOTP、证书、导入导出、认证回调协议 | 3～5 人周 |
| P4 aTrust 与高级网络 | aTrust、CAS/OAuth2、RADIUS、会话刷新、动态策略、Fake IP（如保留） | 6～10 人周 |
| P5 稳定化与发布 | 切网/锁屏/弱网、性能、电量、兼容矩阵、许可证与发布材料 | 4～6 人周 |

**EasyConnect MVP：** 约 7～11 人周，双人并行约 5～7 个自然周。  
**主要功能对等版：** 约 19～31 人周，双人并行约 10～16 个自然周。  
**排期缓冲：** 对 aTrust、多学校策略和厂商 ROM 建议额外预留 20%～30%。

## 11. PoC 验收门禁

正式立项前建议必须通过以下门禁：

1. arm64-v8a 真机可从源码生成并加载 AAR，CI 可重复产物。
2. 至少一个 EasyConnect 环境完成登录、访问目标网段、DNS 和断开。
3. 至少一个目标 aTrust 环境完成实际使用的认证方式；若暂不支持，需明确 MVP 不含 aTrust。
4. 隧道 socket 确认绕过 VPN，无递归回环；Wi-Fi/蜂窝切换后行为可预期。
5. 路由和 DNS 来自服务端/配置协商，不使用硬编码学校参数。
6. 熄屏 30 分钟、后台 2 小时、断网恢复、VPN 授权撤销均无崩溃和资源泄漏。
7. 密码/TOTP 不以明文出现在 SharedPreferences、日志、Intent、崩溃信息和系统备份中。
8. 完成至少 Chrome、系统 WebView、常用 IM 和目标校园应用的访问验证。
9. 明确 AGPL 源码提供和发布方案。

任一核心门禁失败，都应先修复 Go 移动接口或缩小产品范围，而不是继续堆叠 UI 功能。

## 12. 最终结论与建议

### 12.1 可行性结论

| 目标 | 可行性 | 结论 |
|---|---|---|
| Android 基础 EasyConnect 客户端 | 高 | 已有 AAR、Android TUN 栈和历史 PoC，路径明确 |
| 覆盖常见认证并达到可日用水平 | 中高 | 需扩展结构化事件、错误、动态路由/DNS和服务生命周期 |
| 完整覆盖 EZ4Connect 当前 aTrust 能力 | 中 | 技术上可做，但移动 API 需实质开发，是真正关键路径 |
| 直接将现有 Qt 程序编译到 Android | 低 | 平台假设和 UI 模型不匹配，节省的表面工作会转化为长期维护成本 |
| 与桌面版逐项 100% 等价 | 低且无必要 | 多项桌面功能在 Android 上应替换或删除 |

### 12.2 推荐决策

建议立项，但采用分阶段、门禁式推进：

1. **先做 P0，不承诺完整产品日期。** 用真实 EasyConnect/aTrust 环境验证最难的网络与认证路径。
2. **选用 Kotlin/Compose + `VpnService` + Go AAR。** 不直接移植 Qt Widgets，不在 APK 内运行桌面 CLI。
3. **优先把移动 API 建设贡献给上游。** 以 session、callback、structured error、negotiated config 和 network protector 为核心。
4. **MVP 控制范围。** 首版聚焦连接、分流、DNS、重连、多配置、安全存储和日志；系统代理、托盘、sudo、任意 CLI 参数明确删除。
5. **将 aTrust 单独设为里程碑。** 如果目标用户主要依赖 aTrust，则 aTrust PoC 必须前置，不能在 EasyConnect MVP 完成后才验证。
6. **安全和许可证前置。** Keystore、日志脱敏、备份排除和 AGPL 合规应进入架构设计与 CI，而不是发布收尾工作。

综合判断：**Android 迁移值得做，也具备现实技术基础；但应被定义为“共享 Go 核心的新 Android 客户端”，而不是“现有 Qt 桌面程序的平台移植”。**

## 13. 主要依据

### 13.1 本项目文件

- `README.md`：产品定位、支持平台、使用方式和许可证。
- `docs/ARCHITECTURE.md`：分层、依赖方向和主要流程。
- `CMakeLists.txt`：Qt 组件、平台构建和 15 个测试目标。
- `core/connectionprofile.h`：连接配置领域模型。
- `application/connectionsession.cpp`：连接、停止、重连和交互输入。
- `infrastructure/coreprocess/zjuconnectprocess.cpp`：外部核心进程、sudo 和输出事件。
- `infrastructure/coreprocess/corecommandbuilder.cpp`：核心参数和敏感环境变量。
- `infrastructure/platform/systemproxyfunctions.cpp`：三类桌面平台系统代理实现。
- `presentation/coordinators/authdialogcoordinator.cpp`：验证码、短信、TOTP、RADIUS 和 SSO。
- `application/defaultsettings.cpp`、`infrastructure/settings/settingsprofileloader.cpp`：完整配置面。

### 13.2 外部依据

- ZJU Connect 上游：<https://github.com/Mythologyli/ZJU-Connect>
- 上游 Android AAR 工作流：<https://github.com/Mythologyli/ZJU-Connect/blob/main/.github/workflows/build-android.yml>
- 历史 Android 原型：<https://github.com/Mythologyli/ZJUConnectForAndroid>
- Android VPN 开发指南：<https://developer.android.com/develop/connectivity/vpn>
- Android `VpnService` API：<https://developer.android.com/reference/android/net/VpnService>

> 注：外部仓库分析基于调研日获取的主分支快照；上游 `zju-connect` 快照提交为 `4031c52214478d4082189b778ffc6e9744f1d256`（2026-09-22），历史 Android 原型提交为 `d8f842d1017652abceb8b5b21fea16c08ed9d41e`（2024-07-28）。
