/**
 * ============================================
 * HomeScreen.kt - 应用主入口与路由分发器
 * ============================================
 * 
 * 功能说明：
 * 1. 应用的根 Composable，管理所有页面的显示和切换
 * 2. MVI 架构的 View 层实现，监听路由状态并渲染对应页面
 * 3. 处理相机拍照、相册选择等系统交互
 * 4. 提供首页 UI 和快捷入口
 * 
 * 架构角色：
 * - 在 MVI 架构中，本文件是 View 层
 * - 通过 when(route) 实现路由分发（类似 Navigation Graph）
 * - 状态来源：HomeViewModel.route (StateFlow)
 * - 状态消费：collectAsState() 收集并触发重组
 * 
 * 技术亮点：
 * 1. 单 Activity 架构：所有页面都是 Composable，无需多个 Activity
 * 2. 类型安全路由：Sealed Class 保证编译时检查
 * 3. 声明式 UI：状态变化自动驱动 UI 更新
 * 4. 系统集成：Camera、PhotoPicker、FileProvider
 * 
 * 文件结构：
 * - HomeScreen()：路由分发器（50-142 行）
 * - HomeContent()：首页 UI（148-244 行）
 * - HeaderSection()：顶部 Logo（247-318 行）
 * - MidHeroCard()：中部快捷卡片（321-362 行）
 * - QuickHeroBtn()：快捷按钮组件（365-381 行）
 * - createImageFile()：相机拍照辅助函数（386-407 行）
 * 
 * ============================================
 */
package com.example.xingtuclone.ui

// 自定义颜色
import LightGreenBg  // 浅绿色背景（#E8F5E9）

// Android 核心库
import android.content.Context
import android.net.Uri

// Activity Result API（替代 onActivityResult）
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts

// Compose 基础组件
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

// Material 图标
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*   // Filled 样式图标（实心）
import androidx.compose.material.icons.outlined.* // Outlined 样式图标（描边）

// Material 3 组件
import androidx.compose.material3.*

// Compose Runtime（状态管理）
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.collectAsState  // StateFlow → State 转换

// Compose UI 修饰符和工具
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush  // 渐变画刷
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext  // 获取 Context
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Compose 形状
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape

// Android 文件访问
import androidx.core.content.FileProvider  // 安全文件访问（Android 7.0+）

// 项目内部模块
import com.example.xingtuclone.model.MenuItem
import androidx.lifecycle.viewmodel.compose.viewModel  // ViewModel 注入
import com.example.xingtuclone.ui.navigation.AppRoute  // 路由状态定义
import com.example.xingtuclone.ui.components.BigActionButton
import com.example.xingtuclone.ui.components.MenuGridSection

// Java 工具类
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ============================================
// HomeScreen - 应用根组件与路由分发器
// ============================================

/**
 * HomeScreen - 应用的根 Composable 组件
 * 
 * 职责说明：
 * 1. 监听 HomeViewModel 的路由状态
 * 2. 根据路由状态渲染对应页面（路由分发）
 * 3. 处理系统交互（相机拍照、相册选择）
 * 4. 协调各个子页面的切换和数据传递
 * 
 * MVI 架构体现：
 * ```
 * ViewModel (State) → View (HomeScreen) → User Action (Intent)
 *       ↑                                            ↓
 *       └────────────── State Update ──────────────┘
 * ```
 * 
 * 路由分发原理：
 * - when(route) 表达式根据 Sealed Class 的不同子类型渲染不同页面
 * - 类似于传统的 Navigation Graph，但更加类型安全
 * - 编译时检查：when 表达式必须覆盖所有路由类型
 * 
 * 单 Activity 架构：
 * - 整个应用只有一个 MainActivity
 * - 所有"页面"都是 Composable 函数
 * - 页面切换只是状态变化，不创建新 Activity
 * - 优势：更轻量、动画流畅、状态共享简单
 */
