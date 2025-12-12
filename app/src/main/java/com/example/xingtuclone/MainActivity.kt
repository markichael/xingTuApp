/**
 * ============================================
 * MainActivity.kt - 应用唯一入口 Activity
 * ============================================
 * 
 * 功能说明：
 * 应用采用「单 Activity + Jetpack Compose」架构模式
 * 所有页面跳转通过 Compose State 管理，无需多个 Activity/Fragment
 * 
 * ============================================
 * 架构设计：单 Activity 架构
 * ============================================
 * 
 * 【传统多 Activity 架构的问题】
 * 1. 内存开销大：每个 Activity 都有独立的视图层级和生命周期
 * 2. 状态传递复杂：Intent Bundle 传递大对象效率低（需要序列化）
 * 3. 动画不连贯：Activity 切换动画受系统限制，难以自定义
 * 4. 测试困难：需要启动完整 Activity 才能测试 UI
 * 
 * 【单 Activity + Compose 架构的优势】
 * 1. 轻量级导航：页面切换只是 Composable 函数的重组，无需创建新 Activity
 * 2. 状态共享简单：通过 ViewModel 跨页面共享状态，无需序列化
 * 3. 动画流畅：Compose 动画系统完全可控，支持复杂过渡效果
 * 4. 性能更优：减少内存占用，避免 Activity 生命周期带来的开销
 * 5. 易于测试：可以单独测试 Composable 函数，无需启动 Activity
 * 
 * 【本项目导航策略】
 * - 使用 HomeViewModel 管理全局路由状态（MVI 模式）
 * - 所有页面路由通过 Sealed Class AppRoute 定义
 * - 页面切换通过修改 StateFlow<AppRoute> 实现
 * - 无需 Jetpack Navigation 库（避免引入过多依赖）
 * 
 * ============================================
 * 核心职责
 * ============================================
 * 
 * 1. 【启动流程管理】
 *    - 显示 SplashScreen 启动页（品牌展示 + 资源预加载）
 *    - 启动页结束后自动切换到 HomeScreen 主界面
 *    - 通过 remember { mutableStateOf } 控制状态切换
 * 
 * 2. 【Compose UI 初始化】
 *    - 调用 setContent { } 设置 Compose 根节点
 *    - 替代传统 setContentView(R.layout.activity_main) XML 布局
 * 
 * 3. 【工具函数提供】
 *    - 提供 Context.createImageFile() 扩展函数
 *    - 用于相机拍照时创建临时文件 Uri（FileProvider 安全访问）
 * 
 * ============================================
 * 技术亮点
 * ============================================
 * 
 * 【1. Jetpack Compose 声明式 UI】
 * - 无 XML 布局，全部使用 Kotlin 代码描述 UI
 * - 状态驱动：showSplash 变化自动触发 UI 重组
 * - 响应式设计：数据变化立即反映到界面
 * 
 * 【2. remember + mutableStateOf 状态管理】
 * - remember：保证配置变更（如旋转屏幕）时状态不丢失
 * - mutableStateOf：创建可观察状态，变化时自动重组
 * - by 委托：简化语法，showSplash 可以直接读写
 * 
 * 【3. FileProvider 安全文件访问】
 * - Android 7.0+ 禁止直接通过 file:// 分享文件（抛出 FileUriExposedException）
 * - 使用 content:// Uri 实现安全的跨进程文件访问
 * - 必须在 AndroidManifest.xml 中声明 FileProvider 配置
 * 
 * ============================================
 * 面试要点
 * ============================================
 * 
 * Q1: 为什么选择单 Activity 架构而不是多 Activity？
 * A: 
 * - 性能更优：避免 Activity 创建销毁的开销
 * - 状态管理简单：通过 ViewModel 共享状态，无需 Intent 传递
 * - 动画更流畅：Compose 动画系统完全可控
 * - 符合现代 Android 开发趋势（Google 官方推荐）
 * 
 * Q2: 启动页的作用是什么？
 * A:
 * - 品牌展示：显示 Logo 和 Slogan，提升品牌认知
 * - 资源预加载：初始化 ONNX 模型、RenderScript 等重资源
 * - 优化体验：避免直接进入主界面时的卡顿感
 * 
 * Q3: 为什么不使用 Jetpack Navigation 库？
 * A:
 * - 项目路由逻辑简单，自定义 Sealed Class 更轻量级
 * - 避免引入额外依赖，减小 APK 体积
 * - 更灵活的状态管理，配合 MVI 架构更清晰
 * 
 * Q4: createImageFile 为什么使用 externalCacheDir？
 * A:
 * - 相机拍照的临时文件不需要永久保存
 * - externalCacheDir 会在应用卸载时自动清理
 * - 用户可以在「设置→应用→清除缓存」中手动删除
 * - 避免占用用户存储空间配额
 * 
 * ============================================
 */
