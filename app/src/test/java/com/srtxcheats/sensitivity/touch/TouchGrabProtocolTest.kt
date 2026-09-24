package com.srtxcheats.sensitivity.touch

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure-JVM tests for [TouchGrabProtocol] — every line form the native helper can
 * emit (see touchgrab.c header) must map to exactly the right [TouchGrabEvent],
 * and anything malformed must degrade to [TouchGrabEvent.Unknown] rather than throw.
 */
class TouchGrabProtocolTest {

    @Test
    fun ready() {
        assertEquals(TouchGrabEvent.Ready, TouchGrabProtocol.parse("TOUCHGRAB_READY"))
    }

    @Test
    fun device() {
        assertEquals(
            TouchGrabEvent.Device("/dev/input/event3"),
            TouchGrabProtocol.parse("TOUCHGRAB_DEVICE /dev/input/event3"),
        )
    }

    @Test
    fun name_keepsSpaces() {
        assertEquals(
            TouchGrabEvent.Name("synaptics touchscreen"),
            TouchGrabProtocol.parse("TOUCHGRAB_NAME synaptics touchscreen"),
        )
    }

    @Test
    fun range_wellFormed() {
        assertEquals(
            TouchGrabEvent.Range(0, 1080, 0, 2340),
            TouchGrabProtocol.parse("TOUCHGRAB_RANGE 0 1080 0 2340"),
        )
    }

    @Test
    fun range_wrongArity_isUnknown() {
        assertTrue(TouchGrabProtocol.parse("TOUCHGRAB_RANGE 0 1080 0") is TouchGrabEvent.Unknown)
        assertTrue(TouchGrabProtocol.parse("TOUCHGRAB_RANGE 0 1080 0 2340 5") is TouchGrabEvent.Unknown)
    }

    @Test
    fun range_nonNumeric_isUnknown() {
        assertTrue(TouchGrabProtocol.parse("TOUCHGRAB_RANGE a b c d") is TouchGrabEvent.Unknown)
    }

    @Test
    fun status_withDetail() {
        assertEquals(
            TouchGrabEvent.Status("grabbed", "/dev/input/event3"),
            TouchGrabProtocol.parse("TOUCHGRAB_STATUS grabbed /dev/input/event3"),
        )
    }

    @Test
    fun status_withoutDetail() {
        assertEquals(
            TouchGrabEvent.Status("detect_complete_no_grab", null),
            TouchGrabProtocol.parse("TOUCHGRAB_STATUS detect_complete_no_grab"),
        )
        assertEquals(
            TouchGrabEvent.Status("heartbeat_timeout", null),
            TouchGrabProtocol.parse("TOUCHGRAB_STATUS heartbeat_timeout"),
        )
    }

    @Test
    fun error_withoutMessage() {
        assertEquals(
            TouchGrabEvent.Error("touchscreen_not_found", null),
            TouchGrabProtocol.parse("TOUCHGRAB_ERROR touchscreen_not_found"),
        )
    }

    @Test
    fun error_withMessage() {
        assertEquals(
            TouchGrabEvent.Error("grab_failed", "errno=13 message=Permission denied"),
            TouchGrabProtocol.parse("TOUCHGRAB_ERROR grab_failed errno=13 message=Permission denied"),
        )
    }

    @Test
    fun abs_slot() {
        assertEquals(
            TouchGrabEvent.Abs(AbsAxis.SLOT, 1),
            TouchGrabProtocol.parse("EV_ABS ABS_MT_SLOT 00000001"),
        )
    }

    @Test
    fun abs_trackingId_negativeOne_fromTwosComplementHex() {
        // ffffffff (unsigned 32-bit) is -1 as a signed Int — a finger-up marker.
        assertEquals(
            TouchGrabEvent.Abs(AbsAxis.TRACKING_ID, -1),
            TouchGrabProtocol.parse("EV_ABS ABS_MT_TRACKING_ID ffffffff"),
        )
    }

    @Test
    fun abs_positionX_decodesHex() {
        assertEquals(
            TouchGrabEvent.Abs(AbsAxis.POSITION_X, 1000),
            TouchGrabProtocol.parse("EV_ABS ABS_MT_POSITION_X 000003e8"),
        )
    }

    @Test
    fun abs_positionY_decodesHex() {
        assertEquals(
            TouchGrabEvent.Abs(AbsAxis.POSITION_Y, 0xABC),
            TouchGrabProtocol.parse("EV_ABS ABS_MT_POSITION_Y 00000abc"),
        )
    }

    @Test
    fun abs_unknownAxis_isUnknown() {
        assertTrue(TouchGrabProtocol.parse("EV_ABS ABS_MT_PRESSURE 00000001") is TouchGrabEvent.Unknown)
    }

    @Test
    fun synReport() {
        assertEquals(TouchGrabEvent.SynReport(0), TouchGrabProtocol.parse("EV_SYN SYN_REPORT 00000000"))
    }

    @Test
    fun synReport_wrongCode_isUnknown() {
        assertTrue(TouchGrabProtocol.parse("EV_SYN SYN_MT_REPORT 00000000") is TouchGrabEvent.Unknown)
    }

    @Test
    fun candidate_full() {
        assertEquals(
            TouchGrabEvent.Candidate(
                path = "/dev/input/event3",
                score = 27,
                name = "synaptics_dsx",
                xMin = 0, xMax = 1080, yMin = 0, yMax = 2340,
            ),
            TouchGrabProtocol.parse(
                "TOUCHGRAB_CANDIDATE /dev/input/event3 score=27 name=synaptics_dsx x=0:1080 y=0:2340",
            ),
        )
    }

    @Test
    fun candidate_nameWithSpaces() {
        val ev = TouchGrabProtocol.parse(
            "TOUCHGRAB_CANDIDATE /dev/input/event5 score=17 name=Goodix Capacitive TouchScreen x=0:720 y=0:1600",
        )
        assertTrue(ev is TouchGrabEvent.Candidate)
        ev as TouchGrabEvent.Candidate
        assertEquals("/dev/input/event5", ev.path)
        assertEquals(17, ev.score)
        assertEquals("Goodix Capacitive TouchScreen", ev.name)
        assertEquals(720, ev.xMax)
        assertEquals(1600, ev.yMax)
    }

    @Test
    fun emptyLine_isUnknown() {
        assertTrue(TouchGrabProtocol.parse("") is TouchGrabEvent.Unknown)
        assertTrue(TouchGrabProtocol.parse("   ") is TouchGrabEvent.Unknown)
    }

    @Test
    fun garbage_isUnknown() {
        assertTrue(TouchGrabProtocol.parse("hello world") is TouchGrabEvent.Unknown)
    }

    @Test
    fun leadingAndTrailingWhitespace_isTolerated() {
        assertEquals(TouchGrabEvent.Ready, TouchGrabProtocol.parse("  TOUCHGRAB_READY  "))
    }
}
