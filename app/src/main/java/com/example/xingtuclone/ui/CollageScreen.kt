package com.example.xingtuclone.ui

import android.content.ClipData
import android.content.ClipDescription
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.view.DragEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.load
import kotlinx.coroutines.launch
enum class CollageType(val count: Int, val displayName: String) {
    // 2张
    V2(2, "左右分"),
    H2(2, "上下分"),

    // 3张
    V3(3, "三竖条"),
    H3(3, "三横条"),
    L1_R2(3, "左1右2"),  // 经典T型
    T1_B2(3, "上1下2"),

    // 4张
    GRID_4(4, "田字格"),
    V4(4, "四竖条"),
    L1_R3(4, "左1右3"),  // 1大3小
    T1_B3(4, "上1下3"),

    // 5张
    L1_R4(5, "左1右4"),  // 1大4小 (2x2)
    T2_B3(5, "上2下3"),

    // 6张
    GRID_6(6, "六宫格"),
    BIG_MID(6, "中间大")
}
@Composable
fun CollageScreen(imageUris: List<Uri>, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // 🔥 1. 把图片列表变成可变状态，这样交换顺序后界面会自动刷新
    var currentUris by remember { mutableStateOf(imageUris) }

    // 模板逻辑保持不变
    val validTemplates = remember(currentUris.size) {
        CollageType.values().filter { it.count == currentUris.size }
    }
    var currentType by remember {
        mutableStateOf(if (validTemplates.isNotEmpty()) validTemplates[0] else CollageType.V2)
    }

    // 参数保持不变
    var gapSize by remember { mutableFloatStateOf(10f) }
    var cornerRadius by remember { mutableFloatStateOf(0f) }
    var aspectRatio by remember { mutableFloatStateOf(1f) }
    var isSaving by remember { mutableStateOf(false) }
    var captureView: View? = remember { null }

    Column(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        // ... 顶部栏保持不变 ...
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Close, "Back", tint = Color.White, modifier = Modifier.clickable { onBack() })
            Text("拼图 (长按拖拽交换)", color = Color.White, fontSize = 18.sp) // 改个标题提示用户
            Icon(
                Icons.Default.Check, "Save",
                tint = if (isSaving) Color.Gray else Color(0xFFCCFF00),
                modifier = Modifier.clickable(enabled = !isSaving) {
                    isSaving = true
                    scope.launch {
                        // 1. 检查 View 是否存在
                        if (captureView == null || captureView!!.width <= 0) {
                            Toast.makeText(context, "正在渲染中，请稍后再试...", Toast.LENGTH_SHORT).show()
                            isSaving = false
                            return@launch
                        }

                        try {
                            // 2. 生成 Bitmap (使用新写的安全方法)
                            val bitmap = viewToBitmap(captureView!!)

                            if (bitmap != null) {
                                // 3. 保存到相册
                                val success = saveBitmapToGallery(context, bitmap)
                                if (success) {
                                    onBack() // 保存成功才退出
                                } else {
                                    Toast.makeText(context, "保存失败：权限或存储错误", Toast.LENGTH_SHORT).show()
                                }
                                // 记得回收 Bitmap 释放内存
                                if (!bitmap.isRecycled) bitmap.recycle()
                            } else {
                                Toast.makeText(context, "保存失败：内存不足，无法生成图片", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            Toast.makeText(context, "发生未知错误: ${e.message}", Toast.LENGTH_LONG).show()
                        } finally {
                            isSaving = false
                        }
                    }
                }
            )
        }

        // 2. 预览区
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                factory = { ctx ->
                    FrameLayout(ctx).apply { setBackgroundColor(android.graphics.Color.WHITE) }
                },
                update = { view ->
                    captureView = view
                    view.removeAllViews()

                    val containerWidth = view.measuredWidth
                    val containerHeight = (containerWidth / aspectRatio).toInt()

                    if (containerWidth > 0) {
                        val params = view.layoutParams ?: ViewGroup.LayoutParams(0, 0)
                        params.width = containerWidth
                        params.height = containerHeight
                        view.layoutParams = params

                        // 🔥 调用布局生成器，传入交换回调
                        generateRichLayout(
                            view,
                            currentUris, // 传入当前最新的 list
                            currentType,
                            gapSize.toInt(),
                            cornerRadius,
                            containerWidth,
                            containerHeight,
                            onSwap = { fromIndex, toIndex ->
                                // 🔥 核心交换逻辑
                                val newList = currentUris.toMutableList()
                                if (fromIndex in newList.indices && toIndex in newList.indices) {
                                    val temp = newList[fromIndex]
                                    newList[fromIndex] = newList[toIndex]
                                    newList[toIndex] = temp
                                    currentUris = newList // 更新状态，触发重绘
                                }
                            }
                        )
                    }
                },
                modifier = Modifier.aspectRatio(aspectRatio).fillMaxWidth()
            )
        }

        // ... 底部控制区保持不变 ...
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1A1A1A))
                .padding(16.dp)
        ) {
            // 参数滑块
            ControlSlider("边距", gapSize, 0f..50f) { gapSize = it }
            ControlSlider("圆角", cornerRadius, 0f..100f) { cornerRadius = it }

            Spacer(modifier = Modifier.height(12.dp))

            // 比例选择
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                RatioButton("1:1", 1f, aspectRatio) { aspectRatio = 1f }
                RatioButton("3:4", 0.75f, aspectRatio) { aspectRatio = 0.75f }
                RatioButton("4:3", 1.33f, aspectRatio) { aspectRatio = 1.33f }
                RatioButton("9:16", 0.5625f, aspectRatio) { aspectRatio = 0.5625f }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 模板列表
            if (validTemplates.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(validTemplates.size) { index ->
                        val type = validTemplates[index]
                        TemplateItem(type, currentType == type) { currentType = type }
                    }
                }
            }
        }
    }
}

