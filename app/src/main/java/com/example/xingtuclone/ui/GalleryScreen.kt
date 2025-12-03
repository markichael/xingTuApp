package com.example.xingtuclone.ui

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.size.Scale
import coil.size.Precision
import android.os.SystemClock
import com.example.xingtuclone.utils.PerfLogger
import LightGreenBg
import TextBlack
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ButtonDefaults

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(
    onBack: () -> Unit,
    onImageSelected: (Uri) -> Unit = {},
    onImagesSelected: (List<Uri>) -> Unit = {},
    allowMultiSelect: Boolean = false,
    maxSelection: Int = 1,
    vm: GalleryViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val context = LocalContext.current
    val mediaItems by vm.mediaItems.collectAsState()

    // 权限请求逻辑
    val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
    } else {
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    var hasPermission by remember {
        mutableStateOf(
            permissions.all {
                ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
            }
        )
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        hasPermission = perms.values.all { it }
        if (hasPermission) {
            vm.loadMedia(context)
        }
    }

    LaunchedEffect(Unit) {
        PerfLogger.startEnter()
        if (!hasPermission) {
            launcher.launch(permissions)
        } else {
            vm.loadMedia(context)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("选择图片", color = TextBlack) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = TextBlack,
                    navigationIconContentColor = TextBlack
                )
            )
        }
        ,
        containerColor = Color.White
    ) { padding ->
        if (hasPermission) {
            if (mediaItems.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Text("暂无图片或正在加载...", color = TextBlack)
                }
            } else {
                var selected by remember { mutableStateOf(setOf<Uri>()) }
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 120.dp),
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(mediaItems) { item ->
                        Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFF5F5F5))
                                .then(
                                    if (allowMultiSelect && selected.contains(item.uri)) Modifier.border(2.dp, Color.Black, RoundedCornerShape(12.dp)) else Modifier
                                )
                                .clickable {
                                    if (allowMultiSelect) {
                                        val cur = selected
                                        selected = if (cur.contains(item.uri)) cur.minus(item.uri) else {
                                            if (cur.size < maxSelection) cur.plus(item.uri) else cur
                                        }
                                    } else {
                                        onImageSelected(item.uri)
                                    }
                                }
                        ) {
                            val request = remember(item.uri) {
                                ImageRequest.Builder(context)
                                    .data(item.uri)
                                    .crossfade(true)
                                    .allowHardware(true)
                                    .precision(Precision.INEXACT)
                                    .scale(Scale.FILL)
                                    .size(256)
                                    .diskCachePolicy(CachePolicy.ENABLED)
                                    .memoryCachePolicy(CachePolicy.ENABLED)
                                    .listener(
                                        onStart = { PerfLogger.onItemStart(item.uri.toString()) },
                                        onSuccess = { _, _ -> PerfLogger.onItemSuccess(item.uri.toString(), context) }
                                    )
                                    .build()
                            }

                            AsyncImage(
                                model = request,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            if (allowMultiSelect && selected.contains(item.uri)) {
                                Box(
                                    modifier = Modifier.fillMaxSize().background(Color(0x40000000)),
                                    contentAlignment = Alignment.TopEnd
                                ) {
                                    Text("✓", color = Color.White, modifier = Modifier.padding(6.dp))
                                }
                            }
                        }
                    }
                }
                if (allowMultiSelect) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("已选 ${selected.size}/$maxSelection", color = TextBlack)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = onBack) {
                                Text("返回")
                            }
                            Button(
                                onClick = { if (selected.isNotEmpty()) onImagesSelected(selected.toList()) },
                                enabled = selected.isNotEmpty(),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Black, contentColor = Color.White)
                            ) {
                                Text("完成")
                            }
                        }
                    }
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Button(onClick = { launcher.launch(permissions) }) {
                    Text("需要存储权限才能显示图片")
                }
            }
        }
    }
}
