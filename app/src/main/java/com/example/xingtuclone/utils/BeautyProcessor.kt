package com.example.xingtuclone.utils

import android.content.Context
import android.graphics.Bitmap
import android.renderscript.*
import android.util.Log

/**
 * 使用 RenderScript 实现的美颜处理器
 * 替代 GPUImage 以解决部分机型黑屏问题
 */
class BeautyProcessor(context: Context) {
    private var rs: RenderScript = RenderScript.create(context)
    
    private var scriptBlur: ScriptIntrinsicBlur = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs))
    private var scriptColor: ScriptIntrinsicColorMatrix = ScriptIntrinsicColorMatrix.create(rs, Element.U8_4(rs))
    private var scriptConvolve: ScriptIntrinsicConvolve3x3 = ScriptIntrinsicConvolve3x3.create(rs, Element.U8_4(rs))

    // 缓存 Allocation 以提升性能 (仅当尺寸不变时复用)
    private var lastWidth = 0
    private var lastHeight = 0
    private var allocationIn: Allocation? = null
    private var allocationOut: Allocation? = null
    private var allocationTemp1: Allocation? = null
    private var allocationTemp2: Allocation? = null

    fun process(
        bitmap: Bitmap, 
        smooth: Float,  // 0.0 - 10.0 (Blur Radius)
        whiten: Float,  // 0.0 - 1.0 (Brightness)
        ruddy: Float,   // 0.0 - 1.0 (Saturation / Red Boost)
        sharpen: Float  // 0.0 - 4.0 (Sharpen Intensity)
    ): Bitmap? {
        if (bitmap.isRecycled) return null

        try {
            // 1. 准备 Allocation
            prepareAllocations(bitmap)

            // 2. 复制输入数据
            allocationIn?.copyFrom(bitmap)

            // -------------------------------------------------------
            // 步骤 1: 磨皮 (Smooth) -> Gaussian Blur
            // -------------------------------------------------------
            var currentAlloc = allocationIn!!
            var nextAlloc = allocationTemp1!!

            if (smooth > 0) {
                // RenderScript Blur Radius 范围是 0 < r <= 25
                val radius = (smooth * 2.5f).coerceIn(0.1f, 25.0f)
                scriptBlur.setRadius(radius)
                scriptBlur.setInput(currentAlloc)
                scriptBlur.forEach(nextAlloc)
                
                // 交换 buffer
                val temp = currentAlloc
                currentAlloc = nextAlloc
                nextAlloc = temp
            }

            // -------------------------------------------------------
            // 步骤 2: 锐化 (Sharpen) -> Convolve 3x3
            // -------------------------------------------------------
            if (sharpen > 0) {
                // 简单的拉普拉斯锐化算子
                // [ -a  -a  -a ]
                // [ -a 1+8a -a ]
                // [ -a  -a  -a ]
                // 或者十字形
                // [ 0 -a  0 ]
                // [ -a 1+4a -a ]
                // [ 0 -a  0 ]
                
                val a = sharpen * 0.2f // 调整系数
                // 使用 3x3 卷积
                // Center = 1 + 4a, Neighbors = -a
                val kernel = floatArrayOf(
                    0f, -a, 0f,
                    -a, 1 + 4 * a, -a,
                    0f, -a, 0f
                )
                scriptConvolve.setCoefficients(kernel)
                scriptConvolve.setInput(currentAlloc)
                scriptConvolve.forEach(nextAlloc)

                // 交换 buffer
                val temp = currentAlloc
                currentAlloc = nextAlloc
                nextAlloc = temp
            }

            // -------------------------------------------------------
            // 步骤 3: 美白 & 红润 (Whiten & Ruddy) -> ColorMatrix
            // -------------------------------------------------------
            
            if (whiten > 0 || ruddy > 0) {
                // 1. 饱和度 (Ruddy)
                val s = 1.0f + ruddy * 0.6f
                val rw = 0.299f
                val gw = 0.587f
                val bw = 0.114f
                
                // Construct matrix elements (Row-Major logic)
                // Row 0
                val r0c0 = (1 - s) * rw + s
                val r0c1 = (1 - s) * gw
                val r0c2 = (1 - s) * bw
                
                // Row 1
                val r1c0 = (1 - s) * rw
                val r1c1 = (1 - s) * gw + s
                val r1c2 = (1 - s) * bw
                
                // Row 2
                val r2c0 = (1 - s) * rw
                val r2c1 = (1 - s) * gw
                val r2c2 = (1 - s) * bw + s
                
                // Array in Row-Major order
                val satData = floatArrayOf(
                    r0c0, r0c1, r0c2, 0f,
                    r1c0, r1c1, r1c2, 0f,
                    r2c0, r2c1, r2c2, 0f,
                    0f,   0f,   0f,   1f
                )
                val satMat = Matrix4f(satData)
                // Matrix4f(float[]) stores data in column-major order (effectively transposing row-major input)
                // So we need to transpose it back to get the correct matrix
                satMat.transpose()
                
                // 2. 美白 (Brightness) -> Scale RGB
                val b = 1.0f + whiten * 0.3f
                val brightMat = Matrix4f() // Identity
                brightMat.scale(b, b, b) // Scales diagonal
                
                // Combine: Final = Bright * Sat
                brightMat.multiply(satMat)
                
                scriptColor.setColorMatrix(brightMat)
                scriptColor.forEach(currentAlloc, nextAlloc)
                
                // 交换
                val temp = currentAlloc
                currentAlloc = nextAlloc
                nextAlloc = temp
            }

            // 3. 输出
            // 最终结果在 currentAlloc 中
            val outBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config)
            currentAlloc.copyTo(outBitmap)
            
            return outBitmap

        } catch (e: Exception) {
            Log.e("BeautyProcessor", "Process failed", e)
            return null
        }
    }

    private fun prepareAllocations(bitmap: Bitmap) {
        if (allocationIn != null && bitmap.width == lastWidth && bitmap.height == lastHeight) {
            return // 复用
        }

        // 销毁旧的
        destroyAllocations()

        lastWidth = bitmap.width
        lastHeight = bitmap.height

        allocationIn = Allocation.createFromBitmap(
            rs, 
            bitmap, 
            Allocation.MipmapControl.MIPMAP_NONE, 
            Allocation.USAGE_SCRIPT
        )
        allocationOut = Allocation.createTyped(rs, allocationIn!!.type)
        allocationTemp1 = Allocation.createTyped(rs, allocationIn!!.type)
        allocationTemp2 = Allocation.createTyped(rs, allocationIn!!.type)
    }

    private fun destroyAllocations() {
        try {
            allocationIn?.destroy(); allocationIn = null
            allocationOut?.destroy(); allocationOut = null
            allocationTemp1?.destroy(); allocationTemp1 = null
            allocationTemp2?.destroy(); allocationTemp2 = null
        } catch (e: Exception) {}
    }

    fun destroy() {
        destroyAllocations()
        scriptBlur.destroy()
        scriptColor.destroy()
        scriptConvolve.destroy()
        rs.destroy()
    }
}
