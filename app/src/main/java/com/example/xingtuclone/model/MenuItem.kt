/**
 * ============================================
 * MenuItem.kt - 菜单项数据模型
 * ============================================
 * 功能说明：
 * 首页底部功能网格的数据模型
 * 
 * 字段：
 * - title: 菜单标题（如"批量修图"、"魔法消除"）
 * - icon: Material Icons 图标
 * ============================================
 */
package com.example.xingtuclone.model

import androidx.compose.ui.graphics.vector.ImageVector

/**
 * 菜单项数据类
 * 用于首页底部功能网格的显示
 */
data class MenuItem(
    val title: String,      // 菜单标题
    val icon: ImageVector   // Material Icons 图标
)