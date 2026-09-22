package com.srtxcheats.sensitivity

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.srtxcheats.core.SystemCommandExecutor
import com.srtxcheats.data.dataStore
import com.srtxcheats.utils.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

data class SensitivityBackupEntry(
    val namespace: String,
    val key: String,
    val originalValue: String?,
    val existed: Boolean,
    val timestamp: Long
)

data class SensitivityRestoreResult(
    val success: Boolean,
    val message: String,
    val restoredCount: Int,
    val failedCount: Int,
    val restoredSettings: List<String> = emptyList()
)

class SensitivityBackupManager(private val context: Context) {

    companion object {
        private val KEY_SENSITIVITY_BACKUP_JSON = stringPreferencesKey("sensitivity_backup_entries_json")
    }

    /**
     * Reads all current backup entries from DataStore.
     */
    suspend fun getBackupEntries(): List<SensitivityBackupEntry> = withContext(Dispatchers.IO) {
        val prefs = context.dataStore.data.first()
        val rawJson = prefs[KEY_SENSITIVITY_BACKUP_JSON] ?: return@withContext emptyList()
        val entries = mutableListOf<SensitivityBackupEntry>()
        try {
            val arr = JSONArray(rawJson)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                entries.add(
                    SensitivityBackupEntry(
                        namespace = obj.getString("namespace"),
                        key = obj.getString("key"),
                        originalValue = if (obj.isNull("originalValue")) null else obj.getString("originalValue"),
                        existed = obj.getBoolean("existed"),
                        timestamp = obj.getLong("timestamp")
                    )
                )
            }
        } catch (e: Exception) {
            AppLogger.e("Failed to parse sensitivity backup entries: ${e.message}", e)
        }
        entries
    }

    /**
     * Checks if an existing baseline is saved.
     */
    suspend fun hasBackup(): Boolean = withContext(Dispatchers.IO) {
        getBackupEntries().isNotEmpty()
    }

    /**
     * Backs up a setting if not already backed up.
     * Before changing any device setting:
     * - read original value
     * - save original value
     * - save whether it existed
     * - save setting name & namespace
     * - save timestamp
     */
    suspend fun backupSettingIfAbsent(namespace: String, key: String) = withContext(Dispatchers.IO) {
        val currentEntries = getBackupEntries().toMutableList()
        if (currentEntries.any { it.namespace == namespace && it.key == key }) {
            return@withContext // Baseline already securely preserved
        }

        val currentValue = SystemCommandExecutor.readSetting(namespace, key)
        val entry = SensitivityBackupEntry(
            namespace = namespace,
            key = key,
            originalValue = currentValue,
            existed = currentValue != null,
            timestamp = System.currentTimeMillis()
        )
        currentEntries.add(entry)
        saveEntries(currentEntries)
        AppLogger.i("Backed up original setting: $namespace/$key = '$currentValue' (existed=${entry.existed})")
    }

    /**
     * Restores exact original settings saved before the boost:
     * Load backup -> restore values -> read again -> verify -> clear restored backup.
     */
    suspend fun restoreOriginal(): SensitivityRestoreResult = withContext(Dispatchers.IO) {
        val entries = getBackupEntries()
        if (entries.isEmpty()) {
            return@withContext SensitivityRestoreResult(
                success = true,
                message = "Device is already at native 1.0X baseline.",
                restoredCount = 0,
                failedCount = 0
            )
        }

        var restored = 0
        var failed = 0
        val remaining = mutableListOf<SensitivityBackupEntry>()
        val restoredList = mutableListOf<String>()

        for (entry in entries) {
            val ok = if (entry.existed && entry.originalValue != null) {
                // Restore original value and verify
                SystemCommandExecutor.writeSetting(entry.namespace, entry.key, entry.originalValue)
                val readBack = SystemCommandExecutor.readSetting(entry.namespace, entry.key)
                readBack == entry.originalValue
            } else {
                // It didn't exist originally; remove it and verify
                SystemCommandExecutor.execute("settings delete ${entry.namespace} ${entry.key}")
                val readBack = SystemCommandExecutor.readSetting(entry.namespace, entry.key)
                readBack == null
            }

            if (ok) {
                restored++
                restoredList.add("${entry.namespace}/${entry.key} -> ${entry.originalValue ?: "deleted"}")
            } else {
                failed++
                remaining.add(entry)
            }
        }

        // Save only remaining un-restored entries (or clear if all restored)
        saveEntries(remaining)

        val success = failed == 0
        val msg = if (success) {
            "Sensitivity restored to 1.0X baseline ($restored parameters restored)."
        } else {
            "Partially restored: $restored succeeded, $failed failed."
        }

        SensitivityRestoreResult(
            success = success,
            message = msg,
            restoredCount = restored,
            failedCount = failed,
            restoredSettings = restoredList
        )
    }

    /**
     * Clears all saved backup entries.
     */
    suspend fun clearBackup() = withContext(Dispatchers.IO) {
        saveEntries(emptyList())
    }

    private suspend fun saveEntries(entries: List<SensitivityBackupEntry>) {
        val arr = JSONArray()
        for (entry in entries) {
            val obj = JSONObject().apply {
                put("namespace", entry.namespace)
                put("key", entry.key)
                put("originalValue", entry.originalValue ?: JSONObject.NULL)
                put("existed", entry.existed)
                put("timestamp", entry.timestamp)
            }
            arr.put(obj)
        }
        context.dataStore.edit { prefs ->
            prefs[KEY_SENSITIVITY_BACKUP_JSON] = arr.toString()
        }
    }
}
