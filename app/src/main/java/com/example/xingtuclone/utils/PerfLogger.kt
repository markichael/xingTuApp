package com.example.xingtuclone.utils

import android.content.Context
import android.os.SystemClock
import android.util.Log
import java.io.File
import java.util.concurrent.ConcurrentHashMap

object PerfLogger {
    private const val TAG = "AlbumPerf"

    private var enterTs: Long = 0L
    private var firstImageTs: Long = 0L

    private val startMap = ConcurrentHashMap<String, Long>()
    private val durations = mutableListOf<Long>()

    fun startEnter() {
        enterTs = SystemClock.elapsedRealtime()
        firstImageTs = 0L
        startMap.clear()
        durations.clear()
        Log.d(TAG, "enter album screen")
    }

    fun onItemStart(key: String) {
        startMap[key] = SystemClock.elapsedRealtime()
    }

    fun onItemSuccess(key: String, context: Context) {
        val now = SystemClock.elapsedRealtime()
        val st = startMap.remove(key)
        if (st != null) {
            durations.add(now - st)
        }
        if (firstImageTs == 0L) {
            firstImageTs = now
            val ttfr = firstImageTs - enterTs
            Log.d(TAG, "TTFR(ms)=$ttfr")
        }
        if (durations.size == 100) {
            dump(context)
        }
    }

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
            val f = File(context.cacheDir, "album_perf_report.json")
            f.writeText(report)
            Log.d(TAG, "written ${f.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "write report failed", e)
        }
    }

    private fun percentile(p: Int): Long {
        if (durations.isEmpty()) return 0L
        val sorted = durations.sorted()
        val idx = ((p / 100.0) * (sorted.size - 1)).toInt()
        return sorted[idx]
    }
}

