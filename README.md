# Distrust

Distrust 是从 [EZ4Connect](https://github.com/chenx-dust/EZ4Connect) 衍生的校园 VPN 客户端项目，
同时包含原生 Android 客户端与桌面客户端。Android 客户端使用 Kotlin、Jetpack Compose、
Material 3 和 `VpnService` 原生构建，包名为 `idont.trust.atrust`。

## 仓库结构

| 路径 | 说明 |
| --- | --- |
| [`android/`](android/) | 原生 Android 客户端。开发状态、核心集成与构建方式见 [`android/README.md`](android/README.md) |
| [`android/core`](https://github.com/sakimidare/DistrustCore) | 核心子模块 **DistrustCore**，基于 [Mythologyli/zju-connect](https://github.com/Mythologyli/zju-connect) |
| 仓库根目录 | 原 EZ4Connect 桌面客户端（Qt6），保留作为功能与兼容性参照，并使用本仓库维护的核心 |

> Android 版本仍处于早期开发阶段；桌面端沿用 EZ4Connect 的成熟结构，配套使用本仓库维护的核心。

## 核心（DistrustCore）

核心在上游 zju-connect 基础上，针对**校外访问校园网**增强了服务端资源感知的 DNS 解析：

- 多来源并发解析：策略 DNS、`policy-secondary`（经隧道查询第二个策略 DNS）、系统 DNS、历史成功地址、自定义 DNS；
- 命中服务端下发 IP Resource 的地址优先，避免解析到公网 WAF 地址；
- `policy-secondary` 修复了校外**无法解析校园域名**的问题。部分学校（如 SEU）的第一个策略 DNS 校外不可直达，只有经隧道查询第二个策略 DNS 才能得到正确地址；
- 桌面 CLI（`main.go`）与 Android 端使用同一套策略。

构建与同步上游说明见 [DistrustCore](https://github.com/sakimidare/DistrustCore)。

## 桌面客户端（原 EZ4Connect）

*前身为 HITsz Connect for Windows*

![Action](https://github.com/chenx-dust/EZ4Connect/actions/workflows/build.yml/badge.svg)
![Release](https://img.shields.io/github/v/release/chenx-dust/EZ4Connect)
![Downloads](https://img.shields.io/github/downloads/chenx-dust/EZ4Connect/total)
![License](https://img.shields.io/github/license/chenx-dust/EZ4Connect)

改进的 ZJU-Connect 图形界面

## 🎉 现已正式提供 aTrust 支持

如使用中遇到问题，可加入 ZJU-Connect 用户反馈 QQ 群 1037726410 交流。

<div align="center">
<img src="docs/main.png" width="600px">
</div>

## 使用方式

在本项目的 [Releases](https://github.com/sakimidare/Distrust/releases) 页面下载构建产物；界面亦可从上游 [EZ4Connect Releases](https://github.com/chenx-dust/EZ4Connect/releases) 获取（均为绿色版，Windows 解压后双击 `EZ4Connect.exe`）：

> 桌面端配套的核心为 `zju-connect v1.3.1-distrust`（含上述校园 DNS 修复）。如需自行替换，将编译好的 `zju-connect`（Windows 为 `zju-connect.exe`）放到 `EZ4Connect` 同目录即可。

- **Windows 用户**：下载 `EZ4Connect-vX.X.X-windows-ARCH.zip` ，解压至同一目录下，双击运行 `EZ4Connect.exe` ；
  - 如果遇到缺少 DLL 等问题，请先下载安装 Microsoft Visual C++ 可再发行程序包版本（[x64](https://aka.ms/vs/17/release/vc_redist.x64.exe) | [arm64](https://aka.ms/vs/17/release/vc_redist.arm64.exe)），再运行程序；
- **macOS 用户**：下载 `EZ4Connect-vX.X.X-macOS-ARCH.dmg` ，将 EZ4Connect 移动到应用程序目录中；
  - ~~如果遇到“Apple 无法检查 App 是否包含恶意软件”等报错，请参考 [Apple 支持](https://support.apple.com/zh-cn/guide/mac-help/mchleab3a043/mac) 进行操作。~~本软件已通过 Apple 官方公证，可直接运行；
- **Linux 用户**：下载 `EZ4Connect-vX.X.X-linux-ARCH.AppImage` ，赋予执行权限，运行即可；
  - AppImage x64 仅支持系统 `glibc >= 2.31` 的发行版，Ubuntu 22.04 及以上版本可以正常运行（受限于 GitHub Actions Runner）；
  - AppImage arm64 仅支持系统 `glibc >= 2.38` 的发行版，Ubuntu 24.04 及以上版本可以正常运行（受限于 Qt 官方：[参考](https://doc.qt.io/qt-6/supported-platforms.html)）；
  - Arch Linux 用户推荐使用 [AUR](https://aur.archlinux.org/packages/ez4connect) 安装；
  - 如果遇到因依赖问题无法运行的情况，请自行编译运行。

1. 跟随配置向导进行服务器与账户设置，或在“设置”中手动配置；

2. 在主界面中点击“连接服务器”。如果只需进行校园网页浏览，则选择“设置系统代理”后即可使用。

如果需要配合 Clash / Mihomo 进行高级的分流操作，可以参见： [高级使用方式](docs/ADVANCED_USAGE.md)

## 路线图

如有更多好的建议，可以在 Issue 中或是 OSA 群里提出！

- [X] 支持 macOS 系统
- [X] 支持 Linux 系统
- [X] 支持手动设置 Proxy Bypass
- [X] 上传 AUR 包
- [ ] 使用密钥链存储密码等信息

## 开发

项目的分层、依赖方向和新增代码归属规则见
[架构说明](docs/ARCHITECTURE.md)。

## 许可证

本项目遵循 [GNU General Public License Version 3](LICENSE) 开源。

## 致谢

- [Mythologyli/ZJU-Connect-for-Windows](https://github.com/Mythologyli/ZJU-Connect-for-Windows)
- [Mythologyli/zju-connect](https://github.com/Mythologyli/zju-connect)

> 欢迎加入 HITSZ 开源技术协会 [@hitszosa](https://github.com/hitszosa)
