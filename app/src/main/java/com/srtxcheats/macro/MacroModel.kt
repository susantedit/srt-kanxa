package com.srtxcheats.macro

/**
 * Pure data model for the macro subsystem — no Android dependencies so it is unit-testable on the JVM.
 *
 * A [Macro] is an ordered list of [MacroFrame]s. Each frame is a snapshot of every pointer that is
 * down at a given moment (protocol-B multi-touch), timestamped relative to the start of the recording.
 * Playback re-emits the frames on the same cadence (optionally speed-scaled) through the existing
 * touch-injection path, so a recorded gesture reproduces exactly what the user's fingers did.
 */

/** One pointer within a recorded frame. Coordinates are in the digitizer's ABS space (see [Macro] range). */
data class MacroPointer(
    val slot: Int,
    val trackingId: Int,
    val x: Float,
    val y: Float,
)

/**
 * A snapshot of all pointers that are down at [atMs] (milliseconds since the recording started).
 * An empty [pointers] list means every finger is up at that instant (a gap between taps).
 */
data class MacroFrame(
    val atMs: Long,
    val pointers: List<MacroPointer>,
)

/**
 * A named, persistable recorded gesture. [xMin]/[xMax]/[yMin]/[yMax] capture the digitizer range the
 * recording was made against, so playback can map ABS coordinates back to screen pixels correctly.
 */
data class Macro(
    val id: String,
    val name: String,
    val xMin: Int,
    val xMax: Int,
    val yMin: Int,
    val yMax: Int,
    val frames: List<MacroFrame>,
    val createdAtMs: Long,
) {
    /** Total length of the recording, i.e. the timestamp of the last frame. */
    val durationMs: Long get() = frames.lastOrNull()?.atMs ?: 0L

    val frameCount: Int get() = frames.size

    val isEmpty: Boolean get() = frames.isEmpty()

    /** True when the recorder captured a usable digitizer range (needed to map back to the screen). */
    val hasRange: Boolean get() = xMax > xMin && yMax > yMin
}

/** Playback state exposed to the UI. Mirrors what the privileged service is currently doing. */
enum class MacroPhase { IDLE, RECORDING, PLAYING, AUTOCLICK, ERROR }
