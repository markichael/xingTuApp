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
    
    /**
     * 批量修图页
     * 
     * 功能说明：
     * - 同时对多张图片应用相同的编辑参数
     * - 支持的操作：滞镜、调整、裁剪、添加水印
     * - 异步导出：后台队列处理，不阻塞 UI
     * - 进度提示：实时显示已处理/总数
     * 
     * @param uris 批量处理的图片列表（1-20 张）
     *        - 最少 1 张：至少需要一张图片
     *        - 最多 20 张：限制是为了控制内存占用和处理时间
     *        - 超过 20 张建议分多次处理
     * 
     * 技术实现：
     * - 异步队列：使用 Kotlin Coroutines + Flow
     * - 多线程处理：利用 Dispatchers.Default 并行处理
     * - 统一导出：通过 Exporter 单例封装导出逻辑
     * - 进度回调：实时更新 UI 进度条
     */
    data class BatchEdit(val uris: List<Uri>) : AppRoute()
    
    /**
     * 裁剪旋转页
     * 
     * 功能说明：
     * - 集成 uCrop 开源库实现专业级裁剪功能
     * - 支持自由裁剪、固定比例裁剪（1:1、4:3、16:9 等）
     * - 支持 90° 旋转、水平/垂直翻转
     * - 支持网格线、九宫格辅助线
     * 
     * @param uri 待裁剪的图片
     *        - 只支持单图裁剪
     *        - 支持 JPEG/PNG/WebP 格式
     *        - 裁剪后输出为 JPEG 格式（可配置）
     * 
     * uCrop 库特性：
     * - 高性能：基于 OpenGL 渲染，流畅不卡顿
     * - 精准控制：支持像素级精度裁剪
     * - 丰富配置：可自定义 UI 颜色、裁剪边框等
     * 
     * 使用场景：
     * - 社交媒体头像：裁剪成 1:1 正方形
     * - 视频封面：裁剪成 16:9 比例
     * - 海报制作：自由裁剪成任意尺寸
     */
    data class CropRotate(val uri: Uri) : AppRoute()
    
    /**
     * 保存结果展示页
     * 
     * 功能说明：
     * - 显示图片保存成功的确认页面
     * - 提供快捷操作：分享、查看原图、继续编辑
     * - 显示保存路径和文件信息
     * 
     * @param uri 已保存图片的 Uri
     *        - content:// 格式：系统相册 Uri
     *        - file:// 格式：应用私有目录文件
     *        - 用于显示图片预览和分享
     * 
     * 界面元素：
     * - 图片预览：显示保存后的效果
     * - 保存信息：文件名、尺寸、保存路径
     * - 操作按钮：
     *   * 分享：调用系统分享面板
     *   * 查看：在系统相册中打开
     *   * 继续编辑：返回编辑器
     *   * 完成：返回首页
     * 
     * 动画效果：
     * - 成功动画：绿色对勾 + 缩放效果
     * - 图片淡入：从透明度 0 → 1
     * - 按钮上滑：从底部滑入
     */
    data class SaveResult(val uri: Uri) : AppRoute()
}
