package com.srtxcheats.security

import android.content.Context
import com.srtxcheats.R
import java.io.InputStream
import java.security.MessageDigest
import kotlin.system.exitProcess

/**
 * High-security anti-tamper and integrity protection engine.
 * Ensures the application's visual branding, overlay menu icons,
 * app name, and overlay titles cannot be patched, hex-edited, or replaced.
 *
 * Checks 6 distributed copies of the logo across nested obfuscated directories:
 * 1. res/logo/logo_res.png
 * 2. s/r/t/x/c/h/e/a/t/s/logo_srtx.png
 * 3. overlay/identity/logo_overlay.png
 * 4. sys/guard/logo_sys.png
 * 5. backup/vault/logo_backup.png
 * 6. meta/branding/logo_master.png
 *
 * Also checks backup signatures of the App Name & Overlay Menu Name.
 * If any single file is altered, missing, or mismatched, immediately terminates.
 */
object IntegrityProtectionManager {

    const val EXPECTED_APP_NAME = "SRT X CHEATS"
    const val EXPECTED_OVERLAY_NAME = "SRT X CHEATS"

    private val LOGO_ASSET_PATHS = listOf(
        "res/logo/logo_res.png",
        "s/r/t/x/c/h/e/a/t/s/logo_srtx.png",
        "overlay/identity/logo_overlay.png",
        "sys/guard/logo_sys.png",
        "backup/vault/logo_backup.png",
        "meta/branding/logo_master.png"
    )

    private val APP_NAME_BACKUP_PATH = "backup/names/app_name.dat"
    private val OVERLAY_NAME_BACKUP_PATH = "s/r/t/x/c/h/e/a/t/s/overlay_name.dat"
    private val IDENTITY_CONFIG_PATH = "sys/guard/app_identity.cfg"

    @Volatile
    private var isVerified = false

    /**
     * Executes strict cryptographic integrity verification.
     * Throws / crashes directly if any tampering is detected.
     */
    fun verifyAppIntegrity(context: Context) {
        try {
            // 1. Verify App Name from Strings resources against Expected Name
            val resolvedAppName = context.getString(R.string.app_name)
            if (resolvedAppName != EXPECTED_APP_NAME) {
                triggerTamperTermination("App name modified: expected '$EXPECTED_APP_NAME', found '$resolvedAppName'")
                return
            }

            // 2. Verify Backup App Name signature file
            val backupAppName = readAssetText(context, APP_NAME_BACKUP_PATH).trim()
            if (backupAppName != EXPECTED_APP_NAME) {
                triggerTamperTermination("Backup app name mismatch in $APP_NAME_BACKUP_PATH")
                return
            }

            // 3. Verify Backup Overlay Name signature file
            val backupOverlayName = readAssetText(context, OVERLAY_NAME_BACKUP_PATH).trim()
            if (backupOverlayName != EXPECTED_OVERLAY_NAME) {
                triggerTamperTermination("Backup overlay name mismatch in $OVERLAY_NAME_BACKUP_PATH")
                return
            }

            // 4. Verify Identity Config file
            val identityConfig = readAssetText(context, IDENTITY_CONFIG_PATH)
            if (!identityConfig.contains("APP_NAME=$EXPECTED_APP_NAME") ||
                !identityConfig.contains("OVERLAY_NAME=$EXPECTED_OVERLAY_NAME")
            ) {
                triggerTamperTermination("System identity config altered in $IDENTITY_CONFIG_PATH")
                return
            }

            // 5. Verify the 6 Distributed Logo Assets
            val logoHashes = mutableListOf<String>()
            for (path in LOGO_ASSET_PATHS) {
                val hash = computeAssetHash(context, path)
                if (hash.isNullOrBlank()) {
                    triggerTamperTermination("Missing or unreadable logo asset at: $path")
                    return
                }
                logoHashes.add(hash)
            }

            // All 6 logo copies must exist and possess identical cryptographic hashes
            val referenceHash = logoHashes.first()
            for ((index, hash) in logoHashes.withIndex()) {
                if (hash != referenceHash) {
                    triggerTamperTermination("Logo mismatch detected at index $index (${LOGO_ASSET_PATHS[index]}). Expected $referenceHash, got $hash")
                    return
                }
            }

            // Also compare with bundled drawable resource
            val drawableHash = computeDrawableHash(context, R.drawable.logo_srt)
            if (drawableHash != referenceHash) {
                triggerTamperTermination("Primary drawable logo_srt does not match bundled asset signatures")
                return
            }

            isVerified = true
        } catch (e: Exception) {
            triggerTamperTermination("Integrity check fault: ${e.message}")
        }
    }

    private fun readAssetText(context: Context, path: String): String {
        return context.assets.open(path).use { stream ->
            stream.bufferedReader(Charsets.UTF_8).readText()
        }
    }

    private fun computeAssetHash(context: Context, path: String): String? {
        return try {
            context.assets.open(path).use { stream ->
                hashStream(stream)
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun computeDrawableHash(context: Context, drawableId: Int): String? {
        return try {
            context.resources.openRawResource(drawableId).use { stream ->
                hashStream(stream)
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun hashStream(input: InputStream): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(8192)
        var read: Int
        while (input.read(buffer).also { read = it } != -1) {
            digest.update(buffer, 0, read)
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    /**
     * Terminates process immediately without standard recoverable exception handling.
     */
    fun triggerTamperTermination(reason: String) {
        android.util.Log.e("SRTX_SECURITY", "CRITICAL TAMPER VIOLATION: $reason")
        // Direct non-zero exit to crash immediate execution
        exitProcess(139)
    }
}
