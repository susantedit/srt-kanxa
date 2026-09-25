package com.srtxcheats

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.srtxcheats.core.DualSimManager
import com.srtxcheats.core.GameDetector
import com.srtxcheats.core.NetworkBooster
import com.srtxcheats.core.PerformanceMonitor
import com.srtxcheats.core.SignalRadarScanner
import com.srtxcheats.data.DataStoreManager
import com.srtxcheats.data.LogoHelper
import com.srtxcheats.display.StretchScreen
import com.srtxcheats.keySystem.KeyRepository
import com.srtxcheats.keySystem.LoginScreen
import com.srtxcheats.keySystem.LoginUiState
import com.srtxcheats.keySystem.LoginViewModel
import com.srtxcheats.model.GameProfile
import com.srtxcheats.model.SensitivityLevel
import com.srtxcheats.security.IntegrityProtectionManager
import com.srtxcheats.macro.MacroScreen
import com.srtxcheats.sensitivity.SensitivityScreen
import com.srtxcheats.service.OverlayService
import com.srtxcheats.ui.screens.AboutScreen
import com.srtxcheats.ui.screens.GameSelectorScreen
import com.srtxcheats.ui.screens.MainDashboardScreen
import com.srtxcheats.ui.screens.NetworkRadarScreen
import com.srtxcheats.ui.screens.OverlaySettingsScreen
import com.srtxcheats.ui.screens.ShizukuScreen
import com.srtxcheats.ui.screens.SplashScreen
import com.srtxcheats.ui.screens.TouchTestScreen
import com.srtxcheats.ui.theme.AmoledBackground
import com.srtxcheats.ui.theme.AmoledSurface
import com.srtxcheats.ui.theme.MyApplicationTheme
import com.srtxcheats.ui.theme.NeonCyan
import com.srtxcheats.ui.theme.TextMuted
import com.srtxcheats.ui.theme.TextPrimary
import com.srtxcheats.ui.theme.TextSecondary
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

enum class AppTab(val title: String, val icon: ImageVector) {
    DASHBOARD("Dashboard", Icons.Default.Speed),
    RADAR("Radar", Icons.Default.Explore),
    GAMES("Games", Icons.Default.Gamepad),
    STRETCH("Stretch", Icons.Default.AspectRatio),
    SENSI("Sensi", Icons.Default.Tune),
    MACRO("Macro", Icons.Default.TouchApp),
    OVERLAY("Overlay", Icons.Default.Layers),
    SYSTEM_ABOUT("System", Icons.Default.Info)
}

class MainActivity : ComponentActivity() {

    private lateinit var performanceMonitor: PerformanceMonitor
    private lateinit var dataStoreManager: DataStoreManager
    private var logoBitmap: Bitmap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (intent?.getBooleanExtra("KEY_EXPIRED", false) == true) {
            Toast.makeText(this, "License key expired! App closed.", Toast.LENGTH_LONG).show()
            finishAffinity()
            return
        }
        IntegrityProtectionManager.verifyAppIntegrity(this)
        // If a wrong-key alarm was armed and the process was later killed, re-arm it
        // on relaunch. (An explicit user force-stop cancels sticky services by OS design.)
        com.srtxcheats.security.SecurityAlarmSoundPlayer.resumeIfArmed(this)
        enableEdgeToEdge()

        performanceMonitor = PerformanceMonitor(this)
        dataStoreManager = DataStoreManager(this)
        logoBitmap = LogoHelper.getLogoBitmap(this)

        lifecycleScopeLaunch()

        setContent {
            MyApplicationTheme {
                MainAppContent()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.getBooleanExtra("KEY_EXPIRED", false)) {
            Toast.makeText(this, "License key expired! App closed.", Toast.LENGTH_LONG).show()
            finishAffinity()
        }
    }

