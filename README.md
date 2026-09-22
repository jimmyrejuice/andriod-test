# andriod-test
try an andriod test

power by deepseek

# QuickSwitch 快捷开关

Android 快捷跳转/开关 App，Material 3 风格，Kotlin + Jetpack Compose，支持 Android 10 及以上。

## 功能
- 睡前开关：引导关闭 Wi-Fi、移动网络、蓝牙、NFC，并开启省电模式
- 状态圆点：🟢 已开启 / ⚪ 已关闭
- 主题强调色可选
- 关于页：查看版本号、打开 GitHub 项目

## 说明
Android 10+ 无法静默关闭 Wi-Fi、移动网络、蓝牙、NFC，本 App 仅跳转到系统面板或设置页，由用户手动确认，属于“引导式一键流程”。

## 构建
GitHub Actions 自动构建，APK 见 Actions Artifacts。

## 版本
v1.2

## 注意
从旧签名版本升级需先卸载，之后版本可直接覆盖安装。

all files:

| 序号 | 文件 |
| --- | --- |
| 1 | `build.yml` |
| 2 | `settings.gradle.kts` |
| 3 | `build.gradle.kts` |
| 4 | `gradle.properties` |
| 5 | `app/build.gradle.kts` |
| 6 | `app/src/main/AndroidManifest.xml` |
| 7 | `app/src/main/java/com/example/quickswitch/MainActivity.kt` |
| 8 | `app/src/main/res/values/strings.xml` |
| 9 | `app/src/main/res/values/themes.xml` |
| 10 |`app/src/main/res/drawable/ic_launcher.xml` |
