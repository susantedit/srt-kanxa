package com.srtxcheats.core

import android.content.Context
import android.media.MediaScannerConnection
import android.os.Environment
import com.srtxcheats.utils.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ScreenCaptureState(
    val isRecording: Boolean = false,
    val recordingDurationSec: Int = 0,
    val lastScreenshotPath: String? = null,
    val lastVideoPath: String? = null,
    val statusMessage: String = "Ready"
)

class ScreenCaptureManager(private val context: Context) {

    private val _captureState = MutableStateFlow(ScreenCaptureState())
    val captureState: StateFlow<ScreenCaptureState> = _captureState.asStateFlow()

    private var currentRecordFile: File? = null
    private var isRecordingInternal = false

    /**
     * Captures an instant high-resolution screenshot.
     */
    suspend fun captureScreenshot(): String? = withContext(Dispatchers.IO) {
        val picturesDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "SRT_Screenshots")
        if (!picturesDir.exists()) picturesDir.mkdirs()

        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val screenshotFile = File(picturesDir, "SRT_FF_${timeStamp}.png")

        val cmd = "screencap -p \"${screenshotFile.absolutePath}\""
        val res = ShizukuManager.executeCommand(cmd)

        if (res.exitCode == 0 && screenshotFile.exists() && screenshotFile.length() > 0) {
            // Index file into Android system gallery
            MediaScannerConnection.scanFile(
                context,
                arrayOf(screenshotFile.absolutePath),
                arrayOf("image/png"),
                null
            )
            AppLogger.i("Screenshot saved: ${screenshotFile.absolutePath}")
            _captureState.value = _captureState.value.copy(
                lastScreenshotPath = screenshotFile.absolutePath,
                statusMessage = "Screenshot saved to Pictures/SRT_Screenshots"
            )
            return@withContext screenshotFile.absolutePath
        } else {
            AppLogger.w("Privileged screencap failed: ${res.error}")
            _captureState.value = _captureState.value.copy(
                statusMessage = "Screenshot requires Shizuku/Root authorization"
            )
            return@withContext null
        }
    }

    /**
     * Starts high-framerate screen recording.
     */
    suspend fun startRecording(): Boolean = withContext(Dispatchers.IO) {
        if (isRecordingInternal) return@withContext false

        val moviesDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), "SRT_Recordings")
        if (!moviesDir.exists()) moviesDir.mkdirs()

        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val recordFile = File(moviesDir, "FF_Recording_${timeStamp}.mp4")
        currentRecordFile = recordFile

        // Run screenrecord in background
        val cmd = "screenrecord --bit-rate 16000000 --time-limit 300 \"${recordFile.absolutePath}\""
        
        isRecordingInternal = true
        _captureState.value = _captureState.value.copy(
            isRecording = true,
            recordingDurationSec = 0,
            statusMessage = "Recording gameplay at 16Mbps..."
        )

        // Launch async shell command
        kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
            ShizukuManager.executeCommand(cmd)
            isRecordingInternal = false
            _captureState.value = _captureState.value.copy(
                isRecording = false,
                lastVideoPath = recordFile.absolutePath,
                statusMessage = "Recording saved to Movies/SRT_Recordings"
            )
            MediaScannerConnection.scanFile(
                context,
                arrayOf(recordFile.absolutePath),
                arrayOf("video/mp4"),
                null
            )
        }

        // Timer loop
        kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
            var secs = 0
            while (isRecordingInternal) {
                delay(1000L)
                secs++
                _captureState.value = _captureState.value.copy(recordingDurationSec = secs)
            }
        }

        true
    }

    /**
     * Stops the active screen recording cleanly.
     */
    suspend fun stopRecording(): String? = withContext(Dispatchers.IO) {
        if (!isRecordingInternal) return@withContext null

        isRecordingInternal = false
        // SIGINT to stop screenrecord cleanly and finalize MP4 atoms
        ShizukuManager.executeCommand("pkill -2 screenrecord")
        delay(600L)

        val filePath = currentRecordFile?.absolutePath
        if (filePath != null) {
            MediaScannerConnection.scanFile(
                context,
                arrayOf(filePath),
                arrayOf("video/mp4"),
                null
            )
        }

        _captureState.value = _captureState.value.copy(
            isRecording = false,
            lastVideoPath = filePath,
            statusMessage = "Recording saved: ${currentRecordFile?.name}"
        )
        filePath
    }

    suspend fun toggleRecording(): Boolean {
        return if (_captureState.value.isRecording) {
            stopRecording()
            false
        } else {
            startRecording()
        }
    }
}
