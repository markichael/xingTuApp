package com.example.xingtuclone // 👈 确保这里的包名和你文件第一行一样

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.xingtuclone.ui.HomeScreen
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
            // 🔴 之前的代码里这里有个 XingtuCloneTheme { ... }
            // 🟢 我们直接删掉它，只留下一行 HomeScreen() 即可！
            HomeScreen()
        }
    }
}
// 放在 MainActivity.kt 的最底下，不要放在 class 里面
fun Context.createImageFile(): Uri {
    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    val imageFileName = "JPEG_" + timeStamp + "_"

    // 🔥 重点：这里必须是 externalCacheDir，对应 XML 里的 <external-cache-path>
    val image = File.createTempFile(
        imageFileName,
        ".jpg",
        externalCacheDir
    )

    return FileProvider.getUriForFile(
        this,
        "com.example.xingtuclone.fileprovider", // 再次确认这里和 Manifest 里的一模一样
        image
    )
}