@Composable
fun HomeScreen() {
    // ============================================
    // 依赖注入与状态管理
    // ============================================
    
    /**
     * 获取当前 Context
     * 
     * LocalContext 是 Compose 的 CompositionLocal
     * - 类似于 React 的 Context API
     * - 允许在 Composable 树中隐式传递值
     * - 无需逐层传递参数
     * 
     * 用途：
     * - 创建临时文件（相机拍照）
     * - 启动系统 Activity（分享、查看图片）
     * - 访问资源文件（R.string、R.drawable）
     */
    val context = LocalContext.current
    
    /**
     * 获取 ViewModel 实例
     * 
     * viewModel() 函数特性：
     * - 自动绑定到 Activity/Fragment 生命周期
     * - 配置变化（旋转屏幕）时不会重建
     * - 整个应用共享同一个 HomeViewModel 实例
     * - 在 Activity 销毁时自动清理（调用 onCleared）
     * 
     * 为什么用 viewModel() 而不是 remember？
     * - remember：仅在当前 Composable 作用域内保持
     * - viewModel：在整个 Activity 生命周期内保持
     * - 屏幕旋转时，remember 会丢失，viewModel 不会
     */
    val vm: HomeViewModel = viewModel()
    
    /**
     * 收集路由状态流
     * 
     * collectAsState() 详解：
     * - 将 StateFlow<AppRoute> 转换为 State<AppRoute>
     * - StateFlow：Kotlin Coroutines 的响应式流
     * - State：Compose 的状态对象，变化时触发重组
     * 
     * 数据流向：
     * 1. ViewModel 更新 _route.value = AppRoute.Gallery(...)
     * 2. StateFlow 发射新状态
     * 3. collectAsState() 收集到新状态
     * 4. routeState.value 变化
     * 5. when(routeState.value) 重组，渲染新页面
     * 
     * 生命周期绑定：
     * - 自动在 Composable 进入组合时订阅
     * - 自动在 Composable 离开组合时取消订阅
     * - 避免内存泄漏
     */
    val routeState = vm.route.collectAsState()

    // ============================================
    // 系统交互 - 相机拍照
    // ============================================
    
    /**
     * 相机拍照启动器
     * 
     * Activity Result API 详解：
     * - 替代已废弃的 onActivityResult 方式
     * - 类型安全：TakePicture 契约自动处理 Intent 构建
     * - 权限处理：自动请求相机权限
     * 
     * ActivityResultContracts.TakePicture()：
     * - 输入：Uri（照片保存位置）
     * - 输出：Boolean（拍照是否成功）
     * 
     * 调用流程：
     * 1. cameraLauncher.launch(uri)
     * 2. 系统打开相机应用
     * 3. 用户拍照或取消
     * 4. 回调 onResult { success ->
     *      if (success) 照片已保存到 uri
     *      else 用户取消拍照
     *    }
     * 
     * rememberLauncherForActivityResult 特性：
     * - remember 保证 launcher 不会在重组时重建
     * - 生命周期绑定，Composable 销毁时自动清理
     */
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
        onResult = { success ->
            // TODO: 当前实现为空
            // 建议：if (success && tempCameraUri != null) { vm.toEditor(...) }
        }
    )
    
    /**
     * 临时相机照片 Uri
     * 
     * rememberSaveable 详解：
     * - 不仅在重组时保持状态（like remember）
     * - 还能在进程重建时恢复状态（如系统回收内存）
     * - 自动序列化和反序列化（Uri 实现了 Parcelable）
     * 
     * 为什么需要 tempCameraUri？
     * - 相机拍照需要预先指定保存位置
     * - 拍照前创建 Uri，拍照后使用这个 Uri 加载图片
     * - 保存在状态中，避免重组时丢失
     * 
     * 使用场景：
     * 1. 用户点击"相机"按钮
     * 2. 创建临时文件 Uri
     * 3. 保存到 tempCameraUri
     * 4. 启动相机 cameraLauncher.launch(tempCameraUri)
     * 5. 拍照完成后使用 tempCameraUri 访问照片
     */
    var tempCameraUri by rememberSaveable { mutableStateOf<Uri?>(null) }

    // ============================================
    // 路由分发 - MVI 架构的核心
    // ============================================
    
    /**
     * when 表达式路由分发
     * 
     * Sealed Class 穷尽性检查：
     * - AppRoute 是 sealed class，编译器知道所有子类型
     * - when 表达式必须覆盖所有子类型，否则编译报错
     * - 添加新路由时，编译器会提示所有需要更新的地方
     * 
     * 智能类型转换：
     * - when (val r = routeState.value) 解构赋值
     * - 在每个分支中，r 自动转换为对应的子类型
     * - 例如：is AppRoute.Gallery 分支中，r.target、r.allowMulti 可直接访问
     * 
     * 路由参数传递：
     * - 每个路由携带自己的参数（data class 属性）
     * - 例如：Gallery(target, allowMulti, max)
     * - 类型安全，无需字符串 key 解析（如 Bundle）
     * 
     * 对比传统 Navigation：
     * - 传统：navigate("gallery?target=face&multi=false")
     *   * 字符串拼接，容易出错
     *   * 运行时解析，类型不安全
     * - Sealed Class：AppRoute.Gallery("face", false, 1)
     *   * 编译时检查
     *   * 强类型参数
     */
    when (val r = routeState.value) {
        // ============================================
        // 首页路由
        // ============================================
        is AppRoute.Home -> {
            /**
             * 渲染首页内容
             * 
             * 参数设计：
             * - 所有参数都是回调函数（Lambda）
             * - 遵循 Compose 的"提升状态"原则
             * - HomeContent 是无状态组件，不持有 ViewModel
             * - 所有业务逻辑在 HomeScreen 中处理
             * 
             * 回调函数详解：
             * 
             * 1. onImportClick：导入照片
             *    - toGallery("retouch", false, 1)
             *    - target="retouch"：进入综合编辑器
             *    - allowMulti=false：单选模式
             *    - max=1：最多选 1 张
             * 
             * 2. onCameraClick：相机拍照
             *    - createImageFile()：创建临时文件（content:// Uri）
             *    - 保存到 tempCameraUri（状态保持）
             *    - cameraLauncher.launch(uri)：启动系统相机
             *    - 拍照成功后导航到滤镜编辑器
             *    
             *    ⚠️ 注意：当前逻辑有问题
             *    - launch() 是异步的，立即执行下一行代码
             *    - tempCameraUri?.let { } 会立即执行（此时照片还未拍摄）
             *    - 应该移到 cameraLauncher 的 onResult 回调中
             * 
             * 3. onFaceBeautyClick：AI 修人像
             *    - toGallery("face", false, 1)
             *    - target="face"：标记为人像模式
             *    - 选择后进入人像美颜编辑器
             * 
             * 4. onCollageClick：创意拼图
             *    - toGallery("collage", true, 6)
             *    - allowMulti=true：多选模式
             *    - max=6：最多选 6 张（拼图限制）
             * 
             * 5. onBatchClick：批量修图
             *    - toGallery("batch", true, 20)
             *    - max=20：最多选 20 张（性能限制）
             * 
             * 6. onMagicEraseClick：AI 魔法消除
             *    - toGallery("magic", false, 1)
             *    - target="magic"：标记为 AI 消除模式
             *    - 选择后进入涂抹消除页面
             * 
             * 7. onCropRotateClick：裁剪旋转
             *    - toGallery("crop", false, 1)
             *    - 选择后进入 uCrop 裁剪页面
             */
            HomeContent(
                onImportClick = { vm.toGallery("retouch", false, 1) },
                onCameraClick = {
                    val uri = context.createImageFile()
                    tempCameraUri = uri
                    cameraLauncher.launch(uri)
                    // ⚠️ BUG: 这行代码会立即执行，应该移到 onResult 回调
                    tempCameraUri?.let { vm.toEditor(listOf(it), EditorCategory.FILTER) }
                },
                onFaceBeautyClick = { vm.toGallery("face", false, 1) },
                onCollageClick = { vm.toGallery("collage", true, 6) },
                onBatchClick = { vm.toGallery("batch", true, 20) },
                onMagicEraseClick = { vm.toGallery("magic", false, 1) },
                onCropRotateClick = { vm.toGallery("crop", false, 1) }
            )
        }
        // ============================================
        // 相册选择路由
        // ============================================
        is AppRoute.Gallery -> {
            /**
             * 渲染相册选择页面
             * 
             * 路由参数传递：
             * - r.target：目标功能标识符（"retouch", "face", "magic" 等）
             * - r.allowMulti：是否允许多选
             * - r.max：最大选择数量
             * 
             * 智能类型转换：
             * - 在 is AppRoute.Gallery 分支中，r 自动转换为 AppRoute.Gallery 类型
             * - 可以直接访问 r.target、r.allowMulti、r.max
             * - 编译器保证类型安全
             * 
             * 回调函数详解：
             * 
             * 1. onBack：返回首页
             *    - 用户点击左上角返回按钮
             *    - 导航回 AppRoute.Home
             * 
             * 2. onImageSelected：单选模式回调
             *    - 当 allowMulti=false 时触发
             *    - 参数：选中的图片 Uri
             *    - 根据 target 导航到不同页面：
             *      * "retouch" → 滤镜编辑器
             *      * "face" → 人像美颜编辑器（PORTRAIT 类别）
             *      * "magic" → AI 魔法消除
             *      * "crop" → 裁剪旋转
             *      * else → 默认滤镜编辑器
             * 
             * 3. onImagesSelected：多选模式回调
             *    - 当 allowMulti=true 时触发
             *    - 参数：选中的图片 Uri 列表
             *    - 根据 target 导航：
             *      * "collage" → 创意拼图（2-6 张）
             *      * "batch" → 批量修图（最多 20 张）
             *      * else → 取第一张进入编辑器（降级处理）
             * 
             * EditorCategory 说明：
             * - FILTER：滤镜 Tab（复古、清新、黑白等）
             * - PORTRAIT：人像 Tab（美颜、瘦脸、大眼等）
             * - 决定编辑器打开时默认显示哪个 Tab
             */
            GalleryScreen(
                onBack = { vm.toHome() },
                onImageSelected = { uri ->
                    when (r.target) {
                        "retouch" -> vm.toEditor(listOf(uri), EditorCategory.FILTER)
                        "face" -> vm.toEditor(listOf(uri), EditorCategory.PORTRAIT)
                        "magic" -> vm.toMagicErase(uri)
                        "crop" -> vm.toCrop(uri)
                        else -> vm.toEditor(listOf(uri), EditorCategory.FILTER)
                    }
                },
                onImagesSelected = { uris ->
                    when (r.target) {
                        "collage" -> vm.toCollage(uris)
                        "batch" -> vm.toBatch(uris)
                        else -> if (uris.isNotEmpty()) vm.toEditor(listOf(uris.first()), EditorCategory.FILTER)
                    }
                },
                allowMultiSelect = r.allowMulti,
                maxSelection = r.max
            )
        }
        // ============================================
        // 综合编辑器路由
        // ============================================
        is AppRoute.Editor -> {
            /**
             * 渲染综合编辑器（四合一编辑器）
             * 
             * 功能模块：
             * - 美颜：磨皮、瘦脸、大眼、美白等
             * - 滤镜：复古、清新、黑白、日系等
             * - 调整：亮度、对比度、饱和度、色温等
             * - 特效：模糊、锐化、晶粒、暗角等
             * 
             * 参数说明：
             * - imageUris：待编辑图片列表（支持批量）
             * - initialCategory：初始显示的类别（FILTER/PORTRAIT/ADJUST/EFFECT）
             * - onBack：返回首页
             * - onSaved：保存成功后导航到结果展示页
             */
            EditorShell(
                imageUris = r.uris,
                onBack = { vm.toHome() },
                initialCategory = r.initialCategory,
                onSaved = { uri -> vm.toSaveResult(uri) }
            )
        }
        
        // ============================================
        // AI 魔法消除路由
        // ============================================
        is AppRoute.MagicErase -> {
            /**
             * 渲染 AI 魔法消除页面
             * 
             * 技术实现：
             * - ONNX Runtime 端侧 AI 推理
             * - LaMa SOTA 图像修复模型
             * - 用户涂抹 → AI 智能填充
             * 
             * 参数说明：
             * - imageUri：单张待处理图片
             * - onBack：返回首页
             * - onSaved：处理完成后显示结果
             * 
             * 性能优化：
             * - 分块推理：避免大图 OOM
             * - 模型量化：FP32 → FP16（体积减半）
             * - 异步加载：模型在启动页预加载
             */
            MagicEraseScreen(
                imageUri = r.uri,
                onBack = { vm.toHome() },
                onSaved = { uri -> vm.toSaveResult(uri) }
            )
        }
        
        // ============================================
        // 创意拼图路由
        // ============================================
        is AppRoute.Collage -> {
            /**
             * 渲染创意拼图页面
             * 
             * 功能特性：
             * - 14 种布局模板（2-6 图）
             * - 拖拽交换图片位置
             * - 边框宽度/颜色配置
             * - 一键分享
             * 
             * 参数说明：
             * - imageUris：拼图图片列表（2-6 张）
             * - onBack：返回首页
             * - onHome：完成后返回首页
             */
            CollageScreen(
                imageUris = r.uris,
                onBack = { vm.toHome() },
                onHome = { vm.toHome() }
            )
        }
        
        // ============================================
        // 批量修图路由
        // ============================================
        is AppRoute.BatchEdit -> {
            /**
             * 渲染批量修图页面
             * 
             * 功能特性：
             * - 统一滤镜：一键应用到所有图片
             * - 批量调整：亮度、对比度、饱和度
             * - 批量裁剪：统一比例裁剪
             * - 批量导出：队列异步保存
             * 
             * 参数说明：
             * - imageUris：批量图片列表（最多 20 张）
             * - onBack：返回首页
             */
            BatchEditScreen(imageUris = r.uris, onBack = { vm.toHome() })
        }
        
        // ============================================
        // 裁剪旋转路由
        // ============================================
        is AppRoute.CropRotate -> {
            /**
             * 渲染裁剪旋转页面
             * 
             * 技术实现：
             * - 基于 uCrop 开源库
             * - OpenGL 硬件加速
             * - 像素级精准控制
             * 
             * 功能特性：
             * - 自由裁剪/固定比例（1:1, 4:3, 16:9）
             * - 90° 旋转
             * - 水平/垂直翻转
             * - 九宫格辅助线
             * 
             * 参数说明：
             * - srcUri：原始图片
             * - onBack：返回首页
             * - onDone：裁剪完成后进入滤镜编辑器（继续编辑）
             */
            CropRotateScreen(
                srcUri = r.uri,
                onBack = { vm.toHome() },
                onDone = { out -> vm.toEditor(listOf(out), EditorCategory.FILTER) }
            )
        }
        
        // ============================================
        // 保存结果展示路由
        // ============================================
        is AppRoute.SaveResult -> {
            /**
             * 渲染保存成功页面
             * 
             * 功能特性：
             * - 成功动画（绿色对勾 + 缩放）
             * - 图片预览
             * - 文件信息（名称、尺寸、大小、路径）
             * - 快捷操作（分享、查看、继续编辑）
             * 
             * 参数说明：
             * - savedUri：已保存图片的 Uri
             * - onBack：返回上一页
             * - onHome：返回首页
             * - onRetouch：继续编辑（重新进入编辑器）
             */
            SaveResultScreen(
                savedUri = r.uri,
                onBack = { vm.toHome() },
                onHome = { vm.toHome() },
                onRetouch = { vm.toGallery("retouch", false, 1) }
            )
        }
    }
}

