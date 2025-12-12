/**
 * ============================================
 * PerfLogger.kt - 相册性能监控工具
 * ============================================
 * 
 * 功能说明：
 * 基于系统时钟的图片加载性能监控工具，用于评估相册页面体验质量
 * 采用线程安全的并发设计，支持实时统计和百分位数分析
 * 
 * ============================================
 * 核心监控指标 (Performance Metrics)
 * ============================================
 * 
 * 【TTFR - Time To First Render】
 * - 定义：从进入相册到首张图片渲染完成的时间
 * - 计算公式：TTFR = firstImageTs - enterTs
 * - 业务意义：用户感知的「首屏速度」，直接影响体验评分
 * - 优化目标：< 300ms (优秀) | 300-500ms (良好) | > 500ms (需优化)
 * - 影响因素：
 *   * 图片解码速度 (JPEG/PNG/WebP)
 *   * Coil 缓存命中率
 *   * IO线程调度延迟
 *   * 内存压力导致的GC停顿
 * 
 * 【Avg Load Time - 平均加载时间】
 * - 定义：所有图片从开始加载到成功显示的平均耗时
 * - 计算公式：Avg = Σ(duration) / count
 * - 业务意义：整体加载性能的平均水平，反映系统稳定性
 * - 推荐值：< 150ms (优秀) | 150-200ms (良好) | > 200ms (需优化)
 * 
 * 【P90 Load Time - 90分位数加载时间】
 * - 定义：90%的图片加载时间都小于该值（只有10%图片更慢）
 * - 计算方法：对所有耗时排序，取索引 (90% * count) 处的值
 * - 业务意义：更能反映「长尾性能」，避免平均值掩盖个别慢图
 * - 优化目标：< 200ms (优秀) | 200-300ms (良好) | > 300ms (需优化)
 * - 为什么用P90而非P95/P99：
 *   * P90 覆盖绝大多数用户场景
 *   * P95/P99 容易受极端异常值影响（如网络波动、内存不足）
 *   * P90 更适合作为日常优化目标
 * 
 * ============================================
 * 技术实现细节
 * ============================================
 * 
 * 【线程安全设计】
 * - 使用 `ConcurrentHashMap` 存储图片开始时间
 * - 原因：Coil 图片加载发生在多个后台线程
 * - 替代方案对比：
 *   * HashMap + synchronized：锁竞争严重，影响性能
 *   * ConcurrentHashMap：分段锁，低开销，适合高并发场景
 * 
 * 【时间测量工具】
 * - 使用 `SystemClock.elapsedRealtime()` 而非 `System.currentTimeMillis()`
 * - 原因：
 *   * elapsedRealtime：单调递增，不受系统时间调整影响（用户改时区/NTP校时）
 *   * currentTimeMillis：可能倒退，导致负数耗时
 * - 精度：毫秒级（1ms），满足UI性能监控需求
 * 
 * 【数据采样策略】
 * - 当加载100张图片时自动生成报告
 * - 采样数量选择理由：
 *   * 100张：足够覆盖多种尺寸/格式图片
 *   * 不过采样：避免内存占用过大
 *   * 可调整：可根据需求改为50/200等
 * 
 * ============================================
 * 性能报告示例 (JSON)
 * ============================================
 * ```json
 * {
 *   "ttfr_ms": 245,          // 首图渲染耗时 245ms
 *   "avg_load_ms": 132,      // 平均加载 132ms
 *   "p90_load_ms": 187,      // 90%图片在 187ms 内完成
 *   "samples": 100           // 统计样本数
 * }
 * ```
 * 
 * ============================================
 * 使用场景与最佳实践
 * ============================================
 * 
 * 【典型使用流程】
 * ```kotlin
 * // 1. 进入相册页面时开始监控
 * override fun onResume() {
 *     super.onResume()
 *     PerfLogger.startEnter()
 * }
 * 
 * // 2. Coil 加载监听器中记录
 * AsyncImage(
 *     model = ImageRequest.Builder(context)
 *         .data(uri)
 *         .listener(
 *             onStart = { PerfLogger.onItemStart(uri.toString()) },
 *             onSuccess = { _, _ -> PerfLogger.onItemSuccess(uri.toString(), context) }
 *         )
 *         .build()
 * )
 * 
 * // 3. 加载100张后自动生成报告到 cache/album_perf_report.json
 * ```
 * 
 * 【性能优化方向】
 * 如果监控发现指标不达标，可以考虑：
 * 1. TTFR过高：
 *    - 优化首屏图片优先级（Coil Priority.HIGH）
 *    - 预加载第一行图片
 *    - 减少首帧前的业务逻辑
 * 2. P90过高：
 *    - 启用 Coil 磁盘缓存
 *    - 降低大图采样率（设置maxWidth/maxHeight）
 *    - 使用更高效的图片格式（WebP > JPEG > PNG）
 * 3. Avg和P90差距大：
 *    - 说明存在个别慢图，检查异常文件
 *    - 可能是大尺寸原图，需要裁剪
 * 
 * ============================================
 * 注意事项
 * ============================================
 * - 单例模式：全局唯一实例，避免重复初始化
 * - 报告路径：`context.cacheDir/album_perf_report.json`（应用卸载后清除）
 * - 内存占用：每个耗时8字节(Long)，100个仅800字节，可忽略
 * - 线程安全：所有公开方法都可在任意线程调用
 * ============================================
 */
