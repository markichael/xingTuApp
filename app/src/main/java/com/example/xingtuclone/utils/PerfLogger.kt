/**
 * ============================================
 * PerfLogger.kt - 性能监控工具
 * ============================================
 * 功能说明：
 * 相册页面加载性能监控工具，记录关键指标
 * 
 * 监控指标：
 * - TTFR (Time To First Render): 首图渲染时间
 * - Avg Load Time: 平均图片加载时间
 * - P90 Load Time: 90分位数加载时间
 * 
 * 使用场景：
 * - 相册页面加载性能优化
 * - 生成性能报告 (album_perf_report.json)
 * 
 * 优化目标：
 * - TTFR < 300ms
 * - P90 < 200ms
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
 * 追踪图片加载性能，生成性能报告
 */
object PerfLogger {
    private const val TAG = "AlbumPerf"

    private var enterTs: Long = 0L        // 进入相册的时间戳
    private var firstImageTs: Long = 0L   // 首图加载完成时间戳

    private val startMap = ConcurrentHashMap<String, Long>()  // 图片开始加载时间映射
    private val durations = mutableListOf<Long>()              // 所有图片加载时长列表

    /** 开始监控相册页面进入 */
    /** 开始监控相册页面进入 */
    fun startEnter() {
        enterTs = SystemClock.elapsedRealtime()
        firstImageTs = 0L
        startMap.clear()
        durations.clear()
        Log.d(TAG, "enter album screen")
    }

    /** 记录图片开始加载 */
    fun onItemStart(key: String) {
        startMap[key] = SystemClock.elapsedRealtime()
    }

    /** 记录图片加载成功 */
    /** 记录图片加载成功 */
    fun onItemSuccess(key: String, context: Context) {
        val now = SystemClock.elapsedRealtime()
        val st = startMap.remove(key)
        if (st != null) {
            durations.add(now - st)  // 记录加载耗时
        }
        // 记录 TTFR（首图渲染时间）
        if (firstImageTs == 0L) {
            firstImageTs = now
            val ttfr = firstImageTs - enterTs
            Log.d(TAG, "TTFR(ms)=$ttfr")
        }
        // 加载 100 张图后生成报告
        if (durations.size == 100) {
            dump(context)
        }
    }

    /** 生成并保存性能报告 */
    /** 生成并保存性能报告 */
    private fun dump(context: Context) {
        val avg = if (durations.isNotEmpty()) durations.sum() / durations.size else 0L
        val p90 = percentile(90)
        val report = "{" +
                "\"ttfr_ms\":${if (firstImageTs > 0L) firstImageTs - enterTs else 0}," +
                "\"avg_load_ms\":$avg," +
                "\"p90_load_ms\":$p90," +
                "\"samples\":${durations.size}" +
                "}"
        Log.d(TAG, "summary=$report")
        try {
            // 写入 JSON 报告文件
            val f = File(context.cacheDir, "album_perf_report.json")
            f.writeText(report)
            Log.d(TAG, "written ${f.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "write report failed", e)
        }
    }

    /** 计算百分位数 */
    private fun percentile(p: Int): Long {
        if (durations.isEmpty()) return 0L
        val sorted = durations.sorted()
        val idx = ((p / 100.0) * (sorted.size - 1)).toInt()
        return sorted[idx]
    }
}

