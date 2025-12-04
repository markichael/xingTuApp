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
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.border
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.RotateLeft
import androidx.compose.material.icons.filled.RotateRight
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.yalantis.ucrop.UCrop
import java.io.File
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
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.graphicsLayer
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.xingtuclone.utils.CrashLogger
import com.yalantis.ucrop.UCropActivity
import com.yalantis.ucrop.model.AspectRatio
 
import kotlin.math.min
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
    var cropAutoLaunched by remember { mutableStateOf(false) }

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
    var filterStrength by remember { mutableFloatStateOf(1.0f) }
    var brushSize by remember { mutableFloatStateOf(16f) }
    var blurStrength by remember { mutableFloatStateOf(12f) }
    var stickerScale by remember { mutableFloatStateOf(1.0f) }
    var stickerEmoji by remember { mutableStateOf("✨") }
    var doodleColor by remember { mutableStateOf(Color.Black) }
    var stickerPosX by remember { mutableFloatStateOf(0.5f) }
    var stickerPosY by remember { mutableFloatStateOf(0.5f) }

    val adjustOps = remember { mutableStateListOf<String>() }
    val redoOps = remember { mutableStateListOf<String>() }
    

 
    var doodleOverlay by remember { mutableStateOf<Bitmap?>(null) }
    var blurMask by remember { mutableStateOf<Bitmap?>(null) }
    var overlayTick by remember { mutableIntStateOf(0) }
    val doodleStrokes = remember { mutableStateListOf<StrokeSegment>() }
    var freeCropRect by remember { mutableStateOf<android.graphics.RectF?>(null) }
    var freeCropTempRect by remember { mutableStateOf<android.graphics.RectF?>(null) }

    val uCropLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        try {
            val data = result.data
            CrashLogger.log(context, "Crop", "uCrop onResult code=${result.resultCode} data=${data != null}")
            if (result.resultCode == android.app.Activity.RESULT_OK && data != null) {
                val out = UCrop.getOutput(data)
                CrashLogger.log(context, "Crop", "uCrop output=$out")
                if (out != null) {
                    scope.launch {
                        val full = withContext(Dispatchers.IO) { loadCompressedBitmap(context, out, 2560) }
                        if (full != null) {
                            val s = 1080f / kotlin.math.max(full.width, full.height)
                            val prev = if (s < 1f) Bitmap.createScaledBitmap(full, (full.width * s).toInt(), (full.height * s).toInt(), true) else full.copy(full.config, true)
                            original = full
                            preview = prev
                            display = prev
                            adjustOps.clear()
                            redoOps.clear()
                            doodleOverlay = null
                            blurMask = null
                            doodleStrokes.clear()
                            category = EditorCategory.FILTER
                            Toast.makeText(context, "已裁剪", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } else if (data != null) {
                val err = UCrop.getError(data)
                if (err != null) CrashLogger.error(context, "Crop", "uCrop error: ${err.message}", err)
                Toast.makeText(context, "裁剪失败", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            CrashLogger.error(context, "Crop", "uCrop onResult crashed", e)
            Toast.makeText(context, "裁剪结果处理异常", Toast.LENGTH_SHORT).show()
        }
    }
    var cropAspect by remember { mutableStateOf<Float?>(null) }

    LaunchedEffect(category, selectedTool, smooth, whiten, ruddy, sharpen, filterStrength, loaded, isComparing, brushSize, blurStrength, stickerScale, stickerEmoji, stickerPosX, stickerPosY, adjustOps, doodleOverlay, blurMask, freeCropRect, overlayTick) {
        if (!loaded || preview == null) return@LaunchedEffect
        if (isComparing) { display = preview; return@LaunchedEffect }
    val base = withContext(Dispatchers.IO) { applyAdjustOps(preview!!, adjustOps) }
        when (category) {
            EditorCategory.PORTRAIT -> {
                val bmp = withContext(Dispatchers.IO) {
                    beauty.process(base, smooth, whiten, ruddy, sharpen)
                }
                if (bmp != null) display = bmp
            }
            EditorCategory.FILTER -> {
                val target = when (selectedTool) {
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
                val matrix = blendMatrix(android.renderscript.Matrix4f(), target, filterStrength)
                val bmp = withContext(Dispatchers.IO) { applyColorMatrixRS(context, base, matrix) }
                if (bmp != null) display = bmp
            }
            EditorCategory.EFFECT -> {
                if (selectedTool == "涂鸦") {
                    val out = withContext(Dispatchers.IO) { overlayBitmap(base, doodleOverlay) }
                    if (out != null) display = out else display = base
                } else if (selectedTool == "虚化") {
                    val out = withContext(Dispatchers.IO) { applyBlurMaskBlend(context, base, blurMask, blurStrength) }
                    if (out != null) display = out else display = base
                } else {
                    display = base
                }
            }
            EditorCategory.STICKER -> {
                display = base
            }
            else -> { display = preview }
        }
    }

    val portraitTools = listOf("自动美颜")
    val filterTools = listOf("原图", "暖色", "冷色", "黑白", "复古", "增饱和", "降饱和", "柔和", "青橙", "粉调", "绿野")
    val categories = listOf(
        EditorCategory.PORTRAIT to "人像",
        EditorCategory.FILTER to "滤镜",
        EditorCategory.ADJUST to "调整",
        EditorCategory.EFFECT to "特效"
    )

    Column(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        LaunchedEffect(category) {
            if (category == EditorCategory.ADJUST && !cropAutoLaunched) {
                val src = imageUris.firstOrNull()
                try {
                    if (src != null) {
                        val outFile = File(context.externalCacheDir ?: context.cacheDir, "ucrop_${System.currentTimeMillis()}.jpg")
                        val dst = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", outFile)
                        val options = UCrop.Options().apply {
                            setToolbarColor(0xFF000000.toInt())
                            setStatusBarColor(0xFF000000.toInt())
                            setToolbarWidgetColor(0xFFCCFF00.toInt())
                            setActiveControlsWidgetColor(0xFFCCFF00.toInt())
                            setToolbarTitle("裁剪旋转")
                            setCropFrameColor(0xFFFFFFFF.toInt())
                            setCropGridColor(0x55FFFFFF)
                            setCropFrameStrokeWidth(4)
                            setCropGridStrokeWidth(2)
                            setDimmedLayerColor(0xFF000000.toInt())
                            setFreeStyleCropEnabled(true)
                            setHideBottomControls(false)
                            setAllowedGestures(UCropActivity.SCALE, UCropActivity.ROTATE, UCropActivity.SCALE)
                            setAspectRatioOptions(
                                0,
                                AspectRatio("原始比例", 0f, 0f),
                                AspectRatio("1:1", 1f, 1f),
                                AspectRatio("3:4", 3f, 4f),
                                AspectRatio("3:2", 3f, 2f),
                                AspectRatio("16:9", 16f, 9f)
                            )
                        }
                        val intent = UCrop.of(src, dst).withOptions(options).getIntent(context).apply {
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                        }
                        uCropLauncher.launch(intent)
                        cropAutoLaunched = true
                    } else {
                        Toast.makeText(context, "未选择图片", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    CrashLogger.error(context, "Crop", "auto launch uCrop failed", e)
                    Toast.makeText(context, "裁剪功能启动失败", Toast.LENGTH_SHORT).show()
                }
            }
        }
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
                            val baseOrig = applyAdjustOps(original!!, adjustOps)
                            when (category) {
                                EditorCategory.PORTRAIT -> beauty.process(baseOrig, smooth, whiten, ruddy, sharpen)
                                EditorCategory.FILTER -> {
                                    val t = when (selectedTool) {
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
                                    val m = blendMatrix(android.renderscript.Matrix4f(), t, filterStrength)
                                    applyColorMatrixRS(context, baseOrig, m)
                                }
                                EditorCategory.EFFECT -> {
                                    if (selectedTool == "涂鸦") {
                                        val overlay = doodleOverlay?.let { Bitmap.createScaledBitmap(it, baseOrig.width, baseOrig.height, true) }
                                        overlayBitmap(baseOrig, overlay)
                                    } else if (selectedTool == "虚化") {
                                        val mask = blurMask?.let { Bitmap.createScaledBitmap(it, baseOrig.width, baseOrig.height, true) }
                                        applyBlurMaskBlend(context, baseOrig, mask, blurStrength)
                                    } else baseOrig
                                }
                                else -> baseOrig
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
                        freeCropRect = null
                        freeCropTempRect = null
                    }
                }, colors = ButtonDefaults.buttonColors(containerColor = primary)) { Text("导出", color = Color.Black, fontWeight = FontWeight.Bold) }
            }
        }

        var imageBoxSize by remember { mutableStateOf(IntSize.Zero) }
        Box(
            modifier = Modifier.weight(1f).fillMaxWidth().background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            if (display != null) {
                val compareModifier = if (category == EditorCategory.PORTRAIT || category == EditorCategory.FILTER) {
                    Modifier.pointerInput(category) {
                        awaitPointerEventScope {
                            while (true) {
                                val e = awaitPointerEvent()
                                isComparing = e.changes.any { it.pressed }
                            }
                        }
                    }
                } else Modifier
                Image(bitmap = display!!.asImageBitmap(), contentDescription = null, modifier = Modifier
                    .fillMaxSize()
                    .onSizeChanged { imageBoxSize = it }
                    .then(compareModifier)
                .pointerInput(category, selectedTool, brushSize, doodleColor, stickerPosX, stickerPosY, stickerScale, cropAspect, freeCropRect, freeCropTempRect) {
                    if (category == EditorCategory.EFFECT && (selectedTool == "涂鸦" || selectedTool == "虚化")) {
                        var prevX: Float? = null
                        var prevY: Float? = null
                        detectDragGestures(
                            onDragStart = { pos ->
                                val disp = display
                                if (disp != null && imageBoxSize.width > 0 && imageBoxSize.height > 0) {
                                    val scale = min(imageBoxSize.width.toFloat() / disp.width, imageBoxSize.height.toFloat() / disp.height)
                                    val offX = (imageBoxSize.width - disp.width * scale) / 2f
                                    val offY = (imageBoxSize.height - disp.height * scale) / 2f
                                    val bx = ((pos.x - offX) / scale)
                                    val by = ((pos.y - offY) / scale)
                                    if (bx in 0f..disp.width.toFloat() && by in 0f..disp.height.toFloat()) {
                                        prevX = bx
                                        prevY = by
                                        if (selectedTool == "涂鸦") {
                                            if (doodleOverlay == null) doodleOverlay = Bitmap.createBitmap(disp.width, disp.height, Bitmap.Config.ARGB_8888)
                                            val c = android.graphics.Canvas(doodleOverlay!!)
                                            val p = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
                                            val ac = android.graphics.Color.argb(
                                                (doodleColor.alpha * 255).toInt(),
                                                (doodleColor.red * 255).toInt(),
                                                (doodleColor.green * 255).toInt(),
                                                (doodleColor.blue * 255).toInt()
                                            )
                                            p.color = ac
                                            p.style = android.graphics.Paint.Style.FILL
                                            c.drawCircle(bx, by, brushSize, p)
                                            doodleStrokes.add(StrokeSegment(bx, by, bx, by, ac, brushSize * 2f, true))
                                            overlayTick++
                                        } else {
                                            if (blurMask == null) blurMask = Bitmap.createBitmap(disp.width, disp.height, Bitmap.Config.ARGB_8888)
                                            val c = android.graphics.Canvas(blurMask!!)
                                            val p = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
                                            p.color = android.graphics.Color.WHITE
                                            p.alpha = 255
                                            p.style = android.graphics.Paint.Style.FILL
                                            c.drawCircle(bx, by, brushSize, p)
                                            overlayTick++
                                        }
                                    }
                                }
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                val pos = change.position
                                val disp = display
                                if (disp != null && imageBoxSize.width > 0 && imageBoxSize.height > 0) {
                                    val scale = min(imageBoxSize.width.toFloat() / disp.width, imageBoxSize.height.toFloat() / disp.height)
                                    val offX = (imageBoxSize.width - disp.width * scale) / 2f
                                    val offY = (imageBoxSize.height - disp.height * scale) / 2f
                                    val bx = ((pos.x - offX) / scale)
                                    val by = ((pos.y - offY) / scale)
                                    if (bx in 0f..disp.width.toFloat() && by in 0f..disp.height.toFloat()) {
                                        if (selectedTool == "涂鸦") {
                                            if (doodleOverlay == null) doodleOverlay = Bitmap.createBitmap(disp.width, disp.height, Bitmap.Config.ARGB_8888)
                                            val c = android.graphics.Canvas(doodleOverlay!!)
                                            val p = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
                                            val ac = android.graphics.Color.argb(
                                                (doodleColor.alpha * 255).toInt(),
                                                (doodleColor.red * 255).toInt(),
                                                (doodleColor.green * 255).toInt(),
                                                (doodleColor.blue * 255).toInt()
                                            )
                                            p.color = ac
                                            p.style = android.graphics.Paint.Style.STROKE
                                            p.strokeWidth = brushSize * 2f
                                            p.strokeCap = android.graphics.Paint.Cap.ROUND
                                            p.strokeJoin = android.graphics.Paint.Join.ROUND
                                            val sx = prevX ?: bx
                                            val sy = prevY ?: by
                                            c.drawLine(sx, sy, bx, by, p)
                                            doodleStrokes.add(StrokeSegment(sx, sy, bx, by, ac, brushSize * 2f, false))
                                            prevX = bx
                                            prevY = by
                                            overlayTick++
                                        } else {
                                            if (blurMask == null) blurMask = Bitmap.createBitmap(disp.width, disp.height, Bitmap.Config.ARGB_8888)
                                            val c = android.graphics.Canvas(blurMask!!)
                                            val p = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
                                            p.color = android.graphics.Color.WHITE
                                            p.alpha = 255
                                            p.style = android.graphics.Paint.Style.STROKE
                                            p.strokeWidth = brushSize * 2f
                                            p.strokeCap = android.graphics.Paint.Cap.ROUND
                                            p.strokeJoin = android.graphics.Paint.Join.ROUND
                                            val sx = prevX ?: bx
                                            val sy = prevY ?: by
                                            c.drawLine(sx, sy, bx, by, p)
                                            prevX = bx
                                            prevY = by
                                            overlayTick++
                                        }
                                    }
                                }
                            },
                            onDragEnd = {
                                prevX = null
                                prevY = null
                            }
                        )
                    } else if (false) {
                        var sx: Float? = null
                        var sy: Float? = null
                        var mode: String? = null
                        var mx: Float = 0f
                        var my: Float = 0f
                        detectDragGestures(
                            onDragStart = { pos ->
                                val disp = display
                                if (disp != null && imageBoxSize.width > 0 && imageBoxSize.height > 0) {
                                    val scale = min(imageBoxSize.width.toFloat() / disp.width, imageBoxSize.height.toFloat() / disp.height)
                                    val offX = (imageBoxSize.width - disp.width * scale) / 2f
                                    val offY = (imageBoxSize.height - disp.height * scale) / 2f
                                    val bx = ((pos.x - offX) / scale)
                                    val by = ((pos.y - offY) / scale)
                                    if (bx in 0f..disp.width.toFloat() && by in 0f..disp.height.toFloat()) {
                                        val r0 = freeCropRect?.let { android.graphics.RectF(it.left*disp.width, it.top*disp.height, it.right*disp.width, it.bottom*disp.height) }
                                        if (r0 != null) {
                                            val th = 24f
                                            val nearTL = kotlin.math.abs(bx - r0.left) < th && kotlin.math.abs(by - r0.top) < th
                                            val nearTR = kotlin.math.abs(bx - r0.right) < th && kotlin.math.abs(by - r0.top) < th
                                            val nearBL = kotlin.math.abs(bx - r0.left) < th && kotlin.math.abs(by - r0.bottom) < th
                                            val nearBR = kotlin.math.abs(bx - r0.right) < th && kotlin.math.abs(by - r0.bottom) < th
                                            val nearT = kotlin.math.abs(by - r0.top) < th && bx >= r0.left && bx <= r0.right
                                            val nearB = kotlin.math.abs(by - r0.bottom) < th && bx >= r0.left && bx <= r0.right
                                            val nearL = kotlin.math.abs(bx - r0.left) < th && by >= r0.top && by <= r0.bottom
                                            val nearR = kotlin.math.abs(bx - r0.right) < th && by >= r0.top && by <= r0.bottom
                                            mode = when {
                                                nearTL -> "TL"
                                                nearTR -> "TR"
                                                nearBL -> "BL"
                                                nearBR -> "BR"
                                                nearT -> "T"
                                                nearB -> "B"
                                                nearL -> "L"
                                                nearR -> "R"
                                                bx >= r0.left && bx <= r0.right && by >= r0.top && by <= r0.bottom -> {
                                                    mx = bx - r0.left; my = by - r0.top; "MOVE"
                                                }
                                                else -> null
                                            }
                                        }
                                        if (mode == null) { sx = bx; sy = by; freeCropTempRect = android.graphics.RectF(bx, by, bx, by) } else { freeCropTempRect = freeCropRect?.let { android.graphics.RectF(it.left*disp.width, it.top*disp.height, it.right*disp.width, it.bottom*disp.height) } }
                                        
                                    }
                                }
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                val pos = change.position
                                val disp = display
                                if (disp != null && imageBoxSize.width > 0 && imageBoxSize.height > 0) {
                                    val scale = min(imageBoxSize.width.toFloat() / disp.width, imageBoxSize.height.toFloat() / disp.height)
                                    val offX = (imageBoxSize.width - disp.width * scale) / 2f
                                    val offY = (imageBoxSize.height - disp.height * scale) / 2f
                                    val bx = ((pos.x - offX) / scale)
                                    val by = ((pos.y - offY) / scale)
                                    val ax = cropAspect
                                    val cur = freeCropTempRect
                                    if (mode == "MOVE" && cur != null) {
                                        val w = cur.width()
                                        val h = cur.height()
                                        var left = (bx - mx).coerceIn(0f, disp.width - w)
                                        var top = (by - my).coerceIn(0f, disp.height - h)
                                        freeCropTempRect = android.graphics.RectF(left, top, left + w, top + h)
                                    } else if (mode != null && cur != null) {
                                        var left = cur.left
                                        var top = cur.top
                                        var right = cur.right
                                        var bottom = cur.bottom
                                        when (mode) {
                                            "TL" -> { left = bx; top = by }
                                            "TR" -> { right = bx; top = by }
                                            "BL" -> { left = bx; bottom = by }
                                            "BR" -> { right = bx; bottom = by }
                                            "T" -> { top = by }
                                            "B" -> { bottom = by }
                                            "L" -> { left = bx }
                                            "R" -> { right = bx }
                                        }
                                        if (ax != null) {
                                            val cx = (left + right) / 2f
                                            val cy = (top + bottom) / 2f
                                            var w = kotlin.math.abs(right - left)
                                            var h = kotlin.math.abs(bottom - top)
                                            if (mode == "T" || mode == "B") {
                                                w = h * ax
                                                left = cx - w / 2f
                                                right = cx + w / 2f
                                            } else if (mode == "L" || mode == "R") {
                                                h = w / ax
                                                top = cy - h / 2f
                                                bottom = cy + h / 2f
                                            } else {
                                                val targetH = w / ax
                                                if (targetH > h) {
                                                    bottom = if (mode == "BL" || mode == "BR") top + targetH else top - targetH
                                                } else {
                                                    val targetW = h * ax
                                                    right = if (mode == "TR" || mode == "BR") left + targetW else left - targetW
                                                }
                                            }
                                        }
                                        freeCropTempRect = android.graphics.RectF(kotlin.math.min(left, right), kotlin.math.min(top, bottom), kotlin.math.max(left, right), kotlin.math.max(top, bottom))
                                    } else {
                                        val x0 = sx ?: bx
                                        val y0 = sy ?: by
                                        if (ax == null) {
                                            freeCropTempRect = android.graphics.RectF(
                                                kotlin.math.min(x0, bx), kotlin.math.min(y0, by),
                                                kotlin.math.max(x0, bx), kotlin.math.max(y0, by)
                                            )
                                        } else {
                                            val dx = bx - x0
                                            val dy = by - y0
                                            var w = kotlin.math.abs(dx)
                                            var h = kotlin.math.abs(dy)
                                            val targetH = w / ax
                                            if (targetH > h) h = targetH else w = h * ax
                                            val rx = if (dx >= 0) x0 else x0 - w
                                            val ry = if (dy >= 0) y0 else y0 - h
                                            freeCropTempRect = android.graphics.RectF(rx, ry, rx + w, ry + h)
                                        }
                                    }
                                    
                                }
                            },
                            onDragEnd = {
                                val disp = display
                                val r = freeCropTempRect
                                if (disp != null && r != null) {
                                    val snap = 12f
                                    var left = r.left
                                    var right = r.right
                                    var top = r.top
                                    var bottom = r.bottom
                                    if (left < snap) left = 0f
                                    if (top < snap) top = 0f
                                    if (disp.width - right < snap) right = disp.width.toFloat()
                                    if (disp.height - bottom < snap) bottom = disp.height.toFloat()
                                    val norm = android.graphics.RectF(
                                        (left / disp.width).coerceIn(0f,1f),
                                        (top / disp.height).coerceIn(0f,1f),
                                        (right / disp.width).coerceIn(0f,1f),
                                        (bottom / disp.height).coerceIn(0f,1f)
                                    )
                                    freeCropRect = norm
                                    freeCropTempRect = null
                                    
                                }
                            }
                        )
                        if (false) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            val disp = display
                            val cur = freeCropTempRect ?: freeCropRect?.let { android.graphics.RectF(it.left*(disp?.width?:1), it.top*(disp?.height?:1), it.right*(disp?.width?:1), it.bottom*(disp?.height?:1)) }
                            if (disp != null && cur != null) {
                                val cx = (cur.left + cur.right) / 2f
                                val cy = (cur.top + cur.bottom) / 2f
                                var left = cx + (cur.left - cx) * zoom + pan.x
                                var right = cx + (cur.right - cx) * zoom + pan.x
                                var top = cy + (cur.top - cy) * zoom + pan.y
                                var bottom = cy + (cur.bottom - cy) * zoom + pan.y
                                val minSize = 32f
                                if (kotlin.math.abs(right - left) < minSize) { val adjust = (minSize - kotlin.math.abs(right - left))/2f; left -= adjust; right += adjust }
                                if (kotlin.math.abs(bottom - top) < minSize) { val adjust = (minSize - kotlin.math.abs(bottom - top))/2f; top -= adjust; bottom += adjust }
                                left = left.coerceIn(0f, disp.width.toFloat())
                                right = right.coerceIn(0f, disp.width.toFloat())
                                top = top.coerceIn(0f, disp.height.toFloat())
                                bottom = bottom.coerceIn(0f, disp.height.toFloat())
                                freeCropTempRect = android.graphics.RectF(kotlin.math.min(left,right), kotlin.math.min(top,bottom), kotlin.math.max(left,right), kotlin.math.max(top,bottom))
                                overlayTick++
                            }
                        }
                        }
                    }
                }
                , contentScale = ContentScale.Fit)
                
                if (false) {
                    val disp = display!!
                    val scale = min(imageBoxSize.width.toFloat() / disp.width, imageBoxSize.height.toFloat() / disp.height)
                    val offX = (imageBoxSize.width - disp.width * scale) / 2f
                    val offY = (imageBoxSize.height - disp.height * scale) / 2f
                    val r = freeCropTempRect ?: freeCropRect?.let { android.graphics.RectF(it.left*disp.width, it.top*disp.height, it.right*disp.width, it.bottom*disp.height) }
                    if (r != null) {
                        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                            val left = offX + r.left * scale
                            val top = offY + r.top * scale
                            val right = offX + r.right * scale
                            val bottom = offY + r.bottom * scale
                            val w = right - left
                            val h = bottom - top
                            drawRect(color = Color(0x66000000))
                            drawRect(color = Color(0x00000000), topLeft = androidx.compose.ui.geometry.Offset(left, top), size = androidx.compose.ui.geometry.Size(w, h), blendMode = androidx.compose.ui.graphics.BlendMode.Clear)
                            drawRect(color = Color.White, topLeft = androidx.compose.ui.geometry.Offset(left, top), size = androidx.compose.ui.geometry.Size(w, h), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f))
                            val handleSize = 8f
                            drawRect(color = Color.White, topLeft = androidx.compose.ui.geometry.Offset(left - handleSize, top - handleSize), size = androidx.compose.ui.geometry.Size(handleSize*2, handleSize*2))
                            drawRect(color = Color.White, topLeft = androidx.compose.ui.geometry.Offset(right - handleSize, top - handleSize), size = androidx.compose.ui.geometry.Size(handleSize*2, handleSize*2))
                            drawRect(color = Color.White, topLeft = androidx.compose.ui.geometry.Offset(left - handleSize, bottom - handleSize), size = androidx.compose.ui.geometry.Size(handleSize*2, handleSize*2))
                            drawRect(color = Color.White, topLeft = androidx.compose.ui.geometry.Offset(right - handleSize, bottom - handleSize), size = androidx.compose.ui.geometry.Size(handleSize*2, handleSize*2))
                            drawRect(color = Color.White, topLeft = androidx.compose.ui.geometry.Offset((left+right)/2f - handleSize, top - handleSize), size = androidx.compose.ui.geometry.Size(handleSize*2, handleSize*2))
                            drawRect(color = Color.White, topLeft = androidx.compose.ui.geometry.Offset((left+right)/2f - handleSize, bottom - handleSize), size = androidx.compose.ui.geometry.Size(handleSize*2, handleSize*2))
                            drawRect(color = Color.White, topLeft = androidx.compose.ui.geometry.Offset(left - handleSize, (top+bottom)/2f - handleSize), size = androidx.compose.ui.geometry.Size(handleSize*2, handleSize*2))
                            drawRect(color = Color.White, topLeft = androidx.compose.ui.geometry.Offset(right - handleSize, (top+bottom)/2f - handleSize), size = androidx.compose.ui.geometry.Size(handleSize*2, handleSize*2))
                            val oneThirdX1 = left + w/3f
                            val oneThirdX2 = left + 2f*w/3f
                            val oneThirdY1 = top + h/3f
                            val oneThirdY2 = top + 2f*h/3f
                            drawLine(color = Color(0x55FFFFFF), start = androidx.compose.ui.geometry.Offset(oneThirdX1, top), end = androidx.compose.ui.geometry.Offset(oneThirdX1, bottom), strokeWidth = 1f)
                            drawLine(color = Color(0x55FFFFFF), start = androidx.compose.ui.geometry.Offset(oneThirdX2, top), end = androidx.compose.ui.geometry.Offset(oneThirdX2, bottom), strokeWidth = 1f)
                            drawLine(color = Color(0x55FFFFFF), start = androidx.compose.ui.geometry.Offset(left, oneThirdY1), end = androidx.compose.ui.geometry.Offset(right, oneThirdY1), strokeWidth = 1f)
                            drawLine(color = Color(0x55FFFFFF), start = androidx.compose.ui.geometry.Offset(left, oneThirdY2), end = androidx.compose.ui.geometry.Offset(right, oneThirdY2), strokeWidth = 1f)
                        }
                    }
                }
                if (category == EditorCategory.STICKER) {
                    val disp = display!!
                    val scale = min(imageBoxSize.width.toFloat() / disp.width, imageBoxSize.height.toFloat() / disp.height)
                    val offX = (imageBoxSize.width - disp.width * scale) / 2f
                    val offY = (imageBoxSize.height - disp.height * scale) / 2f
                    val px = offX + (stickerPosX * disp.width) * scale
                    val py = offY + (stickerPosY * disp.height) * scale
                    val approxHalf = 24f * stickerScale * scale
                    Box(modifier = Modifier.fillMaxSize()) {
                        Text(
                            text = stickerEmoji,
                            fontSize = (32f * stickerScale).sp,
                            color = Color.White,
                            modifier = Modifier.offset { IntOffset((px - approxHalf).toInt(), (py - approxHalf).toInt()) }
                        )
                    }
                }
            } else {
                if (!loaded) CircularProgressIndicator(color = primary)
            }
        }

        Column(modifier = Modifier.fillMaxWidth().background(Color(0xFF1A1A1A))) {
            if (category != EditorCategory.ADJUST) {
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
            } else {
                Box(modifier = Modifier.fillMaxWidth().height(64.dp), contentAlignment = Alignment.Center) {
                    Text("裁切旋转", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }

            val effectTools = listOf("涂鸦", "虚化")
            if (category == EditorCategory.PORTRAIT || category == EditorCategory.FILTER || category == EditorCategory.EFFECT) {
                val tools = when (category) {
                    EditorCategory.PORTRAIT -> portraitTools
                    EditorCategory.FILTER -> filterTools
                    EditorCategory.EFFECT -> effectTools
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

            if (category == EditorCategory.ADJUST) {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        Button(onClick = {
                            val src = imageUris.firstOrNull()
                            try {
                                if (src != null) {
                                    val outFile = java.io.File(context.externalCacheDir ?: context.cacheDir, "ucrop_${System.currentTimeMillis()}.jpg")
                                    val dst = androidx.core.content.FileProvider.getUriForFile(context, context.packageName + ".fileprovider", outFile)
                                    val options = com.yalantis.ucrop.UCrop.Options().apply {
                                        setToolbarColor(0xFF000000.toInt())
                                        setStatusBarColor(0xFF000000.toInt())
                                        setToolbarWidgetColor(0xFFCCFF00.toInt())
                                        setActiveControlsWidgetColor(0xFFCCFF00.toInt())
                                        setToolbarTitle("裁剪旋转")
                                        setCropFrameColor(0xFFFFFFFF.toInt())
                                        setCropGridColor(0x55FFFFFF)
                                        setCropFrameStrokeWidth(4)
                                        setCropGridStrokeWidth(2)
                                        setDimmedLayerColor(0xFF000000.toInt())
                                        setFreeStyleCropEnabled(true)
                                        setHideBottomControls(false)
                                        setAllowedGestures(com.yalantis.ucrop.UCropActivity.SCALE, com.yalantis.ucrop.UCropActivity.ROTATE, com.yalantis.ucrop.UCropActivity.SCALE)
                                        setAspectRatioOptions(
                                            0,
                                            com.yalantis.ucrop.model.AspectRatio("原始比例", 0f, 0f),
                                            com.yalantis.ucrop.model.AspectRatio("1:1", 1f, 1f),
                                            com.yalantis.ucrop.model.AspectRatio("3:4", 3f, 4f),
                                            com.yalantis.ucrop.model.AspectRatio("3:2", 3f, 2f),
                                            com.yalantis.ucrop.model.AspectRatio("16:9", 16f, 9f)
                                        )
                                    }
                                    val intent = com.yalantis.ucrop.UCrop.of(src, dst).withOptions(options).getIntent(context).apply {
                                        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                                    }
                                    uCropLauncher.launch(intent)
                                } else {
                                    android.widget.Toast.makeText(context, "未选择图片", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                com.example.xingtuclone.utils.CrashLogger.error(context, "Crop", "launch uCrop failed", e)
                                android.widget.Toast.makeText(context, "裁剪功能启动失败", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }, colors = ButtonDefaults.buttonColors(containerColor = Color.Black, contentColor = Color.White)) { Text("裁剪旋转") }
                    }
                }
            }

            if (category == EditorCategory.FILTER && selectedTool != "原图") {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("强度", color = Color.Gray, fontSize = 12.sp)
                        Text("${(filterStrength * 100).toInt()}%", color = Color.Gray, fontSize = 12.sp)
                    }
                    Slider(value = filterStrength, onValueChange = { filterStrength = it }, valueRange = 0f..1f, colors = SliderDefaults.colors(activeTrackColor = primary))
                }
            }

            if (category == EditorCategory.EFFECT && selectedTool == "涂鸦") {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("画笔", color = Color.Gray, fontSize = 12.sp)
                        Text("${brushSize.toInt()}px", color = Color.Gray, fontSize = 12.sp)
                    }
                    Slider(value = brushSize, onValueChange = { brushSize = it }, valueRange = 4f..48f, colors = SliderDefaults.colors(activeTrackColor = primary))
                    Spacer(Modifier.height(8.dp))
                    Text("颜色", color = Color.Gray, fontSize = 12.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                        listOf(Color.Black, Color.White, Color.Red, Color.Green, Color.Blue, Color.Yellow, Color.Cyan, Color.Magenta).forEach { c ->
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(c)
                                    .clickable { doodleColor = c }
                                    .border(1.dp, if (doodleColor == c) primary else Color.DarkGray, CircleShape)
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(onClick = {
                            val disp = display
                            if (doodleStrokes.isNotEmpty() && disp != null) {
                                doodleStrokes.removeLast()
                                doodleOverlay = renderStrokes(disp.width, disp.height, doodleStrokes)
                                overlayTick++
                            }
                        }) { Text("撤销", color = Color.White) }
                    }
                }
            }

            if (category == EditorCategory.EFFECT && selectedTool == "虚化") {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("虚化强度", color = Color.Gray, fontSize = 12.sp)
                        Text("${blurStrength.toInt()}", color = Color.Gray, fontSize = 12.sp)
                    }
                    Slider(value = blurStrength, onValueChange = { blurStrength = it }, valueRange = 4f..25f, colors = SliderDefaults.colors(activeTrackColor = primary))
                }
            }

            
            
        }
    }
}

private fun applyAdjustOps(src: Bitmap, ops: List<String>): Bitmap {
    var cur = src
    ops.forEach { op ->
        cur = when (op) {
            "ROTATE_90" -> rotateBitmap(cur, 90f)
            "ROTATE_-90" -> rotateBitmap(cur, -90f)
            "CROP_1_1" -> centerCrop(cur, 1f)
            "CROP_4_3" -> centerCrop(cur, 4f / 3f)
            "CROP_16_9" -> centerCrop(cur, 16f / 9f)
            else -> {
                if (op.startsWith("FREE_CROP:")) {
                    val parts = op.removePrefix("FREE_CROP:").split(',')
                    if (parts.size == 4) {
                        val l = parts[0].toFloatOrNull() ?: 0f
                        val t = parts[1].toFloatOrNull() ?: 0f
                        val r = parts[2].toFloatOrNull() ?: 1f
                        val b = parts[3].toFloatOrNull() ?: 1f
                        val rect = android.graphics.RectF(l.coerceIn(0f,1f), t.coerceIn(0f,1f), r.coerceIn(0f,1f), b.coerceIn(0f,1f))
                        cropRectNormalized(cur, rect)
                    } else cur
                } else cur
            }
        }
    }
    return cur
}

private fun rotateBitmap(bmp: Bitmap, degrees: Float): Bitmap {
    val m = android.graphics.Matrix()
    m.postRotate(degrees)
    return Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true)
}

