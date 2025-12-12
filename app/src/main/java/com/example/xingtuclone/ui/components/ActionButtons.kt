/**
 * ============================================
 * ActionButtons.kt - 通用按钮组件
 * ============================================
 * 功能说明：
 * 提供可复用的大型行动按钮组件
 * 
 * 使用场景：
 * - 首页的"导入照片"、"相机"、"AI修人像"、"拼图"等按钮
 * - 支持自定义背景色、文字色、图标
 * ============================================
 */
package com.example.xingtuclone.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 大型行动按钮组件
 * 
 * @param text 按钮文字
 * @param icon 按钮图标
 * @param backgroundColor 背景色
 * @param contentColor 内容色（图标+文字）
 * @param modifier 修饰符
 * @param onClick 点击回调
 */
@Composable
fun BigActionButton(
    text: String,
    icon: ImageVector,
    backgroundColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))         // 圆角边框
            .background(backgroundColor)             // 背景色
            .clickable { onClick() }                 // 点击事件
            .padding(vertical = 24.dp),
        horizontalArrangement = Arrangement.Center,  // 水平居中
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 图标
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        // 文字
        Text(
            text = text,
            color = contentColor,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )
    }
}