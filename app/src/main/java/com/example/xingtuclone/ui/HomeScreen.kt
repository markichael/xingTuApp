package com.example.xingtuclone.ui

import LightGreenBg
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.scale
import androidx.core.content.FileProvider
import com.example.xingtuclone.model.MenuItem
import com.example.xingtuclone.ui.components.BigActionButton
import com.example.xingtuclone.ui.components.MenuGridSection
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen() {
    val context = LocalContext.current

    // --- 状态管理 (使用 rememberSaveable 防止后台被杀后数据丢失) ---

    // 1. 普通修图图片的 Uri
    var selectedImageUri by rememberSaveable { mutableStateOf<Uri?>(null) }
    
    // 0. 是否显示自定义相册页
    var showCustomGallery by rememberSaveable { mutableStateOf(false) }
    var galleryMulti by rememberSaveable { mutableStateOf(false) }
    var galleryMax by rememberSaveable { mutableStateOf(1) }
    var galleryTarget by rememberSaveable { mutableStateOf("retouch") }

    // 2. AI 修人像图片的 Uri
    var faceBeautyUri by rememberSaveable { mutableStateOf<Uri?>(null) }
    var savedImageUri by rememberSaveable { mutableStateOf<Uri?>(null) }
    var cropUri by rememberSaveable { mutableStateOf<Uri?>(null) }
    var batchUris by rememberSaveable { mutableStateOf<List<Uri>>(emptyList()) }
    var magicEraseUri by rememberSaveable { mutableStateOf<Uri?>(null) }

    // 3. 拼图图片列表 (多选)
    var collageUris by rememberSaveable { mutableStateOf<List<Uri>>(emptyList()) }


    // 4. 相机拍照的临时 Uri
    var tempCameraUri by rememberSaveable { mutableStateOf<Uri?>(null) }


    // --- 启动器定义 ---

    // A. 单图选择器 (用于普通修图)
    val singlePhotoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> selectedImageUri = uri }
    )

    // B. 单图选择器 (用于 AI 修人像)
    val faceBeautyPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> faceBeautyUri = uri }
    )

    // C. 多图选择器 (用于拼图，最多 6 张)
    val multiplePhotoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(6),
        onResult = { uris ->
            if (uris.isNotEmpty()) {
                collageUris = uris
            }
        }
    )

    // E. 批量修图多选
    val batchPhotoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(20),
        onResult = { uris -> if (uris.isNotEmpty()) batchUris = uris }
    )

    // F. 魔法消除单图选择器
    val magicErasePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> magicEraseUri = uri }
    )


    // D. 相机启动器
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
        onResult = { success ->
            if (success && tempCameraUri != null) {
                // 拍照成功后，默认进入普通修图模式
                selectedImageUri = tempCameraUri
            }
        }
    )

    // --- 页面路由逻辑 (优先级控制) ---
    // 根据哪个状态不为空，显示哪个页面
    // 统一编辑入口：根据按钮优先级，进入统一 EditorScreen
    when {
        savedImageUri != null -> {
            SaveResultScreen(
                savedUri = savedImageUri!!,
                onBack = { savedImageUri = null },
                onHome = {
                    // 统一返回首页：清空所有编辑入口状态
                    savedImageUri = null
                    selectedImageUri = null
                    faceBeautyUri = null
                    collageUris = emptyList()
                },
                onRetouch = {
                    singlePhotoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    savedImageUri = null
                }
            )
        }
        selectedImageUri != null -> {
            EditorShell(
                imageUris = listOf(selectedImageUri!!),
                onBack = { selectedImageUri = null },
                initialCategory = EditorCategory.FILTER,
                onSaved = { uri -> savedImageUri = uri }
            )
        }
        cropUri != null -> {
            CropRotateScreen(
                srcUri = cropUri!!,
                onBack = { cropUri = null },
                onDone = { out ->
                    savedImageUri = out
                    cropUri = null
                }
            )
        }
        showCustomGallery -> {
            GalleryScreen(
                onBack = { showCustomGallery = false },
                onImageSelected = { uri ->
                    when (galleryTarget) {
                        "retouch" -> selectedImageUri = uri
                        "face" -> faceBeautyUri = uri
                        "magic" -> magicEraseUri = uri
                        "crop" -> cropUri = uri
                        else -> selectedImageUri = uri
                    }
                    showCustomGallery = false
                },
                onImagesSelected = { uris ->
                    when (galleryTarget) {
                        "collage" -> collageUris = uris
                        "batch" -> batchUris = uris
                        else -> if (uris.isNotEmpty()) selectedImageUri = uris.first()
                    }
                    showCustomGallery = false
                },
                allowMultiSelect = galleryMulti,
                maxSelection = galleryMax
            )
        }
        faceBeautyUri != null -> {
            EditorShell(
                imageUris = listOf(faceBeautyUri!!),
                onBack = { faceBeautyUri = null },
                initialCategory = EditorCategory.PORTRAIT,
                onSaved = { uri -> savedImageUri = uri }
            )
        }
        magicEraseUri != null -> {
            MagicEraseScreen(
                imageUri = magicEraseUri!!,
                onBack = { magicEraseUri = null },
                onSaved = { uri -> savedImageUri = uri }
            )
        }
        collageUris.isNotEmpty() -> {
            CollageScreen(
                imageUris = collageUris,
                onBack = { collageUris = emptyList() },
                onHome = {
                    collageUris = emptyList()
                    selectedImageUri = null
                    faceBeautyUri = null
                    savedImageUri = null
                }
            )
        }
        batchUris.isNotEmpty() -> {
            BatchEditScreen(imageUris = batchUris, onBack = { batchUris = emptyList() })
        }
        else -> {
            HomeContent(
                onImportClick = {
                    galleryTarget = "retouch"
                    galleryMulti = false
                    galleryMax = 1
                    showCustomGallery = true
                },
                onCameraClick = {
                    val uri = context.createImageFile()
                    tempCameraUri = uri
                    cameraLauncher.launch(uri)
                },
                onFaceBeautyClick = {
                    galleryTarget = "face"
                    galleryMulti = false
                    galleryMax = 1
                    showCustomGallery = true
                },
                onCollageClick = {
                    galleryTarget = "collage"
                    galleryMulti = true
                    galleryMax = 6
                    showCustomGallery = true
                },
                onBatchClick = {
                    galleryTarget = "batch"
                    galleryMulti = true
                    galleryMax = 20
                    showCustomGallery = true
                },
                onMagicEraseClick = {
                    galleryTarget = "magic"
                    galleryMulti = false
                    galleryMax = 1
                    showCustomGallery = true
                },
                onCropRotateClick = {
                    galleryTarget = "crop"
                    galleryMulti = false
                    galleryMax = 1
                    showCustomGallery = true
                }
            )
        }
    }
}

