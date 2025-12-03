package com.example.xingtuclone.ui

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStream

import android.util.Log

/**
 * 将 Bitmap 保存到系统相册
 */
suspend fun saveBitmapToGallery(context: Context, bitmap: Bitmap): Boolean {
    val TAG = "SaveUtils"
    return withContext(Dispatchers.IO) {
        val filename = "Xingtu_${System.currentTimeMillis()}.jpg"
        var fos: OutputStream? = null
        var success = false

        try {
            Log.d(TAG, "Attempting to save image: $filename")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+ 使用 MediaStore
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
                    put(MediaStore.MediaColumns.IS_PENDING, 1) // 标记为正在写入
                }

                val contentResolver = context.contentResolver
                val imageUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

                if (imageUri != null) {
                    Log.d(TAG, "Uri created: $imageUri")
                    fos = contentResolver.openOutputStream(imageUri)
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos!!)
                    fos?.close()

                    // 写入完成，清除 PENDING 标记
                    contentValues.clear()
                    contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    contentResolver.update(imageUri, contentValues, null, null)
                    success = true
                    Log.d(TAG, "Image saved successfully")
                } else {
                    Log.e(TAG, "Failed to create MediaStore Uri")
                }
            } else {
                // Android 9 及以下
                Log.w(TAG, "Android 9 or lower detected. Save might fail if not handled (Legacy storage).")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during save", e)
            e.printStackTrace()
            success = false
        }

        // 切换回主线程显示结果
        withContext(Dispatchers.Main) {
            if (success) {
                Toast.makeText(context, "保存成功！已存入相册", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "保存失败", Toast.LENGTH_SHORT).show()
            }
        }

        return@withContext success
    }
}

/** 保存并返回保存后的 Uri，便于展示保存结果页 */
suspend fun saveBitmapToGalleryReturnUri(context: Context, bitmap: Bitmap): android.net.Uri? {
    return withContext(Dispatchers.IO) {
        val filename = "Xingtu_${System.currentTimeMillis()}.jpg"
        var fos: OutputStream? = null
        var imageUri: android.net.Uri? = null
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
                val resolver = context.contentResolver
                imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                if (imageUri != null) {
                    fos = resolver.openOutputStream(imageUri)
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos!!)
                    fos?.close()
                    contentValues.clear()
                    contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    resolver.update(imageUri!!, contentValues, null, null)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            imageUri = null
        }
        withContext(Dispatchers.Main) {
            Toast.makeText(context, if (imageUri != null) "保存成功！已存入相册" else "保存失败", Toast.LENGTH_SHORT).show()
        }
        imageUri
    }
}
