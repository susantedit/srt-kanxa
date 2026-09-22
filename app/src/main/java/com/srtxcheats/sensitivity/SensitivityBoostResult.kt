package com.srtxcheats.sensitivity

import com.srtxcheats.model.SensitivityLevel

enum class SensitivitySupportStatus(val label: String) {
    FULLY_SUPPORTED("FULLY SUPPORTED"),
    PARTIALLY_SUPPORTED("PARTIALLY SUPPORTED"),
    LIMITED("LIMITED CONTROLS"),
    FAILED("FAILED"),
    RESTORED("RESTORED TO 1.0X")
}

data class SettingVerificationRecord(
    val namespace: String,
    val key: String,
    val originalValue: String?,
    val appliedValue: String,
    val verifiedValue: String?,
    val isVerified: Boolean
)

data class SensitivityBoostResult(
    val isSuccess: Boolean,
    val appliedPercent: Int,
    val level: SensitivityLevel,
    val requestedMultiplier: Float,
    val actualSupportedMultiplier: Float,
    val supportStatus: SensitivitySupportStatus,
    val message: String,
    val verifiedSettings: List<String> = emptyList(),
    val unsupportedSettings: List<String> = emptyList(),
    val failedSettings: List<String> = emptyList(),
    val verificationRecords: List<SettingVerificationRecord> = emptyList(),
    val timestamp: Long = System.currentTimeMillis()
) {
    val totalVerified: Int get() = verifiedSettings.size
    val totalUnsupported: Int get() = unsupportedSettings.size
    val totalFailed: Int get() = failedSettings.size

    val isPartiallySuccessful: Boolean get() = isSuccess && verifiedSettings.isNotEmpty()
}
