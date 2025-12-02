package com.example.xingtuclone.ui

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.xingtuclone.utils.BeautyProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FaceBeautyScreen(imageUri: Uri, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // --- 状态管理 ---
    
    // 原始大图 (用于保存)
    var originalBitmap by remember { mutableStateOf<Bitmap?>(null) }
    // 预览小图 (用于实时滤镜，提升性能)
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    // 显示用的 Bitmap (经过滤镜处理后的预览图)
    var displayBitmap by remember { mutableStateOf<Bitmap?>(null) }

    // 调试信息
    var debugInfo by remember { mutableStateOf("初始化中...") }
    var isLoaded by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }

    // 当前选中的功能 Tab
    var selectedType by remember { mutableStateOf(BeautyType.SMOOTH) }

    // 各个维度的参数值 (UI 显示用)
    var smoothValue by remember { mutableFloatStateOf(0f) } // 0-10
    var whitenValue by remember { mutableFloatStateOf(0f) } // 0-1.0
    var ruddyValue by remember { mutableFloatStateOf(0f) }  // 0-1.0 (映射到 1.0-2.0)
    var sharpenValue by remember { mutableFloatStateOf(0f) } // 0-4.0

    // 实际应用到滤镜的值
    var applySmooth by remember { mutableFloatStateOf(0f) }
    var applyWhiten by remember { mutableFloatStateOf(0f) }
    var applyRuddy by remember { mutableFloatStateOf(0f) }
    var applySharpen by remember { mutableFloatStateOf(0f) }

    // 替代 GPUImage，使用 RenderScript 实现的 BeautyProcessor
    // 这可以解决部分机型 (如小米14) 上 GPUImage 离屏渲染导致的黑屏问题
    val beautyProcessor = remember { BeautyProcessor(context) }

    DisposableEffect(Unit) {
        onDispose {
            beautyProcessor.destroy()
        }
    }

    // --- 预设管理 ---
    data class BeautyPreset(
        val name: String,
        val smooth: Float,
        val whiten: Float,
        val ruddy: Float,
        val sharpen: Float
    )

    val presets = remember {
        listOf(
            BeautyPreset("轻度", 3.0f, 0.2f, 0.2f, 0.5f),
            BeautyPreset("中度", 6.0f, 0.5f, 0.5f, 1.5f),
            BeautyPreset("重度", 9.0f, 0.8f, 0.8f, 2.5f)
        )
    }
    var currentPresetName by remember { mutableStateOf("自定义") }

    fun applyPreset(preset: BeautyPreset) {
        currentPresetName = preset.name
        smoothValue = preset.smooth; applySmooth = preset.smooth
        whitenValue = preset.whiten; applyWhiten = preset.whiten
        ruddyValue = preset.ruddy; applyRuddy = preset.ruddy
        sharpenValue = preset.sharpen; applySharpen = preset.sharpen
    }

    fun logMemory(tag: String) {
        val runtime = Runtime.getRuntime()
        val used = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024
        val max = runtime.maxMemory() / 1024 / 1024
        Log.d("FaceBeauty", "[$tag] Memory: $used MB / $max MB")
    }

    // 加载图片
    LaunchedEffect(imageUri) {
        val startTime = System.currentTimeMillis()
        debugInfo = "开始加载图片: $imageUri"
        Log.d("FaceBeauty", "Start loading image: $imageUri")
        logMemory("StartLoad")
        
        withContext(Dispatchers.IO) {
            try {
                // 1. 加载原始大图 (限制最大 2560px，保证画质但防止极端 OOM)
                val fullBmp = loadCompressedBitmap(context, imageUri, 2560)
                
                if (fullBmp != null) {
                    // 2. 生成预览小图 (限制最大 1080px，保证实时渲染性能)
                    val previewScale = 1080f / Math.max(fullBmp.width, fullBmp.height)
                    val previewBmp = if (previewScale < 1.0f) {
                         Bitmap.createScaledBitmap(
                            fullBmp,
                            (fullBmp.width * previewScale).toInt(),
                            (fullBmp.height * previewScale).toInt(),
                            true
                        )
                    } else {
                        fullBmp.copy(fullBmp.config, true)
                    }

                    withContext(Dispatchers.Main) {
                        originalBitmap = fullBmp
                        previewBitmap = previewBmp
                        displayBitmap = previewBmp // 初始显示原图
                        isLoaded = true
                        
                        logMemory("EndLoad")
                        val loadTime = System.currentTimeMillis() - startTime
                        debugInfo = "加载成功: ${fullBmp.width}x${fullBmp.height}\n预览尺寸: ${previewBmp.width}x${previewBmp.height}\n耗时: ${loadTime}ms"
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        debugInfo = "图片加载失败"
                        Toast.makeText(context, "图片加载失败", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("FaceBeauty", "Error loading image", e)
                withContext(Dispatchers.Main) {
                    debugInfo = "加载出错: ${e.message}"
                }
            }
        }
    }

    var isComparing by remember { mutableStateOf(false) }

    // 监听参数变化，应用滤镜 (使用 RenderScript)
    LaunchedEffect(applySmooth, applyWhiten, applyRuddy, applySharpen, isComparing, isLoaded) {
        if (!isLoaded || previewBitmap == null) return@LaunchedEffect

        val startTime = System.nanoTime()
        
        // 如果是对比模式，直接显示未处理的预览图
        if (isComparing) {
            displayBitmap = previewBitmap
            return@LaunchedEffect
        }

        // 使用 IO 线程进行计算
        val resultBitmap = withContext(Dispatchers.IO) {
             try {
                // 传入 previewBitmap，BeautyProcessor 会返回一个新的处理后的 Bitmap
                beautyProcessor.process(
                    previewBitmap!!,
                    applySmooth,
                    applyWhiten,
                    applyRuddy,
                    applySharpen
                )
            } catch (e: Exception) {
                Log.e("FaceBeauty", "Filter failed", e)
                null
            }
        }

        if (resultBitmap != null) {
            displayBitmap = resultBitmap
            val processTime = (System.nanoTime() - startTime) / 1_000_000.0
            Log.d("FaceBeauty", "Filter process time: ${processTime}ms")
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        // --- 顶部栏 ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Close, 
                "Back", 
                tint = Color.White, 
                modifier = Modifier
                    .size(28.dp)
                    .clickable { onBack() }
            )
            
            Text("AI 美颜", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)

            Row(verticalAlignment = Alignment.CenterVertically) {
                // 对比按钮
                Box(
                    modifier = Modifier
                        .padding(end = 16.dp)
                        .background(if (isComparing) Color(0xFFCCFF00) else Color.DarkGray, CircleShape)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent()
                                    if (event.changes.any { it.pressed }) {
                                        isComparing = true
                                    } else {
                                        isComparing = false
                                    }
                                }
                            }
                        }
                ) {
                    Text(
                        "对比",
                        color = if (isComparing) Color.Black else Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Icon(
                    Icons.Default.Check, 
                    "Save",
                    tint = if (isSaving) Color.Gray else Color(0xFFCCFF00),
                    modifier = Modifier
                        .size(28.dp)
                        .clickable(enabled = !isSaving && isLoaded) {
                        isSaving = true
                        scope.launch {
                            try {
                                if (originalBitmap != null) {
                                    // 保存时使用原图进行高分辨率渲染
                                    val savedBitmap = withContext(Dispatchers.IO) {
                                         beautyProcessor.process(
                                            originalBitmap!!,
                                            applySmooth,
                                            applyWhiten,
                                            applyRuddy,
                                            applySharpen
                                         )
                                    }
                                    
                                    if (savedBitmap != null) {
                                        val success = saveBitmapToGallery(context, savedBitmap)
                                        if (success) {
                                            onBack()
                                        } else {
                                            Toast.makeText(context, "保存失败", Toast.LENGTH_SHORT).show()
                                        }
                                        // 回收 bitmap (如果它不是 originalBitmap)
                                        if (savedBitmap != originalBitmap) savedBitmap.recycle()
                                    } else {
                                        Toast.makeText(context, "生成图片失败", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                                Toast.makeText(context, "保存出错: ${e.message}", Toast.LENGTH_SHORT).show()
                            } finally {
                                isSaving = false
                            }
                        }
                    }
            )
        }
    }

        // --- 图片预览区 ---
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color.Black)
        ) {
            if (displayBitmap != null) {
                Image(
                    bitmap = displayBitmap!!.asImageBitmap(),
                    contentDescription = "Editor Preview",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            } else {
                if (!isLoaded) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = Color(0xFFCCFF00)
                    )
                }
            }
            
            // 3. 调试信息显示 (显示在屏幕左上角)
            Text(
                text = debugInfo,
                color = Color.Red,
                fontSize = 12.sp,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .background(Color.White.copy(alpha = 0.7f))
                    .padding(4.dp)
            )
        }

        // --- 底部控制面板 ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1A1A1A))
                .padding(bottom = 16.dp)
        ) {
            // 0. 预设选择
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                presets.forEach { preset ->
                    val isSelected = currentPresetName == preset.name
                    FilterChip(
                        selected = isSelected,
                        onClick = { applyPreset(preset) },
                        label = { Text(preset.name) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFCCFF00),
                            selectedLabelColor = Color.Black,
                            containerColor = Color(0xFF333333),
                            labelColor = Color.White
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = Color.Transparent,
                            selectedBorderColor = Color.Transparent
                        )
                    )
                }
                // 自定义 (显示状态)
                FilterChip(
                    selected = currentPresetName == "自定义",
                    onClick = { /* 不做操作，通过滑动滑块自动选中 */ },
                    label = { Text("自定义") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFFCCFF00),
                        selectedLabelColor = Color.Black,
                        containerColor = Color(0xFF333333),
                        labelColor = Color.White
                    ),
                     border = FilterChipDefaults.filterChipBorder(
                        borderColor = Color.Transparent,
                        selectedBorderColor = Color.Transparent
                    )
                )
            }

            // 1. 滑块区域 (根据当前选中的 Tab 显示)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 20.dp)
            ) {
                val (currentValue, onValueChange, onFinish) = when (selectedType) {
                    BeautyType.SMOOTH -> Triple(smoothValue, { v: Float -> smoothValue = v; applySmooth = v; currentPresetName = "自定义" }, { })
                    BeautyType.WHITEN -> Triple(whitenValue, { v: Float -> whitenValue = v; applyWhiten = v; currentPresetName = "自定义" }, { })
                    BeautyType.RUDDY -> Triple(ruddyValue, { v: Float -> ruddyValue = v; applyRuddy = v; currentPresetName = "自定义" }, { })
                    BeautyType.SHARPEN -> Triple(sharpenValue, { v: Float -> sharpenValue = v; applySharpen = v; currentPresetName = "自定义" }, { })
                }

                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(selectedType.title, color = Color.Gray, fontSize = 12.sp)
                        Text("${(currentValue * 10).toInt()}%", color = Color.Gray, fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Slider(
                        value = currentValue,
                        onValueChange = onValueChange,
                        onValueChangeFinished = onFinish,
                        valueRange = 0f..selectedType.maxRange,
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = Color(0xFFCCFF00),
                            inactiveTrackColor = Color.Gray
                        )
                    )
                }
            }

            // 2. 底部 Tab 切换
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                BeautyType.values().forEach { type ->
                    BeautyTabItem(
                        type = type,
                        isSelected = selectedType == type,
                        onClick = { selectedType = type }
                    )
                }
            }
        }
    }
}