// ============================================
// HomeContent - 首页 UI 内容
// ============================================

/**
 * HomeContent - 首页主内容 Composable
 * 
 * 设计原则：
 * - 无状态组件（Stateless Component）
 * - 不持有 ViewModel，不直接管理状态
 * - 所有业务逻辑通过回调函数传递
 * - 遵循 Compose 的"提升状态"模式
 * 
 * 提升状态模式详解：
 * - State：状态向上传递到父组件（HomeScreen）
 * - Events：事件向下传递给子组件（HomeContent）
 * - 优势：组件可复用、易于测试、逻辑分离
 * 
 * UI 结构：
 * ```
 * Scaffold
 *   ├─ Column
 *   │   ├─ HeaderSection (顶部 Logo)
 *   │   ├─ BigActionButton x 4 (主要操作按钮)
 *   │   ├─ MidHeroCard (快捷入口卡片)
 *   │   └─ MenuGridSection (功能网格)
 * ```
 * 
 * @param onImportClick 导入照片回调（单图编辑）
 * @param onCameraClick 相机拍照回调
 * @param onFaceBeautyClick AI 修人像回调（人脸检测 + 美颜）
 * @param onCollageClick 创意拼图回调（多图拼接）
 * @param onBatchClick 批量修图回调（最多 20 张）
 * @param onMagicEraseClick AI 魔法消除回调（ONNX 推理）
 * @param onCropRotateClick 裁剪旋转回调（uCrop 库）
 */
