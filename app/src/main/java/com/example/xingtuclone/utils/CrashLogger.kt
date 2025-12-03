package com.example.xingtuclone.utils

import android.content.Context
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CrashLogger {
    private const val TAG = "CrashLogger"

    fun log(context: Context, tag: String, msg: String) {
        Log.d(tag, msg)
        try {
            val f = File(context.cacheDir, "app_debug.log")
            val ts = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date())
            f.appendText("[$ts][$tag] $msg\n")
        } catch (_: Exception) {}
    }

    fun error(context: Context, tag: String, msg: String, t: Throwable? = null) {
        Log.e(tag, msg, t)
        try {
            val f = File(context.cacheDir, "app_error.log")
            val ts = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date())
            f.appendText("[$ts][$tag] $msg\n")
            if (t != null) f.appendText(Log.getStackTraceString(t) + "\n")
        } catch (_: Exception) {}
    }
}