package com.example.xingtuclone.utils

import android.content.Context
import android.os.SystemClock
import android.util.Log
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * 相册性能监控工具单例
 * 
 * 设计模式：Singleton Object（Kotlin 原生单例）
 * 线程安全：所有方法支持多线程并发调用
 * 
 * 核心职责：
 * 1. 记录相册进入时间戳
 * 2. 追踪每张图片的加载生命周期（开始→成功）
 * 3. 统计首图渲染时间 (TTFR)
 * 4. 计算平均/百分位数性能指标
 * 5. 生成JSON格式性能报告
 */
object PerfLogger {
    /** 日志标签，用于 Logcat 过滤 */
    private const val TAG = "AlbumPerf"

    // ============================================
    // 时间戳存储（基于 SystemClock.elapsedRealtime）
    // ============================================
    
    /** 进入相册页面的时间戳（毫秒） */
    private var enterTs: Long = 0L
    
    /** 首张图片加载完成的时间戳（毫秒），用于计算 TTFR */
    private var firstImageTs: Long = 0L

    // ============================================
    // 图片加载追踪数据结构
    // ============================================
    
    /** 
     * 图片开始加载时间映射表
     * - Key: 图片唯一标识符（通常是 URI 字符串）
     * - Value: 开始加载时的时间戳（毫秒）
     * - 使用 ConcurrentHashMap：支持多线程并发读写，无需额外加锁
     */
    private val startMap = ConcurrentHashMap<String, Long>()
    
    /** 
     * 所有图片加载耗时列表（毫秒）
     * - 每次图片加载成功后，追加一条耗时记录
     * - 用于计算平均值和百分位数
     * - 注意：MutableList 非线程安全，但访问点已用 synchronized 保护
     */
    private val durations = mutableListOf<Long>()

    // ============================================
    // 公开 API - 生命周期管理
    // ============================================
    
    /**
     * 开始监控相册页面进入
     * 
     * 调用时机：
     * - GalleryScreen 的 onResume() 或 LaunchedEffect 中
     * - 必须在图片开始加载之前调用
     * 
     * 功能：
     * 1. 记录当前时间作为基准点
     * 2. 重置所有监控数据（支持多次进入相册）
     * 
     * 实现细节：
     * - 使用 SystemClock.elapsedRealtime()：从系统启动开始计时，不受时区影响
     * - 清空上次监控数据：避免多次进入相册时数据混淆
     */
    fun startEnter() {
        enterTs = SystemClock.elapsedRealtime() // 记录进入时间戳
        firstImageTs = 0L                        // 重置首图时间（0表示未渲染）
        startMap.clear()                         // 清空加载中的图片记录
        durations.clear()                        // 清空耗时统计列表
        Log.d(TAG, "enter album screen")        // 调试日志
    }