@Composable
fun HomeContent(
    onImportClick: () -> Unit,
    onCameraClick: () -> Unit,
    onFaceBeautyClick: () -> Unit,
    onCollageClick: () -> Unit,
    onBatchClick: () -> Unit,
    onMagicEraseClick: () -> Unit,
    onCropRotateClick: () -> Unit
) {
    /**
     * 底部菜单项配置
     * 
     * MenuItem 数据类：
     * - title: String (显示文本)
     * - icon: ImageVector (Material Icons)
     * 
     * 为什么在函数内部定义？
     * - 这是静态配置，不需要外部传入
     * - 每次重组都会重新创建列表（但性能影响可忽略）
     * - 如果需要优化，可使用 remember { listOf(...) }
     * 
     * Icons 样式选择：
     * - Icons.Outlined：描边样式，更轻盈
     * - Icons.Default/Filled：填充样式，更突出
     * - 根据 UI 设计风格选择
     */
    val menuItems = listOf(
        MenuItem("批量修图", Icons.Outlined.PhotoLibrary),
        MenuItem("魔法消除", Icons.Default.AutoFixHigh),
        MenuItem("AI修图", Icons.Default.AutoAwesome),
        MenuItem("拼图", Icons.Default.Dashboard)
    )

    /**
     * Scaffold - Material 3 脚手架
     * 
     * Scaffold 特性：
     * - 提供标准化的布局结构
     * - 支持 TopBar、BottomBar、FAB、SnackBar 等
     * - 自动处理 padding（避免内容被边栏遮挡）
     * 
     * containerColor 参数：
     * - 设置背景色为白色
     * - 默认为 Material Theme 的 background 颜色
     * - 覆盖默认值以符合设计需求
     * 
     * paddingValues 参数：
     * - Scaffold 自动计算的内边距
     * - 包含 TopBar、BottomBar、SystemBars 的高度
     * - 必须应用到内容区域，否则内容会被遮挡
     */
    Scaffold(
        containerColor = Color.White  // 白色背景，简洁风格
    ) { paddingValues ->
        /**
         * 主内容垂直布局
         * 
         * Column 特性：
         * - 垂直排列子组件（从上到下）
         * - verticalArrangement 控制垂直间距
         * - horizontalAlignment 控制水平对齐
         * 
         * Modifier 链式调用：
         * - fillMaxSize()：填满父容器
         * - padding(paddingValues)：应用 Scaffold 的内边距
         * - padding(horizontal = 16.dp)：左右内边距 16dp
         * 
         * 为什么有两个 padding？
         * - 第一个：处理系统栏、导航栏的高度
         * - 第二个：应用层面的边距（UI 设计规范）
         */
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)          // Scaffold 内边距
                .padding(horizontal = 16.dp),    // 应用层左右边距
            verticalArrangement = Arrangement.Top  // 从顶部开始排列
        ) {
            /**
             * 顶部内容容器
             * 
             * 为什么又嵌套一层 Column？
             * - 分组逻辑上相关的组件（头部 + 按钮 + 卡片）
             * - 便于修改整体布局（如添加滚动）
             * - 保持代码结构清晰
             */
            Column(modifier = Modifier.fillMaxWidth().padding(top = 48.dp)) {
                // 顶部 Logo 区域
                HeaderSection()
                
                // 适当的空白（视觉呼吸）
                Spacer(modifier = Modifier.height(64.dp))
                /**
                 * 主要操作按钮行 1
                 * 
                 * Row 布局：
                 * - horizontalArrangement.spacedBy(12.dp)：水平间距 12dp
                 * - weight(1f)：按钮均分剩余宽度（各占 50%）
                 * 
                 * BigActionButton 组件：
                 * - 自定义的大型 CTA（Call To Action）按钮
                 * - 支持自定义背景色、内容色、图标、文本
                 * 
                 * 设计语言：
                 * - 黑色按钮：主要操作（导入照片）
                 * - 浅绿色按钮：次要操作（相机）
                 */
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 主要按钮：导入照片
                    BigActionButton(
                        text = "+ 导入照片",
                        icon = Icons.Default.Image,
                        backgroundColor = Color.Black,      // 黑色背景
                        contentColor = Color.White,         // 白色文字/图标
                        modifier = Modifier.weight(1f),     // 占 50% 宽度
                        onClick = onImportClick
                    )
                    // 次要按钮：相机拍照
                    BigActionButton(
                        text = "相机",
                        icon = Icons.Default.CameraAlt,
                        backgroundColor = LightGreenBg,     // 浅绿色背景 (#E8F5E9)
                        contentColor = Color.Black,
                        modifier = Modifier.weight(1f),
                        onClick = onCameraClick
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                /**
                 * 主要操作按钮行 2
                 * 
                 * 功能按钮：
                 * - AI 修人像：人脸检测 + 智能美颜
                 * - 拼图：创意拼图，支持 2-6 图
                 * 
                 * 设计统一性：
                 * - 两个按钮都使用浅绿色（保持视觉一致）
                 * - 均分宽度（weight(1f)）
                 */
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    BigActionButton(
                        text = "AI修人像",
                        icon = Icons.Default.FaceRetouchingNatural,  // Material Icons 人脸图标
                        backgroundColor = LightGreenBg,
                        contentColor = Color.Black,
                        modifier = Modifier.weight(1f),
                        onClick = onFaceBeautyClick
                    )
                    BigActionButton(
                        text = "拼图",
                        icon = Icons.Default.Dashboard,  // 仪表盘图标（代表网格布局）
                        backgroundColor = LightGreenBg,
                        contentColor = Color.Black,
                        modifier = Modifier.weight(1f),
                        onClick = onCollageClick
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                /**
                 * 中部快捷入口卡片
                 * 
                 * 功能说明：
                 * - 展示热门工具的快捷入口
                 * - 渐变背景 + 图标按钮组
                 * - 提升用户发现性和操作效率
                 * 
                 * 参数传递：
                 * - 将回调函数传递给子组件
                 * - 子组件不持有业务逻辑，仅负责 UI 展示
                 */
                MidHeroCard(
                    onMagicEraseClick = onMagicEraseClick,
                    onCollageClick = onCollageClick,
                    onFaceBeautyClick = onFaceBeautyClick,
                    onCropRotateClick = onCropRotateClick
                )
            }
            /**
             * 垂直弹性空间
             * 
             * weight(1f) 特性：
             * - 占据所有剩余空间
             * - 将底部菜单推到屏幕最下方
             * - 类似 CSS 的 flex-grow: 1
             * 
             * 布局效果：
             * - 上方：顶部 + 按钮 + 卡片（固定高度）
             * - 中间：Spacer 弹性伸缩
             * - 下方：菜单网格（始终在底部）
             */
            Spacer(modifier = Modifier.weight(1f))
            /**
             * 底部功能菜单网格
             * 
             * Box 容器：
             * - 允许堆叠子组件
             * - 提供额外的 padding 控制
             * 
             * MenuGridSection 组件：
             * - 使用 LazyVerticalGrid 实现 4 列网格
             * - 支持懒加载（仅渲染可见项）
             * - 自定义点击效果（按下缩放 + 背景变色）
             * 
             * 点击事件分发：
             * - when(item.title) 根据文本匹配功能
             * - 调用对应的回调函数
             * 
             * ⚠️ 优化建议：
             * - 使用 Enum 替代字符串匹配
             * - MenuItem 添加 id 字段，避免硬编码文本
             */
            Box(modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)) {
                MenuGridSection(menuItems) { item ->
                    when (item.title) {
                        "批量修图" -> onBatchClick()
                        "AI修图" -> onFaceBeautyClick()
                        "魔法消除" -> onMagicEraseClick()
                        "拼图" -> onCollageClick()
                        else -> {}  // 默认不处理
                    }
                }
            }
        }
    }
}

