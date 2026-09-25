package com.srtxcheats.macro

/**
 * Line-based serialization for [Macro]s — hand-rolled on purpose so it has zero dependencies and works
 * both in the app and in pure-JVM unit tests (org.json is unavailable off-device, and java.util.Base64
 * needs API 26 while this app's minSdk is 24, so neither is used here).
 *
 * Format (UTF-8, '\n' separated):
 * ```
 * SRTMACRO1
 * N <name>                         # name, spaces allowed, newlines stripped
 * R <xMin> <xMax> <yMin> <yMax>    # digitizer range the recording was made against
 * C <createdAtMs>
 * <atMs>|<slot>:<tid>:<x>:<y>,...  # one line per frame; empty pointer list => "<atMs>|"
 * ...
 * ```
 * Decoding is tolerant of unknown header keys but rejects a missing/incorrect magic line.
 */
object MacroCodec {

    private const val MAGIC = "SRTMACRO1"

    fun encode(m: Macro): String {
        val sb = StringBuilder()
        sb.append(MAGIC).append('\n')
        sb.append("N ").append(sanitizeName(m.name)).append('\n')
        sb.append("R ").append(m.xMin).append(' ').append(m.xMax).append(' ')
            .append(m.yMin).append(' ').append(m.yMax).append('\n')
        sb.append("C ").append(m.createdAtMs).append('\n')
        for (f in m.frames) {
            sb.append(f.atMs).append('|')
            for ((i, p) in f.pointers.withIndex()) {
                if (i > 0) sb.append(',')
                sb.append(p.slot).append(':').append(p.trackingId).append(':')
                    .append(fmt(p.x)).append(':').append(fmt(p.y))
            }
            sb.append('\n')
        }
        return sb.toString()
    }

    /** Returns null when [text] is not a valid macro (bad magic or malformed range/frame). */
    fun decode(id: String, text: String): Macro? {
        val lines = text.split('\n')
        if (lines.isEmpty() || lines[0].trim() != MAGIC) return null

        var name = "Macro"
        var xMin = 0; var xMax = 0; var yMin = 0; var yMax = 0
        var created = 0L
        val frames = ArrayList<MacroFrame>()

        for (idx in 1 until lines.size) {
            val line = lines[idx]
            if (line.isBlank()) continue
            when {
                line.startsWith("N ") -> name = line.substring(2)
                line.startsWith("R ") -> {
                    val parts = line.substring(2).trim().split(' ')
                    if (parts.size != 4) return null
                    xMin = parts[0].toIntOrNull() ?: return null
                    xMax = parts[1].toIntOrNull() ?: return null
                    yMin = parts[2].toIntOrNull() ?: return null
                    yMax = parts[3].toIntOrNull() ?: return null
                }
                line.startsWith("C ") -> created = line.substring(2).trim().toLongOrNull() ?: 0L
                line.contains('|') -> {
                    val bar = line.indexOf('|')
                    val atMs = line.substring(0, bar).trim().toLongOrNull() ?: return null
                    val rest = line.substring(bar + 1)
                    val pointers = if (rest.isBlank()) {
                        emptyList()
                    } else {
                        rest.split(',').mapNotNull { tok -> parsePointer(tok) }
                    }
                    frames.add(MacroFrame(atMs, pointers))
                }
                // Unknown header lines are ignored for forward compatibility.
            }
        }
        return Macro(id, name, xMin, xMax, yMin, yMax, frames, created)
    }

    private fun parsePointer(token: String): MacroPointer? {
        val c = token.split(':')
        if (c.size != 4) return null
        val slot = c[0].toIntOrNull() ?: return null
        val tid = c[1].toIntOrNull() ?: return null
        val x = c[2].toFloatOrNull() ?: return null
        val y = c[3].toFloatOrNull() ?: return null
        return MacroPointer(slot, tid, x, y)
    }

    private fun sanitizeName(s: String): String =
        s.replace('\n', ' ').replace('\r', ' ').trim().ifEmpty { "Macro" }

    /** Compact float: emit whole numbers without a trailing ".0"; Kotlin's Float.toString is '.'-decimal. */
    private fun fmt(v: Float): String {
        val asLong = v.toLong()
        return if (asLong.toFloat() == v) asLong.toString() else v.toString()
    }
}
