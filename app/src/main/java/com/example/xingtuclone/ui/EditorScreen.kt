package com.example.xingtuclone.ui

import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import jp.co.cyberagent.android.gpuimage.GPUImage
import jp.co.cyberagent.android.gpuimage.GPUImageView
import jp.co.cyberagent.android.gpuimage.filter.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import java.io.InputStream
enum class EditMode { FILTER, ADJUST }

@Composable
fun EditorScreen(imageUri: Uri, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope() // 用于启动保存协程

    // --- 状态管理 ---
    var currentMode by remember { mutableStateOf(EditMode.FILTER) }
    var selectedFilterItem by remember { mutableStateOf(filterList[0]) }

    // 调节参数
    var brightnessValue by remember { mutableFloatStateOf(0.0f) }
    var contrastValue by remember { mutableFloatStateOf(1.0f) }
    var saturationValue by remember { mutableFloatStateOf(1.0f) }
    var selectedAdjustType by remember { mutableStateOf(AdjustType.BRIGHTNESS) }

    // 保存加载状态
    var isSaving by remember { mutableStateOf(false) }

    // --- GPUImage 初始化 ---
    // 我们需要把 GPUImageView 存下来，以便后面提取 Bitmap
    val gpuImageView = remember { GPUImageView(context) }

    // 滤镜实例
    val brightnessFilter = remember { GPUImageBrightnessFilter() }
    val contrastFilter = remember { GPUImageContrastFilter() }
    val saturationFilter = remember { GPUImageSaturationFilter() }

    // 加载图片
    LaunchedEffect(imageUri) {
        // 在后台线程加载并处理旋转
        val bitmap = withContext(Dispatchers.IO) {
            loadBitmapWithRotation(context, imageUri)
        }

        // 如果加载成功，把 Bitmap 传给 GPUImage
        if (bitmap != null) {
            gpuImageView.setImage(bitmap)
        } else {
            // 如果加载失败（极少情况），为了防止空指针，做个处理
            Toast.makeText(context, "图片加载失败", Toast.LENGTH_SHORT).show()
            onBack()
        }
    }

    // 组合滤镜逻辑 (当参数变化时自动应用)
    LaunchedEffect(selectedFilterItem, brightnessValue, contrastValue, saturationValue) {
        brightnessFilter.setBrightness(brightnessValue)
        contrastFilter.setContrast(contrastValue)
        saturationFilter.setSaturation(saturationValue)

        val group = GPUImageFilterGroup()
        group.addFilter(selectedFilterItem.filter)
        group.addFilter(brightnessFilter)
        group.addFilter(contrastFilter)
        group.addFilter(saturationFilter)

        gpuImageView.filter = group
    }

    // --- 核心UI ---
    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {

        Column(modifier = Modifier.fillMaxSize()) {
            // 1. 顶部栏
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 返回按钮
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.clickable { onBack() }
                )

                // 🔥 保存按钮逻辑
                Icon(
                    Icons.Default.Check,
                    contentDescription = "Save",
                    tint = if (isSaving) Color.Gray else Color(0xFFCCFF00), // 保存时变灰
                    modifier = Modifier.clickable(enabled = !isSaving) {
                        isSaving = true
                        scope.launch {
                            try {
                                // 1. 从 GPUImage 获取当前渲染的 Bitmap
                                // 注意：capture() 是保存文件，getBitmapWithFilterApplied() 是获取内存中的 Bitmap
                                val resultBitmap: Bitmap? = withContext(Dispatchers.IO) {
                                    try {
                                        gpuImageView.gpuImage.getBitmapWithFilterApplied()
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                        null
                                    }
                                }

                                // 2. 保存到相册
                                if (resultBitmap != null) {
                                    val success = saveBitmapToGallery(context, resultBitmap)
                                    // 3. 保存成功后退出编辑页面，或者留在当前页面
                                    if(success){
                                        onBack();
                                    } else {
                                        Toast.makeText(context, "生成图片失败", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    Toast.makeText(context, "生成图片失败", Toast.LENGTH_SHORT).show()
                                }
                            } finally {
                                isSaving = false
                            }
                        }
                    }
                )
            }

            // 2. 图片预览区
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = {
                        gpuImageView.apply {
                            setScaleType(GPUImage.ScaleType.CENTER_INSIDE)
                        }
                    }
                )
            }

            // 3. 底部操作区
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1A1A1A))
                    .padding(bottom = 16.dp)
            ) {
                // 滑块区域
                if (currentMode == EditMode.ADJUST) {
                    Box(modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp)) {
                        Slider(
                            value = when (selectedAdjustType) {
                                AdjustType.BRIGHTNESS -> brightnessValue
                                AdjustType.CONTRAST -> contrastValue
                                AdjustType.SATURATION -> saturationValue
                            },
                            onValueChange = { newValue ->
                                when (selectedAdjustType) {
                                    AdjustType.BRIGHTNESS -> brightnessValue = newValue
                                    AdjustType.CONTRAST -> contrastValue = newValue
                                    AdjustType.SATURATION -> saturationValue = newValue
                                }
                            },
                            valueRange = when (selectedAdjustType) {
                                AdjustType.BRIGHTNESS -> -0.5f..0.5f
                                AdjustType.CONTRAST -> 0.5f..2.0f
                                AdjustType.SATURATION -> 0.0f..2.0f
                            },
                            colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Color(0xFFCCFF00))
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.height(20.dp))
                }

                // 功能列表
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.height(90.dp)
                ) {
                    if (currentMode == EditMode.FILTER) {
                        items(count = filterList.size) { index ->
                            val item = filterList[index]
                            FilterItemButton(
                                item = item,
                                isSelected = item == selectedFilterItem,
                                onClick = { selectedFilterItem = item }
                            )
                        }
                    } else {
                        items(count = adjustTools.size) { index ->
                            val tool = adjustTools[index]
                            AdjustToolButton(
                                item = tool,
                                isSelected = tool.type == selectedAdjustType,
                                onClick = { selectedAdjustType = tool.type }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 底部 Tab 切换
                Row(modifier = Modifier.fillMaxWidth()) {
                    BottomTabButton("滤镜", currentMode == EditMode.FILTER, Modifier.weight(1f)) { currentMode = EditMode.FILTER }
                    BottomTabButton("调节", currentMode == EditMode.ADJUST, Modifier.weight(1f)) { currentMode = EditMode.ADJUST }
                }
            }
        }

        // --- 全局 Loading 遮罩 ---
        if (isSaving) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable(enabled = false) {}, // 拦截点击
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Color(0xFFCCFF00))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("保存中...", color = Color.White)
                }
            }
        }
    }
}
// ==========================================
// ↓↓↓ 请把这些代码补到 EditorScreen.kt 的最底部 ↓↓↓
// ==========================================