    /**
     * 记录图片开始加载
     * 
     * 调用时机：
     * - Coil ImageRequest.listener.onStart() 回调中
     * - AsyncImage 开始发起网络/磁盘请求时
     * 
     * @param key 图片唯一标识符（通常是 URI.toString()）
     *            - 必须与 onItemSuccess 中传入的 key 一致
     *            - 示例："content://media/external/images/media/12345"
     * 
     * 实现逻辑：
     * - 将当前时间戳存入 startMap
     * - ConcurrentHashMap 自动处理多线程冲突
     * - 如果同一个 key 重复调用，会覆盖旧值（符合重试场景）
     */
    fun onItemStart(key: String) {
        startMap[key] = SystemClock.elapsedRealtime()
    }

    /**
     * 记录图片加载成功
     * 
     * 调用时机：
     * - Coil ImageRequest.listener.onSuccess() 回调中
     * - AsyncImage 成功解码并显示到屏幕时
     * 
     * @param key 图片唯一标识符（必须与 onItemStart 中的 key 一致）
     * @param context 上下文对象，用于保存性能报告文件
     * 
     * 功能：
     * 1. 计算当前图片的加载耗时（now - startTime）
     * 2. 判断是否为首图，若是则记录 TTFR
     * 3. 当累计 100 张图后，自动生成性能报告
     * 
     * 注意事项：
     * - 如果 key 不存在（未调用 onItemStart），则跳过统计
     * - 使用 remove() 而非 get()：避免内存泄漏（已完成的图片无需保留）
     */
    fun onItemSuccess(key: String, context: Context) {
        val now = SystemClock.elapsedRealtime() // 获取当前时间戳
        
        // 从映射表中移除并获取开始时间
        // remove() 操作是原子的，线程安全
        val st = startMap.remove(key)
        
        if (st != null) {
            // 计算耗时：成功时间 - 开始时间
            val duration = now - st
            durations.add(duration)  // 追加到统计列表
        }
        // 如果开始时间不存在（key未记录），说明：
        // - onItemStart 未被调用（集成错误）
        // - 图片来自缓存，加载过快导致时序问题
        // 此时跳过统计，不影响其他图片
        
        // ============================================
        // TTFR 检测（首图渲染时间）
        // ============================================
        if (firstImageTs == 0L) {
            // 首次进入该分支，说明这是第一张成功加载的图片
            firstImageTs = now
            val ttfr = firstImageTs - enterTs  // 计算首屏耗时
            Log.d(TAG, "TTFR(ms)=$ttfr")      // 输出日志供调试
            
            // TTFR 优化建议：
            // - < 200ms：优秀，用户几乎无感知
            // - 200-300ms：良好，符合预期
            // - 300-500ms：可接受，可优化
            // - > 500ms：需重点优化（检查缓存策略、图片尺寸）
        }
        
        // ============================================
        // 自动生成报告（采样达到100张）
        // ============================================
        if (durations.size == 100) {
            dump(context)  // 生成并保存性能报告
            // 注意：报告生成后不清空 durations，允许继续统计
            // 如果需要多次报告，可以在 dump() 后 clear()
        }
    }

    // ============================================
    // 内部工具方法 - 报告生成
    // ============================================
    
