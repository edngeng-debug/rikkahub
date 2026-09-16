<div align="center">
  <img src="https://raw.githubusercontent.com/MeteorNOX/DeepSeek-Balance-Whale-Widget/master/assets/DSniang1.png" alt="大肥鱼" width="180" />
  <h1>RikkaHub · 大肥鱼版</h1>

  <p><b>一个加入「大肥鱼」的 RikkaHub Fork</b></p>

  <p>
    <a href="README_EN.md">English</a> ·
    <a href="https://github.com/edngeng-debug/rikkahub">项目仓库</a> ·
    <a href="https://github.com/MeteorNOX/DeepSeek-Balance-Whale-Widget">大肥鱼组件原始仓库</a>
  </p>

  <p>基于 RikkaHub，尽量保留大肥鱼原有的灵魂，同时针对 Android / WebView 做必要适配。</p>
</div>

## 🐟 这个版本是什么？

这是一个个人维护的 RikkaHub Fork，目标是把大肥鱼集成进 RikkaHub，同时尽可能少改动原组件的核心表现。

## ✨ 当前功能

- 🐟 RikkaHub 聊天页面显示大肥鱼
- 🖱️ 支持触摸拖动与屏幕范围内移动
- 🧲 支持吸附 / 镜像等相关交互
- 💬 保留大肥鱼的气泡、角色、音效、用量提示等能力
- ⚙️ 将主要设置集中到 RikkaHub 的「扩展 → 大肥鱼」页面
- 🔢 金额、时间、滚动间隔等非百分比参数使用数字输入框
- 💾 设置保存到本地
- 🚫 不再提供大肥鱼本体菜单
- 🚫 不再提供大小设置功能

## 📖 这条鱼到底经历了什么

> **以下内容纯属开发实录。鱼是真的，痛苦也是真的。**

这个移植最开始看起来非常简单：把一个 Web 小组件塞进 RikkaHub 的 WebView 里，然后让它能动。

事实证明，“能显示”和“能正常显示”之间，隔着一整片海。

### 第一阶段：鱼游进了 RikkaHub

首先解决的是最基础的集成：

`大肥鱼 Web UI → Android WebView → RikkaHub ChatPage`

看起来成功了。

然后问题开始出现。

### 第二阶段：鱼突然瞬移

拖动功能第一次真正接上以后，鱼开始拥有了一个神奇能力：

> **你拖它，它不一定走；它可能直接瞬移到左上角。**

原因并不是单纯的坐标算错，而是 WebView 的坐标系、Android PopupWindow 的位置，以及网页自身的 fixed 定位同时参与了这场战争。

于是开始处理：

- WebView viewport 坐标
- PopupWindow 坐标
- 网页 root 的 bounding rect
- Android / JavaScript 双向同步
- 拖动前后的坐标换算

### 第三阶段：长按一下，鱼开始抽搐

解决瞬移之后，又出现了一个更加离谱的问题：

> **长按大肥鱼，鱼会先抽一下。**

本来只是想开始拖动，结果第一次触摸事件会同时触发网页自己的交互和 Android 侧的窗口切换。

所以后来增加了“先判断是否真的开始拖动”的逻辑：

- 先记录按下位置
- 移动距离不足时不进入拖动状态
- 超过阈值才切换到可交互 / 全屏状态
- 避免单纯点击造成窗口重定位

### 第四阶段：鱼从设置页面回来以后飞出屏幕

这一次已经不是左上角了。

鱼甚至学会了**离家出走**。

从设置页面返回聊天页之后，因为 PopupWindow 尺寸和网页 root 的参考坐标发生变化，原来的位置可能被重新解释成屏幕外坐标。

于是加入了位置边界限制，并在窗口尺寸切换后重新固定 root 位置。

现在至少要遵守一条鱼类基本行为准则：

> **可以游，但是不能游出屏幕。**

### 第五阶段：设置按钮看起来能点，实际上什么也没发生

又一个经典问题：

扩展页面里的高级设置按钮已经出现了，但点击以后没有明显反应。

原组件的高级编辑器本来属于 Web UI 内部菜单，而现在菜单又被要求隐藏。

于是采用了一个中间方案：

`扩展页点击 → 保存待打开面板 → 返回聊天页 → WebView 检测请求 → 打开对应原面板`

这样既可以把入口放到 RikkaHub 扩展页面，又不用把整个原 Web UI 重写一遍。

### 第六阶段：数字设置别再让我拖 Slider 了

百分比类设置用 Slider 没问题。

但是：

- 余额预警阈值
- 今日预算
- 扣费提示关闭时间
- 提醒显示时间
- 滚动间隔

这些东西硬塞一个 0%～100% 的 Slider 显然不合适。

所以最终改成了数字输入框，并对输入值做范围限制。

### 第七阶段：大小设置——删了

中间还折腾过组件大小调节。

最后得出结论：

> **用户不要。那就删。**

现在大肥鱼保持固定显示尺寸，不再在设置页面提供大小调节，也不再保留那个让人误以为还能调整大小的入口。

### 当前状态

这条鱼已经经历过：

**显示 → 瞬移 → 抽搐 → 飞出屏幕 → 设置没反应 → 数字输入不合理 → 大小设置被处决 → 继续编译。**

而 GitHub Actions 则负责最后一个传统项目仪式：

> **“代码看起来没问题” ≠ “Kotlin 编译器同意”。**

所以这个项目的开发原则逐渐变成了：

1. 不相信“应该可以”。
2. 不相信“这次肯定好了”。
3. 不拿截图当编译结果。
4. 不拿旧 APK 冒充新版本。
5. **真正跑过构建，才算完成。**

## 🔧 技术说明

大肥鱼本体主要以 Web UI 形式运行，由 Android `WebView` 承载。Android 侧负责窗口、触摸与位置同步，网页侧负责大肥鱼自身的视觉和交互。

当前适配重点包括：

- Android WebView 本地资源加载
- ChatPage 全屏布局
- Android / JavaScript 坐标同步
- 拖动状态切换
- PopupWindow 尺寸切换
- 屏幕边界限制
- 设置页与 Web UI 面板之间的桥接
- RikkaHub 扩展页设置
- GitHub Actions Debug APK 构建

## 🚀 构建

项目使用 Android Studio / Gradle 构建，同时提供 GitHub Actions 自动构建 Debug APK。

## 🙏 致谢

### RikkaHub

感谢 RikkaHub 原项目及贡献者提供基础客户端。

### 大肥鱼组件

感谢 **MeteorNOX / DeepSeek-Balance-Whale-Widget** 提供原始组件。

### Android 集成

Android 集成、适配、调试以及这段开发史由 ChatGPT 协助完成。

## 📄 License

本项目主体遵循原 RikkaHub 项目的 **GNU Affero General Public License v3.0 (AGPL-3.0)**。集成的大肥鱼组件请同时遵循其上游仓库的许可证及版权要求。

---

<div align="center">
  <b>🐟 RikkaHub · 大肥鱼版</b><br />
  从左上角瞬移到现在，鱼还活着。
</div>
