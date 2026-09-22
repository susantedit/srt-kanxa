package com.srtxcheats.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.srtxcheats.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

object LogoHelper {
    const val REMOTE_LOGO_URL = "https://i.postimg.cc/d3X5rZ53/file-00000000e4a481f8acf8e5ada6d7e33e.png"
    private const val CACHED_LOGO_FILENAME = "srt_cached_logo.png"

    @Volatile
    private var inMemoryCachedBitmap: Bitmap? = null

    /**
     * Obtains the SRT X CHEATS logo bitmap.
     * 1. Returns in-memory cache if available.
     * 2. Returns local disk cached file if available.
     * 3. Falls back to bundled drawable R.drawable.logo_srt.
     * Guarantees zero repeated network requests during overlay execution.
     */
    fun getLogoBitmap(context: Context): Bitmap {
        inMemoryCachedBitmap?.let { return it }

        val cacheFile = File(context.filesDir, CACHED_LOGO_FILENAME)
        if (cacheFile.exists() && cacheFile.length() > 0) {
            try {
                val bmp = BitmapFactory.decodeFile(cacheFile.absolutePath)
                if (bmp != null) {
                    inMemoryCachedBitmap = bmp
                    return bmp
                }
            } catch (_: Exception) {
                // Ignore and fall back to bundled
            }
        }

        // Fallback to bundled resource
        val bundled = BitmapFactory.decodeResource(context.resources, R.drawable.logo_srt)
        inMemoryCachedBitmap = bundled
        return bundled
    }

    /**
     * Checks if local cache exists, if not, downloads in the background once and saves to disk.
     */
    suspend fun preloadAndCacheLogo(context: Context) {
        withContext(Dispatchers.IO) {
            val cacheFile = File(context.filesDir, CACHED_LOGO_FILENAME)
            if (cacheFile.exists() && cacheFile.length() > 0) return@withContext

            try {
                val url = URL(REMOTE_LOGO_URL)
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 5000
                conn.readTimeout = 5000
                conn.doInput = true
                conn.connect()
                if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                    conn.inputStream.use { input ->
                        FileOutputStream(cacheFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                    val bmp = BitmapFactory.decodeFile(cacheFile.absolutePath)
                    if (bmp != null) {
                        inMemoryCachedBitmap = bmp
                    }
                }
                conn.disconnect()
            } catch (_: Exception) {
                // Network unavailable or offline, keep bundled fallback
            }
        }
    }
}
