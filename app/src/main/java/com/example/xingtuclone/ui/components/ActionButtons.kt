/**
 * ============================================
 * ActionButtons.kt - 通用按钮组件
 * ============================================
 * 
 * 功能说明：
 * 提供可复用的大型行动按钮 (Call-to-Action Button) 组件
 * 用于首页的主要功能入口，强调视觉引导
 * 
 * ============================================
 * 使用场景
 * ============================================
 * 
 * 【首页主要入口】
 * 1. "导入照片": 从相册选择图片
 *    - 背景色：蓝色（主要操作）
 *    - 图标：Icons.Default.Photo
 * 
 * 2. "相机": 拍照并编辑
 *    - 背景色：绿色（次要操作）
 *    - 图标：Icons.Default.CameraAlt
 * 
 * 3. "AI修人像": AI 美颜功能
 *    - 背景色：紫色/渐变色（特色功能）
 *    - 图标：Icons.Default.Face
 * 
 * 4. "拼图": 创意拼图功能
 *    - 背景色：橙色（边缘功能）
 *    - 图标：Icons.Default.GridOn
 * 
 * ============================================
 * 设计原则 (Material Design)
 * ============================================
 * 
 * 【按钮层级】
 * - Filled Button：主要操作（最高强调）
 * - Outlined Button：次要操作（中等强调）
 * - Text Button：辅助操作（最低强调）
 * - BigActionButton：本组件，超大尺寸（特殊强调）
 * 
 * 【适用场景】
 * - 应用首页的核心功能入口
 * - 需要特别强调的操作（如「开始编辑」）
 * - 引导新用户的主要流程
 * 
 * 【不适用场景】
 * - 一般功能按钮（应使用标准 Button）
 * - 列表项中的操作（应使用 IconButton）
 * - 底部导航（应使用 NavigationBarItem）
 * 
 * ============================================
 * 技术实现细节
 * ============================================
 * 
 * 【布局结构】
 * Row (容器)
 *  ├─ Icon (图标)
 *  ├─ Spacer (间距)
 *  └─ Text (文字)
 * 
 * 【Modifier 链式调用顺序】
 * 注意：Modifier 的顺序很重要，不同顺序会产生不同效果
 * 
 * 1. clip(RoundedCornerShape)：先裁剪圆角
 *    - 必须在 background 之前
 *    - 否则背景色不会被裁剪，出现方角
 * 
 * 2. background(backgroundColor)：再添加背景色
 *    - 背景色会填充在已裁剪的形状内
 * 
 * 3. clickable { }：最后添加点击事件
 *    - 点击区域 = 圆角矩形区域
 *    - 涟漪效果也会被裁剪成圆角
 * 
 * 4. padding(vertical)：内边距
 *    - 在 clickable 之后，扩大点击区域
 *    - vertical = 24.dp：上下各 24dp，让按钮更容易点击
 * 
 * 错误示例（常见错误）：
 * ```kotlin
 * // ❌ 错误：background 在 clip 之前
 * Modifier
 *     .background(color)  // 背景不会被裁剪
 *     .clip(RoundedCornerShape(16.dp))
 *     .clickable { }
 * 
 * // ✅ 正确：clip 在 background 之前
 * Modifier
 *     .clip(RoundedCornerShape(16.dp))
 *     .background(color)  // 背景被裁剪成圆角
 *     .clickable { }
 * ```
 * 
 * 【性能优化】
 * - 使用 Modifier 而非 Box 嵌套：减少组件层级
 * - Row 内部直接排列：避免 ConstraintLayout 的开销
 * - 固定尺寸图标：24.dp，避免动态计算
 * 
 * ============================================
 * 面试要点
 * ============================================
 * 
 * Q1: 为什么使用 Row 而不是 Column？
 * A:
 * - Row 水平排列：图标在左，文字在右
 * - 符合阅读习惯（从左到右）
 * - 节省垂直空间，按钮更紧凑
 * 
 * Q2: Modifier.clip 必须在 background 之前吗？
 * A:
 * - 是的，clip 会裁剪后续所有绘制内容
 * - 如果 background 在前，背景不会被裁剪
 * - 与 CSS 的 border-radius + background 顺序相反
 * 
 * Q3: 为什么 contentDescription = null？
 * A:
 * - 装饰性图标，旁边有文字说明
 * - 屏幕阅读器会读取文字，无需重复描述
 * - 如果单独使用图标，则必须提供 contentDescription
 * 
 * Q4: 如何添加按钮点击动画？
 * A:
 * - 方法1：使用 Modifier.clickable（默认有 Ripple 效果）
 * - 方案2：自定义 InteractionSource + scale 动画
 * - 方案3：使用 animateFloatAsState 实现缩放
 * 
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
 * 大型行动按钮组件 (Big Call-to-Action Button)
 * 
 * Composable 组件特性：
 * - 可复用：通过参数自定义样式和行为
 * - 状态无关：纯 UI 组件，不管理业务状态
 * - 高度可配置：6 个参数实现灵活自定义
 * 
 * 参数设计原则：
 * - 必选参数：text, icon, backgroundColor, contentColor, onClick
 *   * 这些是按钮的核心属性，必须明确指定
 * - 可选参数：modifier
 *   * 提供默认值 Modifier，允许调用方添加额外修饰
 * 
 * @param text 按钮文字
 *        - 建议 2-6 个汉字（如「导入照片」）
 *        - 过长文字会占据太多空间，影响布局
 * 
 * @param icon 按钮图标
 *        - 使用 Material Icons：Icons.Default.XXX
 *        - 也可使用自定义矢量图 (ImageVector)
 *        - 尺寸固定为 24.dp（在 Icon 组件中设置）
 * 
 * @param backgroundColor 背景色
 *        - 建议使用鲜艳颜色强调主要操作
 *        - 示例：Color(0xFF1E90FF) 蓝色、Color(0xFF32CD32) 绿色
 * 
 * @param contentColor 内容色（图标+文字）
 *        - 通常使用 Color.White 确保对比度
 *        - 必须与 backgroundColor 有足够对比（WCAG AA 标准）
 * 
 * @param modifier 修饰符
 *        - 调用方可传入额外的 Modifier
 *        - 常用：Modifier.fillMaxWidth()（填充容器宽度）
 *        - 常用：Modifier.weight(1f)（在 Row 中占比）
 * 
 * @param onClick 点击回调
 *        - Lambda 表达式，无参数无返回值
 *        - 实现示例：{ viewModel.navigateTo(AppRoute.Gallery) }
 * 
 * 使用示例：
 * ```kotlin
 * BigActionButton(
 *     text = "导入照片",
 *     icon = Icons.Default.PhotoLibrary,
 *     backgroundColor = Color(0xFF1E90FF),
 *     contentColor = Color.White,
 *     modifier = Modifier.fillMaxWidth(),
 *     onClick = { viewModel.openGallery() }
 * )
 * ```
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
    /**
     * Row 水平布局容器
     * 
     * 关键属性说明：
     * 
     * modifier 链（顺序至关重要）：
     * 
     * 1. modifier：调用方传入的额外修饰符
     *    - 先应用调用方的 Modifier
     *    - 常见：fillMaxWidth() 或 weight(1f)
     * 
     * 2. .clip(RoundedCornerShape(16.dp))：裁剪成圆角矩形
     *    - 圆角半径 16.dp（Material Design 推荐值）
     *    - 必须在 background 之前，否则背景不会被裁剪
     *    - 后续的所有绘制（背景、涟漪）都会被裁剪
     * 
     * 3. .background(backgroundColor)：添加背景色
     *    - 填充在已裁剪的圆角区域内
     *    - 颜色通过参数传入，支持动态配置
     * 
     * 4. .clickable { onClick() }：添加点击交互
     *    - 点击区域 = 圆角矩形区域
     *    - 默认有 Ripple 涟漪效果（Material Design）
     *    - 涟漪也会被裁剪成圆角（因为 clip 在前）
     * 
     * 5. .padding(vertical = 24.dp)：内边距
     *    - 只设置上下内边距，不设置左右
     *    - 24.dp 让按钮高度足够，更易点击（符合 44dp 最小点击区域规范）
     *    - vertical padding 在 clickable 之后，扩大点击区域
     * 
     * horizontalArrangement：水平排列方式
     * - Arrangement.Center：子组件居中排列
     * - 图标+文字作为整体居中
     * 
     * verticalAlignment：垂直对齐方式
     * - Alignment.CenterVertically：子组件垂直居中
     * - 确保图标和文字垂直对齐
     */
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))         // 圆角边框
            .background(backgroundColor)             // 背景色
            .clickable { onClick() }                 // 点击事件
            .padding(vertical = 24.dp),
        horizontalArrangement = Arrangement.Center,  // 水平居中
        verticalAlignment = Alignment.CenterVertically
    ) {
        /**
         * Icon 图标组件
         * 
         * @param imageVector 矢量图标
         *   - 使用 Material Icons：Icons.Default.XXX
         *   - 矢量格式：无限缩放不失真，体积小
         * 
         * @param contentDescription 无障碍描述
         *   - 设置为 null：因为装饰性图标，旁边有文字
         *   - 屏幕阅读器会读取文字，无需重复描述
         * 
         * @param tint 颜色色调
         *   - 使用 contentColor 参数保证与文字一致
         *   - 默认会使用 LocalContentColor，但明确指定更好
         * 
         * @param modifier.size 图标尺寸
         *   - 24.dp 是 Material Design 标准图标尺寸
         *   - 与文字大小 (18.sp) 比例协调
         */
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(24.dp)
        )
        /**
         * Spacer 间距组件
         * 
         * 作用：
         * - 在图标和文字之间添加 8.dp 的水平间距
         * - 让元素之间有透气感，不会过于紧密
         * 
         * 为什么使用 Spacer 而不是 padding？
         * - Spacer 更语义化：明确表示“这里是间距”
         * - padding 是组件内边距，Spacer 是独立的间距元素
         * - 方便调试：可以单独修改 Spacer 宽度
         * 
         * 8.dp 的选择依据：
         * - Material Design 推荐的小间距（4.dp 倍数）
         * - 4dp 太小，12dp 太大，8dp 恰到好处
         */
        Spacer(modifier = Modifier.width(8.dp))
        /**
         * Text 文字组件
         * 
         * @param text 文字内容
         *   - 通过参数传入，支持动态配置
         *   - 建议使用字符串资源：stringResource(R.string.xxx)
         * 
         * @param color 文字颜色
         *   - 使用 contentColor 参数保证与图标一致
         *   - 必须与 backgroundColor 有足够对比度
         * 
         * @param fontWeight 字体粗细
         *   - FontWeight.Bold：加粗，强调重要性
         *   - 符合行动按钮的设计规范
         * 
         * @param fontSize 字体大小
         *   - 18.sp：较大的字号，确保可读性
         *   - sp 单位：支持系统字体缩放（无障碍）
         *   - 对比：常规按钮 14-16sp，本组件 18sp 更突出
         * 
         * 性能说明：
         * - Text 组件自动优化文字测量和排版
         * - 不需要手动设置 maxLines 或 overflow（单行文字）
         */
        Text(
            text = text,
            color = contentColor,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )
    }
}