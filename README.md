# 项目简介

这是一个基于 Android Jetpack Compose 的图片编辑应用（包名 `com.example.xingtuclone`），支持：
- 基础编辑：裁剪、旋转、拼图、滤镜、批量处理
- AI 功能：基于 ONNX Runtime 的魔术橡皮擦（LaMa/扩散模型的 ONNX 推理）（`app/src/main/java/com/example/xingtuclone/inpaint/OnnxLamaInpainter.kt`）
- 人脸检测与美颜：接入 Google ML Kit 与自定义滤镜处理（`app/src/main/java/com/example/xingtuclone/utils/BeautyProcessor.kt`）
- 现代 UI：Material 3 + Compose（`app/src/main/java/com/example/xingtuclone/ui`）

主要依赖：
- `androidx.compose.*` 现代 UI
- `com.google.mlkit:face-detection` 人脸检测
- `com.microsoft.onnxruntime:onnxruntime-android` ONNX 推理
- `jp.co.cyberagent.android:gpuimage` 滤镜
- `io.coil-kt:coil-compose` 图片加载
- `com.github.yalantis:ucrop` 图片裁剪

核心工程文件位置：
- 工程配置：`build.gradle.kts`、`settings.gradle.kts`
- 应用模块：`app/build.gradle.kts`、`app/src/main/AndroidManifest.xml`
- 入口 Activity：`app/src/main/java/com/example/xingtuclone/MainActivity.kt`


## 遇到的困难与解决思路

- 依赖冲突与镜像源访问
  - 现象：国内拉取依赖不稳定、部分依赖解析冲突
  - 解决：在 `settings.gradle.kts:1-23` 配置了阿里云镜像与 JitPack；同时在 `app/build.gradle.kts:93-99` 移除了易冲突的 Guava 相关协程依赖，保留 Play Services 版本以支持 `await`

- Compose 编译器与 AGP 版本兼容
  - 现象：Compose 版本与 Kotlin/AGP 不匹配导致编译失败
  - 解决：统一到 AGP `8.1.1` 与 Kotlin `1.9.0`（`build.gradle.kts:1-5`），并设置 `composeOptions.kotlinCompilerExtensionVersion = "1.5.1"`（`app/build.gradle.kts:47-49`）

- 大型原生库打包与多 Dex
  - 现象：ONNX/MLKit 等引入体积较大且方法数超限
  - 解决：启用 `multiDexEnabled = true`（`app/build.gradle.kts:22-23`），并设置 `packaging.jniLibs.useLegacyPackaging = true` 避免打包异常（`app/build.gradle.kts:54-57`）

- RenderScript 兼容性
  - 现象：历史滤镜组件依赖 RenderScript，较新 SDK 上已废弃
  - 解决：开启支持模式并指定目标 API（`app/build.gradle.kts:24-27`）；同时通过 GPUImage/自研管线替代关键路径

- 可重复、稳定的构建产物输出
  - 现象：构建后产物位置不易记录
  - 解决：提供自定义 Gradle 插件 `com.example.autobuild`（`buildSrc/src/main/kotlin/AutoBuildPlugin.kt`），在 `assembleRelease`/`bundleRelease` 完成后生成依赖树报告并清理缓存，提升可维护性


## 开发环境要求

- JDK：17（与 Android Gradle Plugin 8.1.1 匹配）
- Android Studio：Giraffe/Koala 或更高版本（可选）
- Android SDK：`compileSdk = 34`，`targetSdk = 34`
- Windows 终端构建：使用仓库内 `gradlew.bat`


## 构建与运行说明（Windows）

- 初始化与调试构建：
  - `cd d:\MyApplication`
  - `./gradlew.bat :app:assembleDebug`
  - 调试 APK 输出：`app/build/outputs/apk/debug/app-debug.apk`

- 构建 Release APK（已实测可用）：
  - 命令：`./gradlew.bat :app:assembleRelease`
  - 输出：`app/build/outputs/apk/release/app-release.apk`
  - 注意：当前配置为未混淆且默认签名。如需商店发布，请在 `app/build.gradle.kts` 的 `buildTypes.release` 中配置 `signingConfig` 使用你的 `keystore`

- 构建 Release AAB（已实测可用）：
  - 命令：`./gradlew.bat :app:bundleRelease`
  - 输出：`app/build/outputs/bundle/release/app-release.aab`

- 安装到设备：
  - 开启开发者模式与 USB 调试后，使用：`adb install -r app/build/outputs/apk/release/app-release.apk`
  - 若出现签名或安装问题，可先安装 Debug 包：`adb install -r app/build/outputs/apk/debug/app-debug.apk`


## 签名与发布建议

- 在 `app/build.gradle.kts` 中新增：
  - `signingConfigs`（指向你的 `keystore.jks`、`keyAlias`、`storePassword`、`keyPassword`）
  - 在 `buildTypes.release` 中设置 `signingConfig = signingConfigs.getByName("release")`
