package com.srtxcheats.core

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.telephony.CellInfo
import android.telephony.CellInfoLte
import android.telephony.CellInfoNr
import android.telephony.CellSignalStrengthLte
import android.telephony.CellSignalStrengthNr
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import com.srtxcheats.utils.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

data class SimSlotInfo(
    val slotIndex: Int,
    val subId: Int,
    val displayName: String,
    val carrierName: String,
    val isDefaultData: Boolean,
    val signalLevel: Int, // 0 to 4
    val signalDbm: Int,
    val networkType: String
)

data class DualSimState(
    val simCount: Int = 0,
    val isDualSimActive: Boolean = false,
    val defaultDataSlot: Int = 0,
    val simList: List<SimSlotInfo> = emptyList(),
    val recommendedSlot: Int? = null,
    val recommendationReason: String = ""
)

class DualSimManager(private val context: Context) {

    private val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
    private val subscriptionManager = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as? SubscriptionManager

    private val _simState = MutableStateFlow(DualSimState())
    val simState: StateFlow<DualSimState> = _simState.asStateFlow()

    suspend fun queryDualSimStatus(): DualSimState = withContext(Dispatchers.IO) {
        val slots = mutableListOf<SimSlotInfo>()
        var defaultDataSubId = -1

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            defaultDataSubId = SubscriptionManager.getDefaultDataSubscriptionId()
        }

        try {
            val subs: List<SubscriptionInfo>? = try {
                subscriptionManager?.activeSubscriptionInfoList
            } catch (e: SecurityException) {
                AppLogger.w("Missing READ_PHONE_STATE permission to query subscriptions: ${e.message}")
                null
            }

            if (!subs.isNullOrEmpty()) {
                for (sub in subs) {
                    val isData = (sub.subscriptionId == defaultDataSubId) || (subs.size == 1)
                    val subTelephony = telephonyManager?.createForSubscriptionId(sub.subscriptionId) ?: telephonyManager
                    
                    var sigLevel = 0
                    var sigDbm = -110
                    var netType = "4G LTE"

                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            val signal = subTelephony?.signalStrength
                            if (signal != null) {
                                sigLevel = signal.level
                            }
                        }
                    } catch (_: Exception) {}

                    // Query cell infos if permission granted
                    try {
                        val cells = subTelephony?.allCellInfo
                        if (!cells.isNullOrEmpty()) {
                            for (cell in cells) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && cell is CellInfoNr) {
                                    val nrStrength = cell.cellSignalStrength as? CellSignalStrengthNr
                                    if (nrStrength != null) {
                                        sigDbm = nrStrength.dbm
                                        sigLevel = nrStrength.level
                                        netType = "5G NR"
                                        break
                                    }
                                } else if (cell is CellInfoLte) {
                                    val lteStrength = cell.cellSignalStrength
                                    sigDbm = lteStrength.dbm
                                    sigLevel = lteStrength.level
                                    netType = "4G LTE"
                                    break
                                }
                            }
                        }
                    } catch (_: Exception) {}

                    slots.add(
                        SimSlotInfo(
                            slotIndex = sub.simSlotIndex,
                            subId = sub.subscriptionId,
                            displayName = sub.displayName?.toString() ?: "SIM ${sub.simSlotIndex + 1}",
                            carrierName = sub.carrierName?.toString() ?: "Carrier",
                            isDefaultData = isData,
                            signalLevel = sigLevel.coerceIn(0, 4),
                            signalDbm = sigDbm,
                            networkType = netType
                        )
                    )
                }
            }
        } catch (e: Exception) {
            AppLogger.w("Error reading dual SIM status", e)
        }

        // Evaluate recommendation
        var recSlot: Int? = null
        var recReason = "Single SIM active"

        if (slots.size >= 2) {
            val sim1 = slots[0]
            val sim2 = slots[1]

            if (sim1.signalDbm > sim2.signalDbm + 8) {
                recSlot = sim1.slotIndex
                recReason = "${sim1.displayName} has stronger signal (${sim1.signalDbm} dBm vs ${sim2.signalDbm} dBm)"
            } else if (sim2.signalDbm > sim1.signalDbm + 8) {
                recSlot = sim2.slotIndex
                recReason = "${sim2.displayName} has stronger signal (${sim2.signalDbm} dBm vs ${sim1.signalDbm} dBm)"
            } else {
                recSlot = if (sim1.isDefaultData) sim1.slotIndex else sim2.slotIndex
                recReason = "Both SIM cards have comparable signal stability"
            }
        }

        val state = DualSimState(
            simCount = slots.size,
            isDualSimActive = slots.size >= 2,
            defaultDataSlot = slots.firstOrNull { it.isDefaultData }?.slotIndex ?: 0,
            simList = slots,
            recommendedSlot = recSlot,
            recommendationReason = recReason
        )
        _simState.value = state
        state
    }

    /**
     * Opens system mobile network settings for one-tap SIM switching.
     */
    fun openMobileDataSettings() {
        try {
            val intent = Intent(Settings.ACTION_NETWORK_OPERATOR_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            try {
                val intent = Intent(Settings.ACTION_DATA_ROAMING_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (_: Exception) {}
        }
    }
}