// ============================================
// HeaderSection - 顶部 Logo 区域
// ============================================

/**
 * HeaderSection - 顶部品牌标识区域
 * 
 * 设计特色：
 * - 中心对称布局
 * - 渐变色彩圆形图标 + 品牌名 + 装饰小图标
 * - 色彩丰富（黄绿色 + 青蓝色 + 黑色）
 * - 阴影效果增加层次感
 * 
 * 视觉元素：
 * 1. 左侧大图标：主视觉焦点（64dp 大小）
 * 2. 中间标题："醒图" + 下划线装饰
 * 3. 右侧小图标：平衡视觉（36dp 大小）
 * 
 * 技术亮点：
 * - 径向渐变（Radial Gradient）
 * - 阴影裁剪（clip = true）
 * - 半透明边框（0x88FFFFFF）
 */
@Composable
fun HeaderSection() {
    /**
     * 水平布局 - 中心对齐
     * 
     * Row 参数：
     * - modifier.fillMaxWidth()：横向填满
     * - verticalAlignment.CenterVertically：垂直居中
     * - horizontalArrangement.Center：水平居中
     * 
     * padding 设计：
     * - top = 24.dp：顶部空白
     * - bottom = 16.dp：与下方内容的间距
     */
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp, bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        /**
         * 左侧大图标（主视觉焦点）
         * 
         * Box 堆叠布局：
         * - size(64.dp)：固定尺寸 64x64 dp
         * - shadow(12.dp, CircleShape, clip = true)：圆形阴影
         *   * 12.dp：阴影模糊半径
         *   * CircleShape：圆形阴影
         *   * clip = true：裁剪阴影（与内容形状一致）
         * 
         * 径向渐变背景：
         * - Brush.radialGradient()：从中心向四周扩散
         * - 颜色过渡：
         *   * 0xFFCCFF00 （黄绿色）→ 中心
         *   * 0xFF00E1FF （青蓝色）→ 中间
         *   * Color.Black （黑色）→ 边缘
         * - radius = 120f：渐变半径（像素）
         * 
         * 半透明白色边框：
         * - border(2.dp, Color(0x88FFFFFF), CircleShape)
         * - 0x88FFFFFF：白色 + 53% 透明度（易读性提示）
         * 
         * 内部图标：
         * - AutoFixHigh：魔法棒图标（代表 AI 修图）
         * - size(28.dp)：图标大小
         * - tint = Color.Black：黑色图标（与渐变背景对比）
         */
        Box(
            modifier = Modifier
                .size(64.dp)
                .shadow(12.dp, CircleShape, clip = true)  // 圆形阴影
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFFCCFF00), Color(0xFF00E1FF), Color.Black),
                        radius = 120f
                    ),
                    shape = CircleShape
                )
                .border(2.dp, Color(0x88FFFFFF), CircleShape),  // 半透明边框
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.AutoFixHigh,
                contentDescription = null,
                tint = Color.Black,
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(modifier = Modifier.width(24.dp))
        /**
         * 中间品牌名 + 下划线装饰
         * 
         * Column 垂直布局：
         * - horizontalAlignment.CenterHorizontally：水平居中
         * 
         * Text 文本样式：
         * - text = "醒图"：应用名称
         * - fontSize = 40.sp：大字号（视觉焦点）
         * - fontWeight = FontWeight.ExtraBold：超粗体（900）
         * - color = Color.Black：黑色文字
         * 
         * 下划线装饰：
         * - Box + background + Brush.horizontalGradient
         * - height(4.dp)：线条高度
         * - width(60.dp)：线条宽度
         * - 水平渐变：黄绿色 → 青蓝色
         * 
         * 设计作用：
         * - 增加视觉层次
         * - 突出品牌名
         * - 与图标渐变色彩呼应
         */
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "醒图",
                fontSize = 40.sp,
                fontWeight = FontWeight.ExtraBold,  // 900 字重
                color = Color.Black
            )
            // 渐变下划线
            Box(
                modifier = Modifier
                    .height(4.dp)
                    .width(60.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color(0xFFCCFF00), Color(0xFF00E1FF))
                        )
                    )
            )
        }
        Spacer(modifier = Modifier.width(24.dp))
        /**
         * 右侧小图标（平衡视觉）
         * 
         * 设计特点：
         * - 尺寸较小（36dp vs 64dp）
         * - 半透明效果（不夺去主视觉焦点）
         * - 淡蓝色调（与左侧图标色彩呼应）
         * 
         * 半透明径向渐变：
         * - 0x3300E1FF：青蓝色 + 20% 透明度
         * - Color.Transparent：完全透明
         * - 效果：中心淡蓝色，边缘渐隐
         * 
         * 半透明边框：
         * - 0x3300E1FF：与背景同色系
         * - 1.dp：较细的边框（不夺目）
         * 
         * 图标选择：
         * - Icons.Default.Camera：相机图标
         * - tint = Color(0xFF0099CC)：深蓝色（与背景对比）
         * - size(20.dp)：较小的图标尺寸
         */
        Box(
            modifier = Modifier
                .size(36.dp)
                .shadow(8.dp, CircleShape, clip = true)  // 较小的阴影
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0x3300E1FF), Color.Transparent),  // 半透明
                        radius = 60f
                    ),
                    shape = CircleShape
                )
                .border(1.dp, Color(0x3300E1FF), CircleShape),  // 半透明边框
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Camera,
                contentDescription = null,
                tint = Color(0xFF0099CC),  // 深蓝色
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// ============================================
// MidHeroCard - 中部快捷入口卡片
// ============================================

