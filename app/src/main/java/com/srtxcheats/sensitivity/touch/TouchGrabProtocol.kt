package com.srtxcheats.sensitivity.touch

/**
 * Pure parser for the text protocol emitted by the native `touchgrab` helper on
 * stdout. No Android dependencies — fully unit-testable on the JVM.
 *
 * Every line the helper prints maps to exactly one [TouchGrabEvent]. Unknown or
 * malformed lines parse to [TouchGrabEvent.Unknown] rather than throwing, so a
 * future native addition can never crash the reader loop.
 *
 * Line grammar (see touchgrab.c header):
 *   TOUCHGRAB_READY
 *   TOUCHGRAB_DEVICE <path>
 *   TOUCHGRAB_NAME <name...>
 *   TOUCHGRAB_RANGE <xMin> <xMax> <yMin> <yMax>
 *   TOUCHGRAB_CANDIDATE <path> score=<n> name=<name...> x=<min>:<max> y=<min>:<max>
 *   TOUCHGRAB_STATUS <status> [detail...]
 *   TOUCHGRAB_ERROR <code> [message...]
 *   EV_ABS <ABS_MT_SLOT|ABS_MT_TRACKING_ID|ABS_MT_POSITION_X|ABS_MT_POSITION_Y> <hex>
 *   EV_SYN SYN_REPORT <hex>
 */
object TouchGrabProtocol {

    fun parse(rawLine: String): TouchGrabEvent {
        val line = rawLine.trim()
        if (line.isEmpty()) return TouchGrabEvent.Unknown(rawLine)

        return when {
            line == "TOUCHGRAB_READY" -> TouchGrabEvent.Ready

            line.startsWith("TOUCHGRAB_DEVICE ") ->
                TouchGrabEvent.Device(line.removePrefix("TOUCHGRAB_DEVICE ").trim())

            line.startsWith("TOUCHGRAB_NAME ") ->
                TouchGrabEvent.Name(line.removePrefix("TOUCHGRAB_NAME ").trim())

            line.startsWith("TOUCHGRAB_RANGE ") -> parseRange(line)

            line.startsWith("TOUCHGRAB_CANDIDATE ") -> parseCandidate(line)

            line.startsWith("TOUCHGRAB_STATUS ") -> {
                val rest = line.removePrefix("TOUCHGRAB_STATUS ").trim()
                val sp = rest.indexOf(' ')
                if (sp < 0) TouchGrabEvent.Status(rest, null)
                else TouchGrabEvent.Status(rest.substring(0, sp), rest.substring(sp + 1).trim())
            }

            line.startsWith("TOUCHGRAB_ERROR ") -> {
                val rest = line.removePrefix("TOUCHGRAB_ERROR ").trim()
                val sp = rest.indexOf(' ')
                if (sp < 0) TouchGrabEvent.Error(rest, null)
                else TouchGrabEvent.Error(rest.substring(0, sp), rest.substring(sp + 1).trim())
            }

            line.startsWith("EV_ABS ") -> parseAbs(line)

            line.startsWith("EV_SYN ") -> {
                // EV_SYN SYN_REPORT <hex>
                val parts = line.split(Regex("\\s+"))
                val value = parts.getOrNull(2)?.let { hexToInt(it) }
                if (parts.getOrNull(1) == "SYN_REPORT" && value != null) {
                    TouchGrabEvent.SynReport(value)
                } else {
                    TouchGrabEvent.Unknown(rawLine)
                }
            }

            else -> TouchGrabEvent.Unknown(rawLine)
        }
    }

    private fun parseRange(line: String): TouchGrabEvent {
        val nums = line.removePrefix("TOUCHGRAB_RANGE ").trim().split(Regex("\\s+"))
        if (nums.size != 4) return TouchGrabEvent.Unknown(line)
        val ints = nums.map { it.toIntOrNull() ?: return TouchGrabEvent.Unknown(line) }
        return TouchGrabEvent.Range(ints[0], ints[1], ints[2], ints[3])
    }

    private fun parseAbs(line: String): TouchGrabEvent {
        // EV_ABS <CODE> <hex>
        val parts = line.split(Regex("\\s+"))
        if (parts.size < 3) return TouchGrabEvent.Unknown(line)
        val axis = when (parts[1]) {
            "ABS_MT_SLOT" -> AbsAxis.SLOT
            "ABS_MT_TRACKING_ID" -> AbsAxis.TRACKING_ID
            "ABS_MT_POSITION_X" -> AbsAxis.POSITION_X
            "ABS_MT_POSITION_Y" -> AbsAxis.POSITION_Y
            else -> return TouchGrabEvent.Unknown(line)
        }
        val value = hexToInt(parts[2]) ?: return TouchGrabEvent.Unknown(line)
        return TouchGrabEvent.Abs(axis, value)
    }

    private fun parseCandidate(line: String): TouchGrabEvent {
        // TOUCHGRAB_CANDIDATE <path> score=<n> name=<name...> x=<min>:<max> y=<min>:<max>
        val body = line.removePrefix("TOUCHGRAB_CANDIDATE ").trim()

        val scoreMatch = Regex("score=(-?\\d+)").find(body)
        val xMatch = Regex("x=(-?\\d+):(-?\\d+)").find(body)
        val yMatch = Regex("y=(-?\\d+):(-?\\d+)").find(body)

        val path = body.substringBefore(" score=", body).trim()
        val score = scoreMatch?.groupValues?.get(1)?.toIntOrNull() ?: return TouchGrabEvent.Unknown(line)

        // name= runs up to the first " x=" token.
        val name = Regex("name=(.*?)(?:\\s+x=|$)").find(body)?.groupValues?.get(1)?.trim().orEmpty()

        val xMin = xMatch?.groupValues?.get(1)?.toIntOrNull() ?: 0
        val xMax = xMatch?.groupValues?.get(2)?.toIntOrNull() ?: 0
        val yMin = yMatch?.groupValues?.get(1)?.toIntOrNull() ?: 0
        val yMax = yMatch?.groupValues?.get(2)?.toIntOrNull() ?: 0

        return TouchGrabEvent.Candidate(path, score, name, xMin, xMax, yMin, yMax)
    }

    /** Parse an unsigned 32-bit hex field (as printed by %08x) into a signed Int. */
    private fun hexToInt(token: String): Int? {
        val t = token.removePrefix("0x").removePrefix("0X")
        val long = t.toLongOrNull(16) ?: return null
        return long.toInt()
    }
}

enum class AbsAxis { SLOT, TRACKING_ID, POSITION_X, POSITION_Y }

sealed interface TouchGrabEvent {
    data object Ready : TouchGrabEvent
    data class Device(val path: String) : TouchGrabEvent
    data class Name(val name: String) : TouchGrabEvent
    data class Range(val xMin: Int, val xMax: Int, val yMin: Int, val yMax: Int) : TouchGrabEvent
    data class Candidate(
        val path: String,
        val score: Int,
        val name: String,
        val xMin: Int,
        val xMax: Int,
        val yMin: Int,
        val yMax: Int,
    ) : TouchGrabEvent
    data class Status(val status: String, val detail: String?) : TouchGrabEvent
    data class Error(val code: String, val message: String?) : TouchGrabEvent
    data class Abs(val axis: AbsAxis, val value: Int) : TouchGrabEvent
    data class SynReport(val value: Int) : TouchGrabEvent
    data class Unknown(val raw: String) : TouchGrabEvent
}
