package com.srtxcheats.core

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Process
import android.provider.Settings
import com.srtxcheats.model.InstalledApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object GameDetector {

    val FREE_FIRE_PACKAGES = listOf(
        "com.dts.freefireth",  // Free Fire (Standard / Global)
        "com.dts.freefiremax", // Free Fire MAX
        "com.dts.freefirevn"   // Free Fire Vietnam
    )

    val POPULAR_GAME_PACKAGES = setOf(
        "com.dts.freefireth",
        "com.dts.freefiremax",
        "com.dts.freefirevn",
        "com.pubg.imobile",
        "com.tencent.ig",
        "com.activision.callofduty.shooter",
        "com.miHoYo.GenshinImpact",
        "com.roblox.client",
        "com.mojang.minecraftpe",
        "com.supercell.brawlstars",
        "com.riotgames.league.wildrift"
    )

    sealed class LaunchResult {
        data class Success(
            val launchedPackage: String,
            val appName: String,
            val switchedFromFallback: Boolean
        ) : LaunchResult()

        data class NotFound(
            val targetPackage: String,
            val checkedAlternatives: List<String>
        ) : LaunchResult()

        data class Error(val message: String) : LaunchResult()
    }

    /**
     * Checks if a package is installed on the device.
     */
    fun isPackageInstalled(context: Context, packageName: String): Boolean {
        if (packageName.isBlank()) return false
        val pm = context.packageManager
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(packageName, 0)
            }
            true
        } catch (_: PackageManager.NameNotFoundException) {
            // Also check launch intent or intent activities as fallback
            try {
                if (pm.getLaunchIntentForPackage(packageName) != null) return true
                val intent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_LAUNCHER)
                    setPackage(packageName)
                }
                pm.queryIntentActivities(intent, 0).isNotEmpty()
            } catch (_: Exception) {
                false
            }
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Searches for any installed Free Fire variant (Standard, MAX, VN, etc.).
     * Returns Pair(packageName, appName) or null if none found.
     */
    fun findInstalledFreeFire(context: Context): Pair<String, String>? {
        val pm = context.packageManager

        // 1. First check known package IDs
        for (pkg in FREE_FIRE_PACKAGES) {
            if (isPackageInstalled(context, pkg)) {
                val label = try {
                    val appInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        pm.getApplicationInfo(pkg, PackageManager.ApplicationInfoFlags.of(0))
                    } else {
                        @Suppress("DEPRECATION")
                        pm.getApplicationInfo(pkg, 0)
                    }
                    pm.getApplicationLabel(appInfo).toString()
                } catch (_: Exception) {
                    when (pkg) {
                        "com.dts.freefiremax" -> "Free Fire MAX"
                        "com.dts.freefireth" -> "Free Fire"
                        else -> "Free Fire VN"
                    }
                }
                return Pair(pkg, label)
            }
        }

        // 2. Scan all installed applications for any Free Fire variant
        return try {
            val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val resolveInfos = pm.queryIntentActivities(mainIntent, 0)
            for (ri in resolveInfos) {
                val pkg = ri.activityInfo.packageName
                val name = ri.loadLabel(pm).toString()
                if (pkg.contains("freefire", ignoreCase = true) ||
                    name.contains("Free Fire", ignoreCase = true)
                ) {
                    return Pair(pkg, name)
                }
            }
            null
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Checks if Usage Stats permission is granted by the user.
     */
    fun hasUsageStatsPermission(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun openUsageAccessSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (_: Exception) {}
    }

    /**
     * Gets the currently active foreground package name if usage stats permission is granted.
     */
    fun getForegroundPackageName(context: Context): String? {
        if (!hasUsageStatsPermission(context)) return null

        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager ?: return null
        val now = System.currentTimeMillis()
        val events = usm.queryEvents(now - 10000, now)
        val event = UsageEvents.Event()
        var lastForegroundPkg: String? = null

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                lastForegroundPkg = event.packageName
            }
        }
        return lastForegroundPkg
    }

    /**
     * Retrieves installed launchable apps, prioritizing games.
     */
    suspend fun getInstalledLaunchableApps(context: Context): List<InstalledApp> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val resolveInfos = pm.queryIntentActivities(mainIntent, 0)
        val list = mutableListOf<InstalledApp>()

        for (ri in resolveInfos) {
            val pkg = ri.activityInfo.packageName
            if (pkg == context.packageName) continue

            val name = ri.loadLabel(pm).toString()
            val icon = try { ri.loadIcon(pm) } catch (_: Exception) { null }

            val appInfo = try {
                pm.getApplicationInfo(pkg, 0)
            } catch (_: Exception) { null }

            val isGameCategory = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                appInfo?.category == ApplicationInfo.CATEGORY_GAME
            } else {
                false
            }
            val isPopular = POPULAR_GAME_PACKAGES.contains(pkg) ||
                    pkg.contains("freefire", ignoreCase = true) ||
                    name.contains("Free Fire", ignoreCase = true)

            list.add(
                InstalledApp(
                    packageName = pkg,
                    appName = name,
                    icon = icon,
                    isGame = isGameCategory || isPopular,
                    isPopularGame = isPopular
                )
            )
        }

        // Sort: Popular games first, then other games, then alphabetical
        list.sortedWith(
            compareByDescending<InstalledApp> { it.isPopularGame }
                .thenByDescending { it.isGame }
                .thenBy { it.appName.lowercase() }
        )
    }

    /**
     * Resolves a launchable intent for a package using standard, launcher activity, or leanback query.
     */
    private fun createLaunchIntent(pm: PackageManager, pkg: String): Intent? {
        var intent = pm.getLaunchIntentForPackage(pkg)
        if (intent == null) {
            val queryIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
                setPackage(pkg)
            }
            val resolveInfos = pm.queryIntentActivities(queryIntent, 0)
            if (resolveInfos.isNotEmpty()) {
                val act = resolveInfos[0].activityInfo
                intent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_LAUNCHER)
                    setClassName(act.packageName, act.name)
                }
            }
        }
        if (intent == null) {
            intent = pm.getLeanbackLaunchIntentForPackage(pkg)
        }
        intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        return intent
    }

    /**
     * Launches the selected game with intelligent Free Fire cross-variant detection and fallbacks.
     */
    fun launchGameDetailed(context: Context, targetPackage: String): LaunchResult {
        val pm = context.packageManager

        // 1. First, attempt to launch the explicitly targeted package
        if (isPackageInstalled(context, targetPackage)) {
            val intent = createLaunchIntent(pm, targetPackage)
            if (intent != null) {
                return try {
                    context.startActivity(intent)
                    val appName = try {
                        val ai = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            pm.getApplicationInfo(targetPackage, PackageManager.ApplicationInfoFlags.of(0))
                        } else {
                            @Suppress("DEPRECATION")
                            pm.getApplicationInfo(targetPackage, 0)
                        }
                        pm.getApplicationLabel(ai).toString()
                    } catch (_: Exception) {
                        targetPackage
                    }
                    LaunchResult.Success(
                        launchedPackage = targetPackage,
                        appName = appName,
                        switchedFromFallback = false
                    )
                } catch (e: Exception) {
                    LaunchResult.Error("Failed to launch $targetPackage: ${e.localizedMessage}")
                }
            }
        }

        // 2. If targetPackage was Free Fire or Free Fire MAX, scan for the other Free Fire variants
        val isFreeFireTarget = targetPackage.contains("freefire", ignoreCase = true) ||
                FREE_FIRE_PACKAGES.contains(targetPackage)

        if (isFreeFireTarget) {
            val detected = findInstalledFreeFire(context)
            if (detected != null) {
                val (foundPkg, foundName) = detected
                val fallbackIntent = createLaunchIntent(pm, foundPkg)
                if (fallbackIntent != null) {
                    return try {
                        context.startActivity(fallbackIntent)
                        LaunchResult.Success(
                            launchedPackage = foundPkg,
                            appName = foundName,
                            switchedFromFallback = true
                        )
                    } catch (e: Exception) {
                        LaunchResult.Error("Failed to launch detected Free Fire ($foundPkg): ${e.localizedMessage}")
                    }
                }
            }
        }

        return LaunchResult.NotFound(
            targetPackage = targetPackage,
            checkedAlternatives = if (isFreeFireTarget) FREE_FIRE_PACKAGES else listOf(targetPackage)
        )
    }

    /**
     * Backward-compatible simple launchGame returning boolean.
     */
    fun launchGame(context: Context, packageName: String): Boolean {
        return launchGameDetailed(context, packageName) is LaunchResult.Success
    }

    /**
     * Opens Google Play Store page for downloading/updating the game.
     */
    fun openInPlayStore(context: Context, packageName: String) {
        val targetPkg = if (packageName.isNotBlank()) packageName else "com.dts.freefireth"
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$targetPkg")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            try {
                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$targetPkg")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(webIntent)
            } catch (_: Exception) {}
        }
    }
}
