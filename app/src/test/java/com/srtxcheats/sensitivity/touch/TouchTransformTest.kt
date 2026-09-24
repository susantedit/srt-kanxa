package com.srtxcheats.sensitivity.touch

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure-JVM tests for [TouchTransform] — the delta-gain model that makes a swipe
 * travel further than the finger. Covers the static [TouchTransform.transformAxis]
 * math and the stateful protocol-B event pipeline (anchor latch, gain, smoothing,
 * range clamp, per-slot independence, finger up/down).
 */
class TouchTransformTest {

    private val eps = 0.01f

    private fun params(
        gainX: Float = 2f,
        gainY: Float = 1f,
        smoothing: Float = 0f,
        curve: TouchCurve = TouchCurve.LINEAR,
    ) = TransformParams(gainX, gainY, smoothing, curve, 0, 1000, 0, 1000)

    // ---- transformAxis (pure) --------------------------------------------------

    @Test
    fun transformAxis_gainOne_isPassThrough() {
        assertEquals(600f, TouchTransform.transformAxis(600, 500, 1.0f, TouchCurve.LINEAR, 0, 1000), eps)
    }

    @Test
    fun transformAxis_gainTwo_doublesDisplacement() {
        // delta 100, LINEAR full gain -> anchor + 100*2 = 700
        assertEquals(700f, TouchTransform.transformAxis(600, 500, 2.0f, TouchCurve.LINEAR, 0, 1000), eps)
    }

    @Test
    fun transformAxis_clampsToMax() {
        // 500 + 400*3 = 1700, clamped to 1000
        assertEquals(1000f, TouchTransform.transformAxis(900, 500, 3.0f, TouchCurve.LINEAR, 0, 1000), eps)
    }

    @Test
    fun transformAxis_clampsToMin() {
        // 500 - 400*3 = -700, clamped to 0
        assertEquals(0f, TouchTransform.transformAxis(100, 500, 3.0f, TouchCurve.LINEAR, 0, 1000), eps)
    }

    @Test
    fun transformAxis_zeroDisplacement_staysAtAnchor() {
        // A tap lands exactly where touched regardless of gain.
        assertEquals(500f, TouchTransform.transformAxis(500, 500, 3.0f, TouchCurve.LINEAR, 0, 1000), eps)
    }

    @Test
    fun transformAxis_aggressiveAmplifiesSmallMoreThanEase() {
        val raw = 525
        val anchor = 500 // distNorm = 25 / (1000*0.25) = 0.1
        val aggressive = TouchTransform.transformAxis(raw, anchor, 2.0f, TouchCurve.AGGRESSIVE, 0, 1000)
        val ease = TouchTransform.transformAxis(raw, anchor, 2.0f, TouchCurve.EASE, 0, 1000)
        val linear = TouchTransform.transformAxis(raw, anchor, 2.0f, TouchCurve.LINEAR, 0, 1000)
        assertTrue("aggressive=$aggressive should exceed ease=$ease", aggressive > ease)
        assertTrue("linear=$linear is the ceiling", linear >= aggressive)
    }

    // ---- stateful pipeline -----------------------------------------------------

    private fun down(t: TouchTransform, slot: Int, id: Int, x: Int, y: Int) {
        t.onEvent(TouchGrabEvent.Abs(AbsAxis.SLOT, slot))
        t.onEvent(TouchGrabEvent.Abs(AbsAxis.TRACKING_ID, id))
        t.onEvent(TouchGrabEvent.Abs(AbsAxis.POSITION_X, x))
        t.onEvent(TouchGrabEvent.Abs(AbsAxis.POSITION_Y, y))
    }

    @Test
    fun down_latchesAnchor_firstFrameEqualsRaw() {
        val t = TouchTransform(params(gainX = 2f, gainY = 2f))
        down(t, slot = 0, id = 100, x = 500, y = 500)
        val f = t.onEvent(TouchGrabEvent.SynReport(0))!!
        assertEquals(1, f.pointers.size)
        val p = f.pointers[0]
        assertEquals(0, p.slot)
        assertEquals(100, p.trackingId)
        assertEquals(500f, p.x, eps) // delta 0 at the anchor
        assertEquals(500f, p.y, eps)
    }

    @Test
    fun move_appliesPerAxisGain() {
        val t = TouchTransform(params(gainX = 2f, gainY = 2f, smoothing = 0f))
        down(t, 0, 100, 500, 500)
        t.onEvent(TouchGrabEvent.SynReport(0))
        // Move only X to 600; Y stays.
        t.onEvent(TouchGrabEvent.Abs(AbsAxis.POSITION_X, 600))
        val f = t.onEvent(TouchGrabEvent.SynReport(0))!!
        val p = f.pointers[0]
        assertEquals(700f, p.x, eps) // 500 + 100*2
        assertEquals(500f, p.y, eps) // unchanged
    }

