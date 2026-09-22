package com.srtxcheats.display

import android.content.Context
import com.srtxcheats.core.ShizukuManager
import com.srtxcheats.utils.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DisplayDensityManager(private val context: Context) {

    val defaultPresets = listOf(320, 360, 400, 420, 440, 480)

    fun getCurrentDensity(): Int {
        return context.resources.displayMetrics.densityDpi
    }

    /**
     * Queries window manager density via Shizuku:
     * Output of 'wm density' is:
     * Physical density: 420
     * Override density: 360 (if overridden)
     */
    suspend fun queryWmDensityViaShizuku(): Int? = withContext(Dispatchers.IO) {
        if (!ShizukuManager.isAuthorized()) return@withContext null

        val res = ShizukuManager.executeCommand("wm density")
        if (res.exitCode != 0) return@withContext null

        try {
            val lines = res.output.lines()
            var overrideDpi: Int? = null
            var physicalDpi: Int? = null

            for (line in lines) {
                if (line.contains("Override density:", ignoreCase = true)) {
                    overrideDpi = line.substringAfter(":").trim().toIntOrNull()
                } else if (line.contains("Physical density:", ignoreCase = true)) {
                    physicalDpi = line.substringAfter(":").trim().toIntOrNull()
                }
            }

            overrideDpi ?: physicalDpi
        } catch (e: Exception) {
            AppLogger.w("Could not parse wm density output: ${res.output}", e)
            null
        }
    }

    fun isSafeDpi(dpi: Int): Boolean {
        return dpi in StretchCalculator.MIN_DPI..StretchCalculator.MAX_DPI
    }

    fun generateRecommendedDpi(currentDpi: Int): List<Int> {
        val list = defaultPresets.toMutableList()
        if (!list.contains(currentDpi)) {
            list.add(currentDpi)
            list.sort()
        }
        return list
    }
}
