package com.srtxcheats.display

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import com.srtxcheats.data.dataStore
import com.srtxcheats.utils.AppLogger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

data class DisplayBackup(
    val originalWidth: Int = 1080,
    val originalHeight: Int = 2400,
    val originalDensity: Int = 420,
    val originalOrientation: Int = 1, // 1 = Portrait, 2 = Landscape
    val savedAt: Long = 0L,
    val isValid: Boolean = false
) {
    val formattedResolution: String
        get() = "${originalWidth} × ${originalHeight}"
}

class DisplayBackupManager(private val context: Context) {

    companion object {
        private val KEY_ORIG_WIDTH = intPreferencesKey("original_display_width")
        private val KEY_ORIG_HEIGHT = intPreferencesKey("original_display_height")
        private val KEY_ORIG_DENSITY = intPreferencesKey("original_density")
        private val KEY_ORIG_ORIENTATION = intPreferencesKey("original_orientation")
        private val KEY_BACKUP_SAVED_AT = longPreferencesKey("display_backup_timestamp")
        private val KEY_BACKUP_VALID = booleanPreferencesKey("display_backup_valid")
    }

    val backupFlow: Flow<DisplayBackup> = context.dataStore.data.map { prefs ->
        DisplayBackup(
            originalWidth = prefs[KEY_ORIG_WIDTH] ?: 1080,
            originalHeight = prefs[KEY_ORIG_HEIGHT] ?: 2400,
            originalDensity = prefs[KEY_ORIG_DENSITY] ?: 420,
            originalOrientation = prefs[KEY_ORIG_ORIENTATION] ?: 1,
            savedAt = prefs[KEY_BACKUP_SAVED_AT] ?: 0L,
            isValid = prefs[KEY_BACKUP_VALID] ?: false
        )
    }

    suspend fun getBackup(): DisplayBackup {
        return backupFlow.first()
    }

    /**
     * Captures original display baseline.
     * Prevents overwriting an existing valid backup so repeated "Apply" operations
     * do not corrupt the baseline.
     */
    suspend fun saveBaselineIfEmpty(width: Int, height: Int, density: Int, orientation: Int): Boolean {
        val existing = getBackup()
        if (existing.isValid && existing.originalWidth > 0 && existing.originalHeight > 0) {
            AppLogger.i("Preserving existing display baseline: ${existing.formattedResolution} @ ${existing.originalDensity}dpi")
            return false
        }

        context.dataStore.edit { prefs ->
            prefs[KEY_ORIG_WIDTH] = width
            prefs[KEY_ORIG_HEIGHT] = height
            prefs[KEY_ORIG_DENSITY] = density
            prefs[KEY_ORIG_ORIENTATION] = orientation
            prefs[KEY_BACKUP_SAVED_AT] = System.currentTimeMillis()
            prefs[KEY_BACKUP_VALID] = true
        }
        AppLogger.i("Created new display baseline backup: ${width}x${height} @ ${density}dpi")
        return true
    }

    suspend fun forceResetBaseline(width: Int, height: Int, density: Int, orientation: Int) {
        context.dataStore.edit { prefs ->
            prefs[KEY_ORIG_WIDTH] = width
            prefs[KEY_ORIG_HEIGHT] = height
            prefs[KEY_ORIG_DENSITY] = density
            prefs[KEY_ORIG_ORIENTATION] = orientation
            prefs[KEY_BACKUP_SAVED_AT] = System.currentTimeMillis()
            prefs[KEY_BACKUP_VALID] = true
        }
        AppLogger.i("Force reset display baseline: ${width}x${height} @ ${density}dpi")
    }
}