private fun centerCrop(bmp: Bitmap, aspect: Float): Bitmap {
    val w = bmp.width
    val h = bmp.height
    val targetW = if (w.toFloat() / h > aspect) (h * aspect).toInt() else w
    val targetH = if (w.toFloat() / h > aspect) h else (w / aspect).toInt()
    val x = (w - targetW) / 2
    val y = (h - targetH) / 2
    return Bitmap.createBitmap(bmp, x.coerceAtLeast(0), y.coerceAtLeast(0), targetW.coerceAtMost(w), targetH.coerceAtMost(h))
}

private fun calcCenterCropRectNormalized(w: Int, h: Int, aspect: Float?): android.graphics.RectF {
    if (aspect == null) return android.graphics.RectF(0f, 0f, 1f, 1f)
    val targetW = if (w.toFloat() / h > aspect) (h * aspect).toInt() else w
    val targetH = if (w.toFloat() / h > aspect) h else (w / aspect).toInt()
    val x = (w - targetW) / 2
    val y = (h - targetH) / 2
    val left = x / w.toFloat()
    val top = y / h.toFloat()
    val right = (x + targetW) / w.toFloat()
    val bottom = (y + targetH) / h.toFloat()
    return android.graphics.RectF(left, top, right, bottom)
}

private fun overlayBitmap(base: Bitmap, overlay: Bitmap?): Bitmap {
    if (overlay == null) return base
    val out = base.copy(Bitmap.Config.ARGB_8888, true)
    val c = android.graphics.Canvas(out)
    c.drawBitmap(overlay, 0f, 0f, null)
    return out
}

