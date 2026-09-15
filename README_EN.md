<div align="center">
  <img src="https://raw.githubusercontent.com/MeteorNOX/DeepSeek-Balance-Whale-Widget/master/assets/DSniang1.png" alt="DaFeiYu" width="180" />
  <h1>RikkaHub · DaFeiYu Edition</h1>

  <p><b>A RikkaHub fork integrating the DaFeiYu desktop widget</b></p>

  <p>
    <a href="README.md">简体中文</a> ·
    <a href="https://github.com/edngeng-debug/rikkahub">Project Repository</a> ·
    <a href="https://github.com/MeteorNOX/DeepSeek-Balance-Whale-Widget">Original DaFeiYu Repository</a>
  </p>

  <p>Built on RikkaHub while keeping the original DaFeiYu widget's appearance and interaction as intact as possible, with adaptations for Android.</p>
</div>

## 🐟 What is this?

This is a personal RikkaHub fork whose main goal is simple:

> **Bring DaFeiYu into RikkaHub with as little change to the original widget as possible.**

Only the adaptations required for Android / RikkaHub are added, including WebView hosting, touch handling, display bounds, and component-size controls.

## ✨ Features

- 🐟 DaFeiYu displayed directly in the RikkaHub chat interface
- 🎛️ Original widget menu interaction preserved
- 👁️ Option to hide the menu button
- 📐 Debug controls for DaFeiYu, bubble, font, and menu-button size
- 💾 Debug size settings are persisted locally
- 🔄 Restore default sizes with one tap
- 📱 Android full-screen chat layout adaptation

## 📱 Usage

Install an APK built from this fork and open a RikkaHub chat page. DaFeiYu will appear in the chat interface.

Open the DaFeiYu menu to access its settings. If the character, bubble, or text proportions do not look right, open **Component Size / Debug** and adjust them individually.

Use **Restore Defaults** to return to the original proportions.

## 🔧 Project Sources

This project integrates the existing DaFeiYu widget and independently implements the RikkaHub / Android integration, adaptation, and modifications.

### RikkaHub Fork

**Current project repository:**

👉 https://github.com/edngeng-debug/rikkahub

### DaFeiYu Widget

**Original widget repository used by this project:**

👉 https://github.com/MeteorNOX/DeepSeek-Balance-Whale-Widget

The original DaFeiYu artwork shown above is loaded from the upstream repository's `assets/DSniang1.png`.

## 🛠️ Technical Notes

The DaFeiYu widget is primarily a Web UI and is hosted inside an Android `WebView`. To preserve the original behavior, this project does not rebuild the widget as a native Compose UI. Instead, it uses the **original widget + an Android WebView adaptation layer**.

The main adaptation work includes:

- Local WebView resource loading
- Full-screen placement inside the chat page
- Android touch-event handling
- Menu-state synchronization
- Debug size controls
- Build-time handling of an original RikkaHub Firebase dependency issue

## 🚀 Building

The project can be built with Android Studio / Gradle.

GitHub Actions is also provided to automatically build Debug APKs.

If you only want to use the app rather than develop it, using a Debug APK produced by GitHub Actions is recommended.

## ⚠️ Disclaimer

- This is a RikkaHub fork and is not an official RikkaHub project.
- The DaFeiYu widget comes from a separate upstream project.
- The main changes in this fork are integration and Android adaptation, with the original widget kept as unchanged as practical.
- This fork and the upstream projects may use different versions, build systems, and issue trackers. Please report issues to the appropriate repository.

## 📜 Credits

### RikkaHub

Thanks to the RikkaHub project and its contributors for providing the base client.

### DaFeiYu Widget

Thanks to **MeteorNOX / DeepSeek-Balance-Whale-Widget** for the original widget.

### Integration & Documentation

**Android integration, adaptations, modifications, and this README: independently created by ChatGPT.**

> Both repositories explicitly listed in this README are actual project/source repositories. Please follow the respective licenses and copyright notices.

## 📄 License

The main project follows the original RikkaHub project's **GNU Affero General Public License v3.0 (AGPL-3.0)**. The integrated DaFeiYu widget remains subject to the license and copyright requirements of its upstream repository.

---

<div align="center">
  <b>🐟 RikkaHub · DaFeiYu Edition</b><br />
  Made independently by ChatGPT
</div>