/**
 * MidHeroCard - 中部热门工具快捷入口卡片
 * 
 * 设计目标：
 * - 提供高频使用功能的快捷入口
 * - 增强视觉吸引力（渐变背景 + 圆角卡片）
 * - 提升用户操作效率（一键直达）
 * 
 * 视觉设计：
 * - 160dp 高度卡片
 * - 16dp 圆角
 * - 径向渐变背景（黄绿色 → 青蓝色 → 淡黑色）
 * - 4 个快捷按钮横向排列
 * 
 * 技术实现：
 * - Box 容器 + Column 垂直布局
 * - Brush.radialGradient 径向渐变
 * - Row + QuickHeroBtn 水平按钮组
 * 
 * @param onMagicEraseClick AI 魔法消除回调
 * @param onCollageClick 创意拼图回调
 * @param onFaceBeautyClick AI 修人像回调
 * @param onCropRotateClick 裁剪旋转回调
 */
@Composable
fun MidHeroCard(
    onMagicEraseClick: () -> Unit,
    onCollageClick: () -> Unit,
    onFaceBeautyClick: () -> Unit,
    onCropRotateClick: () -> Unit
) {
    /**
     * 卡片容器
     * 
     * Modifier 链式调用顺序：
     * 1. fillMaxWidth()：填满宽度
     * 2. height(160.dp)：固定高度
     * 3. clip(RoundedCornerShape(16.dp))：裁剪圆角
     * 4. background(Brush.radialGradient)：渐变背景
     * 5. padding(12.dp)：内边距
     * 
     * 径向渐变详解：
     * - 三种颜色过渡：
     *   * 0xFFCCFF00：黄绿色（中心）
     *   * 0xFF00E1FF：青蓝色（中间）
     *   * 0x11000000：淡黑色（边缘，7% 透明度）
     * - radius = 420f：较大的渐变半径，覆盖整个卡片
     * 
     * 为什么 clip 在 background 之前？
     * - 先裁剪形状，再填充背景
     * - 确保渐变背景也是圆角的
     */
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFCCFF00), Color(0xFF00E1FF), Color(0x11000000)),
                    radius = 420f
                )
            )
            .padding(12.dp)
    ) {
        /**
         * 卡片内容垂直布局
         * 
         * 结构：
         * 1. 标题："艺术修图，一键出片"
         * 2. 副标题："热门工具快捷直达"
         * 3. 快捷按钮组：4 个按钮横向排列
         */
        Column(modifier = Modifier.fillMaxWidth()) {
            // 主标题
            Text(
                text = "艺术修图，一键出片",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Spacer(modifier = Modifier.height(8.dp))
            // 副标题
            Text(
                text = "热门工具快捷直达",
                fontSize = 13.sp,
                color = Color(0xFF666666)  // 深灰色
            )
            Spacer(modifier = Modifier.height(12.dp))
            /**
             * 快捷按钮组
             * 
             * Row 布局：
             * - horizontalArrangement.spacedBy(12.dp)：按钮间距 12dp
             * - 4 个按钮横向排列
             * 
             * QuickHeroBtn 组件：
             * - 自定义的小型快捷按钮
             * - 支持按下缩放效果
             * - 半透明背景 + 圆角
             * 
             * 功能按钮：
             * 1. 魔法消除：AI 智能去除物体
             * 2. 拼图：多图创意拼接
             * 3. AI 修图：智能美颜增强
             * 4. 裁剪旋转：图片基础编辑
             */
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                QuickHeroBtn("魔法消除", Icons.Default.AutoFixHigh, onMagicEraseClick)
                QuickHeroBtn("拼图", Icons.Default.Dashboard, onCollageClick)
                QuickHeroBtn("AI修图", Icons.Default.AutoAwesome, onFaceBeautyClick)
                QuickHeroBtn("裁剪旋转", Icons.Default.Crop, onCropRotateClick)
            }
        }
    }
}

