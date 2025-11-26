package com.example.xingtuclone.ui

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Face
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import jp.co.cyberagent.android.gpuimage.GPUImage
import jp.co.cyberagent.android.gpuimage.GPUImageView

// 基础滤镜
import jp.co.cyberagent.android.gpuimage.filter.GPUImageGaussianBlurFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageBrightnessFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageFilterGroup
import jp.co.cyberagent.android.gpuimage.filter.GPUImageSaturationFilter

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

@Composable
fun FaceBeautyScreen(imageUri: Uri, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // --- UI 显示状态 (响应快) ---
    var uiSmoothLevel by remember { mutableFloatStateOf(0.0f) }
    var uiWhiteLevel by remember { mutableFloatStateOf(0.0f) }

    // --- 实际滤镜状态 (响应慢，只在松手时更新) ---
    var applySmoothLevel by remember { mutableFloatStateOf(0.0f) }
    var applyWhiteLevel by remember { mutableFloatStateOf(0.0f) }

    var isAnalyzing by remember { mutableStateOf(true) }
    var hasFace by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }

    val gpuImageView = remember { GPUImageView(context) }
    val smoothFilter = remember { GPUImageGaussianBlurFilter() }
    val brightnessFilter = remember { GPUImageBrightnessFilter() }
    val saturationFilter = remember { GPUImageSaturationFilter() }

    // 初始化加载
    // 初始化加载
    LaunchedEffect(imageUri) {
        val bitmap = withContext(Dispatchers.IO) {
            loadCompressedBitmap(context, imageUri)
        }

        if (bitmap != null) {
            // 🔥 关键修改：先设置背景色，防止闪烁
            gpuImageView.setBackgroundColor(android.graphics.Color.BLACK)
            gpuImageView.setImage(bitmap)
            gpuImageView.setScaleType(GPUImage.ScaleType.CENTER_INSIDE)

            // 🔥 关键修改：手动请求刷新一下
            gpuImageView.requestRender()

            // 2. 异步人脸检测
            val faceCount = detectFaces(bitmap)
            isAnalyzing = false
            hasFace = faceCount > 0

            val msg = if (faceCount > 0) "已优化 $faceCount 张人脸" else "通用增强模式"
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "图片加载失败", Toast.LENGTH_SHORT).show()
            onBack()
        }
    }

    // 🔥 核心优化：只监听 [apply] 状态的变化，不监听 [ui] 状态
    // 只有手指松开时，这里才会执行，避免了疯狂触发 GPU 渲染
    LaunchedEffect(applySmoothLevel, applyWhiteLevel) {
        // 磨皮 (高斯模糊)
        smoothFilter.setBlurSize(applySmoothLevel * 0.05f) // 微调参数
        // 美白 (亮度)
        brightnessFilter.setBrightness(applyWhiteLevel * 0.1f)
        // 气色 (饱和度)
        saturationFilter.setSaturation(1.0f + (applyWhiteLevel * 0.1f))

        val group = GPUImageFilterGroup()
        group.addFilter(smoothFilter)
        group.addFilter(brightnessFilter)
        group.addFilter(saturationFilter)

        gpuImageView.filter = group
    }

    Column(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        // 顶部栏
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Close, "Back", tint = Color.White, modifier = Modifier.clickable { onBack() })
            Text(if (isAnalyzing) "AI 分析中..." else "美颜修图", color = Color.White, fontSize = 18.sp)
            Icon(
                Icons.Default.Check, "Save",
                tint = if (isSaving) Color.Gray else Color(0xFFCCFF00),
                modifier = Modifier.clickable(enabled = !isSaving) {
                    isSaving = true
                    scope.launch {
                        try {
                            val bitmap = gpuImageView.gpuImage.getBitmapWithFilterApplied()
                            if (bitmap != null) {
                                val success = saveBitmapToGallery(context, bitmap)
                                if (success) onBack()
                            }
                        } catch (e: Exception) { e.printStackTrace() }
                        finally { isSaving = false }
                    }
                }
            )
        }

        // 图片区
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { gpuImageView }
            )
            if (isAnalyzing) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = Color(0xFFCCFF00)
                )
            }
        }

        // 底部控制栏
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1A1A1A))
                .padding(16.dp)
        ) {
            // 磨皮滑块
            BeautySlider(
                name = "磨皮",
                value = uiSmoothLevel,
                range = 0f..10f,
                onValueChange = { uiSmoothLevel = it }, // 拖动时只更新 UI
                onValueChangeFinished = { applySmoothLevel = uiSmoothLevel } // 🔥 松手时才更新滤镜
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 美白滑块
            BeautySlider(
                name = "美白",
                value = uiWhiteLevel,
                range = 0f..10f,
                onValueChange = { uiWhiteLevel = it }, // 拖动时只更新 UI
                onValueChangeFinished = { applyWhiteLevel = uiWhiteLevel } // 🔥 松手时才更新滤镜
            )
        }
    }
}

@Composable
fun BeautySlider(
    name: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit // 新增参数
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Face, null, tint = Color.Gray, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(name, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(40.dp))
        Slider(
            value = value,
            onValueChange = onValueChange,
            onValueChangeFinished = onValueChangeFinished, // 绑定松手事件
            valueRange = range,
            colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Color(0xFFCCFF00)),
            modifier = Modifier.weight(1f)
        )
    }
}

// ==========================================
// ↓↓↓ 替换 FaceBeautyScreen.kt 底部的加载函数 ↓↓↓
// ==========================================

fun loadCompressedBitmap(context: Context, uri: Uri): Bitmap? {
    return try {
        val contentResolver = context.contentResolver

        // 1. 先只读取尺寸
        val options = android.graphics.BitmapFactory.Options()
        options.inJustDecodeBounds = true
        contentResolver.openInputStream(uri)?.use {
            android.graphics.BitmapFactory.decodeStream(it, null, options)
        }

        // 2. 计算采样率 (限制图片最大 1280px)
        var inSampleSize = 1
        val reqSize = 1280
        if (options.outHeight > reqSize || options.outWidth > reqSize) {
            val halfHeight = options.outHeight / 2
            val halfWidth = options.outWidth / 2
            while ((halfHeight / inSampleSize) >= reqSize && (halfWidth / inSampleSize) >= reqSize) {
                inSampleSize *= 2
            }
        }

        // 3. 设置加载参数 (关键步骤)
        options.inJustDecodeBounds = false
        options.inSampleSize = inSampleSize
        // 🔥 强制设置为 ARGB_8888，防止加载成 Hardware Bitmap 导致 GPUImage 黑屏
        options.inPreferredConfig = Bitmap.Config.ARGB_8888
        options.inMutable = true

        // 4. 真正加载图片
        var bitmap = contentResolver.openInputStream(uri)?.use {
            android.graphics.BitmapFactory.decodeStream(it, null, options)
        }

        // 🔥 5. 双重保险：如果是 HARDWARE 格式，必须强转！
        if (bitmap != null && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            if (bitmap.config == Bitmap.Config.HARDWARE) {
                val softwareBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, false)
                bitmap.recycle() // 回收旧的
                bitmap = softwareBitmap
            }
        }

        bitmap
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
suspend fun detectFaces(bitmap: Bitmap): Int {
    return try {
        val image = InputImage.fromBitmap(bitmap, 0)
        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
            .build()
        val detector = FaceDetection.getClient(options)
        val faces = detector.process(image).await()
        faces.size
    } catch (e: Exception) { 0 }
}