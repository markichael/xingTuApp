package com.example.xingtuclone // 👈 确保这里的包名和你文件第一行一样

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.example.xingtuclone.ui.HomeScreen
import com.example.xingtuclone.ui.SplashScreen
import android.content.Context
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var showSplash by remember { mutableStateOf(true) }
            if (showSplash) {
                SplashScreen(onFinished = { showSplash = false })
            } else {
                HomeScreen()
            }
        }
    }
}
fun Context.createImageFile(): Uri {
    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    val imageFileName = "JPEG_" + timeStamp + "_"

    //  重点：这里必须是 externalCacheDir，对应 XML 里的 <external-cache-path>
    val image = File.createTempFile(
        imageFileName,
        ".jpg",
        externalCacheDir
    )

    return FileProvider.getUriForFile(
        this,
        "com.example.xingtuclone.fileprovider",
        image
    )
}