    @Test
    fun smoothing_appliesEmaOnOutput() {
        val t = TouchTransform(params(gainX = 2f, gainY = 2f, smoothing = 0.5f))
        down(t, 0, 100, 500, 500)
        t.onEvent(TouchGrabEvent.SynReport(0)) // lastOut = (500,500)
        t.onEvent(TouchGrabEvent.Abs(AbsAxis.POSITION_X, 600))
        val f = t.onEvent(TouchGrabEvent.SynReport(0))!!
        // target tx = 700; alpha = 1-0.5 = 0.5; sx = 500 + (700-500)*0.5 = 600
        assertEquals(600f, f.pointers[0].x, eps)
    }

    @Test
    fun fingerUp_removesPointer() {
        val t = TouchTransform(params())
        down(t, 0, 100, 500, 500)
        t.onEvent(TouchGrabEvent.SynReport(0))
        t.onEvent(TouchGrabEvent.Abs(AbsAxis.TRACKING_ID, -1)) // finger up
        val f = t.onEvent(TouchGrabEvent.SynReport(0))!!
        assertEquals(0, f.pointers.size)
    }

    @Test
    fun reDown_latchesFreshAnchor() {
        val t = TouchTransform(params(gainX = 2f, gainY = 2f))
        down(t, 0, 100, 500, 500)
        t.onEvent(TouchGrabEvent.SynReport(0))
        t.onEvent(TouchGrabEvent.Abs(AbsAxis.TRACKING_ID, -1))
        t.onEvent(TouchGrabEvent.SynReport(0))
        // New finger down at a different spot -> new anchor there.
        t.onEvent(TouchGrabEvent.Abs(AbsAxis.TRACKING_ID, 101))
        t.onEvent(TouchGrabEvent.Abs(AbsAxis.POSITION_X, 300))
        t.onEvent(TouchGrabEvent.Abs(AbsAxis.POSITION_Y, 300))
        val f = t.onEvent(TouchGrabEvent.SynReport(0))!!
        assertEquals(101, f.pointers[0].trackingId)
        assertEquals(300f, f.pointers[0].x, eps) // anchor re-latched -> delta 0
    }

    @Test
    fun twoFingers_areTransformedIndependently() {
        val t = TouchTransform(params(gainX = 2f, gainY = 2f, smoothing = 0f))
        down(t, slot = 0, id = 100, x = 500, y = 500)
        down(t, slot = 1, id = 200, x = 200, y = 200)
        val f = t.onEvent(TouchGrabEvent.SynReport(0))!!
        assertEquals(2, f.pointers.size)

        // Move only slot 0.
        t.onEvent(TouchGrabEvent.Abs(AbsAxis.SLOT, 0))
        t.onEvent(TouchGrabEvent.Abs(AbsAxis.POSITION_X, 700))
        val f2 = t.onEvent(TouchGrabEvent.SynReport(0))!!
        val p0 = f2.pointers.first { it.slot == 0 }
        val p1 = f2.pointers.first { it.slot == 1 }
        assertEquals(900f, p0.x, eps) // 500 + 200*2
        assertEquals(200f, p1.x, eps) // untouched
    }

    @Test
    fun reset_clearsAllSlots() {
        val t = TouchTransform(params())
        down(t, 0, 100, 500, 500)
        t.onEvent(TouchGrabEvent.SynReport(0))
        t.reset()
        val f = t.onEvent(TouchGrabEvent.SynReport(0))!!
        assertEquals(0, f.pointers.size)
    }

    @Test
    fun updateParams_takesEffectOnNextFrame() {
        val t = TouchTransform(params(gainX = 1f, gainY = 1f, smoothing = 0f))
        down(t, 0, 100, 500, 500)
        t.onEvent(TouchGrabEvent.SynReport(0))
        t.updateParams(params(gainX = 3f, gainY = 1f, smoothing = 0f))
        t.onEvent(TouchGrabEvent.Abs(AbsAxis.POSITION_X, 600))
        val f = t.onEvent(TouchGrabEvent.SynReport(0))!!
        assertEquals(800f, f.pointers[0].x, eps) // 500 + 100*3
    }

    @Test
    fun nonTouchEvents_produceNoFrame() {
        val t = TouchTransform(params())
        assertNull(t.onEvent(TouchGrabEvent.Ready))
        assertNull(t.onEvent(TouchGrabEvent.Device("/dev/input/event3")))
        assertNull(t.onEvent(TouchGrabEvent.Status("grabbed", "/dev/input/event3")))
        assertNull(t.onEvent(TouchGrabEvent.Abs(AbsAxis.POSITION_X, 500)))
    }
}