@Composable
fun BeautyTabItem(
    type: BeautyType,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClick() }
            .padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(if (isSelected) Color(0xFF333333) else Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = type.icon,
                contentDescription = type.title,
                tint = if (isSelected) Color(0xFFCCFF00) else Color.Gray,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = type.title,
            color = if (isSelected) Color.White else Color.Gray,
            fontSize = 12.sp
        )
    }
}

// ------------------------------------------------------------------------
// 升级版图片加载工具：压缩 + 硬件位图转换 + 自动旋转修复
// ------------------------------------------------------------------------
fun loadCompressedBitmap(context: Context, uri: Uri, maxReqSize: Int = 1280): Bitmap? {
    var inputStream: java.io.InputStream? = null
    try {
        val contentResolver = context.contentResolver

        // 1. 获取图片旋转角度 (Exif)
        var rotation = 0f
        inputStream = contentResolver.openInputStream(uri)
        if (inputStream != null) {
            val exifInterface = android.media.ExifInterface(inputStream)
            val orientation = exifInterface.getAttributeInt(
                android.media.ExifInterface.TAG_ORIENTATION,
                android.media.ExifInterface.ORIENTATION_NORMAL
            )
            when (orientation) {
                android.media.ExifInterface.ORIENTATION_ROTATE_90 -> rotation = 90f
                android.media.ExifInterface.ORIENTATION_ROTATE_180 -> rotation = 180f
                android.media.ExifInterface.ORIENTATION_ROTATE_270 -> rotation = 270f
            }
            inputStream.close()
        }

        // 2. 获取尺寸
        val options = android.graphics.BitmapFactory.Options()
        options.inJustDecodeBounds = true
        contentResolver.openInputStream(uri)?.use {
            android.graphics.BitmapFactory.decodeStream(it, null, options)
        }

        // 3. 计算采样率
        var inSampleSize = 1
        if (options.outHeight > maxReqSize || options.outWidth > maxReqSize) {
            val halfHeight = options.outHeight / 2
            val halfWidth = options.outWidth / 2
            // 使用 || (OR) 只要有一边大于 reqSize 就继续压缩
            while ((halfHeight / inSampleSize) > maxReqSize || (halfWidth / inSampleSize) > maxReqSize) {
                inSampleSize *= 2
            }
        }

        // 4. 设置解码参数
        options.inJustDecodeBounds = false
        options.inSampleSize = inSampleSize
        options.inPreferredConfig = Bitmap.Config.ARGB_8888 // 防止 Hardware Bitmap
        options.inMutable = true

        // 5. 解码 Bitmap
        var bitmap = contentResolver.openInputStream(uri)?.use {
            android.graphics.BitmapFactory.decodeStream(it, null, options)
        }

        if (bitmap == null) return null

        // 6. 处理 Hardware Bitmap (再次保险)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            if (bitmap!!.config == Bitmap.Config.HARDWARE) {
                val softwareBitmap = bitmap!!.copy(Bitmap.Config.ARGB_8888, false)
                if (softwareBitmap != null) {
                    bitmap!!.recycle()
                    bitmap = softwareBitmap
                }
            }
        }

        // 7. 处理旋转
        if (rotation != 0f) {
            val matrix = android.graphics.Matrix()
            matrix.postRotate(rotation)
            val rotatedBitmap = Bitmap.createBitmap(
                bitmap!!, 0, 0, bitmap!!.width, bitmap!!.height, matrix, true
            )
            if (rotatedBitmap != bitmap) {
                bitmap!!.recycle()
                bitmap = rotatedBitmap
            }
        }

        return bitmap
    } catch (e: Exception) {
        e.printStackTrace()
        return null
    } finally {
        try {
            inputStream?.close()
        } catch (e: Exception) {}
    }
}

