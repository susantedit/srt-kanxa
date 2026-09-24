package com.srtxcheats.sensitivity.touch

import kotlin.math.sqrt

/**
 * Response curve shaping how the base gain is applied as a finger moves away
 * from its touch-down anchor. Pure and deterministic — [weight] is a function
 * of the normalized displacement only, so the whole transform is testable.
 *
 * `weight(t)` returns how strongly the gain applies at normalized displacement
 * `t` in [0,1]; effective gain = 1 + (gain - 1) * weight(t).
 *
 * - [LINEAR]      constant full gain everywhere (matches the APK default).
 * - [EASE]        smoothstep: near the anchor movement is 1:1 for precise aim,
 *                 ramping to full gain on larger swipes.
 * - [AGGRESSIVE]  sqrt: even small movements are amplified quickly.
 *
 * The int mapping matches the AIDL `curve` argument: 0/1/2.
 */
enum class TouchCurve {
    LINEAR,
    EASE,
    AGGRESSIVE;

    fun weight(t: Float): Float {
        val c = t.coerceIn(0f, 1f)
        return when (this) {
            LINEAR -> 1f
            EASE -> c * c * (3f - 2f * c) // smoothstep
            AGGRESSIVE -> sqrt(c)
        }
    }

    companion object {
        fun fromInt(i: Int): TouchCurve = when (i) {
            1 -> EASE
            2 -> AGGRESSIVE
            else -> LINEAR
        }

        fun toInt(c: TouchCurve): Int = when (c) {
            LINEAR -> 0
            EASE -> 1
            AGGRESSIVE -> 2
        }

        /** Display label used in the Response Curve selector. */
        fun label(c: TouchCurve): String = when (c) {
            LINEAR -> "Linear"
            EASE -> "Ease"
            AGGRESSIVE -> "Aggressive"
        }
    }
}
