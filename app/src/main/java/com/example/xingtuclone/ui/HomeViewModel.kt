/**
 * ============================================
 * HomeViewModel.kt - 全局路由状态管理器
 * ============================================
 * 功能说明：
 * 管理应用的页面导航状态，提供统一的路由跳转方法
 * 
 * 架构模式：
 * - MVI (Model-View-Intent) 单向数据流
 * - StateFlow 管理状态，确保状态不可变性
 * - UI 层仅消费状态，不直接修改
 * 
 * 使用示例：
 * ```kotlin
 * val vm: HomeViewModel = viewModel()
 * val route = vm.route.collectAsState()
 * vm.toGallery("face", false, 1) // 跳转到相册选择页
 * ```
 * ============================================
 */
package com.example.xingtuclone.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import com.example.xingtuclone.ui.navigation.AppRoute

/**
 * 首页路由状态管理 ViewModel
 * 负责统一管理全局页面导航状态
 */
class HomeViewModel : ViewModel() {
    // 私有可变状态，仅内部修改
    private val _route = MutableStateFlow<AppRoute>(AppRoute.Home)
    // 对外暴露的不可变状态，UI 层只读
    val route: StateFlow<AppRoute> = _route

    /** 返回首页 */
    fun toHome() { _route.value = AppRoute.Home }
    
    /** 跳转到相册选择页 */
    fun toGallery(target: String, allowMulti: Boolean, max: Int) { 
        _route.value = AppRoute.Gallery(target, allowMulti, max) 
    }
    
    /** 跳转到编辑器 */
    fun toEditor(uris: List<Uri>, initial: EditorCategory) { 
        _route.value = AppRoute.Editor(uris, initial) 
    }
    
    /** 跳转到AI魔法消除 */
    fun toMagicErase(uri: Uri) { _route.value = AppRoute.MagicErase(uri) }
    
    /** 跳转到创意拼图 */
    fun toCollage(uris: List<Uri>) { _route.value = AppRoute.Collage(uris) }
    
    /** 跳转到批量修图 */
    fun toBatch(uris: List<Uri>) { _route.value = AppRoute.BatchEdit(uris) }
    
    /** 跳转到裁剪旋转 */
    fun toCrop(uri: Uri) { _route.value = AppRoute.CropRotate(uri) }
    
    /** 跳转到保存结果页 */
    fun toSaveResult(uri: Uri) { _route.value = AppRoute.SaveResult(uri) }
}