enum class BeautyType(val title: String, val icon: ImageVector, val maxRange: Float) {
    SMOOTH("磨皮", Icons.Default.Face, 10.0f),
    WHITEN("美白", Icons.Default.Star, 1.0f), // Changed from 5.0f to 1.0f for logical scale
    RUDDY("红润", Icons.Default.Favorite, 1.0f), // Changed from 2.5f to 1.0f
    SHARPEN("清晰", Icons.Default.Edit, 4.0f)
}

enum class EditorMode { FILTER, COLLAGE, BEAUTY }

@Composable
fun EditorScreen(imageUris: List<Uri>, onBack: () -> Unit, initialMode: EditorMode = EditorMode.BEAUTY) {
    var mode by remember { mutableStateOf(initialMode) }
    val context = LocalContext.current
    val primary = Color(0xFFCCFF00)

    Column(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(onClick = { mode = EditorMode.FILTER }, colors = ButtonDefaults.buttonColors(containerColor = if (mode == EditorMode.FILTER) primary else Color.DarkGray)) { Text("滤镜", color = if (mode == EditorMode.FILTER) Color.Black else Color.White) }
            Button(onClick = { mode = EditorMode.COLLAGE }, colors = ButtonDefaults.buttonColors(containerColor = if (mode == EditorMode.COLLAGE) primary else Color.DarkGray)) { Text("拼图", color = if (mode == EditorMode.COLLAGE) Color.Black else Color.White) }
            Button(onClick = { mode = EditorMode.BEAUTY }, colors = ButtonDefaults.buttonColors(containerColor = if (mode == EditorMode.BEAUTY) primary else Color.DarkGray)) { Text("AI 修人脸", color = if (mode == EditorMode.BEAUTY) Color.Black else Color.White) }
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                Icons.Default.Close,
                contentDescription = "Back",
                tint = Color.White,
                modifier = Modifier.size(28.dp).clickable { onBack() }
            )
        }

        when (mode) {
            EditorMode.FILTER -> FilterEditor(imageUris.firstOrNull(), onBack)
            EditorMode.COLLAGE -> CollageEditor(imageUris)
            EditorMode.BEAUTY -> {
                val uri = imageUris.firstOrNull()
                if (uri != null) {
                    FaceBeautyScreen(uri, onBack)
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("请选择图片", color = Color.White)
                    }
                }
            }
        }
    }
}

