package com.srtxcheats.keySystem

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.srtxcheats.data.dataStore
import com.srtxcheats.security.DecoyBackendDispatcher
import com.srtxcheats.utils.AppLogger
import com.srtxcheats.utils.DeviceInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class KeyRepository(private val context: Context) {

    companion object {
        // Base64-obfuscated genuine server endpoint to prevent static binary inspection
        private val OBFUSCATED_BASE_URL = String(android.util.Base64.decode("aHR0cHM6Ly9mcHNhcHAub25yZW5kZXIuY29t", android.util.Base64.DEFAULT)).trim()
        const val VERIFY_ENDPOINT = "/api/verify-key"
        const val GET_KEY_URL = "https://cheats.xo.je"

        val BASE_URL: String get() = OBFUSCATED_BASE_URL

        private val KEY_SAVED_LICENSE_KEY = stringPreferencesKey("license_key")
        private val KEY_SAVED_HWID = stringPreferencesKey("license_hwid")
        private val KEY_SAVED_EXPIRES_AT = stringPreferencesKey("license_expires_at")
        private val KEY_SAVED_DAYS_LEFT = intPreferencesKey("license_days_left")
        private val KEY_SAVED_TYPE = stringPreferencesKey("license_type")
        private val KEY_SAVED_TIMESTAMP = longPreferencesKey("license_timestamp")
        private val KEY_SAVED_IS_VALID = booleanPreferencesKey("license_is_valid")
    }

    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    val licenseSessionFlow: Flow<LicenseSession> = context.dataStore.data.map { prefs ->
        val key = prefs[KEY_SAVED_LICENSE_KEY] ?: ""
        val hwid = prefs[KEY_SAVED_HWID] ?: ""
        val expiresAt = prefs[KEY_SAVED_EXPIRES_AT] ?: ""
        val daysLeft = prefs[KEY_SAVED_DAYS_LEFT] ?: 0
        val type = prefs[KEY_SAVED_TYPE] ?: "FREE"
        val timestamp = prefs[KEY_SAVED_TIMESTAMP] ?: 0L
        val isValid = prefs[KEY_SAVED_IS_VALID] ?: false

        LicenseSession(
            key = key,
            hwid = hwid,
            expiresAt = expiresAt,
            daysLeft = daysLeft,
            type = type,
            verifiedTimestamp = timestamp,
            isValid = isValid && key.isNotBlank()
        )
    }

    suspend fun getCachedSession(): LicenseSession {
        return licenseSessionFlow.first()
    }

    /**
     * Verifies key against the Render backend:
     * GET https://fpsapp.onrender.com/api/verify-key?key=KEY&hwid=HWID
     */
    suspend fun verifyKeyOnline(key: String): Result<VerifyKeyResponse> = withContext(Dispatchers.IO) {
        val trimmedKey = key.trim()
        val hwid = DeviceInfo.getHwid(context)

        // Launch 10 decoy requests to confuse network crackers/sniffers
        DecoyBackendDispatcher.dispatchDecoyRequests(trimmedKey, hwid)

        AppLogger.i("Verifying license key: ${AppLogger.maskKey(trimmedKey)} for HWID: ${hwid.take(8)}...")

        try {
            val encodedKey = URLEncoder.encode(trimmedKey, "UTF-8")
            val encodedHwid = URLEncoder.encode(hwid, "UTF-8")
            val url = "$BASE_URL$VERIFY_ENDPOINT?key=$encodedKey&hwid=$encodedHwid"

            val request = Request.Builder()
                .url(url)
                .addHeader("User-Agent", "SRTXCheats-Android/1.0")
                .addHeader("Accept", "application/json")
                .get()
                .build()

            httpClient.newCall(request).execute().use { response ->
                val code = response.code
                val bodyString = response.body?.string().orEmpty()

                if (code in 500..599) {
                    AppLogger.w("Server returned 5xx error: $code")
                    return@withContext Result.failure(Exception("SERVER_OFFLINE"))
                }

                if (bodyString.isBlank()) {
                    return@withContext Result.failure(Exception("EMPTY_RESPONSE"))
                }

                val json = try {
                    JSONObject(bodyString)
                } catch (e: Exception) {
                    AppLogger.e("Malformed JSON from server: $bodyString", e)
                    return@withContext Result.failure(Exception("MALFORMED_JSON"))
                }

                val isValid = json.optBoolean("valid", false)
                val message = json.optString("message", "")
                val respKey = json.optString("key", trimmedKey)
                val expiresAt = json.optString("expiresAt", "")
                val daysLeft = json.optInt("daysLeft", 0)
                val loginCount = json.optInt("loginCount", 1)
                val loginLimit = json.optInt("loginLimit", 1)
                val type = json.optString("type", "PREMIUM")

                val result = VerifyKeyResponse(
                    valid = isValid,
                    message = message,
                    key = respKey,
                    expiresAt = expiresAt,
                    daysLeft = daysLeft,
                    loginCount = loginCount,
                    loginLimit = loginLimit,
                    type = type
                )

                if (isValid) {
                    saveLicenseSession(result, hwid)
                    AppLogger.i("Key verified successfully! Days left: $daysLeft")
                } else {
                    AppLogger.w("Key verification rejected: $message")
                }

                Result.success(result)
            }
        } catch (e: java.net.SocketTimeoutException) {
            AppLogger.w("Connection timed out verifying key")
            Result.failure(Exception("TIMEOUT"))
        } catch (e: java.net.UnknownHostException) {
            AppLogger.w("Network DNS resolution failed (offline)")
            Result.failure(Exception("OFFLINE"))
        } catch (e: Exception) {
            AppLogger.e("Network verification error: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun saveLicenseSession(response: VerifyKeyResponse, hwid: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SAVED_LICENSE_KEY] = response.key.orEmpty()
            prefs[KEY_SAVED_HWID] = hwid
            prefs[KEY_SAVED_EXPIRES_AT] = response.expiresAt.orEmpty()
            prefs[KEY_SAVED_DAYS_LEFT] = response.daysLeft ?: 0
            prefs[KEY_SAVED_TYPE] = response.type ?: "PREMIUM"
            prefs[KEY_SAVED_TIMESTAMP] = System.currentTimeMillis()
            prefs[KEY_SAVED_IS_VALID] = response.valid
        }
    }

    suspend fun clearSession() {
        context.dataStore.edit { prefs ->
            prefs.remove(KEY_SAVED_LICENSE_KEY)
            prefs.remove(KEY_SAVED_HWID)
            prefs.remove(KEY_SAVED_EXPIRES_AT)
            prefs.remove(KEY_SAVED_DAYS_LEFT)
            prefs.remove(KEY_SAVED_TYPE)
            prefs.remove(KEY_SAVED_TIMESTAMP)
            prefs[KEY_SAVED_IS_VALID] = false
        }
        AppLogger.i("License session cleared")
    }

    /**
     * Checks if current license session has expired.
     */
    fun isSessionExpired(session: LicenseSession): Boolean {
        if (!session.isValid || session.key.isBlank()) return true
        if (session.daysLeft <= 0) return true

        if (session.expiresAt.isNotBlank()) {
            try {
                val cleanDate = session.expiresAt.replace("Z", "").take(19)
                val format = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US)
                val parsed = format.parse(cleanDate)
                if (parsed != null && System.currentTimeMillis() > parsed.time) {
                    return true
                }
            } catch (_: Exception) {}
        }

        if (session.verifiedTimestamp > 0L && session.daysLeft > 0) {
            val totalAllowedDuration = session.daysLeft.toLong() * 24L * 60L * 60L * 1000L
            if (System.currentTimeMillis() > (session.verifiedTimestamp + totalAllowedDuration)) {
                return true
            }
        }
        return false
    }

    /**
     * Formats available duration for UI display.
     */
    fun formatRemainingDuration(session: LicenseSession): String {
        if (!session.isValid || session.key.isBlank()) return "No Active Key"
        if (session.daysLeft <= 0) return "Expired"

        val daysStr = "${session.daysLeft} Day${if (session.daysLeft > 1) "s" else ""} Left"
        return if (session.expiresAt.isNotBlank()) {
            val dateDisplay = session.expiresAt.take(10)
            "$daysStr (Valid until: $dateDisplay)"
        } else {
            daysStr
        }
    }
}
