package com.example.xingtuclone.utils

import android.content.Context
import android.graphics.Bitmap
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.Matrix4f
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicColorMatrix

/**
 * 色彩矩阵工具类
 * 提供统一的色彩变换矩阵生成和应用功能
 */
object ColorMatrixUtils {

    /**
     * 创建行主序矩阵 (Row-Major)
     * 由于 Matrix4f 使用列主序存储，需要转置
     */
    fun matRM(vararg v: Float): Matrix4f {
        val m = Matrix4f(v.copyOf())
        m.transpose()
        return m
    }

    /**
     * 生成饱和度调整矩阵
     * @param s 饱和度值，1.0为原始，>1增强，<1降低
     */
    fun saturationMatrix(s: Float): Matrix4f {
        val rw = 0.299f
        val gw = 0.587f
        val bw = 0.114f
        val r0c0 = (1 - s) * rw + s
        val r0c1 = (1 - s) * gw
        val r0c2 = (1 - s) * bw
        val r1c0 = (1 - s) * rw
        val r1c1 = (1 - s) * gw + s
        val r1c2 = (1 - s) * bw
        val r2c0 = (1 - s) * rw
        val r2c1 = (1 - s) * gw
        val r2c2 = (1 - s) * bw + s
        val data = floatArrayOf(
            r0c0, r0c1, r0c2, 0f,
            r1c0, r1c1, r1c2, 0f,
            r2c0, r2c1, r2c2, 0f,
            0f,   0f,   0f,   1f
        )
        return Matrix4f(data).apply { transpose() }
    }

    /**
     * 生成亮度调整矩阵
     * @param b 亮度值，1.0为原始，>1增亮，<1变暗
     */
    fun brightMatrix(b: Float): Matrix4f {
        return Matrix4f(
            floatArrayOf(
                b, 0f, 0f, 0f,
                0f, b, 0f, 0f,
                0f, 0f, b, 0f,
                0f, 0f, 0f, 1f
            )
        )
    }

    /**
     * 混合两个矩阵
     * @param identity 源矩阵（通常为单位矩阵）
     * @param target 目标矩阵
     * @param alpha 混合系数，0为完全使用identity，1为完全使用target
     */
    fun blendMatrix(identity: Matrix4f, target: Matrix4f, alpha: Float): Matrix4f {
        val a = alpha.coerceIn(0f, 1f)
        val id = identity.array
        val tg = target.array
        val out = FloatArray(16)
        for (i in 0 until 16) {
            out[i] = id[i] * (1f - a) + tg[i] * a
        }
        return Matrix4f(out)
    }

    /**
     * 使用 RenderScript 应用色彩矩阵到位图
     * @param context 上下文
     * @param bitmap 输入位图
     * @param matrix 色彩变换矩阵
     * @return 处理后的位图，失败返回null
     */
    fun applyColorMatrixRS(context: Context, bitmap: Bitmap, matrix: Matrix4f): Bitmap? {
        if (bitmap.isRecycled) return null
        return try {
            val rs = RenderScript.create(context)
            val inAlloc = Allocation.createFromBitmap(
                rs, bitmap,
                Allocation.MipmapControl.MIPMAP_NONE,
                Allocation.USAGE_SCRIPT
            )
            val outAlloc = Allocation.createTyped(rs, inAlloc.type)
            val color = ScriptIntrinsicColorMatrix.create(rs, Element.U8_4(rs))
            color.setColorMatrix(matrix)
            color.forEach(inAlloc, outAlloc)
            val out = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config)
            outAlloc.copyTo(out)
            inAlloc.destroy()
            outAlloc.destroy()
            color.destroy()
            rs.destroy()
            out
        } catch (e: Exception) {
            null
        }
    }

    // ============ 预定义滤镜矩阵 ============

    /** 暖色滤镜 */
    fun warmMatrix(): Matrix4f = brightMatrix(1.02f)

    /** 冷色滤镜 */
    fun coolMatrix(): Matrix4f = matRM(
        0.95f, 0f,    0f,    0f,
        0f,    1.0f,  0f,    0f,
        0f,    0.05f, 1.05f, 0f,
        0f,    0f,    0f,    1f
    )

    /** 黑白滤镜 */
    fun grayscaleMatrix(): Matrix4f {
        val rw = 0.299f
        val gw = 0.587f
        val bw = 0.114f
        return matRM(
            rw, gw, bw, 0f,
            rw, gw, bw, 0f,
            rw, gw, bw, 0f,
            0f, 0f, 0f, 1f
        )
    }

    /** 复古滤镜 */
    fun sepiaMatrix(): Matrix4f = matRM(
        0.393f, 0.769f, 0.189f, 0f,
        0.349f, 0.686f, 0.168f, 0f,
        0.272f, 0.534f, 0.131f, 0f,
        0f,     0f,     0f,     1f
    )

    /** 青橙滤镜 */
    fun tealOrangeMatrix(): Matrix4f = matRM(
        1.05f, -0.04f, 0f,    0f,
        0f,     0.95f, 0f,    0f,
        0f,     0.06f, 1.06f, 0f,
        0f,     0f,    0f,    1f
    )

    /** 粉调滤镜 */
    fun pinkMatrix(): Matrix4f = matRM(
        1.06f, 0.02f, 0.02f, 0f,
        0.02f, 0.98f, 0.0f,  0f,
        0.02f, 0.0f,  1.02f, 0f,
        0f,    0f,    0f,    1f
    )

    /** 绿野滤镜 */
    fun greenMatrix(): Matrix4f = matRM(
        0.95f, 0.0f,  0.0f,  0f,
        0.05f, 1.05f, 0.0f,  0f,
        0.0f,  0.0f,  0.95f, 0f,
        0f,    0f,    0f,    1f
    )

    /**
     * 根据滤镜名称获取对应矩阵
     */
    fun getFilterMatrix(name: String): Matrix4f {
        return when (name) {
            "原图" -> Matrix4f()
            "暖色" -> warmMatrix()
            "冷色" -> coolMatrix()
            "黑白" -> grayscaleMatrix()
            "复古" -> sepiaMatrix()
            "增饱和" -> saturationMatrix(1.35f)
            "降饱和" -> saturationMatrix(0.75f)
            "柔和" -> saturationMatrix(0.85f)
            "青橙" -> tealOrangeMatrix()
            "粉调" -> pinkMatrix()
            "绿野" -> greenMatrix()
            else -> Matrix4f()
        }
    }
}
