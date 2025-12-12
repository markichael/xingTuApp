/**
 * ============================================
 * AppRoute.kt - 路由状态定义
 * ============================================
 * 功能说明：
 * 使用 Sealed Class 统一定义应用所有页面路由状态
 * 
 * 架构亮点：
 * - 替代原来 192+ 行分散的 rememberSaveable 状态
 * - 配合 HomeViewModel 实现 MVI 单向数据流
 * - 类型安全，编译时即可检测错误
 * 
 * 路由列表：
 * - Home: 首页
 * - Gallery: 相册选择页
 * - Editor: 综合编辑器（美颜/滤镜/调整/特效）
 * - MagicErase: AI魔法消除
 * - Collage: 创意拼图
 * - BatchEdit: 批量修图
 * - CropRotate: 裁剪旋转
 * - SaveResult: 保存结果页
 * ============================================
 */
package com.example.xingtuclone.ui.navigation

import android.net.Uri
import com.example.xingtuclone.ui.EditorCategory

/**
 * 应用路由定义（Sealed Class）
 * 每个子类代表一个页面状态，携带该页面所需的所有参数
 */
sealed class AppRoute {
    /** 首页 - 应用主界面 */
    object Home : AppRoute()
    
    /** 相册选择页
     * @param target 目标功能标识（retouch/face/magic/crop/collage/batch）
     * @param allowMulti 是否允许多选
     * @param max 最大选择数量
     */
    data class Gallery(val target: String, val allowMulti: Boolean, val max: Int) : AppRoute()
    
    /** 综合编辑器
     * @param uris 待编辑图片列表
     * @param initialCategory 初始显示的编辑类别（美颜/滤镜/调整/特效）
     */
    data class Editor(val uris: List<Uri>, val initialCategory: EditorCategory) : AppRoute()
    
    /** AI魔法消除页
     * @param uri 待处理的单张图片
     */
    data class MagicErase(val uri: Uri) : AppRoute()
    
    /** 创意拼图页
     * @param uris 拼图图片列表（2-6张）
     */
    data class Collage(val uris: List<Uri>) : AppRoute()
    
    /** 批量修图页
     * @param uris 批量处理的图片列表（最多20张）
     */
    data class BatchEdit(val uris: List<Uri>) : AppRoute()
    
    /** 裁剪旋转页（集成 uCrop 库）
     * @param uri 待裁剪的图片
     */
    data class CropRotate(val uri: Uri) : AppRoute()
    
    /** 保存结果展示页
     * @param uri 已保存图片的 Uri
     */
    data class SaveResult(val uri: Uri) : AppRoute()
}