private fun applyBlurMaskBlend(context: android.content.Context, src: Bitmap, mask: Bitmap?, radius: Float): Bitmap {
    if (mask == null) return src
    val rs = android.renderscript.RenderScript.create(context)
    val inAlloc = android.renderscript.Allocation.createFromBitmap(rs, src, android.renderscript.Allocation.MipmapControl.MIPMAP_NONE, android.renderscript.Allocation.USAGE_SCRIPT)
    val outAlloc = android.renderscript.Allocation.createTyped(rs, inAlloc.type)
    val scriptBlur = android.renderscript.ScriptIntrinsicBlur.create(rs, android.renderscript.Element.U8_4(rs))
    scriptBlur.setRadius(radius.coerceIn(1f, 25f))
    scriptBlur.setInput(inAlloc)
    scriptBlur.forEach(outAlloc)
    val blurred = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
    outAlloc.copyTo(blurred)
    val w = src.width
    val h = src.height
    val srcPx = IntArray(w * h)
    val blurPx = IntArray(w * h)
    val maskPx = IntArray(w * h)
    src.getPixels(srcPx, 0, w, 0, 0, w, h)
    blurred.getPixels(blurPx, 0, w, 0, 0, w, h)
    mask.getPixels(maskPx, 0, w, 0, 0, w, h)
    val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val outPx = IntArray(w * h)
    for (i in 0 until w * h) {
        val a = (maskPx[i] ushr 24) and 0xFF
        if (a == 0) {
            outPx[i] = srcPx[i]
        } else {
            val alpha = a / 255f
            val s = srcPx[i]
            val b = blurPx[i]
            val sr = (s ushr 16) and 0xFF
            val sg = (s ushr 8) and 0xFF
            val sb = s and 0xFF
            val br = (b ushr 16) and 0xFF
            val bg = (b ushr 8) and 0xFF
            val bb = b and 0xFF
            val rr = (sr * (1f - alpha) + br * alpha).toInt().coerceIn(0, 255)
            val rg = (sg * (1f - alpha) + bg * alpha).toInt().coerceIn(0, 255)
            val rb = (sb * (1f - alpha) + bb * alpha).toInt().coerceIn(0, 255)
            outPx[i] = (0xFF shl 24) or (rr shl 16) or (rg shl 8) or rb
        }
    }
    out.setPixels(outPx, 0, w, 0, 0, w, h)
    blurred.recycle()
    scriptBlur.destroy()
    outAlloc.destroy()
    inAlloc.destroy()
    rs.destroy()
    return out
}