- 构建签名产物后再提交到应用商店；AAB 为 Play/AppGallery 等商店推荐格式


## 构建产物位置

- APK：`app/build/outputs/apk/release/app-release.apk`
- AAB：`app/build/outputs/bundle/release/app-release.aab`

## 技术实现困难与解决方案（整合）

- 详见整合文档：`技术实现困难与解决方案（整合）.md`

## 分项功能设计与难点记录

- 详见分项文档：`功能设计与难点（分项记录）.md`

## 交互细节与参数说明

- 人像美颜（EditorShell → PORTRAIT）
  - 参数：磨皮（0–10）、美白（0–1）、红润（0–1）、锐化（0–4）
  - 算法：RenderScript 的 `ScriptIntrinsicBlur`（磨皮）、`ScriptIntrinsicConvolve3x3`（锐化）、`ScriptIntrinsicColorMatrix`（美白/红润组合矩阵）
  - 位置：`app/src/main/java/com/example/xingtuclone/utils/BeautyProcessor.kt`

- 滤镜/调色（EditorShell → FILTER）
  - 预置：原图、暖色、冷色、黑白、复古、增/降/柔和饱和、青橙等
  - 组合：`android.renderscript.Matrix4f` 和 GPUImage 预置滤镜混用；调节亮度、对比、饱和
  - 位置：`app/src/main/java/com/example/xingtuclone/ui/EditorShell.kt`、`ui/FilterUtils.kt`

- 魔法消除（AI 修复）
  - 交互：画笔大小与羽化、蒙版显示切换、拖动绘制目标区域
  - 推理：基于 ONNX 的 LaMa/扩散模型进行 inpaint，返回高质量修复图
  - 位置：`app/src/main/java/com/example/xingtuclone/ui/MagicEraseScreen.kt`、`inpaint/OnnxLamaInpainter.kt`

- 裁剪旋转
  - 工具：uCrop，支持自由裁剪与常用比例（1:1、3:4、3:2、16:9）、旋转手势
  - 位置：`app/src/main/java/com/example/xingtuclone/ui/CropRotateScreen.kt`

- 拼图
  - 模板：2–6 张共 14 种布局，支持长按拖拽交换位置、圆角与边距调节、比例选择
  - 保存：AndroidView 容器截图生成 Bitmap，保存相册
  - 位置：`app/src/main/java/com/example/xingtuclone/ui/CollageScreen.kt`

- 批量修图
  - 模式：自动美颜、柔和滤镜；显示进度条与单项完成状态
  - 保存：批量输出到相册并收集结果列表
  - 位置：`app/src/main/java/com/example/xingtuclone/ui/BatchEditScreen.kt`

- 保存与结果页
  - 保存：Android 10+ 使用 MediaStore，返回保存后的 `Uri`
  - 结果页：展示保存图、最近作品、继续修图/返回首页、分享提示
  - 位置：`app/src/main/java/com/example/xingtuclone/ui/SaveUtils.kt`、`SaveResultScreen.kt`

## 性能与内存保护

- 相册性能采集：`utils/PerfLogger.kt` 记录进入相册到首图显示时间（TTFR）、平均与 p90 加载时长，并输出报告到缓存
- 拼图保存 OOM 保护：`CollageScreen.kt` 的 `viewToBitmap()` 在内存不足时自动降尺度保存，防止闪退
- 预览降采样：加载预览图时限制尺寸（如 1080/1280/2560），避免直接加载原图导致内存飙升
- GPU/RS 混合：在 EditorShell 中针对实时预览选择更轻量的矩阵/滤镜组合以保持帧率

## 权限与文件访问

- Android 13+：`READ_MEDIA_IMAGES`
- Android 12-：`READ_EXTERNAL_STORAGE`
- FileProvider：`res/xml/file_paths.xml` 与清单中的 `com.example.xingtuclone.fileprovider`，拍照使用 `externalCacheDir` 临时文件（`MainActivity.kt` 的 `createImageFile()`）

## 已知限制与兼容性

- RenderScript 在新 SDK 上已废弃：本项目开启兼容支持模式，并逐步用 GPUImage/自研管线替代
- 多图/高分辨率场景的内存峰值较高：已采取预览降采样与保存时 OOM 降尺度策略
- 模型体积：ONNX 与 MLKit 引入较大原生库，首次安装包体积较大；可通过混淆与资源裁剪进一步优化

## 进阶与路线图

- 正式签名与混淆：补充 `signingConfigs` 与 `proguard-rules.pro`，开启 R8 缩减
- 模型管理：按设备 ABI/能力选择性加载模型，减少安装体积
- 多平台商店适配：AAB 签名、Play/AppGallery 上传流程脚本化
- 性能监控：构建时依赖树报告与运行时关键路径统计统一汇总到调试面板
