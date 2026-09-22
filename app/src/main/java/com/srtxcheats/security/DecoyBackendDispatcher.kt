package com.srtxcheats.security

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Decoy Network Dispatcher.
 * Generates deceptive network requests to 10 plausible fake authentication and
 * license telemetry backends when the user attempts license key verification.
 * Confuses reverse-engineers, network sniffers (HttpCanary, Charles, Burp),
 * and dynamic analysis sandboxes by flooding inspection logs with decoy endpoints.
 */
object DecoyBackendDispatcher {

    private val DECOY_ENDPOINTS = listOf(
        "https://api.srtx-cloud.net/v2/auth/license-verify",
        "https://license-gateway.nexus-security.io/api/v1/device-check",
        "https://auth.gamebooster-matrix.com/v3/client/validate",
        "https://telemetry.gamemode-kernel.org/api/handshake",
        "https://security.srtxcheats-cdn.com/tokens/query",
        "https://validation.render-guard-gateway.net/api/v1/auth",
        "https://node-eu.cheats-network.xyz/api/verify",
        "https://cloud-api.fps-enhancer.io/v2/handshake",
        "https://defense-probe.apex-fps.org/v1/status",
        "https://gateway.srt-kernel.tech/api/auth-session"
    )

    private val decoyClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(3, TimeUnit.SECONDS)
        .readTimeout(3, TimeUnit.SECONDS)
        .writeTimeout(3, TimeUnit.SECONDS)
        .retryOnConnectionFailure(false)
        .build()

    /**
     * Concurrently launches decoy network requests to all 10 fake backends.
     * All exceptions/failures are silently caught to never affect genuine verification.
     */
    fun dispatchDecoyRequests(key: String, hwid: String) {
        CoroutineScope(Dispatchers.IO).launch {
            for ((index, endpoint) in DECOY_ENDPOINTS.withIndex()) {
                launch {
                    try {
                        val fakeNonce = UUID.randomUUID().toString().take(12)
                        val fakeSignature = (key.hashCode() xor hwid.hashCode() xor index).toString(16)
                        val decoyUrl = "$endpoint?token=$key&mid=$hwid&sig=$fakeSignature&nonce=$fakeNonce"

                        val request = Request.Builder()
                            .url(decoyUrl)
                            .addHeader("User-Agent", "SRTX-Core-Security/2.4 ($fakeNonce)")
                            .addHeader("X-Hardware-Token", hwid)
                            .addHeader("X-Node-Route", "edge-$index")
                            .addHeader("Accept", "application/json")
                            .get()
                            .build()

                        decoyClient.newCall(request).execute().use {
                            // Silently consume response if any
                        }
                    } catch (_: Exception) {
                        // Expected for decoys, silent ignore
                    }
                }
            }
        }
    }
}