// ============================================
// QuickHeroBtn - 快捷按钮组件
// ============================================

/**
 * QuickHeroBtn - 快捷入口按钮组件
 * 
 * 设计特点：
 * - 小巧的按钮，适合横向排列
 * - 按下缩放效果（0.98f）
 * - 半透明背景 + 圆角
 * - 图标 + 文字组合
 * 
 * 交互状态：
 * - 默认：半透明白色背景
 * - 按下：半透明黑色背景 + 98% 缩放
 * 
 * 技术实现：
 * - InteractionSource 收集交互状态
 * - collectIsPressedAsState() 监听按下状态
 * - scale Modifier 实现缩放效果
 * 
 * @param text 按钮文本
 * @param icon 按钮图标
 * @param onClick 点击回调
 */
@Composable
private fun QuickHeroBtn(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    /**
     * 交互状态收集器
     * 
     * MutableInteractionSource 详解：
     * - 用于收集用户交互状态（按下、按住、聚焦等）
     * - remember 保证重组时不重建
     * - collectIsPressedAsState() 转换为 Compose State
     * 
     * 为什么使用 InteractionSource？
     * - 自定义按下效果（缩放 + 背景变色）
     * - indication = null 禁用默认涟漪效果
     * - 实现完全自定义的交互反馈
     */
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    
    /**
     * 按钮容器
     * 
     * Modifier 链详解：
     * 1. clip(RoundedCornerShape(20.dp))：圆角裁剪
     * 2. background()：条件背景色
     *    - pressed → 0x22000000（黑色 13% 透明）
     *    - normal → 0x11FFFFFF（白色 7% 透明）
     * 3. scale()：条件缩放
     *    - pressed → 0.98f（微微缩小）
     *    - normal → 1f（正常大小）
     * 4. padding()：内边距
     * 5. clickable()：点击事件
     *    - interactionSource 传递交互收集器
     *    - indication = null 禁用默认涟漪
     * 
     * 为什么缩放只有 0.98f？
     * - 过大的缩放会显得夹生
     * - 微妙的变化更符合现代 UI 设计
     * - 0.98f 是经验值（参考 iOS/Material Design）
     */
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (pressed) Color(0x22000000) else Color(0x11FFFFFF))
            .scale(if (pressed) 0.98f else 1f)  // 按下缩放效果
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .clickable(interactionSource = interaction, indication = null) { onClick() }
    ) {
        // 图标
        Icon(icon, contentDescription = text, tint = Color.Black, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        // 文字
        Text(text = text, fontSize = 12.sp, color = Color.Black)
    }
}

