<div align="center">
  <img src="docs/icon.png" alt="App Icon" width="100" />
  <h1>RikkaHub · 大肥鱼版</h1>

  <p><b>一个加入「大肥鱼」桌面小组件的 RikkaHub Fork</b></p>

  <p>
    <a href="https://github.com/edngeng-debug/rikkahub">项目仓库</a> ·
    <a href="https://github.com/MeteorNOX/DeepSeek-Balance-Whale-Widget">大肥鱼组件原始仓库</a>
  </p>

  <p>基于 RikkaHub，尽量保持原版大肥鱼组件的外观与交互，同时针对 Android 场景进行适配。</p>
</div>

## 🐟 这个版本是什么？

这是一个个人维护的 RikkaHub Fork，核心目标很简单：

> **把大肥鱼尽量原封不动地放进 RikkaHub。**

在此基础上，仅针对 Android / RikkaHub 的实际使用环境进行必要适配，例如 WebView 承载、触摸区域、显示范围以及组件尺寸调节。

## ✨ 大肥鱼功能

- 🐟 在 RikkaHub 聊天界面显示大肥鱼
- 🎛️ 保留原组件的菜单交互
- 👁️ 支持隐藏菜单按钮
- 📐 提供组件大小调试入口，可分别调整：
  - 大肥鱼大小
  - 气泡大小
  - 字体大小
  - 菜单按钮大小
- 💾 调试尺寸设置保存在本地，重新打开后仍会保留
- 🔄 支持恢复默认尺寸
- 📱 针对 Android 全屏聊天界面进行适配

## 📱 使用方式

安装本 Fork 构建的 APK 后，进入 RikkaHub 的聊天页面即可看到大肥鱼。

大肥鱼的菜单中可以进行相关设置；如果文字、气泡或角色比例不合适，可以进入 **「组件大小 / 调试」** 手动调整。

如果希望恢复原始比例，使用 **「恢复默认」** 即可。

## 🔧 项目来源

本项目集成了现有的大肥鱼组件，并在此基础上独立完成 RikkaHub / Android 端的集成、适配与修改。

### RikkaHub Fork

**本项目仓库（当前使用）：**

👉 https://github.com/edngeng-debug/rikkahub

### 大肥鱼组件

**使用的大肥鱼组件原始仓库：**

👉 https://github.com/MeteorNOX/DeepSeek-Balance-Whale-Widget

感谢原作者 **MeteorNOX** 提供的大肥鱼组件。

## 🛠️ 技术说明

大肥鱼本体主要以 Web UI 形式运行，并由 Android `WebView` 承载。为了尽量避免改变原组件表现，本项目没有把它重新设计成一套原生 Compose UI，而是采用 **原组件 + Android WebView 适配层** 的方式集成。

目前的适配重点包括：

- Android WebView 本地资源加载
- 组件在聊天页面中的全屏布局
- Android 触摸事件区域处理
- 菜单状态同步
- 调试尺寸覆盖层
- 构建时处理 RikkaHub 原项目的 Firebase 构建依赖问题

## 🚀 构建

本项目使用 Android Studio / Gradle 构建。

项目同时提供 GitHub Actions 自动构建流程，用于生成 Debug APK。

如果你只是想使用而不是参与开发，建议直接使用 GitHub Actions 生成的构建产物。

## ⚠️ 注意事项

- 这是 RikkaHub 的 Fork，不代表 RikkaHub 官方项目。
- 大肥鱼组件来源于独立的上游项目。
- 本 Fork 的修改重点是集成与 Android 适配，并尽量减少对原组件的改动。
- Fork 与上游项目可能具有不同的版本、构建方式和问题，请根据对应仓库提交 Issue。

## 📜 致谢与署名

### RikkaHub

感谢 RikkaHub 原项目及其贡献者提供基础客户端。

### 大肥鱼组件

感谢 **MeteorNOX / DeepSeek-Balance-Whale-Widget** 提供原始组件。

### Android 适配与文档

**Android 集成适配、相关修改与本 README：ChatGPT 独立完成。**

> 本 README 中明确列出的两个仓库均为实际使用/来源仓库，请以对应仓库的许可证及版权声明为准。

## 📄 License

本项目主体遵循原 RikkaHub 项目的 **GNU Affero General Public License v3.0 (AGPL-3.0)**。集成的大肥鱼组件请同时遵循其上游仓库的许可证及版权要求。

---

<div align="center">
  <b>🐟 RikkaHub · 大肥鱼版</b><br />
  Made independently by ChatGPT
</div>
