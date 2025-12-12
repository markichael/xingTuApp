/**
 * ============================================
 * CrashLogger.kt - 崩溃日志工具
 * ============================================
 * 功能说明：
 * 统一的日志记录工具，同时写入 Logcat 和本地文件
 * 
 * 特性：
 * - 日志保存到 cacheDir（app_debug.log / app_error.log）
 * - 带时间戳，方便问题追踪
 * - 支持普通日志和错误日志
 * 
 * 使用场景：
 * - 裁剪功能的调试日志
 * - 记录 uCrop 的异常信息
 * ============================================
 */
package com.example.xingtuclone.utils

import android.content.Context
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 崩溃日志记录工具单例
 * 用于记录调试信息和错误堆栈
 */
object CrashLogger {
    private const val TAG = "CrashLogger"

    /**
     * 记录普通日志
     * @param context 上下文
     * @param tag 日志标签
     * @param msg 日志消息
     */
    fun log(context: Context, tag: String, msg: String) {
        Log.d(tag, msg)
        try {
            // 写入本地文件，带时间戳
            val f = File(context.cacheDir, "app_debug.log")
            val ts = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date())
            f.appendText("[$ts][$tag] $msg\n")
        } catch (_: Exception) {}
    }

    /**
     * 记录错误日志
     * @param context 上下文
     * @param tag 日志标签
     * @param msg 错误消息
     * @param t 异常对象（可选）
     */
    fun error(context: Context, tag: String, msg: String, t: Throwable? = null) {
        Log.e(tag, msg, t)
        try {
            // 写入错误日志文件，包含堆栈信息
            val f = File(context.cacheDir, "app_error.log")
            val ts = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date())
            f.appendText("[$ts][$tag] $msg\n")
            if (t != null) f.appendText(Log.getStackTraceString(t) + "\n")
        } catch (_: Exception) {}
    }
}