// --- UI 组件 ---
@Composable
fun ControlSlider(label: String, value: Float, range: ClosedFloatingPointRange<Float>, onValueChange: (Float) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Color.Gray, fontSize = 12.sp, modifier = Modifier.width(40.dp))
        Slider(
            value = value, onValueChange = onValueChange, valueRange = range,
            colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Color(0xFFCCFF00)),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun RatioButton(text: String, ratio: Float, current: Float, onClick: () -> Unit) {
    val isSelected = kotlin.math.abs(current - ratio) < 0.01f
    Text(
        text,
        color = if (isSelected) Color.Black else Color.Gray,
        fontSize = 12.sp,
        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) Color(0xFFCCFF00) else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp)
    )
}

@Composable
fun TemplateItem(type: CollageType, isSelected: Boolean, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onClick() }) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .border(2.dp, if (isSelected) Color(0xFFCCFF00) else Color.Gray, RoundedCornerShape(8.dp))
                .background(Color.DarkGray, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            // 这里以后可以换成真实的 icon 图片
            Text(type.displayName.take(1), color = Color.White, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(type.displayName, color = if (isSelected) Color(0xFFCCFF00) else Color.Gray, fontSize = 10.sp)
    }
}

// =================================================================================
// 🔥🔥🔥 核心布局算法：支持 2-6 张图的 14 种布局 🔥🔥🔥
// =================================================================================
// =================================================================================
// 🔥🔥🔥 核心布局算法：支持拖拽交换 🔥🔥🔥
// =================================================================================
// =================================================================================
// 🔥🔥🔥 核心布局算法 (已修复保存闪退问题) 🔥🔥🔥
// =================================================================================
fun generateRichLayout(
    parent: FrameLayout,
    uris: List<Uri>,
    type: CollageType,
    g: Int,
    c: Float,
    w: Int,
    h: Int,
    onSwap: (Int, Int) -> Unit
) {
    val safeUris = uris.take(type.count)
    if (safeUris.isEmpty()) return

    // 辅助函数：添加图片并绑定拖拽事件
    fun add(index: Int, x: Int, y: Int, width: Int, height: Int) {
        if (index >= safeUris.size) return

        val img = ImageView(parent.context).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP

            // 🔥🔥🔥 重点修改这里 🔥🔥🔥
            load(safeUris[index]) {
                allowHardware(false) // 必须设为 false，否则 viewToBitmap 无法截图保存
                size(1000) // 限制分辨率，防止 6 张原图直接把 App 内存撑爆 (OOM)
            }
            // 🔥🔥🔥 修改结束 🔥🔥🔥

            clipToOutline = true
            outlineProvider = object : android.view.ViewOutlineProvider() {
                override fun getOutline(view: View, outline: android.graphics.Outline) {
                    outline.setRoundRect(0, 0, view.width, view.height, c)
                }
            }

            // --- 拖拽逻辑保持不变 ---
            setOnLongClickListener { view ->
                val item = ClipData.Item(index.toString())
                val data = ClipData(index.toString(), arrayOf(ClipDescription.MIMETYPE_TEXT_PLAIN), item)
                val shadowBuilder = View.DragShadowBuilder(view)
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                    view.startDragAndDrop(data, shadowBuilder, view, 0)
                } else {
                    @Suppress("DEPRECATION")
                    view.startDrag(data, shadowBuilder, view, 0)
                }
                true
            }

            setOnDragListener { v, event ->
                when (event.action) {
                    DragEvent.ACTION_DRAG_STARTED -> true
                    DragEvent.ACTION_DRAG_ENTERED -> { v.alpha = 0.5f; true }
                    DragEvent.ACTION_DRAG_EXITED -> { v.alpha = 1.0f; true }
                    DragEvent.ACTION_DROP -> {
                        v.alpha = 1.0f
                        val item = event.clipData.getItemAt(0)
                        val sourceIndex = item.text.toString().toInt()
                        val targetIndex = index
                        if (sourceIndex != targetIndex) {
                            onSwap(sourceIndex, targetIndex)
                        }
                        true
                    }
                    DragEvent.ACTION_DRAG_ENDED -> { v.alpha = 1.0f; true }
                    else -> false
                }
            }
        }

        val params = FrameLayout.LayoutParams(width, height)
        params.leftMargin = x
        params.topMargin = y
        parent.addView(img, params)
    }

    // ... 下面的布局坐标计算代码完全不用动 ...
    // ... 复制你原来的 switch (type) 逻辑 ...
    // 常用尺寸计算
    val wHalf = (w - 3 * g) / 2
    val hHalf = (h - 3 * g) / 2
    val wThird = (w - 4 * g) / 3
    val hThird = (h - 4 * g) / 3

    when (type) {
        CollageType.V2 -> {
            add(0, g, g, wHalf, h - 2 * g)
            add(1, g + wHalf + g, g, wHalf, h - 2 * g)
        }
        CollageType.H2 -> {
            add(0, g, g, w - 2 * g, hHalf)
            add(1, g, g + hHalf + g, w - 2 * g, hHalf)
        }
        CollageType.V3 -> {
            for (i in 0..2) add(i, g + i * (wThird + g), g, wThird, h - 2 * g)
        }
        CollageType.H3 -> {
            for (i in 0..2) add(i, g, g + i * (hThird + g), w - 2 * g, hThird)
        }
        CollageType.L1_R2 -> {
            add(0, g, g, wHalf, h - 2 * g)
            add(1, g + wHalf + g, g, wHalf, hHalf)
            add(2, g + wHalf + g, g + hHalf + g, wHalf, hHalf)
        }
        CollageType.T1_B2 -> {
            add(0, g, g, w - 2 * g, hHalf)
            add(1, g, g + hHalf + g, wHalf, hHalf)
            add(2, g + wHalf + g, g + hHalf + g, wHalf, hHalf)
        }
        CollageType.GRID_4 -> {
            add(0, g, g, wHalf, hHalf)
            add(1, g + wHalf + g, g, wHalf, hHalf)
            add(2, g, g + hHalf + g, wHalf, hHalf)
            add(3, g + wHalf + g, g + hHalf + g, wHalf, hHalf)
        }
        CollageType.V4 -> {
            val wQuarter = (w - 5 * g) / 4
            for (i in 0..3) add(i, g + i * (wQuarter + g), g, wQuarter, h - 2 * g)
        }
        CollageType.L1_R3 -> {
            add(0, g, g, wHalf, h - 2 * g)
            add(1, g + wHalf + g, g, wHalf, hThird)
            add(2, g + wHalf + g, g + hThird + g, wHalf, hThird)
            add(3, g + wHalf + g, g + 2 * (hThird + g), wHalf, hThird)
        }
        CollageType.T1_B3 -> {
            add(0, g, g, w - 2 * g, hHalf)
            add(1, g, g + hHalf + g, wThird, hHalf)
            add(2, g + wThird + g, g + hHalf + g, wThird, hHalf)
            add(3, g + 2 * (wThird + g), g + hHalf + g, wThird, hHalf)
        }
        CollageType.L1_R4 -> {
            add(0, g, g, wHalf, h - 2 * g)
            // 右侧四个
            val rX = g + wHalf + g
            val rW = wHalf
            val rH = (h - 3 * g) / 2
            add(1, rX, g, (rW - g)/2, rH)
            add(2, rX + (rW - g)/2 + g, g, (rW - g)/2, rH)
            add(3, rX, g + rH + g, (rW - g)/2, rH)
            add(4, rX + (rW - g)/2 + g, g + rH + g, (rW - g)/2, rH)
        }
        CollageType.T2_B3 -> {
            add(0, g, g, wHalf, hHalf)
            add(1, g + wHalf + g, g, wHalf, hHalf)
            add(2, g, g + hHalf + g, wThird, hHalf)
            add(3, g + wThird + g, g + hHalf + g, wThird, hHalf)
            add(4, g + 2 * (wThird + g), g + hHalf + g, wThird, hHalf)
        }
        CollageType.GRID_6 -> {
            val itemW = wThird
            val itemH = hHalf
            for (i in 0..5) {
                val row = i / 3
                val col = i % 3
                add(i, g + col * (itemW + g), g + row * (itemH + g), itemW, itemH)
            }
        }
        CollageType.BIG_MID -> {
            val itemW = wHalf
            val itemH = hThird
            for (i in 0..5) {
                val row = i / 2
                val col = i % 2
                add(i, g + col * (itemW + g), g + row * (itemH + g), itemW, itemH)
            }
        }
        else -> {}
    }
}
// viewToBitmap 保持不变
fun viewToBitmap(view: View): Bitmap? {
    // 1. 检查 View 是否有效
    if (view.width <= 0 || view.height <= 0) {
        return null
    }

    return try {
        // 2. 尝试创建全尺寸 Bitmap
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // 3. 处理背景 (防止保存出来是透明底)
        val bgDrawable = view.background
        if (bgDrawable != null) {
            bgDrawable.draw(canvas)
        } else {
            canvas.drawColor(android.graphics.Color.WHITE)
        }

        // 4. 绘制内容
        view.draw(canvas)
        bitmap
    } catch (e: OutOfMemoryError) {
        // 🔥 核心保护：如果内存爆了，尝试缩小一半尺寸再保存
        e.printStackTrace()
        try {
            val scale = 0.5f
            val bitmap = Bitmap.createBitmap(
                (view.width * scale).toInt(),
                (view.height * scale).toInt(),
                Bitmap.Config.ARGB_8888
            )
            val canvas = Canvas(bitmap)
            canvas.scale(scale, scale) // 缩放画布

            val bgDrawable = view.background
            if (bgDrawable != null) {
                bgDrawable.draw(canvas)
            } else {
                canvas.drawColor(android.graphics.Color.WHITE)
            }
            view.draw(canvas)
            bitmap
        } catch (e2: Exception) {
            e2.printStackTrace()
            null // 实在没办法了
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}