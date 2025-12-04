# 🎨 XingTuClone (Android 图片编辑应用)

> 基于 Android Jetpack Compose 的现代图片编辑工具，集成了 AI 消除、人脸美颜、拼图与批量处理功能。

---

## 📖 目录

- [项目简介](#-项目简介)
- [核心特性](#-核心特性)
- [技术栈](#-技术栈)
- [核心架构与文件](#-核心架构与文件)
- [🚀 快速开始](#-快速开始)
  - [开发环境](#开发环境)
  - [构建与运行](#构建与运行)
  - [构建产物](#构建产物)
- [💡 遇到的困难与解决思路](#-遇到的困难与解决思路)
- [📝 功能详情与参数](#-功能详情与参数)
- [🗺️ 进阶与路线图](#-进阶与路线图)

---

## 🌟 项目简介

这是一个完全使用 **Kotlin** 和 **Jetpack Compose** 构建的 Android 图片编辑应用。项目不仅实现了基础的图片编辑功能，还深度集成了 **Google ML Kit** 进行人脸检测，以及 **ONNX Runtime** 运行深度学习模型（LaMa/扩散模型）实现魔法消除功能。

## ✨ 核心特性

- **🧙‍♂️ AI 魔法消除**：基于 ONNX Runtime 的 LaMa 模型，智能移除画面多余物体。
- **💄 智能美颜**：人脸关键点检测，支持磨皮、美白、红润、锐化调节。
- **🧩 创意拼图**：支持 2-6 张图片拼接，提供 14 种布局模板，支持拖拽交换与样式调节。
- **🎨 专业滤镜**：集成 GPUImage 与 RenderScript，提供多种预置滤镜与参数调节。
- **🖼️ 批量修图**：多选图片队列处理，后台异步导出。
- **✂️ 基础编辑**：基于 uCrop 的高性能裁剪与旋转。

---

## 🛠 技术栈

| 类别 | 库/技术 | 说明 |
| :--- | :--- | :--- |
| **UI 框架** | `androidx.compose.*` | Material 3 现代声明式 UI |
| **图片加载** | `io.coil-kt:coil-compose` | 异步图片加载与内存优化 |
| **AI 推理** | `com.microsoft.onnxruntime` | ONNX 模型端侧推理 (Magic Erase) |
| **人脸检测** | `com.google.mlkit:face-detection` | 高精度人脸关键点识别 |
| **滤镜处理** | `jp.co.cyberagent.android:gpuimage` | GPU 加速图像处理 |
| **图片裁剪** | `com.github.yalantis:ucrop` | 专业的裁剪交互库 |
| **架构模式** | MVVM + MVI | StateFlow 单向数据流 |

---

## 📂 核心架构与文件

### 工程配置
- **全局配置**：`build.gradle.kts`, `settings.gradle.kts`
- **应用模块**：`app/build.gradle.kts`, `app/src/main/AndroidManifest.xml`
- **入口文件**：`app/src/main/java/com/example/xingtuclone/MainActivity.kt`

### 核心模块路径
- **路由与状态 (Architecture)** 🌟
  - **路由定义**：`ui/navigation/AppRoute.kt` (Sealed Class 统一路由)
  - **状态管理**：`ui/HomeViewModel.kt` (StateFlow 导航状态)
- **统一导出工具** 🌟
  - `ui/utils/Exporter.kt` (统一 Bitmap/Uri 导出逻辑)
- **AI 功能**
  - **消除推理**：`inpaint/OnnxLamaInpainter.kt`
  - **美颜算法**：`utils/BeautyProcessor.kt`
- **UI 界面**
  - 所有界面位于：`app/src/main/java/com/example/xingtuclone/ui`

---

## 🚀 快速开始

### 开发环境
- **JDK**: 17 (匹配 AGP 8.1.1)
- **Android Studio**: Giraffe / Koala 或更高版本
- **Android SDK**: Compile/Target SDK 34

### 构建与运行
Windows 终端使用仓库内 `gradlew.bat`：

1.  **初始化与调试**
    ```powershell
    cd d:\MyApplication
    ./gradlew.bat :app:assembleDebug
    # 输出: app/build/outputs/apk/debug/app-debug.apk
    ```

2.  **构建 Release 包**
    ```powershell
    ./gradlew.bat :app:assembleRelease
    # 输出: app/build/outputs/apk/release/app-release.apk
    ```

3.  **安装到设备**
    ```powershell
    adb install -r app/build/outputs/apk/release/app-release.apk
    ```

### 构建产物
- **APK**: `app/build/outputs/apk/release/app-release.apk`
- **AAB**: `app/build/outputs/bundle/release/app-release.aab`

---

## 💡 遇到的困难与解决思路

> 这里记录了项目开发过程中的核心技术难点与解决方案，展示了解决复杂问题的能力。

### 1. 架构重构：路由状态管理混乱 (已解决)
- **现象**：`HomeScreen` 曾存在 192 行+ 的分散 `rememberSaveable` 状态，导致页面跳转逻辑难以维护。
- **解决**：引入 **MVI 思想**，创建 `AppRoute` Sealed Class 定义所有页面状态，配合 `HomeViewModel` 实现单向数据流，UI 变为纯粹的状态消费者。

### 2. 架构重构：导出逻辑不统一 (已解决)
- **现象**：拼图、批量修图、消除页面的导出逻辑重复且不一致，导致部分页面 Bug。
- **解决**：封装 `Exporter` 单例，统一处理 Context、Bitmap 保存与 Uri 返回，消除重复代码。

### 3. 大型原生库打包与多 Dex
- **现象**：引入 ONNX 和 ML Kit 导致包体积大且方法数超限。
- **解决**：启用 `multiDexEnabled = true`，并设置 `packaging.jniLibs.useLegacyPackaging = true` 解决原生库压缩导致的加载问题。

### 4. RenderScript 兼容性
- **现象**：历史滤镜算法依赖 RenderScript，但在 Android 12+ 已废弃。
- **解决**：开启 RenderScript 支持模式，并逐步使用 GPUImage 替代关键路径。

### 5. 构建自动化
- **现象**：构建产物分散，依赖版本管理混乱。
- **解决**：开发自定义 Gradle 插件 `com.example.autobuild`，自动生成依赖树报告并清理缓存。

---

## 📝 功能详情与参数

### 人像美颜 (EditorShell → PORTRAIT)
- **参数**：磨皮 (0–10)、美白 (0–1)、红润 (0–1)、锐化 (0–4)
- **算法**：RenderScript 流水线 (`ScriptIntrinsicBlur` + `Convolve3x3` + `ColorMatrix`)
- **位置**：`utils/BeautyProcessor.kt`

### 魔法消除 (Magic Erase)
- **交互**：画笔涂抹生成蒙版，支持撤销与蒙版显示切换。
- **技术**：ONNX LaMa 模型端侧推理。
- **位置**：`ui/MagicEraseScreen.kt`

### 拼图 (Collage)
- **功能**：支持 2–6 张图片，14 种布局，长按拖拽交换。
- **内存保护**：实现 OOM 自动降尺度保存策略。
- **位置**：`ui/CollageScreen.kt`

