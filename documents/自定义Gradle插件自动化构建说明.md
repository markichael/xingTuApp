# 自定义 Gradle 插件自动化构建说明

## 目标

* 为项目增加自动化构建能力：

  * 在 App 编译前清理指定缓存目录

  * 在构建完成后生成项目依赖树并写入文件

* 交付构建产物：`app-release.apk` 与 `app-release.aab`

## 我们完成了什么

* 新增一个自定义 Gradle 插件（ID：`com.example.autobuild`），在 `buildSrc` 中实现。

* 插件内置两个任务：

  * `autoBuildClearCache`：在 `preBuild` 前执行，清理你配置的缓存目录

  * `autoBuildWriteDependencyTree`：在 `assembleRelease`/`bundleRelease`/`build` 完成后，输出依赖树到文件

* 将插件接入到 `app` 模块，并配置扩展参数。

* 在低内存环境下调整构建参数，成功产出 APK 与 AAB。

## 相关文件

* 插件工程与实现

  * `buildSrc/build.gradle.kts`

  * `buildSrc/src/main/kotlin/AutoBuildPlugin.kt`

    * 插件入口类：`buildSrc/src/main/kotlin/AutoBuildPlugin.kt:68`

    * 任务注册：`buildSrc/src/main/kotlin/AutoBuildPlugin.kt:72-80`

    * 任务挂接到构建流程：`buildSrc/src/main/kotlin/AutoBuildPlugin.kt:82-101`

  * 插件 ID 声明：`buildSrc/src/main/resources/META-INF/gradle-plugins/com.example.autobuild.properties`

* 应用插件与参数配置

  * `app/build.gradle.kts:1-5` 应用插件 ID

  * `app/build.gradle.kts:61-64` 配置扩展 `autobuild { ... }`

* 依赖树输出文件

  * `build/reports/dependencies/dependency-tree.txt`

## 如何使用

* 清理缓存与构建期间自动写依赖树，无需额外命令（插件已挂接到构建流程）。

* 手动生成依赖树（不触发完整构建）：

  * 运行命令：`./gradlew :app:autoBuildWriteDependencyTree --no-daemon`

* 构建产物：

  * APK：`./gradlew :app:assembleRelease --no-daemon --max-workers=1`

  * AAB：`./gradlew :app:bundleRelease --no-daemon --max-workers=1`

## 运行结果

* 依赖树任务输出（已验证运行成功）：

  * 控制台日志：`[AutoBuild] Dependency tree written to: D:\MyApplication\build\reports\dependencies\dependency-tree.txt`

  * 文件已生成：`d:\MyApplication\build\reports\dependencies\dependency-tree.txt`

* 构建产物（已生成）：

  * APK：`d:\MyApplication\app\build\outputs\apk\release\app-release.apk`

  * AAB：`d:\MyApplication\app\build\outputs\bundle\release\app-release.aab`

## 项目内插件配置示例

* 在 `app/build.gradle.kts` 中：

  * 应用插件：`id("com.example.autobuild")`

  * 配置扩展块（`app/build.gradle.kts:61-64`）：

    ```
    autobuild {
        cacheDir = "${project.buildDir}/cache_to_clear"
        dependencyOutputFile = "${rootProject.buildDir}/reports/dependencies/dependency-tree.txt"
    }
    ```

## 构建内存优化（低内存环境）

* `gradle.properties` 推荐设置（已应用）：

  * `org.gradle.jvmargs=-Xmx768m -XX:MaxMetaspaceSize=256m -XX:ReservedCodeCacheSize=64m -XX:+UseSerialGC -Dfile.encoding=UTF-8`

  * `org.gradle.daemon=false`

  * `org.gradle.workers.max=1`

  * `kotlin.daemon.jvm.options=-Xmx512m`

* `app/build.gradle.kts`：

  * `release { isMinifyEnabled = false }`（关闭 R8 混淆以减小内存消耗）

  * `lint { checkReleaseBuilds = false; abortOnError = false }`（避免 release lint 占用内存）

  * `defaultConfig { multiDexEnabled = true }` + 依赖 `androidx.multidex:multidex:2.0.1`

  * `packaging { jniLibs { useLegacyPackaging = true } }`

* 构建命令建议：`--no-daemon --max-workers=1`

## 常见问题

* APK 未签名：当前生成的 `app-release.apk` 为未签名包。若需正式发布，请在 `app/build.gradle.kts` 配置 `signingConfigs` 与 `buildTypes.release.signingConfig`。

* 构建 OOM：逐步降低并发（`org.gradle.workers.max=1`）、禁用守护进程（`org.gradle.daemon=false`）、关闭混淆与 lint，必要时提升 `org.gradle.jvmargs`。

## 可扩展能力

* 继续扩展插件：

  * 编译前执行静态检查或资源清理

  * 构建后自动上传产物、生成额外报告（依赖、许可证、方法数）

  * 按构建类型（debug/release）动态开启不同任务

## 快速回顾

* 做了什么：写了一个 `buildSrc` 插件，自动清缓存与输出依赖树，并把它接到构建流程里。

* 怎么做的：在 `buildSrc` 实现任务与插件，对 `preBuild` 和 `assembleRelease/bundleRelease` 进行挂接；在 `app` 模块应用插件并配置扩展参数；优化低内存构建设置。

* 结果：依赖树文件已生成，`app-release.apk` 与 `app-release.aab` 已产出且路径清晰。

