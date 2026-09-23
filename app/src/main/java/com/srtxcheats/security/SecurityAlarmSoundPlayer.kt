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
 * Security Alarm & Sequential Sound Player:
 * Replays 1.mp3 -> 2.mp3 -> 3.mp3 -> 4.mp3 -> last.mp3 in strict sequential order.
 * Plays at maximum hardware stream volume with LoudnessEnhancer acoustic gain (+35dB).
 * Used when "FREE PREMIUM ACCESS" is clicked and when wrong credentials/bypass attempts occur.
 */
object SecurityAlarmSoundPlayer {

    private val SEQUENCE_RES_IDS = listOf(
        R.raw.sound_1,
        R.raw.sound_2,
        R.raw.sound_3,
        R.raw.sound_4,
        R.raw.sound_5,
        R.raw.sound_last
    )

    private var mediaPlayer: MediaPlayer? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null
    private var volumeEnforcerJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)
    private var isPlaying = false

    /**
     * Plays the audio sequence in order:
     * 1.mp3 -> 2.mp3 -> 3.mp3 -> 4.mp3 -> last.mp3
     * with maximum volume and +35dB acoustic gain.
     */
    fun playAudioSequence(context: Context) {
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

            isPlaying = true

            // Volume enforcer: keeps volume maximized throughout playback
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

            playTrackAtIndex(context.applicationContext, 0)
        }
    }

    private fun playTrackAtIndex(appContext: Context, index: Int) {
        if (!isPlaying || index >= SEQUENCE_RES_IDS.size) {
            stopCurrent()
            return
        }

        try {
            loudnessEnhancer?.release()
        } catch (_: Exception) {}
        loudnessEnhancer = null

        try {
            mediaPlayer?.release()
        } catch (_: Exception) {}
        mediaPlayer = null

        try {
            val resId = SEQUENCE_RES_IDS[index]
            val mp = MediaPlayer.create(appContext, resId) ?: run {
                AppLogger.w("Failed to load track $index, skipping to next")
                playTrackAtIndex(appContext, index + 1)
                return
            }

            mediaPlayer = mp
            mp.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            mp.setVolume(1.0f, 1.0f)

            // Attach hardware LoudnessEnhancer for maximum gain amplification
            try {
                val enhancer = LoudnessEnhancer(mp.audioSessionId).apply {
                    setTargetGain(3500) // 3500 mB = +35 dB hardware acoustic gain
                    enabled = true
                }
                loudnessEnhancer = enhancer
            } catch (e: Exception) {
                AppLogger.w("LoudnessEnhancer error: ${e.message}")
            }

            mp.setOnCompletionListener {
                playTrackAtIndex(appContext, index + 1)
            }

            mp.setOnErrorListener { _, _, _ ->
                playTrackAtIndex(appContext, index + 1)
                true
            }

            mp.start()
            AppLogger.i("▶️ Playing sequence track ${index + 1}/${SEQUENCE_RES_IDS.size}")
        } catch (e: Exception) {
            AppLogger.e("Error playing track index $index: ${e.message}")
            playTrackAtIndex(appContext, index + 1)
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

    fun playAlarm5Times(context: Context) {
        playAudioSequence(context)
    }
}
