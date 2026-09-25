package com.srtxcheats.macro

import android.content.Context
import android.util.Log
import java.io.File

/** Lightweight macro listing entry for the UI — no frame payload, cheap to hold in state. */
data class MacroSummary(
    val id: String,
    val name: String,
    val frameCount: Int,
    val durationMs: Long,
    val createdAtMs: Long,
)

/**
 * Persists recorded macros as individual files under `filesDir/macros`, one `<id>.srtmacro`
 * file each in the [MacroCodec] text format. Every call does blocking IO — invoke off the main
 * thread (the ViewModel dispatches these on Dispatchers.IO).
 */
class MacroStore(context: Context) {

    private val dir = File(context.filesDir, DIR_NAME)

    private fun ensureDir() {
        if (!dir.exists()) dir.mkdirs()
    }

    /** All stored macros, newest first. Corrupt files are skipped rather than throwing. */
    fun list(): List<Macro> {
        ensureDir()
        val files = dir.listFiles { f -> f.isFile && f.name.endsWith(EXT) } ?: return emptyList()
        return files
            .mapNotNull { f -> runCatching { MacroCodec.decode(f.nameWithoutExtension, f.readText()) }.getOrNull() }
            .sortedByDescending { it.createdAtMs }
    }

    fun summaries(): List<MacroSummary> = list().map {
        MacroSummary(it.id, it.name, it.frameCount, it.durationMs, it.createdAtMs)
    }

    fun read(id: String): Macro? {
        val f = File(dir, id + EXT)
        if (!f.exists()) return null
        return runCatching { MacroCodec.decode(id, f.readText()) }.getOrNull()
    }

    fun save(macro: Macro): Macro {
        ensureDir()
        runCatching { File(dir, macro.id + EXT).writeText(MacroCodec.encode(macro)) }
            .onFailure { Log.e(TAG, "save failed for ${macro.id}", it) }
        return macro
    }

    /** Persist a freshly-recorded serialized macro under a new id and the given display name. */
    fun saveRecorded(serialized: String, name: String): Macro? {
        val decoded = MacroCodec.decode(newId(), serialized) ?: return null
        return save(decoded.copy(name = name.ifBlank { "Macro" }))
    }

    fun rename(id: String, newName: String): Macro? {
        val m = read(id) ?: return null
        return save(m.copy(name = newName.ifBlank { m.name }))
    }

    fun delete(id: String): Boolean =
        runCatching { File(dir, id + EXT).delete() }.getOrDefault(false)

    private fun newId(): String = "m" + System.currentTimeMillis()

    companion object {
        private const val TAG = "MacroStore"
        private const val DIR_NAME = "macros"
        private const val EXT = ".srtmacro"
    }
}