fun applyColorMatrixRS(context: Context, bitmap: Bitmap, matrix: android.renderscript.Matrix4f): Bitmap? {
    if (bitmap.isRecycled) return null
    return try {
        val rs = android.renderscript.RenderScript.create(context)
        val inAlloc = android.renderscript.Allocation.createFromBitmap(rs, bitmap, android.renderscript.Allocation.MipmapControl.MIPMAP_NONE, android.renderscript.Allocation.USAGE_SCRIPT)
        val outAlloc = android.renderscript.Allocation.createTyped(rs, inAlloc.type)
        val color = android.renderscript.ScriptIntrinsicColorMatrix.create(rs, android.renderscript.Element.U8_4(rs))
        color.setColorMatrix(matrix)
        color.forEach(inAlloc, outAlloc)
        val out = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config)
        outAlloc.copyTo(out)
        inAlloc.destroy(); outAlloc.destroy(); color.destroy(); rs.destroy()
        out
    } catch (e: Exception) {
        null
    }
}

fun blendMatrix(identity: android.renderscript.Matrix4f, target: android.renderscript.Matrix4f, alpha: Float): android.renderscript.Matrix4f {
    val a = alpha.coerceIn(0f, 1f)
    val id = identity.array
    val tg = target.array
    val out = FloatArray(16)
    for (i in 0 until 16) {
        out[i] = id[i] * (1f - a) + tg[i] * a
    }
    return android.renderscript.Matrix4f(out)
}

