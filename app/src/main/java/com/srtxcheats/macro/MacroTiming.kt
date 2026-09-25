package com.srtxcheats.macro

/**
 * Pure timing math for the macro subsystem — no Android dependencies, fully unit-testable.
 *
 * Covers the two rate concepts the UI exposes:
 *  - the auto-clicker rate in clicks-per-second (the user's "10-100 click/s" request), and
 *  - playback speed as a percentage that scales a recorded macro's inter-frame delays.
 */
object MacroTiming {

    // ---- Auto-clicker rate (clicks per second) ----
    const val MIN_CPS = 10
    const val MAX_CPS = 100
    const val DEFAULT_CPS = 20

    fun clampCps(cps: Int): Int = cps.coerceIn(MIN_CPS, MAX_CPS)

    /** Full period of one click (down+up+idle) in ms for a given rate. Always >= 1ms. */
    fun clickPeriodMs(cps: Int): Long {
        val c = clampCps(cps)
        return (1000L / c).coerceAtLeast(1L)
    }

    /**
     * How long the finger is held down within a click. Roughly 40% of the period so fast rates still
     * register as distinct taps, clamped to a sane 3..60ms and never longer than the period minus 1ms
     * (so there is always a gap between consecutive taps).
     */
    fun clickHoldMs(cps: Int): Long {
        val period = clickPeriodMs(cps)
        val hold = (period * 4 / 10).coerceIn(3L, 60L)
        return hold.coerceAtMost((period - 1L).coerceAtLeast(1L))
    }

    // ---- Playback speed (percent of original) ----
    const val MIN_SPEED_PERCENT = 25
    const val MAX_SPEED_PERCENT = 400
    const val DEFAULT_SPEED_PERCENT = 100

    fun clampSpeedPercent(p: Int): Int = p.coerceIn(MIN_SPEED_PERCENT, MAX_SPEED_PERCENT)

    /**
     * Scale a recorded inter-frame delay by playback speed. 100% keeps the original timing, 200% halves
     * every wait (twice as fast), 50% doubles it. Non-positive deltas produce no wait.
     */
    fun scaledDelayMs(rawDeltaMs: Long, speedPercent: Int): Long {
        if (rawDeltaMs <= 0L) return 0L
        val p = clampSpeedPercent(speedPercent)
        return (rawDeltaMs * 100L / p).coerceAtLeast(0L)
    }
}
