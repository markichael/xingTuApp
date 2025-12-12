/**
 * ============================================
 * MenuGrid.kt - 功能网格组件
 * ============================================
 * 
 * 功能说明：
 * 首页底部的 4 列功能网格显示组件
 * 用于展示所有编辑工具的入口（如美颜、滞镜、裁剪、AI 消除等）
 * 
 * ============================================
 * 设计规范 (Material Design Grid)
 * ============================================
 * 
 * 【网格布局优势】
 * - 信息密度高：单屏展示更多功能
 * - 视觉整齐：格子状排列更整洁
 * - 易于扩展：添加新功能无需改版
 * - 符合直觉：用户熟悉网格式应用界面
 * 
 * 【为什么选择 4 列而不是 3/5 列？】
 * - 3 列：图标过大，信息密度低，浪费空间
 * - 4 列：最佳平衡，图标大小适中，单行可展示 4 个功能
 * - 5 列：图标过小，难以点击，不符合 44dp 最小点击区域
 * 
 * 【用户体验考虑】
 * - 单手操作：4 列布局适合大屏手机单手双侧操作
 * - 点击热区：每个项包含 8.dp padding，扩大点击区域
 * - 视觉反馈：按压时缩放 + 半透明背景
 * 
 * ============================================
 * 技术实现细节
 * ============================================
 * 
 * 【LazyVerticalGrid vs Column + Row】
 * - LazyVerticalGrid（本项目使用）：
 *   * 优势：懒加载，仅渲染可见项，性能更好
 *   * 优势：自动处理滚动，支持大量数据
 *   * 缺点：需要设置 userScrollEnabled = false 禁用内部滚动
 * 
 * - Column + Row：
 *   * 优势：简单直接，适合少量项
 *   * 缺点：项过多时性能下降（全部渲染）
 * 
 * 【GridCells.Fixed vs GridCells.Adaptive】
 * - GridCells.Fixed(4)（本项目使用）：
 *   * 固定 4 列，不管屏幕宽度
 *   * 适合功能数量固定的场景
 * 
 * - GridCells.Adaptive(minSize = 80.dp)：
 *   * 自适应列数，根据屏幕宽度自动计算
 *   * 适合响应式布局，但本项目不需要
 * 
 * 【userScrollEnabled = false 的原因】
 * - 首页使用 Column + verticalScroll 实现整体滚动
 * - 如果 MenuGrid 内部也可滚动，会出现滚动冲突
 * - 禁用内部滚动，由父容器统一管理滚动事件
 * 
 * ============================================
 * 交互设计：按压反馈
 * ============================================
 * 
 * 【InteractionSource 原理】
 * - MutableInteractionSource：收集用户交互事件（点击、按下、拖动等）
 * - collectIsPressedAsState()：将按下状态转为 Compose State
 * - 当按下时 pressed = true，释放时 pressed = false
 * 
 * 【视觉反馈效果】
 * 1. 背景变化：
 *    - 未按下：Color.Transparent（透明）
 *    - 按下：Color(0x11000000)（10% 不透明度的黑色）
 *    - 符合 Material Design 的轻微高亮原则
 * 
 * 2. 缩放动画：
 *    - 未按下：scale = 1f（原始大小）
 *    - 按下：scale = 0.95f（缩小到 95%）
 *    - 模拟实体按钮被按下的感觉
 * 
 * 【indication = null 的作用】
 * - 移除默认的 Ripple 涟漪效果
 * - 因为已经有自定义的背景 + 缩放效果
 * - 避免视觉效果重复叠加
 * 
 * ============================================
 * 性能优化说明
 * ============================================
 * 
 * 1. remember { MutableInteractionSource() }
 *    - 每个 item 都创建独立的 InteractionSource
 *    - remember 保证重组时不会重新创建
 *    - 避免不必要的对象分配
 * 
 * 2. items(menuItems) 而非 itemsIndexed
 *    - 不需要索引，直接使用 item 对象
 *    - 减少不必要的参数传递
 * 
 * 3. contentPadding vs Modifier.padding
 *    - contentPadding：LazyGrid 内容的内边距
 *    - 不会影响 LazyGrid 本身的尺寸
 *    - 滚动时内容与边缘保持距离
 * 
 * ============================================
 * 面试要点
 * ============================================
 * 
 * Q1: 为什么使用 LazyVerticalGrid 而不是 Column + Row？
 * A:
 * - LazyGrid 支持懒加载，仅渲染可见项，性能更好
 * - 支持大量数据场景（虽然本项目功能项不多）
 * - 自动处理项的测量和布局，代码更简洁
 * - 为未来扩展留下空间（如功能项增加到 20+）
 * 
 * Q2: userScrollEnabled = false 的作用？
 * A:
 * - 禁用 LazyGrid 的内部滚动
 * - 由父容器（Column + verticalScroll）统一管理滚动
 * - 避免嵌套滚动带来的交互冲突
 * 
 * Q3: InteractionSource 的作用是什么？
 * A:
 * - 收集用户交互事件（点击、按下、拖动等）
 * - 转为 Compose State，触发 UI 重组
 * - 实现自定义交互效果（背景变化 + 缩放动画）
 * - indication = null 移除默认 Ripple，避免效果重复
 * 
 * Q4: 如何实现更复杂的点击动画？
 * A:
 * - 使用 animateFloatAsState 实现缓动动画
 * - 使用 animateColorAsState 实现颜色渐变
 * - 组合 Modifier.graphicsLayer 实现 3D 旋转/倒影效果
 * 
 * ============================================
 */
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

