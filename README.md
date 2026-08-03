# Doki 电子木鱼 🪵

一款简洁的电子木鱼 App。敲击木鱼，积累功德，平和心境。

Material Design 3 · Kotlin + Jetpack Compose

## 功能

- 🪵 敲击木鱼：真实木鱼音效（多谐波合成）+ 震动反馈 + 回弹动画
- 📿 功德计数：今日计数 / 累计计数，每日自动清零
- 🎉 每日敲满 1000 次触发庆祝动画
- ⏰ 提醒功能（三种模式）：
  - **随机时间**：每天在设定时段内随机提醒 3 次
  - **自定义间隔**：每 N 分钟 / 小时 / 天提醒一次
  - **固定时间 · 日历**：每天固定时间写入系统日历，由日历 App 提醒（支持 Google 日历云端同步），Doki 无需后台运行
- ⚡ 互动速度调节：0.5 / 0.75 / 1.0 / 1.25
- 🎨 主题：多套木色系主题色 + 明暗模式 + 三语言（简中 / 繁中 / English）
- 🔄 应用内自动更新：启动时检查 GitHub Releases，发现新版本一键下载安装
- 🛡️ 签名自校验：检测到 APK 被二次打包（签名不一致）时拒绝运行

## 下载

前往 [Releases](https://github.com/lihongxi-g/wooden-fish/releases) 下载最新 APK。

应用内「关于 → 检查更新」也可直接更新。

## 构建

使用 GitHub Actions 自动构建（每次 push 到 main 自动编译并发布 Release）。

本地构建：

```bash
./gradlew assembleDebug
```

要求：JDK 17、Android SDK 34。

签名：使用项目 GitHub Secrets 中的固定 keystore（`DOKI_KEYSTORE_B64` 等），保证所有版本签名一致、可直接覆盖安装。

## 开源协议

[GPL-3.0](LICENSE)

本项目采用 GPL-3.0 协议：你可以自由使用、修改、分发，但**基于本项目的修改版本必须同样以 GPL-3.0 开源**。
