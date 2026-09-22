package com.srtxcheats.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import android.widget.Toast
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.NotificationCompat
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.srtxcheats.MainActivity
import com.srtxcheats.R
import com.srtxcheats.core.GameDetector
import com.srtxcheats.core.MemoryOptimizer
import com.srtxcheats.core.PerformanceMonitor
import com.srtxcheats.data.DataStoreManager
import com.srtxcheats.data.LogoHelper
import com.srtxcheats.keySystem.KeyRepository
import com.srtxcheats.model.GameProfile
import com.srtxcheats.model.SensitivityLevel
import com.srtxcheats.ui.overlay.FloatingOverlayContent
import com.srtxcheats.ui.overlay.ScreenMarkerOverlay
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class OverlayService : Service() {

    companion object {
        const val CHANNEL_ID = "srt_gaming_monitor_channel"
        const val NOTIFICATION_ID = 101

        const val ACTION_START = "com.srtxcheats.service.ACTION_START"
        const val ACTION_STOP = "com.srtxcheats.service.ACTION_STOP"
        const val ACTION_GAME_BOOST = "com.srtxcheats.service.ACTION_GAME_BOOST"
        const val ACTION_RAM_BOOST = "com.srtxcheats.service.ACTION_RAM_BOOST"

        @Volatile
        var isServiceRunning = false
            private set

        fun start(context: Context) {
            val intent = Intent(context, OverlayService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, OverlayService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private lateinit var dataStoreManager: DataStoreManager
    private lateinit var performanceMonitor: PerformanceMonitor
    private lateinit var windowManager: WindowManager

    private var overlayView: ComposeView? = null
    private var markerView: ComposeView? = null
    private var overlayParams: WindowManager.LayoutParams? = null
    private var markerParams: WindowManager.LayoutParams? = null

    private val lifecycleOwner = ServiceLifecycleOwner()
    private var isExpanded by mutableStateOf(false)
    private var currentProfile by mutableStateOf(GameProfile.defaultFor("", "Free Fire MAX"))
    private lateinit var keyRepository: KeyRepository
    private val sensitivityEngine by lazy { com.srtxcheats.sensitivity.SensitivityEngine(this) }

    override fun onCreate() {
        super.onCreate()
        com.srtxcheats.security.IntegrityProtectionManager.verifyAppIntegrity(this)
        lifecycleOwner.onCreate()
        lifecycleOwner.onStart()

        dataStoreManager = DataStoreManager(this)
        performanceMonitor = PerformanceMonitor(this)
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        keyRepository = KeyRepository(this)

        createNotificationChannel()
        startForegroundNotification()

        isServiceRunning = true
        performanceMonitor.startMonitoring(1000L)

        // Real-time license expiry watcher: if key expires, auto close overlay and entire app!
        serviceScope.launch(Dispatchers.IO) {
            while (isActive) {
                val session = keyRepository.getCachedSession()
                if (keyRepository.isSessionExpired(session)) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@OverlayService, "License key expired! Closing overlay and app.", Toast.LENGTH_LONG).show()
                        try {
                            val exitIntent = Intent(this@OverlayService, com.srtxcheats.MainActivity::class.java).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                putExtra("KEY_EXPIRED", true)
                            }
                            startActivity(exitIntent)
                        } catch (_: Exception) {}
                        stopSelf()
                    }
                    break
                }
                delay(3000)
            }
        }

        serviceScope.launch {
            dataStoreManager.gameProfileFlow.collectLatest { profile ->
                currentProfile = profile
                updateOverlayWindowAttributes(profile)
                updateMarkerOverlay(profile)
            }
        }

        // Background monitor for selected game auto-management
        serviceScope.launch(Dispatchers.IO) {
            while (isActive) {
                val fgPkg = GameDetector.getForegroundPackageName(this@OverlayService)
                if (fgPkg != null && fgPkg == currentProfile.packageName) {
                    if (!currentProfile.isGameBoostActive) {
                        dataStoreManager.setGameBoostActive(true)
                    }
                }
                delay(3000)
            }
        }

        setupOverlayView()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_RAM_BOOST -> {
                serviceScope.launch {
                    MemoryOptimizer.optimizeMemory(this@OverlayService)
                }
            }
            ACTION_GAME_BOOST -> {
                serviceScope.launch {
                    val newState = !currentProfile.isGameBoostActive
                    dataStoreManager.setGameBoostActive(newState)
                }
            }
        }
        return START_STICKY
    }

    private fun setupOverlayView() {
        if (overlayView != null) return

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = currentProfile.overlayPosX
            y = currentProfile.overlayPosY
        }
        overlayParams = params

        val logoBitmap = LogoHelper.getLogoBitmap(this)

        val composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeViewModelStoreOwner(lifecycleOwner)
            setViewTreeSavedStateRegistryOwner(lifecycleOwner)

            setContent {
                val metrics by performanceMonitor.metrics.collectAsState()
                val sessionState by keyRepository.licenseSessionFlow.collectAsState(initial = com.srtxcheats.keySystem.LicenseSession.EMPTY)
                val durationText = keyRepository.formatRemainingDuration(sessionState)

                FloatingOverlayContent(
                    metrics = metrics,
                    profile = currentProfile,
                    logoBitmap = logoBitmap,
                    isExpanded = isExpanded,
                    keyDurationText = durationText,
                    onToggleExpand = {
                        isExpanded = !isExpanded
                    },
                    onDragDelta = { dx, dy ->
                        params.x = (params.x + dx.toInt()).coerceAtLeast(0)
                        params.y = (params.y + dy.toInt()).coerceAtLeast(0)
                        try {
                            windowManager.updateViewLayout(this, params)
                        } catch (_: Exception) {}
                        serviceScope.launch {
                            dataStoreManager.saveOverlayPosition(params.x, params.y)
                        }
                    },
                    onSensitivityChange = { level ->
                        serviceScope.launch {
                            dataStoreManager.saveSensitivityLevel(level)
                            dataStoreManager.saveSensitivityPercent(level.targetPercent)
                            sensitivityEngine.applyPreset(level)
                        }
                    },
                    onSensitivityPercentChange = { pct ->
                        serviceScope.launch {
                            dataStoreManager.saveSensitivityPercent(pct)
                            val lvl = SensitivityLevel.fromPercent(pct)
                            dataStoreManager.saveSensitivityLevel(lvl)
                            sensitivityEngine.applySensitivity(pct)
                        }
                    },
                    onUpdateCrosshairOffset = { x, y ->
                        serviceScope.launch {
                            dataStoreManager.saveCrosshairOffset(x, y)
                        }
                    },
                    onToggleRamBoost = {
                        serviceScope.launch {
                            val nextState = !currentProfile.isRamBoostActive
                            dataStoreManager.setRamBoostActive(nextState)
                            val boostResult = com.srtxcheats.core.RamBooster.performBoost(this@OverlayService)
                            dataStoreManager.saveLastRamFreedMb(boostResult.freedMb)
                        }
                    },
                    onToggleGameBoost = {
                        serviceScope.launch {
                            val nextState = !currentProfile.isGameBoostActive
                            dataStoreManager.setGameBoostActive(nextState)
                        }
                    },
                    onToggleMetric = { metricKey ->
                        serviceScope.launch {
                            val cur = when (metricKey) {
                                "fps" -> currentProfile.showFps
                                "frame_time" -> currentProfile.showFrameTime
                                "cpu" -> currentProfile.showCpu
                                "clock" -> currentProfile.showClock
                                "ram" -> currentProfile.showRam
                                "temp" -> currentProfile.showTemp
                                "battery" -> currentProfile.showBattery
                                else -> true
                            }
                            dataStoreManager.updateMetricToggle(metricKey, !cur)
                        }
                    },
                    onToggleGraph = {
                        serviceScope.launch {
                            dataStoreManager.updateMetricToggle("graph", !currentProfile.showGraph)
                        }
                    },
                    onToggleCrosshair = {
                        serviceScope.launch {
                            dataStoreManager.updateMetricToggle("crosshair", !currentProfile.showCrosshair)
                        }
                    },
                    onUpdateMenuSize = { size ->
                        serviceScope.launch {
                            dataStoreManager.saveExpandedMenuSize(size)
                        }
                    },
                    onUpdateTransparency = { alpha ->
                        serviceScope.launch {
                            dataStoreManager.saveGlassTransparency(alpha)
                        }
                    },
                    onOpenApp = {
                        val appIntent = Intent(this@OverlayService, MainActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                        }
                        startActivity(appIntent)
                    }
                )
            }
        }

        overlayView = composeView
        try {
            windowManager.addView(composeView, params)
        } catch (_: Exception) {}
    }

    private fun updateOverlayWindowAttributes(profile: GameProfile) {
        overlayParams?.let { params ->
            // Keep window bounds responsive
            overlayView?.let { view ->
                try {
                    windowManager.updateViewLayout(view, params)
                } catch (_: Exception) {}
            }
        }
    }

    private fun updateMarkerOverlay(profile: GameProfile) {
        if (profile.showCrosshair) {
            if (markerView == null) {
                val params = WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT,
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                    } else {
                        @Suppress("DEPRECATION")
                        WindowManager.LayoutParams.TYPE_PHONE
                    },
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                    PixelFormat.TRANSLUCENT
                ).apply {
                    gravity = Gravity.CENTER
                }
                markerParams = params

                val view = ComposeView(this).apply {
                    setViewTreeLifecycleOwner(lifecycleOwner)
                    setViewTreeViewModelStoreOwner(lifecycleOwner)
                    setViewTreeSavedStateRegistryOwner(lifecycleOwner)

                    setContent {
                        val prof = currentProfile
                        ScreenMarkerOverlay(
                            style = prof.crosshairStyle,
                            colorLong = prof.crosshairColor,
                            sizeDp = prof.crosshairSizeDp,
                            offsetX = prof.crosshairOffsetX,
                            offsetY = prof.crosshairOffsetY
                        )
                    }
                }
                markerView = view
                try {
                    windowManager.addView(view, params)
                } catch (_: Exception) {}
            }
        } else {
            markerView?.let { view ->
                try {
                    windowManager.removeView(view)
                } catch (_: Exception) {}
                markerView = null
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "SRT X CHEATS Monitor",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Foreground gaming telemetry & transparent overlay"
                setShowBadge(false)
            }
            val nm = getSystemService(NotificationManager::class.java)
            nm?.createNotificationChannel(channel)
        }
    }

    private fun startForegroundNotification() {
        val openIntent = Intent(this, MainActivity::class.java)
        val openPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = Intent(this, OverlayService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("SRT X CHEATS")
            .setContentText("Gaming monitor is active")
            .setSmallIcon(R.drawable.logo_srt)
            .setContentIntent(openPendingIntent)
            .addAction(R.drawable.logo_srt, "STOP", stopPendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

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

    override fun onDestroy() {
        super.onDestroy()
        isServiceRunning = false

        overlayView?.let {
            try {
                windowManager.removeView(it)
            } catch (_: Exception) {}
            overlayView = null
        }

        markerView?.let {
            try {
                windowManager.removeView(it)
            } catch (_: Exception) {}
            markerView = null
        }

        performanceMonitor.stopMonitoring()
        lifecycleOwner.onDestroy()
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
