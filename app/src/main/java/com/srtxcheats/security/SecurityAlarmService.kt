package com.srtxcheats.security

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.srtxcheats.MainActivity
import com.srtxcheats.R

/**
 * Foreground service that keeps the wrong-key security alarm alive.
 *
 * This is the persistence layer behind [SecurityAlarmSoundPlayer.arm]: it runs as
 * a `specialUse` foreground service with [START_STICKY], so the system restarts it
 * if the process is killed, and on every (re)start it calls
 * [SecurityAlarmSoundPlayer.resumeIfArmed] — which re-checks the persisted armed
 * latch and resumes the looping 1→2→3→4→5→last sequence at maximum volume.
 *
 * Leaving the login screen, backgrounding, or swiping the app from recents does not
 * silence it; only [SecurityAlarmSoundPlayer.disarm] (a valid key finally accepted)
 * clears the latch and lets this service exit. There is deliberately no user-facing
 * "stop" action on the notification.
 */
class SecurityAlarmService : Service() {

    override fun onCreate() {
        super.onCreate()
        createChannel()
        startForegroundNotification()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Whether started by us or restarted by the system, make sure we're
        // showing as foreground and the alarm is (re)armed.
        startForegroundNotification()
        SecurityAlarmSoundPlayer.resumeIfArmed(this)
        // Sticky: if the system kills us while still armed, come back and re-arm.
        return START_STICKY
    }

    /**
     * The app was swiped from recents. If still armed, ensure we are restarted so
     * the alarm cannot be dismissed by clearing the task.
     */
    override fun onTaskRemoved(rootIntent: Intent?) {
        if (SecurityAlarmSoundPlayer.wasArmed(this)) {
            val restart = Intent(applicationContext, SecurityAlarmService::class.java)
            val pi = PendingIntent.getService(
                this,
                RESTART_REQUEST,
                restart,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_ONE_SHOT
            )
            runCatching {
                val am = getSystemService(Context.ALARM_SERVICE) as? android.app.AlarmManager
                am?.set(
                    android.app.AlarmManager.RTC,
                    System.currentTimeMillis() + 1000L,
                    pi
                )
            }
        }
        super.onTaskRemoved(rootIntent)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "SRT X Security",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "License protection alarm"
                setShowBadge(false)
                setSound(null, null)
                enableVibration(false)
            }
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
    }

    private fun startForegroundNotification() {
        val openIntent = Intent(this, MainActivity::class.java)
        val openPending = PendingIntent.getActivity(
            this,
            0,
            openIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("SRT X CHEATS")
            .setContentText("License verification required")
            .setSmallIcon(R.drawable.logo_srt)
            .setContentIntent(openPending)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        }
    }

    companion object {
        const val CHANNEL_ID = "srt_security_alarm_channel"
        const val NOTIFICATION_ID = 909
        private const val RESTART_REQUEST = 9090
    }
}
