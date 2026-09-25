package com.srtxcheats.macro

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pure-JVM tests for the hand-rolled macro serializer. */
class MacroCodecTest {

    private fun sample() = Macro(
        id = "m1",
        name = "Free Fire drag shot",
        xMin = 0, xMax = 32767, yMin = 0, yMax = 32767,
        frames = listOf(
            MacroFrame(0L, listOf(MacroPointer(0, 100, 1000f, 2000f))),
            MacroFrame(16L, listOf(MacroPointer(0, 100, 1050.5f, 2000f), MacroPointer(1, 101, 500f, 400f))),
            MacroFrame(32L, emptyList()),
        ),
        createdAtMs = 1_700_000_000_000L,
    )

    @Test
    fun roundTrip_preservesEverything() {
        val original = sample()
        val decoded = MacroCodec.decode(original.id, MacroCodec.encode(original))
        assertEquals(original, decoded)
    }

    @Test
    fun decode_rejectsBadMagic() {
        assertNull(MacroCodec.decode("x", "NOPE\nN foo\n"))
        assertNull(MacroCodec.decode("x", ""))
    }

    @Test
    fun decode_rejectsMalformedRange() {
        val bad = "SRTMACRO1\nN foo\nR 0 100 200\n" // only 3 range values
        assertNull(MacroCodec.decode("x", bad))
    }

    @Test
    fun decode_rejectsMalformedFrameTimestamp() {
        val bad = "SRTMACRO1\nN foo\nR 0 1 0 1\nNOTANUMBER|0:1:2:3\n"
        assertNull(MacroCodec.decode("x", bad))
    }

    @Test
    fun name_withSpaces_isPreserved_andNewlinesStripped() {
        val m = sample().copy(name = "my   spaced\nname")
        val decoded = MacroCodec.decode(m.id, MacroCodec.encode(m))!!
        // Newline replaced by space, content otherwise intact.
        assertEquals("my   spaced name", decoded.name)
    }

    @Test
    fun emptyName_fallsBackToDefault() {
        val m = sample().copy(name = "   ")
        val decoded = MacroCodec.decode(m.id, MacroCodec.encode(m))!!
        assertEquals("Macro", decoded.name)
    }

    @Test
    fun emptyFrame_meansAllPointersUp() {
        val decoded = MacroCodec.decode(sample().id, MacroCodec.encode(sample()))!!
        assertTrue(decoded.frames[2].pointers.isEmpty())
        assertEquals(32L, decoded.frames[2].atMs)
    }

    @Test
    fun wholeNumberFloats_encodeWithoutTrailingDecimal() {
        val encoded = MacroCodec.encode(sample())
        // 1000f should serialize as "1000", not "1000.0"; 1050.5f keeps its fraction.
        assertTrue(encoded.contains("0:100:1000:2000"))
        assertTrue(encoded.contains("1050.5"))
    }

    @Test
    fun durationMs_isLastFrameTimestamp() {
        assertEquals(32L, sample().durationMs)
        assertEquals(3, sample().frameCount)
        assertTrue(sample().hasRange)
    }
}