/**
 * 功能菜单网格组件
 * 
 * Composable 组件说明：
 * - 可复用：通过参数传入不同的菜单项
 * - 与 MenuItem 数据类配合使用
 * - 支持点击回调，实现导航逻辑
 * 
 * 参数设计：
 * @param menuItems 菜单项列表
 *        - 类型：List<MenuItem>
 *        - MenuItem 包含：icon(图标)、title(标题)、route(路由）
 *        - 建议数量：8-16 个（过多需要分类）
 * 
 * @param onItemClick 点击回调
 *        - 类型：(MenuItem) -> Unit
 *        - 默认值：{}（空实现）
 *        - 生产环境应传入：{ item -> viewModel.navigateTo(item.route) }
 * 
 * 使用示例：
 * ```kotlin
 * val menuItems = listOf(
 *     MenuItem(Icons.Default.Face, "美颜", AppRoute.Editor(...)),
 *     MenuItem(Icons.Default.FilterVintage, "滞镜", AppRoute.Editor(...)),
 *     // ...
 * )
 * 
 * MenuGridSection(
 *     menuItems = menuItems,
 *     onItemClick = { item ->
 *         viewModel.navigateTo(item.route)
 *     }
 * )
 * ```
 */
@Composable
fun MenuGridSection(menuItems: List<MenuItem>, onItemClick: (MenuItem) -> Unit = {}) {
    /**
     * LazyVerticalGrid - 懒加载垂直网格布局
     * 
     * 关键属性详解：
     * 
     * columns：列数配置
     * - GridCells.Fixed(4)：固定 4 列
     * - 每列宽度自动计算 = (容器宽度 - padding) / 4
     * - 不随屏幕宽度变化，保证布局一致性
     * 
     * modifier：
     * - Modifier.fillMaxWidth()：填充父容器宽度
     * - 高度自动根据内容计算（LazyGrid 特性）
     * 
     * userScrollEnabled：是否启用内部滚动
     * - false：禁用 LazyGrid 的滚动
     * - 原因：父容器（Column）已经可滚动
     * - 避免嵌套滚动导致的交互冲突（滑动不流畅）
     * 
     * contentPadding：内容内边距
     * - PaddingValues(vertical = 16.dp)：上下各 16.dp
     * - 与外部元素（如顶部按钮）保持间距
     * - 滚动时首尾项不会紧贴边缘
     * 
     * verticalArrangement：垂直排列方式
     * - Arrangement.spacedBy(24.dp)：每行间隔 24.dp
     * - 让网格项有足够的呼吸感
     * - 避免过于紧密引起的视觉疲劳
     * 
     * 性能优化：
     * - LazyGrid 只渲染可见项 + 预加载项
     * - 不可见的项会被回收，节省内存
     * - 预加载 1-2 屏幕的内容，避免滚动时闪烁
     */
    LazyVerticalGrid(
        columns = GridCells.Fixed(4),  // 固定 4 列布局
        modifier = Modifier.fillMaxWidth(),
        userScrollEnabled = false,  // 禁止内部滚动，由父布局控制
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        /**
         * items - 渲染网格项
         * 
         * LazyGrid DSL 说明：
         * - items(menuItems)：遍历 menuItems 列表，为每个 item 生成一个网格单元
         * - { item -> ... }：Lambda 表达式，定义每个单元的布局
         * - item 是 MenuItem 类型，包含 icon、title、route 属性
         * 
         * 为什么不用 itemsIndexed？
         * - 不需要索引，直接使用 item 对象即可
         * - 减少不必要的参数，代码更简洁
         */
        items(menuItems) { item ->
            /**
             * InteractionSource - 交互状态收集器
             * 
             * 作用：
             * - 收集用户与组件的交互事件（点击、按下、拖动等）
             * - 提供各种交互状态的 State 流
             * - 用于实现自定义交互效果
             * 
             * remember 的作用：
             * - 避免每次重组时重新创建 InteractionSource
             * - 保证对象的稳定性，减少内存分配
             * 
             * 为什么每个 item 都需要独立的 InteractionSource？
             * - 每个网格项的交互状态独立
             * - 按下一个不会影响其他项
             */
            val interaction = remember { MutableInteractionSource() }
            
            /**
             * collectIsPressedAsState - 收集按下状态
             * 
             * 返回值：
             * - State<Boolean>：可观察的布尔状态
             * - true：正在按下（手指接触屏幕）
             * - false：未按下（手指离开屏幕）
             * 
             * by 委托：
             * - 简化 State 访问，直接使用 pressed 而非 pressed.value
             * 
             * 自动重组：
             * - pressed 状态变化时自动触发 Recomposition
             * - 更新背景色和缩放比例
             */
            val pressed by interaction.collectIsPressedAsState()
            
            /**
             * Column - 单个网格项的容器
             * 
             * 布局方向：
             * - 垂直排列：图标在上，标题在下
             * - horizontalAlignment.CenterHorizontally：水平居中
             * - verticalArrangement.Center：垂直居中
             * 
             * Modifier 链（按顺序执行）：
             * 
             * 1. .padding(8.dp)：内边距
             *    - 扩大点击区域，让按钮更易点击
             *    - 8.dp 确保与相邻项有足够间隔
             * 
             * 2. .clip(RoundedCornerShape(12.dp))：裁剪圆角
             *    - 圆角半径 12.dp（比按钮的 16.dp 小）
             *    - 必须在 background 之前
             * 
             * 3. .background(...)：条件背景色
             *    - 按下时：Color(0x11000000) 半透明黑色
             *    - 未按下：Color.Transparent 透明
             *    - 0x11000000：17/255 ≈ 6.7% 不透明度，轻微高亮
             * 
             * 4. .scale(...)：缩放动画
             *    - 按下时：0.95f 缩小到 95%
             *    - 未按下：1f 原始大小
             *    - 模拟按钮被按下的物理反馈
             * 
             * 5. .clickable(...)：点击事件
             *    - interactionSource: 传入自定义的 InteractionSource
             *    - indication = null: 移除默认的 Ripple 效果
             *    - 原因：已有自定义背景 + 缩放效果，避免重复
             *    - onClick：调用回调函数，传入当前 item
             * 
             * 视觉效果总结：
             * - 未按下：透明背景 + 原始大小
             * - 按下时：半透明黑色背景 + 缩小 5%
             * - 释放时：自动恢复到初始状态
             */
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .padding(8.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (pressed) Color(0x11000000) else Color.Transparent)
                    .scale(if (pressed) 0.95f else 1f)  // 按压时缩放效果
                    .clickable(interactionSource = interaction, indication = null) { 
                        onItemClick(item) 
                    }
            ) {
                /**
                 * Icon - 功能图标
                 * 
                 * @param imageVector 矢量图标
                 *   - 从 MenuItem.icon 获取
                 *   - Material Icons 或自定义矢量图
                 * 
                 * @param contentDescription 无障碍描述
                 *   - 使用 item.title 作为描述
                 *   - 屏幕阅读器会读取：“美颜 图标”
                 * 
                 * @param modifier.size 图标尺寸
                 *   - 32.dp 比按钮中的 24.dp 更大
                 *   - 网格项空间充足，使用较大图标提升可识别性
                 * 
                 * @param tint 图标颜色
                 *   - Color.Black 黑色
                 *   - 与白色背景形成对比
                 */
                Icon(
                    imageVector = item.icon,
                    contentDescription = item.title,
                    modifier = Modifier.size(32.dp),
                    tint = Color.Black
                )
                /**
                 * Text - 功能标题
                 * 
                 * @param text 文字内容
                 *   - 从 MenuItem.title 获取
                 *   - 建议 2-4 个汉字（如「美颜」、「AI消除」）
                 * 
                 * @param fontSize 字体大小
                 *   - 12.sp 较小的字号
                 *   - 适合作为辅助标签，与大图标配合
                 * 
                 * @param color 文字颜色
                 *   - Color.Gray 灰色
                 *   - 弱化文字，突出图标
                 *   - 符合视觉层级：图标（主）> 文字（次）
                 * 
                 * @param lineHeight 行高
                 *   - 20.sp 稍大于 fontSize (12.sp)
                 *   - 增加行间距，防止文字过于紧密
                 *   - 提升多行文字的可读性（虽然单行为主）
                 * 
                 * 注意事项：
                 * - 未设置 maxLines，默认自动换行
                 * - 如果标题过长，会导致布局错乱
                 * - 建议添加：maxLines = 1, overflow = TextOverflow.Ellipsis
                 */
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
