// ui/components/MenuGrid.kt
package com.example.xingtuclone.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.xingtuclone.model.MenuItem

@Composable
fun MenuGridSection(menuItems: List<MenuItem>, onItemClick: (MenuItem) -> Unit = {}) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(4), // 一行4个
        modifier = Modifier
            .fillMaxWidth(),
        //.height(200.dp), // 如果是在Scrollable Column里，不要设置固定高度，或者用非Lazy的Grid
        userScrollEnabled = false, // 禁止内部滚动，由父布局滚动
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        items(menuItems) { item ->
            val interaction = remember { MutableInteractionSource() }
            val pressed by interaction.collectIsPressedAsState()
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .padding(8.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (pressed) Color(0x11000000) else Color.Transparent)
                    .scale(if (pressed) 0.95f else 1f)
                    .clickable(interactionSource = interaction, indication = null) { onItemClick(item) }
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = item.title,
                    modifier = Modifier.size(32.dp),
                    tint = Color.Black
                )
                Text(
                    text = item.title,
                    fontSize = 12.sp,
                    color = Color.Gray,
                    lineHeight = 20.sp
                )
            }
        }
    }
}