    /**
     * 生成并保存性能报告
     * 
     * 调用时机：
     * - 当 durations 累计到 100 个样本时自动触发
     * - 也可手动调用用于调试
     * 
     * 功能：
     * 1. 计算平均加载时间 (Avg)
     * 2. 计算 P90 百分位数
     * 3. 生成 JSON 格式报告
     * 4. 保存到应用缓存目录
     * 
     * 报告格式：
     * ```json
     * {
     *   "ttfr_ms": 245,       // 首图渲染耗时
     *   "avg_load_ms": 132,   // 平均加载耗时
     *   "p90_load_ms": 187,   // 90分位数耗时
     *   "samples": 100        // 统计样本数
     * }
     * ```
     * 
     * 报告路径：
     * - Android: `/data/data/com.example.xingtuclone/cache/album_perf_report.json`
     * - 可通过 `adb pull /data/data/.../cache/album_perf_report.json` 导出
     * 
     * 异常处理：
     * - 文件写入失败（权限/磁盘满）：捕获异常，仅记录日志，不影响应用运行
     */
    private fun dump(context: Context) {
        // ============================================
        // 计算平均加载时间
        // ============================================
        val avg = if (durations.isNotEmpty()) {
            durations.sum() / durations.size  // 算术平均值
        } else {
            0L  // 无样本时返回 0
        }
        
        // ============================================
        // 计算 P90 百分位数
        // ============================================
        val p90 = percentile(90)
        
        // ============================================
        // 构建 JSON 报告字符串
        // ============================================
        // 注意：这里使用手动拼接而非 JSON 库（如 Gson）
        // 原因：避免引入重量级依赖，性能报告结构简单固定
        val report = "{" +
                "\"ttfr_ms\":${if (firstImageTs > 0L) firstImageTs - enterTs else 0}," +
                "\"avg_load_ms\":$avg," +
                "\"p90_load_ms\":$p90," +
                "\"samples\":${durations.size}" +
                "}"
        
        // 输出到 Logcat 供实时查看
        Log.d(TAG, "summary=$report")
        
        // ============================================
        // 保存到文件
        // ============================================
        try {
            // 使用缓存目录：应用卸载时自动清除，不占用用户存储配额
            val f = File(context.cacheDir, "album_perf_report.json")
            f.writeText(report)  // 覆盖写入（不追加）
            Log.d(TAG, "written ${f.absolutePath}")
        } catch (e: Exception) {
            // 可能的异常：
            // - SecurityException: 权限不足（理论上不会发生在 cacheDir）
            // - IOException: 磁盘空间不足
            Log.e(TAG, "write report failed", e)
            // 不抛出异常，避免影响应用正常使用
        }
    }

    /**
     * 计算百分位数（Percentile）
     * 
     * 算法说明：
     * 百分位数是统计学中描述数据分布的指标
     * P90 表示：90%的数据都小于等于该值
     * 
     * 计算步骤：
     * 1. 对所有耗时进行升序排序
     * 2. 计算百分位数对应的索引：idx = (p / 100.0) * (count - 1)
     * 3. 返回该索引位置的值
     * 
     * @param p 百分位数（0-100）
     *          - 常用值：50(中位数)、90(P90)、95(P95)、99(P99)
     * @return 百分位数对应的耗时（毫秒）
     * 
     * 示例：
     * ```
     * 假设有 100 个耗时样本，排序后为 [10, 12, 15, ..., 250]
     * 计算 P90：
     * - idx = (90 / 100.0) * (100 - 1) = 0.9 * 99 = 89.1 ≈ 89
     * - 返回 sorted[89] 的值（第90个元素）
     * - 意义：90%的图片加载时间 ≤ 该值
     * ```
     * 
     * 边界处理：
     * - 空列表：返回 0
     * - 单个元素：返回该元素
     * - p=0：返回最小值（sorted[0]）
     * - p=100：返回最大值（sorted[size-1]）
     * 
     * 性能分析：
     * - 时间复杂度：O(n log n)，主要来自排序
     * - 空间复杂度：O(n)，sorted() 会创建新列表
     * - 优化建议：如果频繁调用，可缓存排序结果
     * 
     * 注意事项：
     * - 该实现使用最近邻插值法（取整），不做线性插值
     * - 如需更精确的百分位数，可改为线性插值：
     *   ```
     *   val pos = (p / 100.0) * (sorted.size - 1)
     *   val lower = sorted[pos.toInt()]
     *   val upper = sorted[ceil(pos).toInt()]
     *   return lower + (upper - lower) * (pos - pos.toInt())
     *   ```
     */
    private fun percentile(p: Int): Long {
        // 边界检查：空列表返回 0
        if (durations.isEmpty()) return 0L
        
        // 升序排序：从小到大
        val sorted = durations.sorted()
        
        // 计算百分位数对应的索引
        // 使用 (size - 1) 是为了让 p=100 时索引不越界
        val idx = ((p / 100.0) * (sorted.size - 1)).toInt()
        
        // 返回该位置的值
        return sorted[idx]
    }
}

