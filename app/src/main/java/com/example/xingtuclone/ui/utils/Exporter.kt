package com.example.xingtuclone.ui.utils

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.example.xingtuclone.ui.saveBitmapToGalleryReturnUri

object Exporter {
    suspend fun exportBitmap(context: Context, bitmap: Bitmap): Uri? {
        return saveBitmapToGalleryReturnUri(context, bitmap)
    }

    suspend fun exportBitmaps(context: Context, bitmaps: List<Bitmap>): List<Uri> {
        val out = mutableListOf<Uri>()
        for (b in bitmaps) {
            val u = saveBitmapToGalleryReturnUri(context, b)
            if (u != null) out += u
        }
        return out
    }
}
