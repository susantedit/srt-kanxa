package com.srtxcheats.macro

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pure-JVM tests for macro/auto-clicker timing math. */
class MacroTimingTest {

    @Test
    fun clampCps_staysWithin10to100() {
        assertEquals(10, MacroTiming.clampCps(1))
        assertEquals(10, MacroTiming.clampCps(-50))
        assertEquals(55, MacroTiming.clampCps(55))
        assertEquals(100, MacroTiming.clampCps(100))
        assertEquals(100, MacroTiming.clampCps(9999))
    }

    @Test
    fun clickPeriod_matchesRate() {
        assertEquals(100L, MacroTiming.clickPeriodMs(10)) // 10 cps -> 100ms
        assertEquals(50L, MacroTiming.clickPeriodMs(20))  // 20 cps -> 50ms
        assertEquals(10L, MacroTiming.clickPeriodMs(100)) // 100 cps -> 10ms
    }

    @Test
    fun clickPeriod_clampsRateFirst() {
        // Below the minimum still clamps to 10 cps, not a huge period.
        assertEquals(100L, MacroTiming.clickPeriodMs(1))
    }

    @Test
    fun clickHold_isPositive_andLeavesAGap() {
        for (cps in MacroTiming.MIN_CPS..MacroTiming.MAX_CPS) {
            val period = MacroTiming.clickPeriodMs(cps)
            val hold = MacroTiming.clickHoldMs(cps)
            assertTrue("hold>0 for cps=$cps", hold >= 1L)
            assertTrue("hold<period for cps=$cps (period=$period, hold=$hold)", hold < period)
        }
    }

    @Test
    fun clickHold_cappedAt60ms() {
        // At 10 cps, 40% of 100ms = 40ms (within cap); the cap only bites for very slow rates.
        assertEquals(40L, MacroTiming.clickHoldMs(10))
        assertTrue(MacroTiming.clickHoldMs(100) in 3L..60L)
    }

    @Test
    fun scaledDelay_speedScaling() {
        assertEquals(100L, MacroTiming.scaledDelayMs(100L, 100)) // unchanged at 100%
        assertEquals(50L, MacroTiming.scaledDelayMs(100L, 200))  // twice as fast
        assertEquals(200L, MacroTiming.scaledDelayMs(100L, 50))  // half speed
    }

    @Test
    fun scaledDelay_nonPositiveDeltaIsZero() {
        assertEquals(0L, MacroTiming.scaledDelayMs(0L, 100))
        assertEquals(0L, MacroTiming.scaledDelayMs(-5L, 100))
    }

    @Test
    fun scaledDelay_clampsSpeed() {
        // Beyond the max speed the wait keeps shrinking only to the clamp, not to zero.
        val atClamp = MacroTiming.scaledDelayMs(1000L, MacroTiming.MAX_SPEED_PERCENT)
        assertEquals(atClamp, MacroTiming.scaledDelayMs(1000L, 99999))
    }
}
