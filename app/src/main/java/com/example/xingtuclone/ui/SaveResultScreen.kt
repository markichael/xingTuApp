package com.example.xingtuclone.ui

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import android.widget.ImageView
import coil.load

@Composable
fun SaveResultScreen(
    savedUri: Uri,
    onBack: () -> Unit,
    onHome: () -> Unit,
    onRetouch: () -> Unit
) {
    val context = LocalContext.current
    val primary = Color(0xFFCCFF00)

    val recentUris by remember { mutableStateOf(queryRecentImages(context, 4)) }

    Scaffold(containerColor = Color.White) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.ArrowBack, null, tint = Color.Black, modifier = Modifier.size(24.dp).clickable { onBack() })
                Text("已保存至相册和作品集", color = Color.Black, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Icon(Icons.Default.Home, null, tint = Color.Black, modifier = Modifier.size(24.dp).clickable { onHome() })
            }

            Spacer(Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .background(Color.LightGray),
                contentAlignment = Alignment.Center
            ) {
                AndroidView(factory = { ctx -> ImageView(ctx) }, update = { iv -> iv.load(savedUri) { allowHardware(false) } }, modifier = Modifier.fillMaxSize())
            }

            Spacer(Modifier.height(16.dp))

            Button(onClick = onRetouch, colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent), border = ButtonDefaults.outlinedButtonBorder, modifier = Modifier.fillMaxWidth()) {
                Text("再修一张", color = Color.Black)
            }

            Spacer(Modifier.height(16.dp))
            Text("分享你的作品", color = Color.DarkGray, fontSize = 14.sp)
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(recentUris) { uri ->
                    Box(modifier = Modifier.size(72.dp).background(Color.LightGray)) {
                        AndroidView(factory = { ctx -> ImageView(ctx) }, update = { iv -> iv.load(uri) { allowHardware(false); size(300) } }, modifier = Modifier.fillMaxSize())
                    }
                }
                item {
                    Box(modifier = Modifier.size(72.dp).background(Color(0xFFEFEFEF)), contentAlignment = Alignment.Center) {
                        Text("+", color = Color.Black, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Button(onClick = {}, colors = ButtonDefaults.buttonColors(containerColor = Color.Black), modifier = Modifier.fillMaxWidth()) {
                Text("发抖音有机会获流量扶持", color = primary, fontWeight = FontWeight.Bold)
            }
        }
    }
}

fun queryRecentImages(context: Context, limit: Int): List<Uri> {
    val list = mutableListOf<Uri>()
    val projection = arrayOf(MediaStore.Images.Media._ID)
    val sortOrder = MediaStore.Images.Media.DATE_ADDED + " DESC"
    val uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
    val cursor = context.contentResolver.query(uri, projection, null, null, sortOrder)
    cursor?.use {
        val idIndex = it.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
        var count = 0
        while (it.moveToNext() && count < limit) {
            val id = it.getLong(idIndex)
            val contentUri = Uri.withAppendedPath(uri, id.toString())
            list.add(contentUri)
            count++
        }
    }
    return list
}

