package com.srtxcheats.display

enum class StretchMode {
    HORIZONTAL,
    VERTICAL,
    BOTH
}

data class StretchConfiguration(
    val targetWidth: Int,
    val targetHeight: Int,
    val targetDensity: Int,
    val orientation: String,
    val mode: StretchMode
) {
    val resolutionLabel: String
        get() = "${targetWidth} × ${targetHeight}"
}

/**
 * StretchCalculator:
 * Deterministic calculation engine for system-level display scaling via Shizuku/ADB wm size & density.
 *
 * Mathematical Principles:
 * 1. Aspect Ratio Scaling:
 *    - In Portrait: Standard mobile displays range from 19.5:9 to 20:9.
 *      Vertical Stretch adjusts the width to compress horizontally (e.g., 4:3 or 16:9 equivalent),
 *      producing the coveted waterfall/cutout side margins and wider vertical vision in competitive games.
 *    - In Landscape: Horizontal Stretch widens the logical field of view or trims top/bottom letterboxes,
 *      allowing ultra-wide stretch representation without distorted touch mapping.
 *
 * 2. DPI (Density) Preservation:
 *    - System UI elements and touch hitboxes scale proportionally to density.
 *    - Density is scaled in tandem with resolution to prevent UI elements from becoming microscopically small
 *      or oversized offscreen: targetDensity = (baseDpi * targetWidth) / baseWidth, clamped to safe boundaries (160..640 DPI).
 *
 * 3. Safe Limits:
 *    - Width and Height are clamped to min 480px and max 3840px.
 *    - Multiples of 8 or 16 are maintained for GPU rasterization alignment.
 */
object StretchCalculator {

    const val MIN_WIDTH = 480
    const val MAX_WIDTH = 3840
    const val MIN_HEIGHT = 480
    const val MAX_HEIGHT = 3840
    const val MIN_DPI = 160
    const val MAX_DPI = 640

    fun calculate(
        physicalWidth: Int,
        physicalHeight: Int,
        selectedWidth: Int,
        selectedHeight: Int,
        selectedDpi: Int,
        isLandscape: Boolean,
        mode: StretchMode
    ): StretchConfiguration {
        // Base sanity clamp on physical inputs
        val baseW = physicalWidth.coerceIn(MIN_WIDTH, MAX_WIDTH)
        val baseH = physicalHeight.coerceIn(MIN_HEIGHT, MAX_HEIGHT)

        var finalW: Int
        var finalH: Int

        when (mode) {
            StretchMode.VERTICAL -> {
                if (!isLandscape) {
                    // Portrait: Vertical stretch narrows the width relative to height (e.g. 4:3 stretch)
                    // Aspect ratio is tightened to 4:3 (height * 3 / 4)
                    val calculatedW = (baseH * 3) / 4
                    finalW = (if (calculatedW < baseW) calculatedW else (baseW * 85) / 100).coerceAtLeast(MIN_WIDTH)
                    finalH = selectedHeight.coerceIn(MIN_HEIGHT, MAX_HEIGHT)
                } else {
                    // Landscape: Vertical stretch adjusts height downwards for letterbox cutout effect
                    val calculatedH = (baseW * 3) / 4
                    finalW = selectedWidth.coerceIn(MIN_WIDTH, MAX_WIDTH)
                    finalH = (if (calculatedH < baseH) calculatedH else (baseH * 85) / 100).coerceAtLeast(MIN_HEIGHT)
                }
            }

            StretchMode.HORIZONTAL -> {
                if (!isLandscape) {
                    // Portrait: Horizontal stretch adjusts height to compress vertically
                    finalW = selectedWidth.coerceIn(MIN_WIDTH, MAX_WIDTH)
                    finalH = ((baseH * 90) / 100).coerceAtLeast(MIN_HEIGHT)
                } else {
                    // Landscape: Horizontal stretch widens aspect ratio to 21:9 or compresses vertical height
                    // for maximum horizontal field stretch
                    finalW = selectedWidth.coerceIn(MIN_WIDTH, MAX_WIDTH)
                    val calculatedH = (baseW * 9) / 21
                    finalH = calculatedH.coerceIn(MIN_HEIGHT, MAX_HEIGHT)
                }
            }

            StretchMode.BOTH -> {
                // Both / Full Stretch: directly use selected target resolution
                finalW = selectedWidth.coerceIn(MIN_WIDTH, MAX_WIDTH)
                finalH = selectedHeight.coerceIn(MIN_HEIGHT, MAX_HEIGHT)
            }
        }

        // Align dimensions to even numbers (GPU rasterization friendly)
        finalW = (finalW / 2) * 2
        finalH = (finalH / 2) * 2

        val finalDpi = selectedDpi.coerceIn(MIN_DPI, MAX_DPI)

        return StretchConfiguration(
            targetWidth = finalW,
            targetHeight = finalH,
            targetDensity = finalDpi,
            orientation = if (isLandscape) "LANDSCAPE" else "PORTRAIT",
            mode = mode
        )
    }
}
