package com.example.xingtuclone.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import com.example.xingtuclone.ui.navigation.AppRoute

class HomeViewModel : ViewModel() {
    private val _route = MutableStateFlow<AppRoute>(AppRoute.Home)
    val route: StateFlow<AppRoute> = _route

    fun toHome() { _route.value = AppRoute.Home }
    fun toGallery(target: String, allowMulti: Boolean, max: Int) { _route.value = AppRoute.Gallery(target, allowMulti, max) }
    fun toEditor(uris: List<Uri>, initial: EditorCategory) { _route.value = AppRoute.Editor(uris, initial) }
    fun toMagicErase(uri: Uri) { _route.value = AppRoute.MagicErase(uri) }
    fun toCollage(uris: List<Uri>) { _route.value = AppRoute.Collage(uris) }
    fun toBatch(uris: List<Uri>) { _route.value = AppRoute.BatchEdit(uris) }
    fun toCrop(uri: Uri) { _route.value = AppRoute.CropRotate(uri) }
    fun toSaveResult(uri: Uri) { _route.value = AppRoute.SaveResult(uri) }
}
