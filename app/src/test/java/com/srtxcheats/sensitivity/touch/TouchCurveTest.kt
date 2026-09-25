package com.srtxcheats.sensitivity.touch

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Pure-JVM tests for [TouchCurve]. No Android dependencies, so these run under
 * `testDebugUnitTest` without Robolectric.
 */
class TouchCurveTest {

    private val eps = 0.0001f

    @Test
    fun linear_isConstantFullGainEverywhere() {
        assertEquals(1f, TouchCurve.LINEAR.weight(0f), eps)
        assertEquals(1f, TouchCurve.LINEAR.weight(0.5f), eps)
        assertEquals(1f, TouchCurve.LINEAR.weight(1f), eps)
    }

    @Test
    fun ease_isSmoothstep() {
        // smoothstep: c*c*(3-2c). Endpoints 0 and 1, midpoint 0.5.
        assertEquals(0f, TouchCurve.EASE.weight(0f), eps)
        assertEquals(1f, TouchCurve.EASE.weight(1f), eps)
        assertEquals(0.5f, TouchCurve.EASE.weight(0.5f), eps)
    }

    @Test
    fun aggressive_isSqrt() {
        assertEquals(0f, TouchCurve.AGGRESSIVE.weight(0f), eps)
        assertEquals(1f, TouchCurve.AGGRESSIVE.weight(1f), eps)
        assertEquals(0.5f, TouchCurve.AGGRESSIVE.weight(0.25f), eps) // sqrt(0.25)
    }

    @Test
    fun aggressive_rampsFasterThanEaseAtSmallDisplacement() {
        // The whole point of AGGRESSIVE: small movement already amplified, while
        // EASE keeps small movement near 1:1. LINEAR is the constant ceiling.
        val c = 0.1f
        val ease = TouchCurve.EASE.weight(c)
        val aggressive = TouchCurve.AGGRESSIVE.weight(c)
        val linear = TouchCurve.LINEAR.weight(c)
        assert(aggressive > ease) { "aggressive=$aggressive should exceed ease=$ease" }
        assert(linear >= aggressive) { "linear=$linear should be the ceiling" }
    }

    @Test
    fun weight_clampsInputToUnitInterval() {
        // Negative and >1 inputs are coerced into [0,1] before shaping.
        assertEquals(0f, TouchCurve.AGGRESSIVE.weight(-1f), eps)
        assertEquals(1f, TouchCurve.AGGRESSIVE.weight(2f), eps)
        assertEquals(0f, TouchCurve.EASE.weight(-5f), eps)
        assertEquals(1f, TouchCurve.EASE.weight(3f), eps)
    }

    @Test
    fun fromInt_matchesAidlMapping() {
        assertEquals(TouchCurve.LINEAR, TouchCurve.fromInt(0))
        assertEquals(TouchCurve.EASE, TouchCurve.fromInt(1))
        assertEquals(TouchCurve.AGGRESSIVE, TouchCurve.fromInt(2))
        // Anything out of range falls back to LINEAR.
        assertEquals(TouchCurve.LINEAR, TouchCurve.fromInt(99))
        assertEquals(TouchCurve.LINEAR, TouchCurve.fromInt(-1))
    }

    @Test
    fun toInt_matchesAidlMapping() {
        assertEquals(0, TouchCurve.toInt(TouchCurve.LINEAR))
        assertEquals(1, TouchCurve.toInt(TouchCurve.EASE))
        assertEquals(2, TouchCurve.toInt(TouchCurve.AGGRESSIVE))
    }

    @Test
    fun intMapping_roundTrips() {
        for (c in TouchCurve.values()) {
            assertEquals(c, TouchCurve.fromInt(TouchCurve.toInt(c)))
        }
    }

    @Test
    fun label_isHumanReadable() {
        assertEquals("Linear", TouchCurve.label(TouchCurve.LINEAR))
        assertEquals("Ease", TouchCurve.label(TouchCurve.EASE))
        assertEquals("Aggressive", TouchCurve.label(TouchCurve.AGGRESSIVE))
    }
}
