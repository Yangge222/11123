# MC 辅助工具 APK 构建说明

## 📱 项目简介

仿 Minecraft 外挂风格的 Android 悬浮窗辅助工具。

### 功能特性

| 分类 | 功能 |
|------|------|
| ⚔ 战斗 | 自动攻击、暴击辅助、自动格挡、范围伤害、一击必杀 |
| 🏃 移动 | 飞行模式、疾跑加速、无限跳跃、无摔落伤害 |
| 👁 视觉 | 透视(X-Ray)、夜视模式、高亮实体、全图迷雾去除 |
| 🛠 辅助 | 自动采集、自动搭桥、防踢检测绕过、反AFK、自动钓鱼、物品整理 |

### 交互流程
1. 启动 App → 弹出权限引导页
2. 点击"前往授予悬浮窗权限" → 跳转系统设置
3. 授权完成回来 → "启动悬浮球"按钮变亮
4. 点击启动 → App 最小化，绿色圆形悬浮球出现在屏幕上
5. **拖动**悬浮球可移动位置
6. **点击**悬浮球展开/收起功能面板
7. 在面板内打开各个功能开关

---

## 🔧 构建步骤

### 方法一：Android Studio（推荐）

1. **安装 Android Studio**
   - 下载：https://developer.android.com/studio
   - 安装时勾选 Android SDK (API 34)

2. **导入项目**
   - 打开 Android Studio
   - File → Open → 选择本文件夹（新建文件夹）

3. **同步 Gradle**
   - 等待右下角同步完成（首次需下载依赖，约几分钟）

4. **构建 APK**
   - 菜单：Build → Build Bundle(s)/APK(s) → Build APK(s)
   - 等待构建完成

5. **找到 APK**
   - 路径：`app/build/outputs/apk/debug/app-debug.apk`
   - 也会弹出"locate"按钮直接打开文件夹

### 方法二：命令行构建

```bash
# Windows
cd 新建文件夹
gradlew.bat assembleDebug

# 输出路径
app\build\outputs\apk\debug\app-debug.apk
```

---

## 📲 安装到手机

1. 手机开启"允许安装未知来源应用"
   - 设置 → 安全 → 未知来源（或安装未知应用）

2. 将 APK 传到手机（USB / 微信 / QQ）

3. 点击 APK 文件安装

4. 安装完成后打开，按引导授权即可使用

---

## 📁 项目结构

```
新建文件夹/
├── app/
│   ├── build.gradle                          # app 模块构建配置
│   ├── proguard-rules.pro
│   └── src/main/
│       ├── AndroidManifest.xml               # 权限声明
│       ├── java/com/mcplugin/helper/
│       │   ├── PermissionActivity.java       # 权限引导页
│       │   └── FloatingWindowService.java    # 悬浮窗服务
│       └── res/
│           ├── layout/activity_permission.xml
│           ├── values/strings.xml
│           ├── values/styles.xml
│           └── drawable/                     # 各按钮/背景资源
├── build.gradle                              # 根构建配置
├── settings.gradle
└── gradle/wrapper/gradle-wrapper.properties
```

---

## ⚠️ 注意事项

- 本项目为**UI演示**，开关状态仅展示 Toast 提示，不包含实际游戏注入逻辑
- 悬浮窗功能需 Android 6.0 (API 23) 及以上
- 部分手机(小米/MIUI)需要额外开启"后台弹出界面"权限
