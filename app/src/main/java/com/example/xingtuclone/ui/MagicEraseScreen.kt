package com.example.xingtuclone.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.BlurMaskFilter
import android.net.Uri
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.gestures.detectDragGestures
import kotlinx.coroutines.launch
import com.example.xingtuclone.inpaint.OnnxLamaInpainter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MagicEraseScreen(imageUri: Uri, onBack: () -> Unit, onSaved: (Uri) -> Unit) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    var src by remember { mutableStateOf<Bitmap?>(null) }
    var mask by remember { mutableStateOf<Bitmap?>(null) }
    var preview by remember { mutableStateOf<Bitmap?>(null) }
    var brushSize by remember { mutableStateOf(24f) }
    var feather by remember { mutableStateOf(12f) }
    var showMask by remember { mutableStateOf(false) }

    LaunchedEffect(imageUri) {
        val bmp = loadCompressedBitmap(context, imageUri, 1280)
        src = bmp
        if (bmp != null) {
            mask = Bitmap.createBitmap(bmp.width, bmp.height, Bitmap.Config.ARGB_8888)
            preview = bmp.copy(Bitmap.Config.ARGB_8888, true)
        }
    }

    fun updatePreview() {
        val s = src ?: return
        val m = mask ?: return
        val out = OnnxLamaInpainter.inpaint(context, s, m)
        preview = out
    }

    fun drawOnMask(x: Float, y: Float) {
        val m = mask ?: return
        val c = Canvas(m)
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            alpha = 255
            style = Paint.Style.FILL
            maskFilter = BlurMaskFilter(feather, BlurMaskFilter.Blur.NORMAL)
        }
        c.drawCircle(x, y, brushSize, p)
        updatePreview()
    }

    Column(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.ArrowBack, null, tint = Color.White, modifier = Modifier
                .padding(4.dp)
                .pointerInput(Unit) {}
                .background(Color.Transparent)
                .clickable { onBack() })
            Text("魔法消除", color = Color.White)
            Icon(Icons.Default.Check, null, tint = Color(0xFFCCFF00), modifier = Modifier
                .padding(4.dp)
                .pointerInput(Unit) {})
        }

        Box(modifier = Modifier.fillMaxWidth().weight(1f).background(Color.Black), contentAlignment = Alignment.Center) {
            val p = preview
            val s = src
            val m = mask
            if (p != null) {
                val displayBmp = if (showMask && m != null) overlayMaskOnPreview(p, m) else p
                Image(
                    bitmap = displayBmp.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .size(with(density) { p.width.toDp() }, with(density) { p.height.toDp() })
                        .pointerInput(showMask, brushSize, feather) {
                            detectDragGestures(
                                onDragStart = { offset -> drawOnMask(offset.x, offset.y) },
                                onDrag = { change, _ ->
                                    drawOnMask(change.position.x, change.position.y)
                                    change.consume()
                                }
                            )
                        }
                )
            }
        }

        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("画笔大小", color = Color.White, modifier = Modifier.width(80.dp))
                Slider(value = brushSize, onValueChange = { brushSize = it }, valueRange = 4f..64f)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("羽化", color = Color.White, modifier = Modifier.width(80.dp))
                Slider(value = feather, onValueChange = { feather = it }, valueRange = 0f..32f)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                FilterChip(selected = showMask, onClick = { showMask = !showMask }, label = { Text(if (showMask) "隐藏蒙版" else "显示蒙版") })
                Button(onClick = {
                    val s = src
                    val m = mask
                    if (s != null && m != null) {
                        val out = OnnxLamaInpainter.inpaint(context, s, m)
                        if (out != null) {
                            scope.launch {
                                val saved = saveBitmapToGalleryReturnUri(context, out)
                                if (saved != null) onSaved(saved)
                            }
                        }
                    }
                }) { Text("保存") }
            }
        }
    }
}

private fun overlayMaskOnPreview(preview: Bitmap, mask: Bitmap): Bitmap {
    val out = preview.copy(Bitmap.Config.ARGB_8888, true)
    val c = Canvas(out)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.RED
        alpha = 96
        style = Paint.Style.FILL
    }
    val w = mask.width
    val h = mask.height
    val px = IntArray(w * h)
    mask.getPixels(px, 0, w, 0, 0, w, h)
    val overlay = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val outPx = IntArray(w * h)
    for (i in px.indices) {
        val a = (px[i] ushr 24) and 0xFF
        outPx[i] = (a shl 24) or (0xFF shl 16) // red channel
    }
    overlay.setPixels(outPx, 0, w, 0, 0, w, h)
    c.drawBitmap(overlay, 0f, 0f, paint)
    return out
}


