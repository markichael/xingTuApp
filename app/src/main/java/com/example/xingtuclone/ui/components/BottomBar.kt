/**
 * ============================================
 * BottomBar.kt - 底部导航栏组件
 * ============================================
 * 
 * 功能说明：
 * 应用底部的固定导航栏，提供主要功能入口切换
 * 采用 Material Design 3 规范的 NavigationBar 组件
 * 
 * ============================================
 * 设计规范 (Material Design 3)
 * ============================================
 * 
 * 【底部导航栏使用场景】
 * - 适用于 3-5 个顶级目标页面的切换
 * - 用户需要频繁在这些页面间切换
 * - 每个页面具有同等重要性
 * 
 * 【不适合使用底部导航栏的场景】
 * - 少于 3 个目标（建议使用 Tabs）
 * - 超过 5 个目标（建议使用 NavigationDrawer）
 * - 存在明确的层级关系（建议使用 TopAppBar）
 * 
 * 【本应用的导航结构】
 * 1. 修图 (Home) - 主功能页，默认选中
 *    - 所有编辑工具入口
 *    - AI 功能入口
 *    - 相册/相机入口
 * 
 * 2. 灵感 (Star) - 内容发现页
 *    - 推荐模板
 *    - 热门作品
 *    - 编辑教程
 * 
 * 3. 我的 (Person) - 个人中心
 *    - 本地作品
 *    - 个人设置
 *    - 使用统计
 * 
 * ============================================
 * 技术实现细节
 * ============================================
 * 
 * 【NavigationBar vs BottomNavigation】
 * - NavigationBar: Material 3 新组件（本项目使用）
 * - BottomNavigation: Material 2 旧组件（已弃用）
 * 
 * 主要区别：
 * - NavigationBar 默认使用 Surface Tint（更现代）
 * - NavigationBar 选中指示器更圆润
 * - NavigationBar 更好的无障碍支持
 * 
 * 【颜色系统】
 * - containerColor: 容器背景色（黑色）
 * - contentColor: 内容默认颜色（白色）
 * - selectedIconColor: 选中图标颜色（白色）
 * - selectedTextColor: 选中文字颜色（白色）
 * - unselectedIconColor: 未选中图标颜色（灰色）
 * - unselectedTextColor: 未选中文字颜色（灰色）
 * - indicatorColor: 选中指示器颜色（透明，隐藏默认椭圆背景）
 * 
 * ============================================
 * 性能优化说明
 * ============================================
 * 
 * 【为什么使用 Transparent 而非移除 indicator】
 * - NavigationBar 的 indicator 是内置动画的一部分
 * - 直接移除会破坏动画流畅性
 * - 设置为透明既保留动画，又符合设计需求
 * 
 * 【状态管理优化】
 * 当前实现：
 * - selected 状态硬编码（true/false）
 * - onClick 为空实现
 * 
 * 生产环境建议：
 * ```kotlin
 * @Composable
 * fun XingtuBottomBar(
 *     selectedTab: Int,
 *     onTabSelected: (Int) -> Unit
 * ) {
 *     NavigationBar(...) {
 *         tabs.forEachIndexed { index, tab ->
 *             NavigationBarItem(
 *                 selected = selectedTab == index,
 *                 onClick = { onTabSelected(index) },
 *                 ...
 *             )
 *         }
 *     }
 * }
 * ```
 * 
 * ============================================
 * 面试要点
 * ============================================
 * 
 * Q1: 为什么底部导航栏背景是黑色而不是白色？
 * A:
 * - 符合图片编辑应用的设计习惯（如 Adobe、美图秀秀）
 * - 黑色背景让彩色图片更突出
 * - 减少视觉干扰，聚焦内容
 * - 夜间模式友好，护眼
 * 
 * Q2: 如何实现底部导航栏的页面切换？
 * A:
 * - 配合 HomeViewModel 的路由状态管理
 * - onClick 中调用 viewModel.switchTab(index)
 * - 根据 selectedTab 状态渲染不同页面
 * - 使用 AnimatedContent 实现页面切换动画
 * 
 * Q3: indicatorColor 设置为 Transparent 的作用？
 * A:
 * - Material 3 默认会显示圆角矩形选中指示器
 * - 本应用设计风格更简洁，不需要背景高亮
 * - 透明色既隐藏指示器，又保留触摸反馈动画
 * 
 * Q4: 如何处理底部导航栏的状态保存？
 * A:
 * - 使用 rememberSaveable 保存 selectedTab
 * - 配置变更（旋转屏幕）时自动恢复
 * - 配合 Navigation 库可实现完整的后退栈管理
 * 
 * ============================================
 */
package com.example.xingtuclone.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * 星图底部导航栏组件
 * 
 * Composable 函数说明：
 * - @Composable 注解标记该函数为 Compose UI 组件
 * - 函数名采用 PascalCase（大驼峰）命名
 * - 无参数：当前为静态 UI，未集成状态管理
 * 
 * 组件结构：
 * NavigationBar (容器)
 *  └─ NavigationBarItem × 3 (导航项)
 *      ├─ Icon (图标)
 *      └─ Text (标签)
 * 
 * 设计规范遵循：
 * - Material Design 3 底部导航规范
 * - 图标 + 文字双重标识
 * - 选中态明确视觉反馈
 * - 无障碍支持（contentDescription）
 */
