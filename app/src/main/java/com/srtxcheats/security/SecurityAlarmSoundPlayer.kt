package com.srtxcheats.security

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.audiofx.LoudnessEnhancer
import com.srtxcheats.R
import com.srtxcheats.utils.AppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Security Alarm Sound Player:
 * Triggered when a user attempts to bypass license validation or enters invalid credentials.
 * Replays wrong_api_sound.mp3 exactly 5 times at maximum hardware volume with
 * LoudnessEnhancer acoustic boost (+35dB gain).
 */
object SecurityAlarmSoundPlayer {

    private var mediaPlayer: MediaPlayer? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null
    private var volumeEnforcerJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)
    private var remainingRepeats = 0
    private var isPlaying = false

    /**
     * Replays wrong_api_sound 5 times at maximum stream volume with LoudnessEnhancer amplification.
     */
    fun playAlarm5Times(context: Context) {
        scope.launch {
            stopCurrent()

            val audioManager = context.applicationContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                ?: return@launch

            val maxMusic = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            val maxAlarm = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
            val maxNotification = audioManager.getStreamMaxVolume(AudioManager.STREAM_NOTIFICATION)

            try {
                audioManager.setStreamVolume(AudioManager.STREAM_ALARM, maxAlarm, 0)
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxMusic, AudioManager.FLAG_PLAY_SOUND)
                audioManager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, maxNotification, 0)
            } catch (_: Exception) {}

            remainingRepeats = 4 // 1 initial play + 4 repeats = 5 times total
            isPlaying = true

            try {
                val mp = MediaPlayer.create(context.applicationContext, R.raw.wrong_api_sound) ?: run {
                    AppLogger.e("Failed to create MediaPlayer for wrong_api_sound")
                    return@launch
                }

                mediaPlayer = mp

                mp.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                mp.setVolume(1.0f, 1.0f)

                // Attach LoudnessEnhancer audio effect (+35 dB hardware acoustic boost)
                try {
                    val enhancer = LoudnessEnhancer(mp.audioSessionId).apply {
                        setTargetGain(3500) // 3500 mB = +35 dB acoustic gain
                        enabled = true
                    }
                    loudnessEnhancer = enhancer
                } catch (e: Exception) {
                    AppLogger.w("LoudnessEnhancer initialization warning: ${e.message}")
                }

                // Continuously re-enforce maximum volume while alarm is active
                volumeEnforcerJob?.cancel()
                volumeEnforcerJob = scope.launch(Dispatchers.IO) {
                    while (isActive && isPlaying) {
                        try {
                            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, maxAlarm, 0)
                            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxMusic, 0)
                        } catch (_: Exception) {}
                        delay(250)
                    }
                }

                mp.setOnCompletionListener { player ->
                    if (remainingRepeats > 0) {
                        remainingRepeats--
                        try {
                            player.seekTo(0)
                            player.start()
                        } catch (_: Exception) {
                            stopCurrent()
                        }
                    } else {
                        stopCurrent()
                    }
                }

                mp.setOnErrorListener { _, _, _ ->
                    stopCurrent()
                    true
                }

                mp.start()
                AppLogger.w("🚨 Security Alarm Active: 5x repeat playback initialized at max volume with +35dB LoudnessEnhancer.")
            } catch (e: Exception) {
                AppLogger.e("SecurityAlarmSoundPlayer error: ${e.message}")
                stopCurrent()
            }
        }
    }

    fun stopCurrent() {
        isPlaying = false
        volumeEnforcerJob?.cancel()
        volumeEnforcerJob = null

        try {
            loudnessEnhancer?.release()
        } catch (_: Exception) {}
        loudnessEnhancer = null

        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (_: Exception) {}
        mediaPlayer = null
    }
}
