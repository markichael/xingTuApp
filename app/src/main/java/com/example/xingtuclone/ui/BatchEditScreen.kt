package com.example.xingtuclone.ui

import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.xingtuclone.utils.BeautyProcessor
import com.example.xingtuclone.utils.ColorMatrixUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatchEditScreen(imageUris: List<Uri>, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val primary = Color(0xFFCCFF00)

    var mode by remember { mutableStateOf("自动美颜") } // 或 "柔和滤镜"
    var progress by remember { mutableStateOf(0) }
    var isRunning by remember { mutableStateOf(false) }
    var results by remember { mutableStateOf<List<Uri>>(emptyList()) }

    val previewBitmaps = remember { mutableStateListOf<Bitmap?>(*arrayOfNulls<Bitmap>(imageUris.size)) }

    LaunchedEffect(imageUris) {
        withContext(Dispatchers.IO) {
            imageUris.forEachIndexed { i, uri ->
                val bmp = loadCompressedBitmap(context, uri, 1080)
                withContext(Dispatchers.Main) { previewBitmaps[i] = bmp }
            }
        }
    }

    fun processOne(input: Bitmap): Bitmap? {
        return when (mode) {
            "自动美颜" -> {
                val p = BeautyProcessor(context)
                val out = p.process(input, 4f, 0.3f, 0.3f, 1.0f)
                p.destroy()
                out
            }
            else -> {
                val mat = ColorMatrixUtils.saturationMatrix(0.85f)
                ColorMatrixUtils.applyColorMatrixRS(context, input, mat)
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.ArrowBack, null, tint = Color.White, modifier = Modifier.clickable { onBack() })
            Text("批量修图", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Icon(Icons.Default.Check, null, tint = if (isRunning) Color.Gray else primary, modifier = Modifier.clickable(enabled = !isRunning) {
                if (isRunning) return@clickable
                isRunning = true
                results = emptyList()
                progress = 0
                scope.launch {
                    withContext(Dispatchers.IO) {
                        imageUris.forEachIndexed { idx, uri ->
                            val full = loadCompressedBitmap(context, uri, 2560)
                            val out = if (full != null) processOne(full) else null
                            if (out != null) {
                                val saved = saveBitmapToGalleryReturnUri(context, out)
                                withContext(Dispatchers.Main) {
                                    results = results + listOfNotNull(saved)
                                    progress = idx + 1
                                }
                                if (out != full) out.recycle()
                            }
                        }
                    }
                    isRunning = false
                    Toast.makeText(context, "完成 ${results.size}/${imageUris.size}", Toast.LENGTH_SHORT).show()
                    onBack()
                }
            })
        }

        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FilterChip(selected = mode == "自动美颜", onClick = { mode = "自动美颜" }, label = { Text("自动美颜") })
            FilterChip(selected = mode == "柔和滤镜", onClick = { mode = "柔和滤镜" }, label = { Text("柔和滤镜") })
        }

        LinearProgressIndicator(progress = progress / imageUris.size.toFloat(), modifier = Modifier.fillMaxWidth().padding(16.dp), color = primary)

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            itemsIndexed(imageUris) { index, _ ->
                val bmp = previewBitmaps.getOrNull(index)
                Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(64.dp).background(Color.DarkGray), contentAlignment = Alignment.Center) {
                        if (bmp != null) Image(bitmap = bmp.asImageBitmap(), contentDescription = null)
                    }
                    Spacer(Modifier.width(12.dp))
                    Text("图片 ${index + 1}", color = Color.White)
                    Spacer(Modifier.weight(1f))
                    val done = index < progress
                    Text(if (done) "已完成" else "待处理", color = if (done) primary else Color.Gray)
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Button(
            onClick = {
                if (isRunning) return@Button
                isRunning = true
                results = emptyList()
                progress = 0
                scope.launch {
                    withContext(Dispatchers.IO) {
                        imageUris.forEachIndexed { idx, uri ->
                            val full = loadCompressedBitmap(context, uri, 2560)
                            val out = if (full != null) processOne(full) else null
                            if (out != null) {
                                val saved = saveBitmapToGalleryReturnUri(context, out)
                                withContext(Dispatchers.Main) {
                                    results = results + listOfNotNull(saved)
                                    progress = idx + 1
                                }
                                if (out != full) out.recycle()
                            }
                        }
                    }
                    isRunning = false
                    Toast.makeText(context, "已导出 ${results.size}/${imageUris.size}", Toast.LENGTH_SHORT).show()
                    onBack()
                }
            },
            enabled = !isRunning,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFCCFF00), contentColor = Color.Black),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Text("导出全部", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