    private fun lifecycleScopeLaunch() {
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            LogoHelper.preloadAndCacheLogo(this@MainActivity)
            logoBitmap = LogoHelper.getLogoBitmap(this@MainActivity)

            // Auto-detect installed Free Fire on the phone and set target profile
            try {
                val currentPkg = dataStoreManager.selectedPackageFlow.first()
                if (!GameDetector.isPackageInstalled(this@MainActivity, currentPkg)) {
                    val detected = GameDetector.findInstalledFreeFire(this@MainActivity)
                    if (detected != null) {
                        dataStoreManager.saveSelectedGame(detected.first, detected.second)
                    }
                }
            } catch (_: Exception) {}
        }
    }

    @Composable
    private fun MainAppContent() {
        val coroutineScope = rememberCoroutineScope()
        var showSplash by remember { mutableStateOf(true) }
        var currentTab by remember { mutableStateOf(AppTab.DASHBOARD) }

        val metrics by performanceMonitor.metrics.collectAsState()
        val profile by dataStoreManager.gameProfileFlow.collectAsState(
            initial = GameProfile.defaultFor("", "Free Fire MAX")
        )

        val keyRepository = remember { KeyRepository(this@MainActivity) }
        val loginViewModel = remember { LoginViewModel(keyRepository) }
        val loginUiState by loginViewModel.uiState.collectAsState()
        val sessionState by keyRepository.licenseSessionFlow.collectAsState(initial = com.srtxcheats.keySystem.LicenseSession.EMPTY)
        val durationText = keyRepository.formatRemainingDuration(sessionState)
        val sensitivityEngine = remember { com.srtxcheats.sensitivity.SensitivityEngine(this@MainActivity) }

        // Watch for key expiration: if expired, close app immediately!
        LaunchedEffect(sessionState) {
            if (loginUiState is LoginUiState.Success && keyRepository.isSessionExpired(sessionState)) {
                Toast.makeText(this@MainActivity, "Activation key has expired! Closing app.", Toast.LENGTH_LONG).show()
                finishAffinity()
            }
        }

        // A valid key being accepted is the only legitimate way to silence the wrong-key
        // alarm. This MUST live here, not in LoginScreen: LoginScreen leaves composition
        // the instant loginUiState turns Success (the branch below switches to the
        // dashboard), so a disarm effect inside it would never run for the Success value.
        LaunchedEffect(loginUiState) {
            if (loginUiState is LoginUiState.Success) {
                com.srtxcheats.security.SecurityAlarmSoundPlayer.disarm(this@MainActivity)
            }
        }

        DisposableEffect(Unit) {
            performanceMonitor.startMonitoring(1000L)
            onDispose {
                performanceMonitor.stopMonitoring()
            }
        }

        if (showSplash) {
            SplashScreen(
                logoBitmap = logoBitmap,
                onFinish = { showSplash = false }
            )
        } else if (loginUiState !is LoginUiState.Success) {
            LoginScreen(
                uiState = loginUiState,
                logoBitmap = logoBitmap,
                onActivate = { key -> loginViewModel.activateKey(key) },
                onSuccessDismiss = { /* Automatically transitions */ }
            )
        } else {
            Scaffold(
                modifier = Modifier
                    .fillMaxSize()
                    .background(AmoledBackground),
                bottomBar = {
                    NavigationBar(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .height(64.dp)
                            .border(width = 0.5.dp, color = Color(0x2200E5FF)),
                        containerColor = AmoledSurface,
                        tonalElevation = 8.dp
                    ) {
                        AppTab.values().forEach { tab ->
                            val selected = currentTab == tab
                            NavigationBarItem(
                                selected = selected,
                                onClick = { currentTab = tab },
                                icon = {
                                    Icon(
                                        imageVector = tab.icon,
                                        contentDescription = tab.title,
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                                label = {
                                    Text(
                                        text = tab.title,
                                        fontSize = 9.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color.Black,
                                    selectedTextColor = NeonCyan,
                                    indicatorColor = NeonCyan,
                                    unselectedIconColor = TextMuted,
                                    unselectedTextColor = TextSecondary
                                ),
                                modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                            )
                        }
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    Crossfade(targetState = currentTab, label = "tab_transition") { tab ->
                        when (tab) {
                            AppTab.DASHBOARD -> MainDashboardScreen(
                                metrics = metrics,
                                profile = profile,
                                logoBitmap = logoBitmap,
                                isOverlayServiceActive = OverlayService.isServiceRunning,
                                keyDurationText = durationText,
                                onStartOverlay = {
                                    OverlayService.start(this@MainActivity)
                                },
                                onStopOverlay = {
                                    OverlayService.stop(this@MainActivity)
                                },
                                onSelectGameClicked = {
                                    currentTab = AppTab.GAMES
                                },
                                onGameSelected = { pkg, name ->
                                    coroutineScope.launch {
                                        dataStoreManager.saveSelectedGame(pkg, name)
                                    }
                                },
                                onSensitivityLevelSelected = { level ->
                                    coroutineScope.launch {
                                        dataStoreManager.saveSensitivityLevel(level)
                                        dataStoreManager.saveSensitivityPercent(level.targetPercent)
                                        sensitivityEngine.applyPreset(level)
                                    }
                                },
                                onSensitivityPercentSelected = { pct ->
                                    coroutineScope.launch {
                                        dataStoreManager.saveSensitivityPercent(pct)
                                        val lvl = SensitivityLevel.fromPercent(pct)
                                        dataStoreManager.saveSensitivityLevel(lvl)
                                        sensitivityEngine.applySensitivity(pct)
                                    }
                                },
                                onTogglePlus50Boost = { active ->
                                    coroutineScope.launch {
                                        dataStoreManager.setPlus50BoostActive(active)
                                    }
                                },
                                onToggleFreeStyle800 = { active ->
                                    coroutineScope.launch {
                                        dataStoreManager.setFreeStyle800Active(active)
                                    }
                                },
                                onToggleUltraProMax = { active ->
                                    coroutineScope.launch {
                                        dataStoreManager.setUltraProMaxActive(active)
                                    }
                                },
                                onToggleDragMode = { active ->
                                    coroutineScope.launch {
                                        dataStoreManager.setDragModeActive(active)
                                    }
                                },
                                onSaveFreeStyleDpi = { dpi ->
                                    coroutineScope.launch {
                                        dataStoreManager.saveFreeStyleDpi(dpi)
                                    }
                                },
                                onToggleGameBoost = { active ->
                                    coroutineScope.launch {
                                        dataStoreManager.setGameBoostActive(active)
                                    }
                                },
                                onToggleGamingMode = { active ->
                                    coroutineScope.launch {
                                        dataStoreManager.setGamingModeActive(active)
                                    }
                                },
                                onTogglePerformanceBoost = { active ->
                                    coroutineScope.launch {
                                        dataStoreManager.setPerformanceBoostActive(active)
                                    }
                                },
                                onOpenShizuku = {
                                    currentTab = AppTab.SYSTEM_ABOUT
                                },
                                onOpenTouchTest = {
                                    currentTab = AppTab.SENSI
                                },
                                onOpenRadar = {
                                    currentTab = AppTab.RADAR
                                }
                            )

                            AppTab.RADAR -> {
                                val dualSimMgr = remember { DualSimManager(this@MainActivity) }
                                val radarScanner = remember { SignalRadarScanner(this@MainActivity) }
                                val netBooster = remember { NetworkBooster(this@MainActivity) }
                                NetworkRadarScreen(
                                    dualSimManager = dualSimMgr,
                                    radarScanner = radarScanner,
                                    networkBooster = netBooster
                                )
                            }

                            AppTab.GAMES -> GameSelectorScreen(
                                currentSelectedPackage = profile.packageName,
                                onGameSelected = { pkg, name ->
                                    coroutineScope.launch {
                                        dataStoreManager.saveSelectedGame(pkg, name)
                                        Toast.makeText(this@MainActivity, "Target set: $name", Toast.LENGTH_SHORT).show()
                                        currentTab = AppTab.DASHBOARD
                                    }
                                }
                            )

                            AppTab.OVERLAY -> OverlaySettingsScreen(
                                profile = profile,
                                logoBitmap = logoBitmap,
                                onUpdateScale = { scale ->
                                    coroutineScope.launch { dataStoreManager.saveOverlayScale(scale) }
                                },
                                onUpdateMenuSize = { size ->
                                    coroutineScope.launch { dataStoreManager.saveExpandedMenuSize(size) }
                                },
                                onUpdateAlpha = { alpha ->
                                    coroutineScope.launch { dataStoreManager.saveOverlayAlpha(alpha) }
                                },
                                onUpdateTransparency = { a ->
                                    coroutineScope.launch { dataStoreManager.saveGlassTransparency(a) }
                                },
                                onUpdateGlassBlur = { blur ->
                                    coroutineScope.launch { dataStoreManager.saveGlassBlur(blur) }
                                },
                                onSensitivityPercentChange = { pct ->
                                    coroutineScope.launch {
                                        dataStoreManager.saveSensitivityPercent(pct)
                                        val lvl = SensitivityLevel.fromPercent(pct)
                                        dataStoreManager.saveSensitivityLevel(lvl)
                                        sensitivityEngine.applySensitivity(pct)
                                    }
                                },
                                onToggleMetric = { key, show ->
                                    coroutineScope.launch { dataStoreManager.updateMetricToggle(key, show) }
                                },
                                onToggleCrosshair = { show ->
                                    coroutineScope.launch { dataStoreManager.updateMetricToggle("crosshair", show) }
                                },
                                onUpdateCrosshairSettings = { style, color, size ->
                                    coroutineScope.launch { dataStoreManager.saveCrosshairSettings(style, color, size) }
                                },
                                onUpdateCrosshairOffset = { x, y ->
                                    coroutineScope.launch { dataStoreManager.saveCrosshairOffset(x, y) }
                                },
                                onToggleGraph = { show ->
                                    coroutineScope.launch { dataStoreManager.updateMetricToggle("graph", show) }
                                }
                            )

                            AppTab.STRETCH -> StretchScreen(
                                onNavigateToShizuku = { currentTab = AppTab.SYSTEM_ABOUT }
                            )

                            AppTab.SENSI -> SensitivityScreen(
                                onNavigateBack = { currentTab = AppTab.DASHBOARD }
                            )

                            AppTab.MACRO -> MacroScreen(
                                onNavigateBack = { currentTab = AppTab.DASHBOARD }
                            )

                            AppTab.SYSTEM_ABOUT -> {
                                var innerSection by remember { mutableStateOf("SHIZUKU") }
                                androidx.compose.foundation.layout.Column(modifier = Modifier.fillMaxSize()) {
                                    androidx.compose.foundation.layout.Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 8.dp),
                                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
                                    ) {
                                        listOf("SHIZUKU" to "SHIZUKU & ADB", "ABOUT" to "ABOUT & SPECS").forEach { (key, label) ->
                                            val isSel = innerSection == key
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(if (isSel) NeonCyan else Color(0x33101726))
                                                    .border(1.dp, if (isSel) NeonCyan else Color(0x22FFFFFF), RoundedCornerShape(8.dp))
                                                    .clickable { innerSection = key }
                                                    .padding(vertical = 8.dp),
                                                contentAlignment = androidx.compose.ui.Alignment.Center
                                            ) {
                                                Text(
                                                    text = label,
                                                    color = if (isSel) Color.Black else TextPrimary,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }

                                    Box(modifier = Modifier.weight(1f)) {
                                        if (innerSection == "SHIZUKU") {
                                            ShizukuScreen()
                                        } else {
                                            AboutScreen(
                                                logoBitmap = logoBitmap,
                                                onResetSettings = {
                                                    coroutineScope.launch {
                                                        dataStoreManager.resetToDefaults()
                                                        Toast.makeText(this@MainActivity, "Settings reset to defaults", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
