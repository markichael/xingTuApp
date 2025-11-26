// ui/FilterUtils.kt
package com.example.xingtuclone.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.InvertColors
import androidx.compose.ui.graphics.vector.ImageVector
import jp.co.cyberagent.android.gpuimage.filter.*

// 1. 滤镜的数据模型
data class FilterItem(
    val name: String,
    val filter: GPUImageFilter
)

// 2. 调节工具的数据模型
data class AdjustItem(
    val name: String,
    val icon: ImageVector,
    val type: AdjustType
)

// 🔥 3. 必须要有这个枚举类，不然 EditorScreen 就会爆红！
enum class AdjustType {
    BRIGHTNESS, // 亮度
    CONTRAST,   // 对比度
    SATURATION  // 饱和度
}

// 4. 滤镜列表数据
val filterList = listOf(
    FilterItem("原图", GPUImageFilter()),
    FilterItem("黑白", GPUImageGrayscaleFilter()),
    FilterItem("怀旧", GPUImageSepiaToneFilter()),
    FilterItem("素描", GPUImageSketchFilter()),
    FilterItem("卡通", GPUImageToonFilter()),
    FilterItem("马赛克", GPUImagePixelationFilter().apply { setPixel(30f) }),
    FilterItem("浮雕", GPUImageEmbossFilter()),
    FilterItem("暗角", GPUImageVignetteFilter()),
    FilterItem("水晶球", GPUImageGlassSphereFilter()),
    FilterItem("漩涡", GPUImageSwirlFilter())
)

// 5. 调节工具列表数据
val adjustTools = listOf(
    AdjustItem("亮度", Icons.Default.Brightness6, AdjustType.BRIGHTNESS),
    AdjustItem("对比度", Icons.Default.Contrast, AdjustType.CONTRAST),
    AdjustItem("饱和度", Icons.Default.InvertColors, AdjustType.SATURATION)
)