package com.example.xingtuclone

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.example.xingtuclone.ui.HomeScreen
import com.example.xingtuclone.ui.SplashScreen
import android.content.Context
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 应用主 Activity
 * 
 * 设计模式：单 Activity 架构
 * 继承：ComponentActivity（支持 Jetpack Compose 的 Activity 基类）
 * 
 * 生命周期说明：
 * - onCreate：应用启动时调用，初始化 UI
 * - onResume/onPause：页面可见性变化时调用（由父类处理）
 * - onDestroy：应用退出时调用，自动释放资源
 */
class MainActivity : ComponentActivity() {
    
    /**
     * Activity 创建回调
     * 
     * 调用时机：
     * 1. 应用首次启动
     * 2. 应用从后台切换回前台（如果已被系统销毁）
     * 3. 配置变更时（如旋转屏幕、切换语言）
     * 
     * @param savedInstanceState 保存的状态数据
     *        - 首次启动时为 null
     *        - 配置变更时包含上次保存的状态（通过 onSaveInstanceState）
     *        - 注意：Compose 的 remember 会自动处理状态恢复，无需手动读取
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // ============================================
        // 设置 Compose UI 根节点
        // ============================================
        // setContent 是 ComponentActivity 的扩展函数
        // 作用：将 Compose 内容设置为 Activity 的视图
        // 等价于传统的 setContentView(R.layout.activity_main)
        setContent {
            // ============================================
            // 启动页状态管理
            // ============================================
            // remember { mutableStateOf(true) }：
            // - 创建可观察状态，初始值为 true（显示启动页）
            // - remember：在 Recomposition 时保留状态值
            // - mutableStateOf：状态变化时自动触发 UI 重组
            // 
            // by 委托：
            // - 语法糖，简化 .value 访问
            // - showSplash 等价于 showSplash.value
            var showSplash by remember { mutableStateOf(true) }
            
            // ============================================
            // 条件渲染：启动页 vs 主界面
            // ============================================
            if (showSplash) {
                // 【启动页阶段】
                // 显示品牌 Logo 和动画
                // onFinished 回调：动画播放完毕后执行
                SplashScreen(onFinished = { 
                    showSplash = false  // 修改状态触发重组，切换到主界面
                })
                
                // 启动页持续时间：通常 1.5-3 秒
                // - 太短：用户无法感知品牌信息
                // -太长：影响用户体验，增加等待焦虑
                // - 本项目：2 秒（可在 SplashScreen 中调整）
                
            } else {
                // 【主界面阶段】
                // 进入应用主界面（包含所有功能入口）
                // HomeScreen 内部管理：
                // - 功能网格菜单（AI 消除、美颜、拼图等）
                // - 页面路由切换（通过 HomeViewModel）
                // - 底部导航栏（如果有多个 Tab）
                HomeScreen()
                
                // 注意：HomeScreen 不会被销毁，即使跳转到其他页面
                // 因为导航是通过状态切换实现的，不是 Activity 替换
            }
        }
        
        // ============================================
        // 性能优化说明
        // ============================================
        // 1. 懒加载：
        //    - SplashScreen 和 HomeScreen 不会同时存在于内存
        //    - if-else 确保只渲染当前需要的页面
        // 
        // 2. 状态提升：
        //    - showSplash 状态定义在顶层，子组件无法直接修改
        //    - 通过回调函数 onFinished 实现状态流动（单向数据流）
        // 
        // 3. 避免过度重组：
        //    - showSplash 只会改变一次（true → false）
        //    - 切换后 SplashScreen 被销毁，不再占用资源
    }
}

/**
 * Context 扩展函数：为相机拍照创建临时图片文件 Uri
 * 
 * ============================================
 * 功能说明
 * ============================================
 * 该函数为相机拍照场景提供安全的文件访问路径
 * 解决 Android 7.0+ 禁止直接使用 file:// Uri 的限制
 * 
 * ============================================
 * 使用场景
 * ============================================
 * 当用户点击「拍照」按钮时：
 * 1. 调用此函数创建临时文件 Uri
 * 2. 将 Uri 传递给相机 Intent（EXTRA_OUTPUT）
 * 3. 相机拍照后将图片保存到该 Uri 对应的文件
 * 4. 应用读取文件进行后续处理（裁剪、编辑等）
 * 
 * 代码示例：
 * ```kotlin
 * val photoUri = context.createImageFile()
 * val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
 *     putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
 * }
 * cameraLauncher.launch(cameraIntent)
 * ```
 * 
 * ============================================
 * 技术要点：FileProvider 安全访问
 * ============================================
 * 
 * 【Android 7.0 之前】
 * - 可以直接使用 file:///storage/emulated/0/xxx.jpg
 * - 任何应用都能访问该路径（安全隐患）
 * 
 * 【Android 7.0+ (API 24+)】
 * - 禁止使用 file:// Uri 分享文件
 * - 如果强行使用会抛出 FileUriExposedException
 * - 必须使用 content:// Uri（通过 FileProvider）
 * 
 * 【FileProvider 原理】
 * - 在应用内部创建临时文件
 * - 通过 content:// Uri 暴露给其他应用（如相机）
 * - 系统自动管理权限，读取完成后自动回收
 * 
 * ============================================
 * 配置要求（AndroidManifest.xml）
 * ============================================
 * ```xml
 * <provider
 *     android:name="androidx.core.content.FileProvider"
 *     android:authorities="com.example.xingtuclone.fileprovider"
 *     android:exported="false"
 *     android:grantUriPermissions="true">
 *     <meta-data
 *         android:name="android.support.FILE_PROVIDER_PATHS"
 *         android:resource="@xml/file_paths" />
 * </provider>
 * ```
 * 
 * res/xml/file_paths.xml：
 * ```xml
 * <paths>
 *     <external-cache-path name="my_images" path="/" />
 * </paths>
 * ```
 * 
 * ============================================
 * @return 图片文件的 content:// Uri
 *         格式：content://com.example.xingtuclone.fileprovider/my_images/JPEG_20231215_143052_xxx.jpg
 *         
 * @throws IllegalArgumentException 如果 FileProvider 配置错误
 * @throws IOException 如果磁盘空间不足或权限不足
 * ============================================
 */
fun Context.createImageFile(): Uri {
    // ============================================
    // 生成唯一文件名（基于时间戳）
    // ============================================
    // 格式：JPEG_20231215_143052_
    // - yyyyMMdd：年月日（避免日期冲突）
    // - HHmmss：时分秒（避免同一天内冲突）
    // - Locale.getDefault()：使用系统默认时区
    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    val imageFileName = "JPEG_" + timeStamp + "_"
    // 最终文件名示例：JPEG_20231215_143052_1234567890.jpg
    // 后缀 _1234567890 由 createTempFile 自动添加（保证唯一性）

    // ============================================
    // 创建临时文件
    // ============================================
    // File.createTempFile() 参数说明：
    // @param prefix 前缀（至少 3 个字符）：JPEG_20231215_143052_
    // @param suffix 后缀：.jpg
    // @param directory 目标目录：externalCacheDir
    // 
    // externalCacheDir 路径示例：
    // /storage/emulated/0/Android/data/com.example.xingtuclone/cache/
    // 
    // 为什么用 externalCacheDir 而不是其他目录？
    // - externalCacheDir：外部缓存（应用卸载时自动清理）
    // - filesDir：内部存储（需要手动清理，占用应用配额）
    // - getExternalStoragePublicDirectory：共享存储（Android 10+ 需要 SAF）
    // 
    // ⚠️ 重点：目录必须与 file_paths.xml 中的 <external-cache-path> 配置一致
    val image = File.createTempFile(
        imageFileName,  // 前缀：JPEG_20231215_143052_
        ".jpg",         // 后缀：.jpg（确保相机识别为图片）
        externalCacheDir // 目录：外部缓存目录
    )
    // 创建结果示例：
    // /storage/emulated/0/Android/data/.../cache/JPEG_20231215_143052_1234567890.jpg

    // ============================================
    // 转换为 FileProvider Uri
    // ============================================
    // FileProvider.getUriForFile() 参数说明：
    // @param context 上下文（this，因为这是扩展函数）
    // @param authority FileProvider 的授权标识（必须与 Manifest 中一致）
    // @param file 要分享的文件对象
    // 
    // 返回的 Uri 格式：
    // content://com.example.xingtuclone.fileprovider/my_images/JPEG_20231215_143052_xxx.jpg
    // 
    // content:// 的优势：
    // - 系统自动管理权限（临时授权给相机应用）
    // - 无需手动申请 READ_EXTERNAL_STORAGE 权限
    // - 读取完成后自动回收权限，安全性更高
    return FileProvider.getUriForFile(
        this,                                        // Context 对象
        "com.example.xingtuclone.fileprovider",    // Authority（与 Manifest 一致）
        image                                        // 文件对象
    )
    
    // ============================================
    // 常见错误排查
    // ============================================
    // 1. IllegalArgumentException: Failed to find configured root
    //    - 检查 file_paths.xml 是否正确配置 <external-cache-path>
    //    - 检查 directory 是否为 externalCacheDir
    // 
    // 2. FileUriExposedException
    //    - 说明直接使用了 file:// Uri，必须改用 FileProvider
    // 
    // 3. IOException: No space left on device
    //    - 磁盘空间不足，提示用户清理缓存
    // 
    // 4. SecurityException: Permission Denial
    //    - 检查 Manifest 中 grantUriPermissions="true"
    //    - 确保 Intent 添加了 FLAG_GRANT_READ_URI_PERMISSION
}
