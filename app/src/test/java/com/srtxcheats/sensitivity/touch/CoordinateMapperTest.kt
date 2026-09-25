package com.srtxcheats.sensitivity.touch

import com.srtxcheats.sensitivity.touch.CoordinateMapper.Range
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Pure-JVM tests for [CoordinateMapper]. The digitizer reports in panel-native
 * orientation; injected events must be rotated to the display's current
 * orientation (games run landscape), so each rotation case is pinned here.
 */
class CoordinateMapperTest {

    private val eps = 0.001f
    private val range = Range(xMin = 0, xMax = 1000, yMin = 0, yMax = 2000)
    private val w = 1000
    private val h = 2000

    private fun assertPair(ex: Float, ey: Float, actual: Pair<Float, Float>) {
        assertEquals(ex, actual.first, eps)
        assertEquals(ey, actual.second, eps)
    }

    @Test
    fun rotation0_isDirectNormalizedScale() {
        // nx=0.5, ny=0.5 -> centre of the display.
        assertPair(500f, 1000f, CoordinateMapper.toScreen(500f, 1000f, range, w, h, 0))
    }

    @Test
    fun rotation90() {
        // nx=0.25, ny=0.5 -> (ny*W, (1-nx)*H) = (500, 1500)
        assertPair(500f, 1500f, CoordinateMapper.toScreen(250f, 1000f, range, w, h, 1))
    }

    @Test
    fun rotation180() {
        // nx=0.25, ny=0.5 -> ((1-nx)*W, (1-ny)*H) = (750, 1000)
        assertPair(750f, 1000f, CoordinateMapper.toScreen(250f, 1000f, range, w, h, 2))
    }

    @Test
    fun rotation270() {
        // nx=0.25, ny=0.5 -> ((1-ny)*W, nx*H) = (500, 500)
        assertPair(500f, 500f, CoordinateMapper.toScreen(250f, 1000f, range, w, h, 3))
    }

    @Test
    fun unknownRotation_fallsBackToZero() {
        assertPair(500f, 1000f, CoordinateMapper.toScreen(500f, 1000f, range, w, h, 7))
    }

    @Test
    fun outOfRangeInput_isClampedToUnitInterval() {
        // absX beyond xMax and below xMin clamp to the display edges.
        assertPair(1000f, 0f, CoordinateMapper.toScreen(9999f, -50f, range, w, h, 0))
        assertPair(0f, 2000f, CoordinateMapper.toScreen(-1f, 9999f, range, w, h, 0))
    }

    @Test
    fun degenerateRange_normalizesToZero() {
        val flat = Range(xMin = 500, xMax = 500, yMin = 0, yMax = 2000)
        // span<=0 on X -> nx=0 regardless of absX.
        assertPair(0f, 1000f, CoordinateMapper.toScreen(500f, 1000f, flat, w, h, 0))
    }

    @Test
    fun cornersMapToDisplayCorners_rotation0() {
        assertPair(0f, 0f, CoordinateMapper.toScreen(0f, 0f, range, w, h, 0))
        assertPair(1000f, 2000f, CoordinateMapper.toScreen(1000f, 2000f, range, w, h, 0))
    }
}
