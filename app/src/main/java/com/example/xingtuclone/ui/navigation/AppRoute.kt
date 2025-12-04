package com.example.xingtuclone.ui.navigation

import android.net.Uri
import com.example.xingtuclone.ui.EditorCategory

sealed class AppRoute {
    object Home : AppRoute()
    data class Gallery(val target: String, val allowMulti: Boolean, val max: Int) : AppRoute()
    data class Editor(val uris: List<Uri>, val initialCategory: EditorCategory) : AppRoute()
    data class MagicErase(val uri: Uri) : AppRoute()
    data class Collage(val uris: List<Uri>) : AppRoute()
    data class BatchEdit(val uris: List<Uri>) : AppRoute()
    data class CropRotate(val uri: Uri) : AppRoute()
    data class SaveResult(val uri: Uri) : AppRoute()
}