@Composable
fun XingtuBottomBar() {
    /**
     * Material 3 底部导航栏容器
     * 
     * 关键属性：
     * - containerColor: 容器背景色
     *   * 设置为黑色（Color.Black）而非默认的 surfaceContainer
     *   * 原因：图片编辑应用惯例，突出内容区域
     * 
     * - contentColor: 内容默认颜色
     *   * 设置为白色（Color.White）确保图标/文字可见
     *   * 作为子组件的默认 tint 颜色
     * 
     * 布局行为：
     * - 自动占据屏幕底部
     * - 高度固定为 80.dp（Material 规范）
     * - 宽度填充父容器
     * - elevation 默认为 3.dp（投影效果）
     */
    NavigationBar(
        containerColor = Color.Black, // 底部背景黑色
        contentColor = Color.White
    ) {
        /**
         * 导航项 1: 修图（首页）
         * 
         * 功能定位：
         * - 应用的主要功能入口
         * - 默认选中状态（selected = true）
         * - 包含所有编辑工具
         * 
         * 关键属性：
         * @param icon 图标组件
         *   - Icons.Default.Home: Material Icons 预定义图标
         *   - contentDescription = null: 因为有 label，无障碍已满足
         * 
         * @param label 文字标签
         *   - "修图": 功能名称，简洁明了
         *   - 选中时文字会加粗（Material 默认行为）
         * 
         * @param selected 选中状态
         *   - true: 当前页面处于选中状态
         *   - 触发选中样式（白色图标+文字）
         * 
         * @param onClick 点击回调
         *   - 当前为空实现：{ }
         *   - 生产环境应调用：viewModel.switchTab(0)
         * 
         * @param colors 颜色配置
         *   - selectedIconColor: 选中时图标颜色（白色）
         *   - selectedTextColor: 选中时文字颜色（白色）
         *   - indicatorColor: 选中指示器颜色（透明，隐藏默认背景）
         *   - unselectedIconColor: 未选中图标颜色（灰色）
         *   - unselectedTextColor: 未选中文字颜色（灰色）
         * 
         * 视觉效果：
         * - 选中态：白色图标 + 白色文字 + 无背景高亮
         * - 未选中：灰色图标 + 灰色文字
         * - 点击时有涟漪动画（Ripple Effect）
         */
        NavigationBarItem(
            icon = { Icon(Icons.Default.Home, contentDescription = null) },
            label = { Text("修图") },
            selected = true,
            onClick = { },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color.White,
                selectedTextColor = Color.White,
                indicatorColor = Color.Transparent, // 去掉选中时的椭圆背景
                unselectedIconColor = Color.Gray,
                unselectedTextColor = Color.Gray
            )
        )
        /**
         * 导航项 2: 灵感（发现页）
         * 
         * 功能定位：
         * - 内容发现和推荐页面
         * - 提供模板、教程、热门作品等
         * - 激发用户创作灵感
         * 
         * 图标选择：
         * - Icons.Default.Star: 星标图标
         * - 寓意：精选内容、灵感闪现
         * 
         * 状态说明：
         * - selected = false: 当前未选中
         * - 显示为灰色图标+文字
         * - 点击后应切换为选中状态
         * 
         * 颜色配置简化：
         * - 只配置 unselectedIconColor 和 unselectedTextColor
         * - 选中颜色继承父级 NavigationBar 的 contentColor
         * - 与第一个导航项的完整配置等效
         */
        NavigationBarItem(
            icon = { Icon(Icons.Default.Star, contentDescription = null) },
            label = { Text("灵感") },
            selected = false,
            onClick = { },
            colors = NavigationBarItemDefaults.colors(
                unselectedIconColor = Color.Gray,
                unselectedTextColor = Color.Gray
            )
        )
        /**
         * 导航项 3: 我的（个人中心）
         * 
         * 功能定位：
         * - 个人资料和设置页面
         * - 本地作品管理
         * - 应用配置和偏好设置
         * 
         * 图标选择：
         * - Icons.Default.Person: 人像图标
         * - 通用的个人中心标识
         * 
         * 交互设计：
         * - 点击后应显示个人中心页面
         * - 包含登录状态、作品列表、设置入口等
         * 
         * 实现建议：
         * 生产环境中应替换为：
         * ```kotlin
         * onClick = { 
         *     viewModel.navigateTo(AppRoute.Profile)
         *     // 或 viewModel.switchTab(2)
         * }
         * ```
         */
        NavigationBarItem(
            icon = { Icon(Icons.Default.Person, contentDescription = null) },
            label = { Text("我的") },
            selected = false,
            onClick = { },
            colors = NavigationBarItemDefaults.colors(
                unselectedIconColor = Color.Gray,
                unselectedTextColor = Color.Gray
            )
        )
    }
}