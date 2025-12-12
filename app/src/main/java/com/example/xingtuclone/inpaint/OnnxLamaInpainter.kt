/**
 * ============================================
 * OnnxLamaInpainter.kt - ONNX LaMa 模型推理器
 * ============================================
 * 功能说明：
 * AI魔法消除功能的核心，基于 ONNX Runtime 运行 LaMa 深度学习模型
 * 
 * 技术亮点：
 * - 端侧推理：无需云服务，所有计算在本地完成
 * - LaMa 模型：SOTA 图像修复模型，效果优秀
 * - Fallback 机制：模型加载失败时降级为高斯模糊混合
 * - 单例模式：避免重复加载模型，节省内存
 * 
 * 核心流程：
 * 1. 初始化：加载 assets/models/lama.onnx 模型
 * 2. 预处理：图像和蒙版 resize 到 512x512
 * 3. 转换：将 Bitmap 转为 CHW (Channel-Height-Width) Float 张量
 * 4. 推理：运行 ONNX 模型进行图像修复
 * 5. 后处理：将输出转换回 Bitmap 并还原尺寸
 * ============================================
 */
package com.example.xingtuclone.inpaint

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.BlurMaskFilter
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import java.nio.FloatBuffer

/**
 * ONNX LaMa 模型推理器单例
 * 负责 AI 魔法消除功能的核心推理逻辑
 */
object OnnxLamaInpainter {
    @Volatile private var env: OrtEnvironment? = null
    @Volatile private var session: OrtSession? = null
    @Volatile private var initialized = false

    /**
     * 初始化 ONNX 环境和加载模型
     * 只会初始化一次，多线程安全
     * @param context 上下文，用于读取 assets 中的模型文件
     */
    /**
     * 初始化 ONNX 环境和加载模型
     * 只会初始化一次，多线程安全
     * @param context 上下文，用于读取 assets 中的模型文件
     */
    fun init(context: Context) {
        if (initialized) return
        try {
            // 从 assets 中读取 lama.onnx 模型文件
            val modelBytes = context.assets.open("models/lama.onnx").use { it.readBytes() }
            val environment = OrtEnvironment.getEnvironment()
            val sess = environment.createSession(modelBytes)
            env = environment
            session = sess
            initialized = true
        } catch (_: Throwable) {
            // 模型加载失败，将使用 fallback 方法
            initialized = false
        }
    }

    /**
     * 执行图像修复（Inpainting）
     * @param context 上下文
     * @param src 原始图像
     * @param mask 蒙版（白色区域表示需要消除的部分）
     * @return 修复后的图像
     */
    fun inpaint(context: Context, src: Bitmap, mask: Bitmap): Bitmap {
        init(context)
        return try {
            val s = session ?: throw IllegalStateException("ONNX session not ready")
            runOnnxLama(s, src, mask)
        } catch (_: Throwable) {
            fallbackBlurBlend(context, src, mask, 18f)
        }
     }

    private fun runOnnxLama(session: OrtSession, src: Bitmap, mask: Bitmap): Bitmap {
        val targetSize = 512
        val resizedImg = Bitmap.createScaledBitmap(src, targetSize, targetSize, true)
        val resizedMask = Bitmap.createScaledBitmap(mask, targetSize, targetSize, true)

        val imgTensor = bitmapToCHWFloat(resizedImg)
        val maskTensor = maskToCHWFloat(resizedMask)

        val imgShape = longArrayOf(1, 3, targetSize.toLong(), targetSize.toLong())
        val maskShape = longArrayOf(1, 1, targetSize.toLong(), targetSize.toLong())

        val inputNames = session.inputNames.toList()
        val imageName = inputNames.firstOrNull { it.contains("image", true) } ?: inputNames.getOrNull(0) ?: "image"
        val maskName = inputNames.firstOrNull { it.contains("mask", true) } ?: inputNames.getOrNull(1) ?: "mask"

        OnnxTensor.createTensor(OrtEnvironment.getEnvironment(), imgTensor, imgShape).use { imageT ->
            OnnxTensor.createTensor(OrtEnvironment.getEnvironment(), maskTensor, maskShape).use { maskT ->
                val results = session.run(mapOf(imageName to imageT, maskName to maskT))
                results.use {
                    val out = it[0] as OnnxTensor
                    val outBuffer = out.floatBuffer
                    val outBitmap = chwFloatToBitmap(outBuffer, targetSize, targetSize)
                    return Bitmap.createScaledBitmap(outBitmap, src.width, src.height, true)
                }
            }
        }
    }

