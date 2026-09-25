package com.srtxcheats.security

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.audiofx.LoudnessEnhancer
import android.os.Build
import com.srtxcheats.R
import com.srtxcheats.utils.AppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/**
 * Security Alarm & Sequential Sound Player.
 *
 * Replays sound_1 → sound_2 → sound_3 → sound_4 → sound_5 → sound_last in strict
 * order at maximum hardware volume with LoudnessEnhancer acoustic gain (+35 dB).
 *
 * When *armed* (a wrong license key / bypass attempt), it does not stop after one
 * pass: it loops the whole sequence continuously, re-maximises every audio stream
 * a few times a second, and holds audio focus so nothing else can duck it. The
 * only legitimate way out is [disarm] — invoked when a valid key is finally
 * accepted. [SecurityAlarmService] keeps the alarm alive as a foreground service
 * so leaving the login screen (or the system killing the process) cannot silence
 * it; the persisted [armed] flag re-arms it on restart.
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

    private const val PREFS = "srt_alarm_state"
    private const val KEY_ARMED = "armed"

    private var mediaPlayer: MediaPlayer? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null
    private var volumeEnforcerJob: Job? = null
    private var playbackJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    @Volatile private var isPlaying = false
    @Volatile private var armed = false
    private var audioManager: AudioManager? = null
    private var focusRequest: AudioFocusRequest? = null

    /** True once a wrong-key alarm has been armed and not yet disarmed. */
    fun isArmed(): Boolean = armed

    /** Persisted across process death so [SecurityAlarmService] can re-arm. */
    fun wasArmed(context: Context): Boolean =
        context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_ARMED, false)

    private fun persistArmed(context: Context, value: Boolean) {
        runCatching {
            context.applicationContext
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_ARMED, value)
                .apply()
        }
    }

    /**
     * Arm the alarm: start the looping sequence and launch the foreground service
     * that keeps it alive. Idempotent while already armed.
     */
    fun arm(context: Context) {
        val appContext = context.applicationContext
        persistArmed(appContext, true)
        armed = true
        SecurityAlarmService.start(appContext)
        startLoop(appContext, loop = true)
    }

    /**
     * The only legitimate stop: clears the armed latch, stops playback, releases
     * audio focus, and lets the foreground service exit. Called when a valid key
     * is accepted.
     */
    fun disarm(context: Context) {
        armed = false
        persistArmed(context, false)
        stopCurrent()
        SecurityAlarmService.stop(context.applicationContext)
    }

    /**
     * Plays the sequence once (used by non-alarm surfaces, e.g. the decoy
     * "FREE PREMIUM ACCESS" button). Does not arm the persistent latch.
     */
    fun playAudioSequence(context: Context) {
        startLoop(context.applicationContext, loop = false)
    }

    /** Wrong-key entry point: arms the unstoppable looping alarm. */
    fun playAlarm5Times(context: Context) {
        arm(context)
    }

    /**
     * Called by [SecurityAlarmService] on (re)start. If the persisted latch says
     * we should be blaring, resume the loop; the service having been recreated by
     * the system is exactly the "can't be killed" path.
     */
    fun resumeIfArmed(context: Context) {
        if (wasArmed(context)) {
            armed = true
            startLoop(context.applicationContext, loop = true)
        }
    }

    private fun startLoop(appContext: Context, loop: Boolean) {
        stopCurrent()
        isPlaying = true

        val am = appContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        audioManager = am
        if (am != null) {
            requestFocus(am)
            maximizeVolume(am)
            // Volume enforcer: keeps every relevant stream pinned to max and
            // re-acquires focus if something steals it, for as long as we play.
            volumeEnforcerJob = scope.launch(Dispatchers.IO) {
                while (isActive && isPlaying) {
                    maximizeVolume(am)
                    if (armed) requestFocus(am)
                    delay(300)
                }
            }
        }

        playbackJob = scope.launch {
            do {
                for (index in SEQUENCE_RES_IDS.indices) {
                    if (!isActive || !isPlaying) break
                    playTrack(appContext, SEQUENCE_RES_IDS[index], index)
                    delay(80) // let hardware audio buffers clear between tracks
                }
                // While armed we never stop; loop the whole sequence again.
            } while (loop && armed && isActive && isPlaying)
            if (!armed) stopCurrent()
        }
    }

    private fun maximizeVolume(am: AudioManager) {
        try {
            am.setStreamVolume(AudioManager.STREAM_ALARM, am.getStreamMaxVolume(AudioManager.STREAM_ALARM), 0)
            am.setStreamVolume(AudioManager.STREAM_MUSIC, am.getStreamMaxVolume(AudioManager.STREAM_MUSIC), 0)
            am.setStreamVolume(AudioManager.STREAM_NOTIFICATION, am.getStreamMaxVolume(AudioManager.STREAM_NOTIFICATION), 0)
        } catch (_: Exception) {}
    }

    private fun requestFocus(am: AudioManager) {
        try {
            val attrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val req = focusRequest ?: AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(attrs)
                    .setWillPauseWhenDucked(false)
                    .build()
                    .also { focusRequest = it }
                am.requestAudioFocus(req)
            } else {
                @Suppress("DEPRECATION")
                am.requestAudioFocus(null, AudioManager.STREAM_ALARM, AudioManager.AUDIOFOCUS_GAIN)
            }
        } catch (_: Exception) {}
    }

    private fun abandonFocus() {
        val am = audioManager ?: return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                focusRequest?.let { am.abandonAudioFocusRequest(it) }
            } else {
                @Suppress("DEPRECATION")
                am.abandonAudioFocus(null)
            }
        } catch (_: Exception) {}
    }

    private suspend fun playTrack(appContext: Context, resId: Int, index: Int) {
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        var player: MediaPlayer? = null
        try {
            // Overloaded MediaPlayer.create sets AudioAttributes BEFORE prepare
            player = MediaPlayer.create(appContext, resId, attributes, 0)
            if (player == null) {
                AppLogger.w("Failed to load track $index, skipping to next")
                return
            }

            mediaPlayer = player
            player.setVolume(1.0f, 1.0f)

            // Attach hardware LoudnessEnhancer for maximum gain amplification
            try {
                loudnessEnhancer?.release()
                loudnessEnhancer = LoudnessEnhancer(player.audioSessionId).apply {
                    setTargetGain(3500) // 3500 mB = +35 dB hardware acoustic gain
                    enabled = true
                }
            } catch (e: Exception) {
                AppLogger.w("LoudnessEnhancer error: ${e.message}")
            }

            val trackDurationMs = try {
                player.duration.toLong().coerceAtLeast(500L)
            } catch (_: Exception) {
                5000L
            }

            AppLogger.i("▶️ Playing sequence track ${index + 1}/${SEQUENCE_RES_IDS.size} (duration: ${trackDurationMs}ms)")

            // Suspend until completion or fallback timeout
            withTimeoutOrNull(trackDurationMs + 1500L) {
                suspendCancellableCoroutine<Unit> { continuation ->
                    player.setOnCompletionListener {
                        if (continuation.isActive) {
                            continuation.resume(Unit)
                        }
                    }

                    player.setOnErrorListener { _, what, extra ->
                        AppLogger.w("MediaPlayer error on track $index ($what, $extra)")
                        if (continuation.isActive) {
                            continuation.resume(Unit)
                        }
                        true
                    }

                    continuation.invokeOnCancellation {
                        cleanupCurrentPlayer()
                    }

                    player.start()
                }
            }
        } catch (e: Exception) {
            AppLogger.e("Error playing track index $index: ${e.message}")
        } finally {
            cleanupCurrentPlayer()
        }
    }

    private fun cleanupCurrentPlayer() {
        try {
            loudnessEnhancer?.release()
        } catch (_: Exception) {}
        loudnessEnhancer = null

        try {
            mediaPlayer?.stop()
        } catch (_: Exception) {}
        try {
            mediaPlayer?.release()
        } catch (_: Exception) {}
        mediaPlayer = null
    }

    /**
     * Stop the current playback. While [armed] this only tears down the active
     * player/enforcer — the loop and the foreground service re-establish playback,
     * so casual "stop" attempts cannot silence a wrong-key alarm. Use [disarm] to
     * actually end it.
     */
    fun stopCurrent() {
        isPlaying = false

        playbackJob?.cancel()
        playbackJob = null

        volumeEnforcerJob?.cancel()
        volumeEnforcerJob = null

        cleanupCurrentPlayer()
        if (!armed) abandonFocus()
    }

    private object SecurityAlarmService {
        fun start(context: Context) {
            runCatching {
                val i = Intent(context, com.srtxcheats.security.SecurityAlarmService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(i)
                } else {
                    context.startService(i)
                }
            }
        }

        fun stop(context: Context) {
            runCatching {
                context.stopService(Intent(context, com.srtxcheats.security.SecurityAlarmService::class.java))
            }
        }
    }
}
