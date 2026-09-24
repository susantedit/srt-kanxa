package com.srtxcheats.sensitivity.touch

/**
 * Pure ABS-digitizer → display-pixel coordinate mapping. No Android deps so the
 * rotation math is unit-testable.
 *
 * The touchscreen reports positions in its panel-native ABS range, which does
 * not rotate with the display. A game running landscape sees the logical display
 * coordinate space, so injected events must be rotated to match. Rotation values
 * follow `android.view.Surface.ROTATION_*` (0, 1, 2, 3 = 0°, 90°, 180°, 270°).
 */
object CoordinateMapper {

    data class Range(val xMin: Int, val xMax: Int, val yMin: Int, val yMax: Int)

    private fun norm(v: Float, min: Int, max: Int): Float {
        val span = (max - min).toFloat()
        if (span <= 0f) return 0f
        return ((v - min) / span).coerceIn(0f, 1f)
    }

    /**
     * @param displayW/displayH logical display size in the CURRENT rotation.
     * @return (x, y) in display pixels.
     */
    fun toScreen(
        absX: Float,
        absY: Float,
        range: Range,
        displayW: Int,
        displayH: Int,
        rotation: Int,
    ): Pair<Float, Float> {
        val nx = norm(absX, range.xMin, range.xMax)
        val ny = norm(absY, range.yMin, range.yMax)
        return when (rotation) {
            1 -> Pair(ny * displayW, (1f - nx) * displayH)      // 90°
            2 -> Pair((1f - nx) * displayW, (1f - ny) * displayH) // 180°
            3 -> Pair((1f - ny) * displayW, nx * displayH)      // 270°
            else -> Pair(nx * displayW, ny * displayH)          // 0°
        }
    }
}
