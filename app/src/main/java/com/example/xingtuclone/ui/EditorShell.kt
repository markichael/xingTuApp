package com.example.xingtuclone.ui

import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.xingtuclone.utils.BeautyProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class EditorCategory { TEMPLATE, PORTRAIT, FILTER, ADJUST, STICKER, TEXT, EFFECT }

private fun matRM(vararg v: Float): android.renderscript.Matrix4f {
    val m = android.renderscript.Matrix4f(v)
    m.transpose()
    return m
}

private fun saturationMatrix(s: Float): android.renderscript.Matrix4f {
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
    return android.renderscript.Matrix4f(data).apply { transpose() }
}

private fun brightMatrix(b: Float): android.renderscript.Matrix4f {
    return android.renderscript.Matrix4f(
        floatArrayOf(
            b, 0f, 0f, 0f,
            0f, b, 0f, 0f,
            0f, 0f, b, 0f,
            0f, 0f, 0f, 1f
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorShell(
    imageUris: List<Uri>,
    onBack: () -> Unit,
    initialCategory: EditorCategory = EditorCategory.PORTRAIT,
    onSaved: ((Uri) -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val primary = Color(0xFFCCFF00)

    var category by remember { mutableStateOf(initialCategory) }
    var selectedTool by remember { mutableStateOf("自动美颜") }
    var showMenu by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var isComparing by remember { mutableStateOf(false) }

    var original by remember { mutableStateOf<Bitmap?>(null) }
    var preview by remember { mutableStateOf<Bitmap?>(null) }
    var display by remember { mutableStateOf<Bitmap?>(null) }
    var loaded by remember { mutableStateOf(false) }

    val beauty = remember { BeautyProcessor(context) }

    DisposableEffect(Unit) { onDispose { beauty.destroy() } }

    LaunchedEffect(imageUris) {
        val uri = imageUris.firstOrNull() ?: return@LaunchedEffect
        withContext(Dispatchers.IO) {
            val full = loadCompressedBitmap(context, uri, 2560)
            if (full != null) {
                val s = 1080f / kotlin.math.max(full.width, full.height)
                val prev = if (s < 1f) Bitmap.createScaledBitmap(full, (full.width * s).toInt(), (full.height * s).toInt(), true) else full.copy(full.config, true)
                withContext(Dispatchers.Main) {
                    original = full
                    preview = prev
                    display = prev
                    loaded = true
                }
            }
        }
    }

    var smooth by remember { mutableFloatStateOf(4f) }
    var whiten by remember { mutableFloatStateOf(0.3f) }
    var ruddy by remember { mutableFloatStateOf(0.3f) }
    var sharpen by remember { mutableFloatStateOf(1.0f) }

    LaunchedEffect(category, selectedTool, smooth, whiten, ruddy, sharpen, loaded, isComparing) {
        if (!loaded || preview == null) return@LaunchedEffect
        if (isComparing) { display = preview; return@LaunchedEffect }
        when (category) {
            EditorCategory.PORTRAIT -> {
                val bmp = withContext(Dispatchers.IO) {
                    beauty.process(preview!!, smooth, whiten, ruddy, sharpen)
                }
                if (bmp != null) display = bmp
            }
            EditorCategory.FILTER -> {
                val matrix = when (selectedTool) {
                    "原图" -> android.renderscript.Matrix4f()
                    "暖色" -> brightMatrix(1.02f)
                    "冷色" -> matRM(
                        0.95f, 0f,    0f,    0f,
                        0f,    1.0f,  0f,    0f,
                        0f,    0.05f, 1.05f, 0f,
                        0f,    0f,    0f,    1f
                    )
                    "黑白" -> {
                        val rw = 0.299f; val gw = 0.587f; val bw = 0.114f
                        matRM(
                            rw, gw, bw, 0f,
                            rw, gw, bw, 0f,
                            rw, gw, bw, 0f,
                            0f, 0f, 0f, 1f
                        )
                    }
                    "复古" -> matRM(
                        0.393f, 0.769f, 0.189f, 0f,
                        0.349f, 0.686f, 0.168f, 0f,
                        0.272f, 0.534f, 0.131f, 0f,
                        0f,     0f,     0f,     1f
                    )
                    "增饱和" -> saturationMatrix(1.35f)
                    "降饱和" -> saturationMatrix(0.75f)
                    "柔和" -> saturationMatrix(0.85f)
                    "青橙" -> matRM(
                        1.05f, -0.04f, 0f,   0f,
                        0f,    0.95f,  0f,   0f,
                        0f,     0.06f, 1.06f,0f,
                        0f,     0f,    0f,   1f
                    )
                    "粉调" -> matRM(
                        1.06f, 0.02f, 0.02f, 0f,
                        0.02f, 0.98f, 0.0f,  0f,
                        0.02f, 0.0f,  1.02f, 0f,
                        0f,    0f,    0f,    1f
                    )
                    "绿野" -> matRM(
                        0.95f, 0.0f,  0.0f,  0f,
                        0.05f, 1.05f, 0.0f,  0f,
                        0.0f,  0.0f,  0.95f, 0f,
                        0f,    0f,    0f,    1f
                    )
                    else -> android.renderscript.Matrix4f()
                }
                val bmp = withContext(Dispatchers.IO) { applyColorMatrixRS(context, preview!!, matrix) }
                if (bmp != null) display = bmp
            }
            else -> { display = preview }
        }
    }

    val portraitTools = listOf("自动美颜")
    val filterTools = listOf("原图", "暖色", "冷色", "黑白", "复古", "增饱和", "降饱和", "柔和", "青橙", "粉调", "绿野")
    val categories = listOf(
        EditorCategory.PORTRAIT to "人像",
        EditorCategory.FILTER to "滤镜"
    )

    Column(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(24.dp).clickable { onBack() })
                Spacer(Modifier.width(16.dp))
                Icon(Icons.Default.HelpOutline, null, tint = Color.White, modifier = Modifier.size(24.dp))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box {
                    Button(onClick = { showMenu = true }, colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)) {
                        Text("一键超清", color = Color.White)
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(text = { Text("原图") }, onClick = { showMenu = false })
                        DropdownMenuItem(text = { Text("降噪") }, onClick = { showMenu = false })
                        DropdownMenuItem(text = { Text("锐化") }, onClick = { showMenu = false })
                        DropdownMenuItem(text = { Text("增强") }, onClick = { showMenu = false })
                    }
                }
                Spacer(Modifier.width(12.dp))
                Button(onClick = {
                    if (isSaving || original == null || display == null) return@Button
                    isSaving = true
                    scope.launch {
                        val out = withContext(Dispatchers.IO) {
                            when (category) {
                                EditorCategory.PORTRAIT -> beauty.process(original!!, smooth, whiten, ruddy, sharpen)
                                EditorCategory.FILTER -> display
                                else -> display
                            }
                        }
                        if (out != null) {
                            val savedUri = saveBitmapToGalleryReturnUri(context, out)
                            if (savedUri != null) {
                                if (out != original) out.recycle()
                                if (onSaved != null) {
                                    onSaved(savedUri)
                                } else {
                                    onBack()
                                }
                            } else {
                                Toast.makeText(context, "保存失败", Toast.LENGTH_SHORT).show()
                                if (out != original) out.recycle()
                            }
                        } else {
                            Toast.makeText(context, "生成图片失败", Toast.LENGTH_SHORT).show()
                        }
                        isSaving = false
                    }
                }, colors = ButtonDefaults.buttonColors(containerColor = primary)) { Text("导出", color = Color.Black, fontWeight = FontWeight.Bold) }
            }
        }

        Box(
            modifier = Modifier.weight(1f).fillMaxWidth().background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            if (display != null) {
                Image(bitmap = display!!.asImageBitmap(), contentDescription = null, modifier = Modifier.fillMaxSize().pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val e = awaitPointerEvent()
                            isComparing = e.changes.any { it.pressed }
                        }
                    }
                }, contentScale = ContentScale.Fit)
            } else {
                if (!loaded) CircularProgressIndicator(color = primary)
            }
        }

        Column(modifier = Modifier.fillMaxWidth().background(Color(0xFF1A1A1A))) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(categories) { pair ->
                    val selected = category == pair.first
                    Text(
                        text = pair.second,
                        color = if (selected) primary else Color.Gray,
                        fontSize = 16.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.clickable {
                            category = pair.first
                        }
                    )
                }
            }

            val tools = when (category) {
                EditorCategory.PORTRAIT -> portraitTools
                EditorCategory.FILTER -> filterTools
                else -> emptyList()
            }
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(88.dp)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(tools) { tool ->
                    val selected = selectedTool == tool
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (selected) primary else Color(0xFF333333))
                            .clickable { selectedTool = tool }
                            .padding(horizontal = 18.dp, vertical = 12.dp)
                    ) {
                        Text(tool, color = if (selected) Color.Black else Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (category == EditorCategory.PORTRAIT && selectedTool == "自动美颜") {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("磨皮", color = Color.Gray, fontSize = 12.sp)
                        Text("${(smooth * 10).toInt()}%", color = Color.Gray, fontSize = 12.sp)
                    }
                    Text("平滑皮肤纹理，弱化毛孔/细纹", color = Color(0xFFAAAAAA), fontSize = 11.sp)
                    Slider(value = smooth, onValueChange = { smooth = it }, valueRange = 0f..10f, colors = SliderDefaults.colors(activeTrackColor = primary))

                    Spacer(Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("美白", color = Color.Gray, fontSize = 12.sp)
                        Text("${(whiten * 100).toInt()}%", color = Color.Gray, fontSize = 12.sp)
                    }
                    Text("提升整体亮度，让肤色更通透", color = Color(0xFFAAAAAA), fontSize = 11.sp)
                    Slider(value = whiten, onValueChange = { whiten = it }, valueRange = 0f..1f, colors = SliderDefaults.colors(activeTrackColor = primary))

                    Spacer(Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("红润", color = Color.Gray, fontSize = 12.sp)
                        Text("${(ruddy * 100).toInt()}%", color = Color.Gray, fontSize = 12.sp)
                    }
                    Text("增加饱和度与血色感，气色更好", color = Color(0xFFAAAAAA), fontSize = 11.sp)
                    Slider(value = ruddy, onValueChange = { ruddy = it }, valueRange = 0f..1f, colors = SliderDefaults.colors(activeTrackColor = primary))

                    Spacer(Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("清晰", color = Color.Gray, fontSize = 12.sp)
                        Text("${(sharpen * 25).toInt()}%", color = Color.Gray, fontSize = 12.sp)
                    }
                    Text("增强细节边缘，让画面更清晰", color = Color(0xFFAAAAAA), fontSize = 11.sp)
                    Slider(value = sharpen, onValueChange = { sharpen = it }, valueRange = 0f..4f, colors = SliderDefaults.colors(activeTrackColor = primary))
                }
            }
        }
    }
}