    private fun bitmapToCHWFloat(bmp: Bitmap): FloatBuffer {
        val w = bmp.width
        val h = bmp.height
        val pixels = IntArray(w * h)
        bmp.getPixels(pixels, 0, w, 0, 0, w, h)
        val buffer = FloatBuffer.allocate(1 * 3 * w * h)
        for (i in 0 until w * h) {
            val p = pixels[i]
            buffer.put(((p ushr 16) and 0xFF) / 255f)
        }
        for (i in 0 until w * h) {
            val p = pixels[i]
            buffer.put(((p ushr 8) and 0xFF) / 255f)
        }
        for (i in 0 until w * h) {
            val p = pixels[i]
            buffer.put((p and 0xFF) / 255f)
        }
        buffer.rewind()
        return buffer
    }

    private fun maskToCHWFloat(mask: Bitmap): FloatBuffer {
        val w = mask.width
        val h = mask.height
        val pixels = IntArray(w * h)
        mask.getPixels(pixels, 0, w, 0, 0, w, h)
        val buffer = FloatBuffer.allocate(1 * 1 * w * h)
        for (i in 0 until w * h) {
            val a = (pixels[i] ushr 24) and 0xFF
            buffer.put(a / 255f)
        }
        buffer.rewind()
        return buffer
    }

    private fun chwFloatToBitmap(buf: FloatBuffer, w: Int, h: Int): Bitmap {
        buf.rewind()
        val planeSize = w * h
        val r = FloatArray(planeSize)
        val g = FloatArray(planeSize)
        val b = FloatArray(planeSize)
        buf.get(r)
        buf.get(g)
        buf.get(b)
        val outPx = IntArray(planeSize)
        for (i in 0 until planeSize) {
            val rr = (r[i].coerceIn(0f, 1f) * 255f).toInt()
            val gg = (g[i].coerceIn(0f, 1f) * 255f).toInt()
            val bb = (b[i].coerceIn(0f, 1f) * 255f).toInt()
            outPx[i] = (0xFF shl 24) or (rr shl 16) or (gg shl 8) or bb
        }
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        out.setPixels(outPx, 0, w, 0, 0, w, h)
        return out
    }

    private fun fallbackBlurBlend(context: Context, src: Bitmap, mask: Bitmap, blurRadius: Float): Bitmap {
        val rs = RenderScript.create(context)
        val inAlloc = Allocation.createFromBitmap(rs, src, Allocation.MipmapControl.MIPMAP_NONE, Allocation.USAGE_SCRIPT)
        val outAlloc = Allocation.createTyped(rs, inAlloc.type)
        val scriptBlur = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs))
        scriptBlur.setRadius(blurRadius.coerceIn(1f, 25f))
        scriptBlur.setInput(inAlloc)
        scriptBlur.forEach(outAlloc)

        val blurred = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
        outAlloc.copyTo(blurred)

        val w = src.width
        val h = src.height
        val srcPx = IntArray(w * h)
        val blurPx = IntArray(w * h)
        val maskPx = IntArray(w * h)
        src.getPixels(srcPx, 0, w, 0, 0, w, h)
        blurred.getPixels(blurPx, 0, w, 0, 0, w, h)
        mask.getPixels(maskPx, 0, w, 0, 0, w, h)

        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val outPx = IntArray(w * h)
        for (i in 0 until w * h) {
            val a = (maskPx[i] ushr 24) and 0xFF
            if (a == 0) {
                outPx[i] = srcPx[i]
            } else {
                val alpha = a / 255f
                val s = srcPx[i]
                val b = blurPx[i]
                val sr = (s ushr 16) and 0xFF
                val sg = (s ushr 8) and 0xFF
                val sb = s and 0xFF
                val br = (b ushr 16) and 0xFF
                val bg = (b ushr 8) and 0xFF
                val bb = b and 0xFF
                val rr = (sr * (1f - alpha) + br * alpha).toInt().coerceIn(0, 255)
                val rg = (sg * (1f - alpha) + bg * alpha).toInt().coerceIn(0, 255)
                val rb = (sb * (1f - alpha) + bb * alpha).toInt().coerceIn(0, 255)
                outPx[i] = (0xFF shl 24) or (rr shl 16) or (rg shl 8) or rb
            }
        }
        out.setPixels(outPx, 0, w, 0, 0, w, h)

        blurred.recycle()
        scriptBlur.destroy()
        outAlloc.destroy()
        inAlloc.destroy()
        rs.destroy()
        return out
    }
}

