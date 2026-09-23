# andriod-test
try an andriod test

#power by deepseek

# QuickSwitch 快捷开关

Android 快捷跳转 / 开关 App，Material 3 风格，Kotlin + Jetpack Compose，支持 Android 10 及以上。

## 功能

### 睡前开关
- 引导关闭 Wi-Fi、移动网络、蓝牙、NFC
- 引导开启省电模式
- 状态圆点：🟢 已开启 / ⚪ 已关闭
- 从系统设置返回后自动刷新状态

### 睡眠状态
- 日历视图：翻月、展开/收起（月视图 / 一周视图）
- 一键记录：点「我睡了」记入睡，点「我起了」记起床
- 自动补记：打开 App 时，若未记录起床且系统闹钟时间已过，用闹钟时间补上
- 周图表：深蓝紫实心圆 = 睡觉，橙色空心圆 = 起床，竖线连接同一晚
- 空状态提示：无数据时显示引导文案

### 设置
- 主题强调色：默认蓝 / 紫罗兰 / 草木绿 / 落日橙 / 樱花粉
- 导出睡眠数据：CSV 保存到 `Downloads/sleep/sleep_data.csv`
- 导入睡眠数据：选择 CSV 按日期合并（同日期覆盖）
- 关于：查看版本号、打开 GitHub 项目

## 说明

Android 10+ 无法静默关闭 Wi-Fi、移动网络、蓝牙、NFC，本 App 仅跳转到系统面板或设置页，由用户手动确认，属于「引导式一键流程」。

闹钟接口限制：Android 只开放 `AlarmManager.getNextAlarmClock()` 读取下一个闹钟时间，无法监听闹钟响铃。因此「自动记录起床」实际是打开 App 时补记，不是实时触发。

## 构建

GitHub Actions 自动构建，APK 见 Actions Artifacts。

## 版本

v1.3.2

## 注意

从旧签名版本升级需先卸载，之后版本可直接覆盖安装。

## 项目地址

https://github.com/jimmyrejuice/andriod-test/

andriod-test/
├── .github/
│   └── workflows/
│       └── build.yml                # GitHub Actions 构建脚本（含 keystore 签名）
├── app/
│   ├── build.gradle.kts             # app 模块配置（versionCode=6, versionName=1.3.2）
│   └── src/
│       └── main/
│           ├── AndroidManifest.xml  # 权限、Activity 声明、图标引用
│           ├── java/
│           │   └── com/example/quickswitch/
│           │       ├── MainActivity.kt    # 主入口、抽屉、胶囊 Tab、开关页
│           │       ├── SleepData.kt       # 睡眠数据模型 + 存储 + 导入导出
│           │       └── SleepScreen.kt     # 睡眠状态页（日历 + 图表）
│           └── res/
│               ├── drawable/
│               │   └── ic_launcher.xml    # 应用图标（自适应图标）
│               └── values/
│                   ├── strings.xml        # 字符串资源
│                   └── themes.xml         # 主题样式
├── gradle.properties                # Gradle 全局配置
├── build.gradle.kts                 # 根 build 配置（插件版本）
├── settings.gradle.kts              # 模块声明、仓库配置
└── README.md                        # 项目说明