@Composable
fun FilterItemButton(item: FilterItem, isSelected: Boolean, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(if (isSelected) Color(0xFFCCFF00) else Color.Gray),
            contentAlignment = Alignment.Center
        ) {
            // 取滤镜名字的第一个字作为预览图占位
            Text(
                text = item.name.first().toString(),
                color = if (isSelected) Color.Black else Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = item.name,
            color = if (isSelected) Color(0xFFCCFF00) else Color.Gray,
            fontSize = 12.sp
        )
    }
}

@Composable
fun AdjustToolButton(item: AdjustItem, isSelected: Boolean, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClick() }
            .width(60.dp)
    ) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(CircleShape)
                .background(if (isSelected) Color(0xFF333333) else Color.Transparent)
                .border(1.dp, if (isSelected) Color(0xFFCCFF00) else Color.Gray, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = null,
                tint = if (isSelected) Color(0xFFCCFF00) else Color.White
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = item.name,
            color = Color.Gray,
            fontSize = 12.sp
        )
    }
}

@Composable
fun BottomTabButton(text: String, isSelected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier = modifier
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = text,
            color = if (isSelected) Color.White else Color.Gray,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            fontSize = 16.sp
        )
        if (isSelected) {
            Box(
                modifier = Modifier
                    .padding(top = 4.dp)
                    .size(4.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFCCFF00))
            )
        }
    }
}
// ==========================================
// ↓↓↓ 把这个函数放到 EditorScreen.kt 最底部 ↓↓↓
// ==========================================


/**
 * 安全加载图片并处理旋转（解决 GPUImage 崩溃的核心）
 */
fun loadBitmapWithRotation(context: Context, uri: Uri): Bitmap? {
    var inputStream: InputStream? = null
    try {
        val contentResolver = context.contentResolver
        inputStream = contentResolver.openInputStream(uri) ?: return null

        // 1. 解码图片
        val bitmap = BitmapFactory.decodeStream(inputStream) ?: return null
        inputStream.close()

        // 2. 读取旋转信息 (Exif)
        inputStream = contentResolver.openInputStream(uri) ?: return bitmap
        val exifInterface = ExifInterface(inputStream)
        val orientation = exifInterface.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )
        inputStream.close()

        // 3. 如果需要旋转，处理 Bitmap
        var rotation = 0f
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> rotation = 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> rotation = 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> rotation = 270f
        }

        return if (rotation != 0f) {
            val matrix = Matrix()
            matrix.postRotate(rotation)
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } else {
            bitmap
        }

    } catch (e: Exception) {
        e.printStackTrace()
        return null
    } finally {
        inputStream?.close()
    }
}