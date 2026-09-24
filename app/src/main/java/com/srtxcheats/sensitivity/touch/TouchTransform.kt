package com.srtxcheats.sensitivity.touch

import kotlin.math.abs

/**
 * Transform parameters for a single grab session. Immutable; swapped atomically
 * via [TouchTransform.updateParams] when the user moves a slider.
 *
 * @param gainX/gainY displacement multipliers (0.5..3.0). 1.0 = pass-through.
 * @param smoothing   EMA factor 0..1 applied to output; higher = smoother/laggier.
 * @param curve       response curve shaping gain vs. displacement.
 * @param xMin..yMax  the digitizer's ABS_MT range (from TOUCHGRAB_RANGE) used to
 *                    clamp output and to normalize displacement for the curve.
 */
data class TransformParams(
    val gainX: Float,
    val gainY: Float,
    val smoothing: Float,
    val curve: TouchCurve,
    val xMin: Int,
    val xMax: Int,
    val yMin: Int,
    val yMax: Int,
)

/** One transformed pointer in a completed frame. Coordinates are in ABS units. */
data class PointerOut(
    val slot: Int,
    val trackingId: Int,
    val x: Float,
    val y: Float,
)

/** Snapshot of every down pointer at a SYN_REPORT boundary, transformed. */
data class TouchFrame(val pointers: List<PointerOut>)

/**
 * The delta-gain touch transform. Pure Kotlin, no Android dependencies.
 *
 * Model (per multi-touch slot):
 *   1. On finger-down, latch an anchor at the raw touch-down coordinate.
 *   2. output = anchor + (raw - anchor) * effectiveGain, where effectiveGain is
 *      the base gain shaped by the response curve against normalized displacement.
 *   3. Exponential moving average on the output smooths jitter.
 *   4. Clamp to the digitizer range so we never inject off-panel coordinates.
 *
 * This amplifies how far a swipe travels relative to the finger — the same
 * "sensitivity X/Y scales touch displacement" behaviour recovered from the
 * reference app — while the anchor keeps taps landing exactly where touched.
 *
 * Feed parsed [TouchGrabEvent]s in order; [onEvent] returns a [TouchFrame] at
 * each SYN_REPORT and null otherwise.
 */
class TouchTransform(initial: TransformParams) {

    @Volatile
    private var params: TransformParams = initial

    private val slots = LinkedHashMap<Int, Slot>()
    private var activeSlot = 0

    fun updateParams(p: TransformParams) {
        params = p
    }

    fun reset() {
        slots.clear()
        activeSlot = 0
    }

    fun onEvent(ev: TouchGrabEvent): TouchFrame? {
        when (ev) {
            is TouchGrabEvent.Abs -> applyAbs(ev)
            is TouchGrabEvent.SynReport -> return buildFrame()
            else -> {}
        }
        return null
    }

    private fun applyAbs(ev: TouchGrabEvent.Abs) {
        when (ev.axis) {
            AbsAxis.SLOT -> activeSlot = ev.value
            AbsAxis.TRACKING_ID -> {
                if (ev.value < 0) {
                    // Finger up on this slot.
                    slots.remove(activeSlot)
                } else {
                    // New finger down; force a fresh anchor on the next frame.
                    val s = slots.getOrPut(activeSlot) { Slot() }
                    s.trackingId = ev.value
                    s.hasAnchor = false
                    s.hasLastOut = false
                }
            }
            AbsAxis.POSITION_X -> {
                val s = slots.getOrPut(activeSlot) { Slot() }
                s.rawX = ev.value
                s.hasRawX = true
            }
            AbsAxis.POSITION_Y -> {
                val s = slots.getOrPut(activeSlot) { Slot() }
                s.rawY = ev.value
                s.hasRawY = true
            }
        }
    }

    private fun buildFrame(): TouchFrame {
        val p = params
        val out = ArrayList<PointerOut>(slots.size)
        for ((slotIdx, s) in slots) {
            if (s.trackingId < 0 || !s.hasRawX || !s.hasRawY) continue
            if (!s.hasAnchor) {
                s.anchorX = s.rawX
                s.anchorY = s.rawY
                s.hasAnchor = true
            }
            val tx = transformAxis(s.rawX, s.anchorX, p.gainX, p.curve, p.xMin, p.xMax)
            val ty = transformAxis(s.rawY, s.anchorY, p.gainY, p.curve, p.yMin, p.yMax)

            val sx: Float
            val sy: Float
            if (s.hasLastOut) {
                val alpha = 1f - p.smoothing.coerceIn(0f, SMOOTHING_MAX)
                sx = s.lastOutX + (tx - s.lastOutX) * alpha
                sy = s.lastOutY + (ty - s.lastOutY) * alpha
            } else {
                sx = tx
                sy = ty
            }
            s.lastOutX = sx
            s.lastOutY = sy
            s.hasLastOut = true

            out.add(PointerOut(slotIdx, s.trackingId, sx, sy))
        }
        return TouchFrame(out)
    }

    private class Slot {
        var trackingId: Int = -1
        var rawX: Int = 0
        var rawY: Int = 0
        var hasRawX: Boolean = false
        var hasRawY: Boolean = false
        var anchorX: Int = 0
        var anchorY: Int = 0
        var hasAnchor: Boolean = false
        var lastOutX: Float = 0f
        var lastOutY: Float = 0f
        var hasLastOut: Boolean = false
    }

    companion object {
        /**
         * Displacement (as a fraction of the axis range) at which a shaped curve
         * reaches full gain. A quarter-screen swipe hits the ceiling.
         */
        const val CURVE_REFERENCE_FRACTION = 0.25f

        /** Cap smoothing so alpha never reaches 0 (which would freeze the pointer). */
        const val SMOOTHING_MAX = 0.99f

        /**
         * Pure per-axis delta-gain with response curve and range clamp.
         * Exposed for unit testing; deterministic in its inputs.
         */
        fun transformAxis(
            raw: Int,
            anchor: Int,
            gain: Float,
            curve: TouchCurve,
            min: Int,
            max: Int,
        ): Float {
            val delta = (raw - anchor).toFloat()
            val span = (max - min).toFloat().coerceAtLeast(1f)
            val distNorm = (abs(delta) / (span * CURVE_REFERENCE_FRACTION)).coerceIn(0f, 1f)
            val weight = curve.weight(distNorm)
            val effectiveGain = 1f + (gain - 1f) * weight
            val outF = anchor + delta * effectiveGain
            return outF.coerceIn(min.toFloat(), max.toFloat())
        }
    }
}
