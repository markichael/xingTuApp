package com.example.xingtuclone.ui

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.yalantis.ucrop.UCrop
import com.yalantis.ucrop.UCropActivity
import com.yalantis.ucrop.model.AspectRatio
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CropRotateScreen(
    srcUri: Uri,
    onBack: () -> Unit,
    onDone: (Uri) -> Unit
) {
    val context = LocalContext.current
    var launched by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val data = result.data
        if (result.resultCode == android.app.Activity.RESULT_OK && data != null) {
            val out = UCrop.getOutput(data)
            if (out != null) {
                onDone(out)
            } else {
                Toast.makeText(context, "未获取到裁剪结果", Toast.LENGTH_SHORT).show()
                onBack()
            }
        } else {
            val err = data?.let { UCrop.getError(it) }
            if (err != null) {
                Toast.makeText(context, "裁剪失败: ${err.message}", Toast.LENGTH_SHORT).show()
            }
            onBack()
        }
    }

    LaunchedEffect(Unit) {
        if (!launched) {
            launched = true
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
            val intent = UCrop.of(srcUri, dst).withOptions(options).getIntent(context).apply {
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            }
            launcher.launch(intent)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("裁剪旋转", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Color.White) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black)
            )
        },
        containerColor = Color.Black
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding).background(Color.Black), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color(0xFFCCFF00))
        }
    }
}
