/**
 * ============================================
 * MainActivity.kt
 * ============================================
 * 功能说明：
 * 应用唯一的 Activity 入口，负责初始化 Compose UI 框架
 * 
 * 职责：
 * 1. 显示启动页面（SplashScreen）
 * 2. 启动页结束后切换到主界面（HomeScreen）
 * 3. 提供相机拍照的临时文件创建工具（createImageFile扩展函数）
 * 
 * 技术点：
 * - 使用 Jetpack Compose 作为 UI 框架
 * - 通过 remember 管理启动页显示状态
 * - 集成 FileProvider 用于相机拍照的 URI 生成
 * ============================================
 */
package com.example.xingtuclone

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

/**
 * 应用主Activity
 * 采用单 Activity 架构，所有页面通过 Compose 导航管理
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 设置 Compose UI 内容
        setContent {
            // 控制启动页显示状态
            var showSplash by remember { mutableStateOf(true) }
            if (showSplash) {
                // 显示启动页，动画播放完毕后切换
                SplashScreen(onFinished = { showSplash = false })
            } else {
                // 进入主界面
                HomeScreen()
            }
        }
    }
}

/**
 * Context 扩展函数：为相机拍照创建临时图片文件
 * 
 * 功能：
 * - 在 externalCacheDir 创建临时 .jpg 文件
 * - 通过 FileProvider 获取安全的 Uri 用于相机拍照
 * 
 * @return 图片文件的 Uri，格式为 content://com.example.xingtuclone.fileprovider/...
 */
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
