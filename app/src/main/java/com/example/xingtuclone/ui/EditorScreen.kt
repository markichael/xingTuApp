package com.example.xingtuclone.ui

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import jp.co.cyberagent.android.gpuimage.GPUImage
import jp.co.cyberagent.android.gpuimage.GPUImageView
import jp.co.cyberagent.android.gpuimage.filter.*

enum class EditMode { FILTER, ADJUST }

@Composable
fun EditorScreen(imageUri: Uri, onBack: () -> Unit) {
    val context = LocalContext.current

    // --- 状态管理 ---
    var currentMode by remember { mutableStateOf(EditMode.FILTER) }
    var selectedFilterItem by remember { mutableStateOf(filterList[0]) }

    // 调节参数
    var brightnessValue by remember { mutableFloatStateOf(0.0f) }
    var contrastValue by remember { mutableFloatStateOf(1.0f) }
    var saturationValue by remember { mutableFloatStateOf(1.0f) }

    // 当前选中的调节工具
    var selectedAdjustType by remember { mutableStateOf(AdjustType.BRIGHTNESS) }

    // --- GPUImage 初始化 ---
    val gpuImageView = remember { GPUImageView(context) }

    // 预先创建调节滤镜实例
    val brightnessFilter = remember { GPUImageBrightnessFilter() }
    val contrastFilter = remember { GPUImageContrastFilter() }
    val saturationFilter = remember { GPUImageSaturationFilter() }

    // 1. 图片变化时加载
    LaunchedEffect(imageUri) {
        gpuImageView.setImage(imageUri)
    }

    // 2. 任何参数变化时，重新组合滤镜链
    LaunchedEffect(selectedFilterItem, brightnessValue, contrastValue, saturationValue) {
        brightnessFilter.setBrightness(brightnessValue)
        contrastFilter.setContrast(contrastValue)
        saturationFilter.setSaturation(saturationValue)

        val group = GPUImageFilterGroup()
        // 叠加顺序：基础滤镜 -> 亮度 -> 对比度 -> 饱和度
        group.addFilter(selectedFilterItem.filter)
        group.addFilter(brightnessFilter)
        group.addFilter(contrastFilter)
        group.addFilter(saturationFilter)

        gpuImageView.filter = group
    }

    Column(modifier = Modifier.fillMaxSize().background(Color.Black)) {

        // 顶部栏
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Close, "Back", tint = Color.White, modifier = Modifier.clickable { onBack() })
            Icon(Icons.Default.Check, "Save", tint = Color(0xFFCCFF00))
        }

        // 中间图片预览区
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = {
                    gpuImageView.apply {
                        // 🔥 修复点：使用 GPUImage.ScaleType，确保不报错
                        setScaleType(GPUImage.ScaleType.CENTER_INSIDE)
                    }
                }
            )
        }

        // 底部操作区
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1A1A1A))
                .padding(bottom = 16.dp)
        ) {
            // 滑块区域 (仅在调节模式下显示)
            if (currentMode == EditMode.ADJUST) {
                Box(modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp)) {
                    Slider(
                        value = when (selectedAdjustType) {
                            AdjustType.BRIGHTNESS -> brightnessValue
                            AdjustType.CONTRAST -> contrastValue
                            AdjustType.SATURATION -> saturationValue
                        },
                        onValueChange = { newValue ->
                            when (selectedAdjustType) {
                                AdjustType.BRIGHTNESS -> brightnessValue = newValue
                                AdjustType.CONTRAST -> contrastValue = newValue
                                AdjustType.SATURATION -> saturationValue = newValue
                            }
                        },
                        valueRange = when (selectedAdjustType) {
                            AdjustType.BRIGHTNESS -> -0.5f..0.5f
                            AdjustType.CONTRAST -> 0.5f..2.0f
                            AdjustType.SATURATION -> 0.0f..2.0f
                        },
                        colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Color(0xFFCCFF00))
                    )
                }
            } else {
                Spacer(modifier = Modifier.height(20.dp))
            }

            // 功能列表 (滤镜 或 调节工具)
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.height(90.dp)
            ) {
                if (currentMode == EditMode.FILTER) {
                    // 🔥 修复点：使用 count + index 的方式，彻底解决 import 爆红问题
                    items(count = filterList.size) { index ->
                        val item = filterList[index]
                        FilterItemButton(
                            item = item,
                            isSelected = item == selectedFilterItem,
                            onClick = { selectedFilterItem = item }
                        )
                    }
                } else {
                    // 同上，解决 import 问题
                    items(count = adjustTools.size) { index ->
                        val tool = adjustTools[index]
                        AdjustToolButton(
                            item = tool,
                            isSelected = tool.type == selectedAdjustType,
                            onClick = { selectedAdjustType = tool.type }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 底部 Tab 切换
            Row(modifier = Modifier.fillMaxWidth()) {
                BottomTabButton("滤镜", currentMode == EditMode.FILTER, Modifier.weight(1f)) { currentMode = EditMode.FILTER }
                BottomTabButton("调节", currentMode == EditMode.ADJUST, Modifier.weight(1f)) { currentMode = EditMode.ADJUST }
            }
        }
    }
}

// --- 以下是提取出来的小组件 ---

@Composable
fun FilterItemButton(item: FilterItem, isSelected: Boolean, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onClick() }) {
        Box(
            modifier = Modifier.size(60.dp).clip(RoundedCornerShape(8.dp))
                .background(if (isSelected) Color(0xFFCCFF00) else Color.Gray),
            contentAlignment = Alignment.Center
        ) {
            Text(item.name.first().toString(), color = if (isSelected) Color.Black else Color.White, fontSize = 20.sp)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(item.name, color = if (isSelected) Color(0xFFCCFF00) else Color.Gray, fontSize = 12.sp)
    }
}

@Composable
fun AdjustToolButton(item: AdjustItem, isSelected: Boolean, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onClick() }.width(60.dp)) {
        Box(
            modifier = Modifier.size(50.dp).clip(CircleShape)
                .background(if (isSelected) Color(0xFF333333) else Color.Transparent)
                // 🔥 修复点：这里用到了 border，上面必须 import androidx.compose.foundation.border
                .border(1.dp, if (isSelected) Color(0xFFCCFF00) else Color.Gray, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(item.icon, null, tint = if (isSelected) Color(0xFFCCFF00) else Color.White)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(item.name, color = Color.Gray, fontSize = 12.sp)
    }
}

@Composable
fun BottomTabButton(text: String, isSelected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Column(modifier = modifier.clickable { onClick() }.padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text, color = if (isSelected) Color.White else Color.Gray, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
        if (isSelected) Box(modifier = Modifier.padding(top = 4.dp).size(4.dp).clip(CircleShape).background(Color(0xFFCCFF00)))
    }
}