package com.srtxcheats.core

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build
import com.srtxcheats.utils.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket

data class PingResult(
    val serverName: String,
    val host: String,
    val pingMs: Int,
    val isReachable: Boolean
)

data class NetworkStatus(
    val isConnected: Boolean = false,
    val isWifi: Boolean = false,
    val isCellular: Boolean = false,
    val linkDownSpeedMbps: Int = 0,
    val linkUpSpeedMbps: Int = 0,
    val wifiSsid: String? = null,
    val wifiFrequencyMhz: Int = 0,
    val bestPingMs: Int = 0,
    val pingResults: List<PingResult> = emptyList(),
    val isLowLatencyActive: Boolean = false
)

class NetworkBooster(private val context: Context) {

    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager

    private var wifiLock: WifiManager.WifiLock? = null

    private val _networkState = MutableStateFlow(NetworkStatus())
    val networkState: StateFlow<NetworkStatus> = _networkState.asStateFlow()

    private val testServers = listOf(
        "Game SEA (Singapore)" to Pair("1.1.1.1", 53),
        "Game Global (Cloudflare)" to Pair("1.0.0.1", 53),
        "Game ASIA (Google DNS)" to Pair("8.8.8.8", 53),
        "Garena Regional Relay" to Pair("8.8.4.4", 53)
    )

    /**
     * Enables hardware Wi-Fi low latency lock to reduce jitter and packet drops.
     */
    fun acquireLowLatencyLock() {
        try {
            if (wifiLock == null && wifiManager != null) {
                val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    WifiManager.WIFI_MODE_FULL_LOW_LATENCY
                } else {
                    @Suppress("DEPRECATION")
                    WifiManager.WIFI_MODE_FULL_HIGH_PERF
                }
                wifiLock = wifiManager.createWifiLock(mode, "SRT_GAME_ASSIST_WIFI_LOCK").apply {
                    setReferenceCounted(false)
                    acquire()
                }
                _networkState.value = _networkState.value.copy(isLowLatencyActive = true)
                AppLogger.i("Low-latency Wi-Fi Lock acquired.")
            }
        } catch (e: Exception) {
            AppLogger.w("Failed to acquire low latency lock", e)
        }
    }

    /**
     * Releases Wi-Fi low latency lock.
     */
    fun releaseLowLatencyLock() {
        try {
            wifiLock?.let {
                if (it.isHeld) it.release()
            }
            wifiLock = null
            _networkState.value = _networkState.value.copy(isLowLatencyActive = false)
            AppLogger.i("Low-latency Wi-Fi Lock released.")
        } catch (e: Exception) {
            AppLogger.w("Failed to release low latency lock", e)
        }
    }

    /**
     * Measures exact socket connection latency across competitive gaming relay endpoints.
     */
    suspend fun sampleNetworkMetrics(): NetworkStatus = withContext(Dispatchers.IO) {
        val activeNetwork = connectivityManager.activeNetwork
        val caps = connectivityManager.getNetworkCapabilities(activeNetwork)

        val isConnected = caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        val isWifi = caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ?: false
        val isCellular = caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ?: false

        val downSpeed = caps?.linkDownstreamBandwidthKbps?.div(1000) ?: 0
        val upSpeed = caps?.linkUpstreamBandwidthKbps?.div(1000) ?: 0

        var ssid: String? = null
        var freq = 0
        if (isWifi && wifiManager != null) {
            try {
                val info = wifiManager.connectionInfo
                ssid = info.ssid?.replace("\"", "")
                freq = info.frequency
            } catch (_: Exception) {}
        }

        // Test real ping to game relays
        val pings = mutableListOf<PingResult>()
        var lowestPing = 999

        for ((name, target) in testServers) {
            val (host, port) = target
            val ping = measureTcpPing(host, port, 1500)
            if (ping >= 0) {
                pings.add(PingResult(name, host, ping, true))
                if (ping < lowestPing) lowestPing = ping
            } else {
                pings.add(PingResult(name, host, 999, false))
            }
        }

        val status = NetworkStatus(
            isConnected = isConnected,
            isWifi = isWifi,
            isCellular = isCellular,
            linkDownSpeedMbps = downSpeed,
            linkUpSpeedMbps = upSpeed,
            wifiSsid = ssid,
            wifiFrequencyMhz = freq,
            bestPingMs = if (lowestPing < 999) lowestPing else 0,
            pingResults = pings,
            isLowLatencyActive = wifiLock?.isHeld == true
        )
        _networkState.value = status
        status
    }

    private fun measureTcpPing(host: String, port: Int, timeoutMs: Int): Int {
        return try {
            val socket = Socket()
            val start = System.currentTimeMillis()
            socket.connect(InetSocketAddress(host, port), timeoutMs)
            val duration = (System.currentTimeMillis() - start).toInt()
            socket.close()
            duration
        } catch (_: Exception) {
            -1
        }
    }
}
