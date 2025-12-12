/**
 * ============================================
 * Exporter.kt - 统一导出工具类
 * ============================================
 * 功能说明：
 * 封装统一的 Bitmap 导出逻辑，解决了之前各页面导出代码不一致的问题
 * 
 * 架构亮点：
 * - 单例模式（Object），全局只有一个实例
 * - 统一的 Context 和 Uri 返回处理
 * - 支持单图和批量导出
 * 
 * 使用场景：
 * - 拼图功能保存结果
 * - 批量修图导出
 * - AI消除后保存
 * - 编辑器导出
 * ============================================
 */
package com.example.xingtuclone.ui.utils

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.example.xingtuclone.ui.saveBitmapToGalleryReturnUri

/**
 * 统一导出工具单例
 * 提供统一的图片保存接口，消除各页面重复逻辑
 */
object Exporter {
    /**
     * 导出单张 Bitmap 到相册
     * @param context 上下文
     * @param bitmap 待保存的位图
     * @return 保存后的 Uri，失败返回 null
     */
    suspend fun exportBitmap(context: Context, bitmap: Bitmap): Uri? {
        return saveBitmapToGalleryReturnUri(context, bitmap)
    }

    /**
     * 批量导出 Bitmap 到相册
     * @param context 上下文
     * @param bitmaps 待保存的位图列表
     * @return 成功保存的 Uri 列表
     */
    suspend fun exportBitmaps(context: Context, bitmaps: List<Bitmap>): List<Uri> {
        val out = mutableListOf<Uri>()
        for (b in bitmaps) {
            val u = saveBitmapToGalleryReturnUri(context, b)
            if (u != null) out += u
        }
        return out
    }
}
