package com.srtxcheats.display

import android.content.Context
import android.util.DisplayMetrics
import android.view.WindowManager
import com.srtxcheats.core.ShizukuManager
import com.srtxcheats.utils.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ResolutionPreset(
    val label: String,
    val width: Int,
    val height: Int,
    val description: String
) {
    val resolutionKey: String
        get() = "${width}x${height}"
}

class DisplayResolutionManager(private val context: Context) {

    data class DisplayMetricsInfo(
        val physicalWidth: Int,
        val physicalHeight: Int,
        val currentWidth: Int,
        val currentHeight: Int
    )

    @Suppress("DEPRECATION")
    fun getDisplayMetrics(): DisplayMetricsInfo {
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val realMetrics = DisplayMetrics()
        wm.defaultDisplay.getRealMetrics(realMetrics)

        return DisplayMetricsInfo(
            physicalWidth = realMetrics.widthPixels,
            physicalHeight = realMetrics.heightPixels,
            currentWidth = realMetrics.widthPixels,
            currentHeight = realMetrics.heightPixels
        )
    }

    /**
     * Reads the current active window manager size via Shizuku if authorized:
     * Output of 'wm size' is typically:
     * Physical size: 1080x2400
     * Override size: 720x1600 (if overridden)
     */
    suspend fun queryWmSizeViaShizuku(): Pair<Int, Int>? = withContext(Dispatchers.IO) {
        if (!ShizukuManager.isAuthorized()) return@withContext null

        val res = ShizukuManager.executeCommand("wm size")
        if (res.exitCode != 0) return@withContext null

        try {
            val lines = res.output.lines()
            var overrideSize: Pair<Int, Int>? = null
            var physicalSize: Pair<Int, Int>? = null

            for (line in lines) {
                if (line.contains("Override size:", ignoreCase = true)) {
                    val parts = line.substringAfter(":").trim().split("x")
                    if (parts.size == 2) {
                        overrideSize = Pair(parts[0].trim().toInt(), parts[1].trim().toInt())
                    }
                } else if (line.contains("Physical size:", ignoreCase = true)) {
                    val parts = line.substringAfter(":").trim().split("x")
                    if (parts.size == 2) {
                        physicalSize = Pair(parts[0].trim().toInt(), parts[1].trim().toInt())
                    }
                }
            }

            overrideSize ?: physicalSize
        } catch (e: Exception) {
            AppLogger.w("Could not parse wm size output: ${res.output}", e)
            null
        }
    }

    /**
     * Generates device-adaptive presets derived from this specific device's real hardware dimensions.
     */
    fun generatePresets(physicalWidth: Int, physicalHeight: Int): List<ResolutionPreset> {
        val presets = mutableListOf<ResolutionPreset>()

        // 1. Native / Original (100%)
        presets.add(
            ResolutionPreset(
                label = "Native (${physicalWidth} × ${physicalHeight})",
                width = physicalWidth,
                height = physicalHeight,
                description = "Original 1:1 hardware pixel mapping"
            )
        )

        // 2. 90% Scale (Ultra Stretch)
        val w90 = ((physicalWidth * 0.9f).toInt() / 2) * 2
        val h90 = ((physicalHeight * 0.9f).toInt() / 2) * 2
        presets.add(
            ResolutionPreset(
                label = "Scaled 90% (${w90} × ${h90})",
                width = w90,
                height = h90,
                description = "Balanced aspect ratio stretch"
            )
        )

        // 3. 80% Scale (Competitive 4:3 FOV Stretch)
        val w80 = ((physicalWidth * 0.8f).toInt() / 2) * 2
        val h80 = ((physicalHeight * 0.8f).toInt() / 2) * 2
        presets.add(
            ResolutionPreset(
                label = "Scaled 80% (${w80} × ${h80})",
                width = w80,
                height = h80,
                description = "High FPS boost & wider player hitbox appearance"
            )
        )

        // 4. 75% Scale (Extreme Performance / Waterfall)
        val w75 = ((physicalWidth * 0.75f).toInt() / 2) * 2
        val h75 = ((physicalHeight * 0.75f).toInt() / 2) * 2
        presets.add(
            ResolutionPreset(
                label = "Scaled 75% (${w75} × ${h75})",
                width = w75,
                height = h75,
                description = "Maximum GPU load reduction and aggressive stretch"
            )
        )

        // 5. Standard HD+ 720p baseline if distinct from native
        val hdW = if (physicalWidth < physicalHeight) 720 else 1600
        val hdH = if (physicalWidth < physicalHeight) 1600 else 720
        if (hdW != physicalWidth && hdH != physicalHeight) {
            presets.add(
                ResolutionPreset(
                    label = "HD+ 720p (${hdW} × ${hdH})",
                    width = hdW,
                    height = hdH,
                    description = "Ultra high frame-rate esport preset"
                )
            )
        }

        return presets
    }

    fun isValidResolution(w: Int, h: Int): Boolean {
        return w in StretchCalculator.MIN_WIDTH..StretchCalculator.MAX_WIDTH &&
               h in StretchCalculator.MIN_HEIGHT..StretchCalculator.MAX_HEIGHT
    }
}
