package com.example.xingtuclone.ui

import LightGreenBg
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.example.xingtuclone.createImageFile
import com.example.xingtuclone.model.MenuItem
import com.example.xingtuclone.ui.components.BigActionButton
import com.example.xingtuclone.ui.components.MenuGridSection
import com.example.xingtuclone.ui.components.XingtuBottomBar

@Composable
fun HomeScreen() {
    val context = LocalContext.current // 🔥 获取上下文
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    // 🔥 新增：用于临时存放相机拍的照片的 URI
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }

    // 1. 相册选择器 (之前的)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> selectedImageUri = uri }
    )

    // 🔥 2. 新增：相机启动器
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
        onResult = { success ->
            // 如果拍照成功，就把临时 URI 赋值给展示用的 URI
            if (success && tempCameraUri != null) {
                selectedImageUri = tempCameraUri
            }
        }
    )

    if (selectedImageUri != null) {
        EditorScreen(
            imageUri = selectedImageUri!!,
            onBack = { selectedImageUri = null }
        )
    } else {
        HomeContent(
            onImportClick = {
                photoPickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            onCameraClick = {
                // 🔥 点击相机按钮的逻辑
                // 1. 先创建一个空文件的 URI
                val uri = context.createImageFile()
                tempCameraUri = uri // 记下来，等会儿拍完照要用
                // 2. 启动相机，让它把照片存到这个 URI 里
                cameraLauncher.launch(uri)
            }
        )
    }
}

// ------------------------------------------------------------
// 为了让代码整洁，把原来的首页内容抽离出来
@Composable
fun HomeContent(
    onImportClick: () -> Unit,
    onCameraClick: () -> Unit
) {
    val menuItems = listOf(
        MenuItem("批量修图", Icons.Outlined.PhotoLibrary),
        MenuItem("画质超清", Icons.Outlined.HighQuality),
        MenuItem("魔法消除", Icons.Default.AutoFixHigh),
        MenuItem("智能抠图", Icons.Outlined.ContentCut),
        MenuItem("AI修图", Icons.Default.AutoAwesome),
        MenuItem("一键消除", Icons.Outlined.CleaningServices),
        MenuItem("瘦脸瘦身", Icons.Default.Face),
        MenuItem("所有工具", Icons.Default.GridView)
    )

    Scaffold(
        bottomBar = { XingtuBottomBar() },
        containerColor = Color.White
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            HeaderSection()
            Spacer(modifier = Modifier.height(20.dp))

            // 第一排按钮
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
                    onClick = onImportClick // 🔥 绑定点击
                )
                BigActionButton(
                    text = "相机",
                    icon = Icons.Default.CameraAlt,
                    backgroundColor = LightGreenBg,
                    contentColor = Color.Black,
                    modifier = Modifier.weight(1f),
                    onClick = onCameraClick // 🔥 绑定点击
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 第二排按钮 (暂时还没加功能，onClick 传个空函数)
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
                    onClick = {}
                )
                BigActionButton(
                    text = "拼图",
                    icon = Icons.Default.Dashboard,
                    backgroundColor = LightGreenBg,
                    contentColor = Color.Black,
                    modifier = Modifier.weight(1f),
                    onClick = {}
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
            Box(modifier = Modifier.height(250.dp)) {
                MenuGridSection(menuItems)
            }
        }
    }
}

// ------------------------------------------------------------
// 一个简单的预览/编辑页面，用于展示选中的图片
@Composable
fun SimpleEditorScreen(imageUri: Uri, onBack: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().background(Color.Black),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // 顶部栏
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.Start
        ) {
            Text(
                text = "🔙 返回首页",
                color = Color.White,
                fontSize = 18.sp,
                modifier = Modifier.clickable { onBack() }
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 🔥 使用 Coil 显示选中的图片
        Image(
            painter = rememberAsyncImagePainter(imageUri),
            contentDescription = "Selected Image",
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f), // 让图片占据剩余空间
            contentScale = ContentScale.Fit // 保持比例展示
        )

        Spacer(modifier = Modifier.height(50.dp))
        Text("这里以后放修图工具栏", color = Color.Gray)
        Spacer(modifier = Modifier.height(50.dp))
    }
}

@Composable
fun HeaderSection() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 20.dp, bottom = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFFCCFF00), Color.Black),
                        radius = 80f
                    ),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                )
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = "醒图",
            fontSize = 40.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.Black
        )
        Spacer(modifier = Modifier.width(16.dp))
        Icon(
            imageVector = Icons.Default.Camera,
            contentDescription = "Great Pic",
            tint = Color(0xFF0099CC),
            modifier = Modifier.size(24.dp)
        )
    }
}