// ============================================
// createImageFile - 相机拍照辅助函数
// ============================================

/**
 * Context 扩展函数 - 创建相机拍照临时文件
 * 
 * 功能说明：
 * - 在应用私有目录创建临时图片文件
 * - 返回 content:// Uri（通过 FileProvider）
 * - 用于相机拍照的保存目标
 * 
 * Android 文件访问限制：
 * - Android 7.0+ 禁止直接使用 file:// Uri
 * - 必须使用 content:// Uri + FileProvider
 * - 否则抛出 FileUriExposedException
 * 
 * FileProvider 配置要求：
 * 1. AndroidManifest.xml 注册 Provider：
 *    ```xml
 *    <provider
 *        android:name="androidx.core.content.FileProvider"
 *        android:authorities="com.example.xingtuclone.fileprovider"
 *        android:exported="false"
 *        android:grantUriPermissions="true">
 *        <meta-data
 *            android:name="android.support.FILE_PROVIDER_PATHS"
 *            android:resource="@xml/file_paths" />
 *    </provider>
 *    ```
 * 
 * 2. res/xml/file_paths.xml 配置路径：
 *    ```xml
 *    <paths>
 *        <external-cache-path name="camera_images" path="" />
 *    </paths>
 *    ```
 * 
 * 为什么使用 externalCacheDir？
 * - 应用私有目录，不需要存储权限
 * - 缓存文件，系统可自动清理
 * - 卸载应用时自动删除
 * - 适合临时文件场景
 * 
 * @return content:// Uri，用于相机拍照
 * @throws IOException 文件创建失败
 */
fun Context.createImageFile(): Uri {
    /**
     * 步骤 1：生成唯一文件名
     * 
     * SimpleDateFormat 格式：
     * - "yyyyMMdd_HHmmss"：20231215_143052
     * - Locale.getDefault()：使用系统语言
     * 
     * 文件名示例：
     * - JPEG_20231215_143052_
     * - 确保文件名唯一，避免覆盖
     */
    // 1. 创建文件名 (例如: JPEG_20231126_120000_)
    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    val imageFileName = "JPEG_" + timeStamp + "_"

    /**
     * 步骤 2：创建临时文件
     * 
     * File.createTempFile() 参数：
     * - prefix: 文件名前缀（JPEG_20231215_143052_）
     * - suffix: 文件名后缀（.jpg）
     * - directory: 文件保存目录（externalCacheDir）
     * 
     * 最终文件名示例：
     * - JPEG_20231215_143052_1234567890.jpg
     * - 系统自动添加随机数字保证唯一性
     * 
     * 文件路径示例：
     * - /storage/emulated/0/Android/data/com.example.xingtuclone/cache/JPEG_xxx.jpg
     * 
     * ⚠️ 注意：
     * - 必须使用 externalCacheDir
     * - 对应 file_paths.xml 中的 <external-cache-path>
     * - 如果使用其他目录，必须修改 file_paths.xml
     */
    // 2. 创建临时文件
    // 注意：必须使用 externalCacheDir，对应 file_paths.xml 里的 external-cache-path
    val image = File.createTempFile(
        imageFileName,  // 前缀
        ".jpg",        // 后缀
        externalCacheDir  // 目录
    )

    /**
     * 步骤 3：转换为 content:// Uri
     * 
     * FileProvider.getUriForFile() 参数：
     * - context: 当前 Context
     * - authority: Provider 权限标识（必须与 AndroidManifest.xml 一致）
     * - file: 文件对象
     * 
     * authority 格式：
     * - 包名 + ".fileprovider"
     * - com.example.xingtuclone.fileprovider
     * 
     * 返回 Uri 示例：
     * - content://com.example.xingtuclone.fileprovider/camera_images/JPEG_xxx.jpg
     * 
     * ⚠️⚠️⚠️ 警告：
     * - 如果修改了应用包名，必须同时修改：
     *   1. AndroidManifest.xml 中的 authorities
     *   2. 下方字符串的包名部分
     * - 否则会抛出 IllegalArgumentException
     */
    // 3. 获取 URI
    // ⚠️⚠️⚠️ 警告：如果你改了包名，下面字符串必须改成你的包名 + ".fileprovider"
    // 请检查 AndroidManifest.xml 里的 authorities 是否一致
    return FileProvider.getUriForFile(
        this,
        "com.example.xingtuclone.fileprovider",  // 必须与 Manifest 中的 authorities 一致
        image
    )
}