private fun addSticker(base: Bitmap, emoji: String, scale: Float): Bitmap {
    val out = base.copy(Bitmap.Config.ARGB_8888, true)
    val c = android.graphics.Canvas(out)
    val p = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
    p.color = android.graphics.Color.WHITE
    p.textSize = 64f * scale
    p.setShadowLayer(8f, 0f, 0f, android.graphics.Color.argb(64, 0, 0, 0))
    val text = emoji
    val x = out.width / 2f
    val y = out.height / 2f
    val bounds = android.graphics.Rect()
    p.getTextBounds(text, 0, text.length, bounds)
    c.drawText(text, x - bounds.width() / 2f, y + bounds.height() / 2f, p)
    return out
}

private fun addStickerAtPosition(base: Bitmap, emoji: String, scale: Float, fx: Float, fy: Float): Bitmap {
    val out = base.copy(Bitmap.Config.ARGB_8888, true)
    val c = android.graphics.Canvas(out)
    val p = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
    p.color = android.graphics.Color.WHITE
    p.textSize = 64f * scale
    p.setShadowLayer(8f, 0f, 0f, android.graphics.Color.argb(64, 0, 0, 0))
    val text = emoji
    val cx = (fx * out.width).coerceIn(0f, out.width.toFloat())
    val cy = (fy * out.height).coerceIn(0f, out.height.toFloat())
    val bounds = android.graphics.Rect()
    p.getTextBounds(text, 0, text.length, bounds)
    c.drawText(text, cx - bounds.width() / 2f, cy + bounds.height() / 2f, p)
    return out
}
private fun cropRectNormalized(bmp: Bitmap, rect: android.graphics.RectF): Bitmap {
    val x0 = (rect.left * bmp.width).toInt()
    val y0 = (rect.top * bmp.height).toInt()
    val x1 = (rect.right * bmp.width).toInt()
    val y1 = (rect.bottom * bmp.height).toInt()
    val left = x0.coerceIn(0, bmp.width - 1)
    val top = y0.coerceIn(0, bmp.height - 1)
    val right = x1.coerceIn(left + 1, bmp.width)
    val bottom = y1.coerceIn(top + 1, bmp.height)
    return Bitmap.createBitmap(bmp, left, top, right - left, bottom - top)
}

private data class StrokeSegment(
    val x1: Float,
    val y1: Float,
    val x2: Float,
    val y2: Float,
    val color: Int,
    val width: Float,
    val dot: Boolean
)

private fun renderStrokes(w: Int, h: Int, strokes: List<StrokeSegment>): Bitmap {
    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val c = android.graphics.Canvas(bmp)
    val p = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
    strokes.forEach { s ->
        p.color = s.color
        if (s.dot) {
            p.style = android.graphics.Paint.Style.FILL
            c.drawCircle(s.x1, s.y1, s.width / 2f, p)
        } else {
            p.style = android.graphics.Paint.Style.STROKE
            p.strokeWidth = s.width
            p.strokeCap = android.graphics.Paint.Cap.ROUND
            p.strokeJoin = android.graphics.Paint.Join.ROUND
            c.drawLine(s.x1, s.y1, s.x2, s.y2, p)
        }
    }
    return bmp
}
