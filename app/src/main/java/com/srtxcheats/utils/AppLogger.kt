package com.srtxcheats.utils

import android.util.Log

object AppLogger {
    private const val TAG = "SRTXCheats"

    fun d(message: String) {
        Log.d(TAG, message)
    }

    fun i(message: String) {
        Log.i(TAG, message)
    }

    fun w(message: String, throwable: Throwable? = null) {
        Log.w(TAG, message, throwable)
    }

    fun e(message: String, throwable: Throwable? = null) {
        Log.e(TAG, message, throwable)
    }

    /**
     * Masks sensitive license keys: e.g. SRT_12345678ABCD -> SRT_1234****ABCD
     */
    fun maskKey(key: String?): String {
        if (key.isNullOrBlank()) return "[EMPTY_KEY]"
        val trimmed = key.trim()
        return if (trimmed.length > 8) {
            val prefix = trimmed.take(6)
            val suffix = trimmed.takeLast(4)
            "$prefix****$suffix"
        } else {
            "****"
        }
    }

    fun logCommand(command: String, exitCode: Int, output: String, error: String) {
        i("[CMD_EXEC] exitCode=$exitCode | cmd='$command' | out='${output.trim()}' | err='${error.trim()}'")
    }

    fun logDisplayChange(action: String, width: Int, height: Int, density: Int) {
        i("[DISPLAY_$action] target=${width}x${height} @ ${density}dpi")
    }

    fun logSensitivityChange(action: String, xPercent: Int, yPercent: Int, mappedValue: Int) {
        i("[SENSI_$action] X=${xPercent}% Y=${yPercent}% mappedValue=$mappedValue")
    }

    fun logKeyVerification(key: String, valid: Boolean, status: String) {
        i("[KEY_VERIFY] key=${maskKey(key)} valid=$valid status=$status")
    }
}