@Composable
fun FilterEditor(imageUri: Uri?, onBack: () -> Unit) {
    val context = LocalContext.current
    var original by remember { mutableStateOf<Bitmap?>(null) }
    var display by remember { mutableStateOf<Bitmap?>(null) }
    var isLoaded by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(imageUri) {
        if (imageUri == null) return@LaunchedEffect
        withContext(Dispatchers.IO) {
            val bmp = loadCompressedBitmap(context, imageUri, 2560)
            if (bmp != null) {
                val previewScale = 1080f / kotlin.math.max(bmp.width, bmp.height)
                val previewBmp = if (previewScale < 1f) Bitmap.createScaledBitmap(bmp, (bmp.width * previewScale).toInt(), (bmp.height * previewScale).toInt(), true) else bmp.copy(bmp.config, true)
                withContext(Dispatchers.Main) {
                    original = bmp
                    display = previewBmp
                    isLoaded = true
                }
            }
        }
    }

    val filters = listOf(
        "原图" to android.renderscript.Matrix4f(),
        "暖色" to android.renderscript.Matrix4f(floatArrayOf(
            1.1f, 0f, 0f, 0f,
            0f, 1.0f, 0f, 0f,
            0f, 0f, 0.9f, 0f,
            0f, 0f, 0f, 1f
        )),
        "冷色" to android.renderscript.Matrix4f(floatArrayOf(
            0.9f, 0f, 0f, 0f,
            0f, 1.0f, 0f, 0f,
            0f, 0f, 1.1f, 0f,
            0f, 0f, 0f, 1f
        )),
        "黑白" to run {
            val rw = 0.299f; val gw = 0.587f; val bw = 0.114f
            android.renderscript.Matrix4f(floatArrayOf(
                rw, gw, bw, 0f,
                rw, gw, bw, 0f,
                rw, gw, bw, 0f,
                0f, 0f, 0f, 1f
            ))
        }
    )

    Column(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        Box(modifier = Modifier.weight(1f).fillMaxWidth().background(Color.Black)) {
            if (display != null) {
                Image(bitmap = display!!.asImageBitmap(), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
            } else {
                if (!isLoaded) CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = Color(0xFFCCFF00))
            }
        }
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            filters.forEach { pair ->
                Button(onClick = {
                    val bmp = original ?: return@Button
                    scope.launch {
                        val preview = withContext(Dispatchers.IO) { applyColorMatrixRS(context, bmp, pair.second) }
                        if (preview != null) display = preview
                    }
                }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF333333))) { Text(pair.first, color = Color.White) }
            }
            Button(onClick = {
                val bmp = original ?: return@Button
                scope.launch {
                    val current = display ?: bmp
                    val saved = withContext(Dispatchers.IO) { current }
                    val success = saveBitmapToGallery(context, saved)
                    if (success) onBack() else Toast.makeText(context, "保存失败", Toast.LENGTH_SHORT).show()
                }
            }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFCCFF00))) { Text("保存", color = Color.Black) }
        }
    }
}

@Composable
fun CollageEditor(imageUris: List<Uri>) {
    val context = LocalContext.current
    var bitmaps by remember { mutableStateOf<List<Bitmap>>(emptyList()) }
    LaunchedEffect(imageUris) {
        withContext(Dispatchers.IO) {
            val list = imageUris.take(4).mapNotNull { loadCompressedBitmap(context, it, 1280) }
            withContext(Dispatchers.Main) { bitmaps = list }
        }
    }
    Column(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (bitmaps.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("请选择图片", color = Color.White) }
        } else {
            val rows = if (bitmaps.size <= 2) 1 else 2
            val cols = if (bitmaps.size == 1) 1 else 2
            Column(modifier = Modifier.weight(1f).fillMaxWidth().padding(8.dp)) {
                var index = 0
                repeat(rows) {
                    Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
                        repeat(cols) {
                            Box(modifier = Modifier.weight(1f).padding(4.dp).background(Color.DarkGray)) {
                                val bmp = bitmaps.getOrNull(index)
                                if (bmp != null) {
                                    Image(bitmap = bmp.asImageBitmap(), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                }
                            }
                            index++
                        }
                    }
                }
            }
        }
    }
}