// ------------------------------------------------------------
// 首页 UI 内容
// ------------------------------------------------------------
@Composable
fun HomeContent(
    onImportClick: () -> Unit,
    onCameraClick: () -> Unit,
    onFaceBeautyClick: () -> Unit,
    onCollageClick: () -> Unit,
    onBatchClick: () -> Unit,
    onMagicEraseClick: () -> Unit,
    onCropRotateClick: () -> Unit
) {
    val menuItems = listOf(
        MenuItem("批量修图", Icons.Outlined.PhotoLibrary),
        MenuItem("魔法消除", Icons.Default.AutoFixHigh),
        MenuItem("AI修图", Icons.Default.AutoAwesome),
        MenuItem("拼图", Icons.Default.Dashboard)
    )

    Scaffold(
        containerColor = Color.White
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.Top
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(top = 48.dp)) {
                HeaderSection()
                Spacer(modifier = Modifier.height(64.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    BigActionButton(
                        text = "+ 导入照片",
                        icon = Icons.Default.Image,
                        backgroundColor = Color.Black,
                        contentColor = Color.White,
                        modifier = Modifier.weight(1f),
                        onClick = onImportClick
                    )
                    BigActionButton(
                        text = "相机",
                        icon = Icons.Default.CameraAlt,
                        backgroundColor = LightGreenBg,
                        contentColor = Color.Black,
                        modifier = Modifier.weight(1f),
                        onClick = onCameraClick
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            BigActionButton(
                text = "AI修人像",
                icon = Icons.Default.FaceRetouchingNatural,
                backgroundColor = LightGreenBg,
                contentColor = Color.Black,
                modifier = Modifier.weight(1f),
                onClick = onFaceBeautyClick
            )
            BigActionButton(
                text = "拼图",
                icon = Icons.Default.Dashboard,
                backgroundColor = LightGreenBg,
                contentColor = Color.Black,
                modifier = Modifier.weight(1f),
                onClick = onCollageClick
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        MidHeroCard(
            onMagicEraseClick = onMagicEraseClick,
            onCollageClick = onCollageClick,
            onFaceBeautyClick = onFaceBeautyClick,
            onCropRotateClick = onCropRotateClick
        )
    }
            Spacer(modifier = Modifier.weight(1f))
            Box(modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)) {
                MenuGridSection(menuItems) { item ->
                    when (item.title) {
                        "批量修图" -> onBatchClick()
                        "AI修图" -> onFaceBeautyClick()
                        "魔法消除" -> onMagicEraseClick()
                        "拼图" -> onCollageClick()
                        else -> {}
                    }
                }
            }
        }
    }
}

@Composable
fun HeaderSection() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp, bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .shadow(12.dp, CircleShape, clip = true)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFFCCFF00), Color(0xFF00E1FF), Color.Black),
                        radius = 120f
                    ),
                    shape = CircleShape
                )
                .border(2.dp, Color(0x88FFFFFF), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.AutoFixHigh,
                contentDescription = null,
                tint = Color.Black,
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(modifier = Modifier.width(24.dp))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "醒图",
                fontSize = 40.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.Black
            )
            Box(
                modifier = Modifier
                    .height(4.dp)
                    .width(60.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color(0xFFCCFF00), Color(0xFF00E1FF))
                        )
                    )
            )
        }
        Spacer(modifier = Modifier.width(24.dp))
        Box(
            modifier = Modifier
                .size(36.dp)
                .shadow(8.dp, CircleShape, clip = true)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0x3300E1FF), Color.Transparent),
                        radius = 60f
                    ),
                    shape = CircleShape
                )
                .border(1.dp, Color(0x3300E1FF), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Camera,
                contentDescription = null,
                tint = Color(0xFF0099CC),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun MidHeroCard(
    onMagicEraseClick: () -> Unit,
    onCollageClick: () -> Unit,
    onFaceBeautyClick: () -> Unit,
    onCropRotateClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFCCFF00), Color(0xFF00E1FF), Color(0x11000000)),
                    radius = 420f
                )
            )
            .padding(12.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "艺术修图，一键出片",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "热门工具快捷直达",
                fontSize = 13.sp,
                color = Color(0xFF666666)
            )
            Spacer(modifier = Modifier.height(12.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                QuickHeroBtn("魔法消除", Icons.Default.AutoFixHigh, onMagicEraseClick)
                QuickHeroBtn("拼图", Icons.Default.Dashboard, onCollageClick)
                QuickHeroBtn("AI修图", Icons.Default.AutoAwesome, onFaceBeautyClick)
                QuickHeroBtn("裁剪旋转", Icons.Default.Crop, onCropRotateClick)
            }
        }
    }
}

@Composable
private fun QuickHeroBtn(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (pressed) Color(0x22000000) else Color(0x11FFFFFF))
            .scale(if (pressed) 0.98f else 1f)
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .clickable(interactionSource = interaction, indication = null) { onClick() }
    ) {
        Icon(icon, contentDescription = text, tint = Color.Black, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = text, fontSize = 12.sp, color = Color.Black)
    }
}

// ------------------------------------------------------------
// 辅助工具：创建临时图片文件
// ------------------------------------------------------------
fun Context.createImageFile(): Uri {
    // 1. 创建文件名 (例如: JPEG_20231126_120000_)
    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    val imageFileName = "JPEG_" + timeStamp + "_"

    // 2. 创建临时文件
    // 注意：必须使用 externalCacheDir，对应 file_paths.xml 里的 external-cache-path
    val image = File.createTempFile(
        imageFileName,
        ".jpg",
        externalCacheDir
    )

    // 3. 获取 URI
    // ⚠️⚠️⚠️ 警告：如果你改了包名，下面字符串必须改成你的包名 + ".fileprovider"
    // 请检查 AndroidManifest.xml 里的 authorities 是否一致
    return FileProvider.getUriForFile(
        this,
        "com.example.xingtuclone.fileprovider",
        image
    )
}
