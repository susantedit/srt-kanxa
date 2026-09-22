package com.srtxcheats.model

/**
 * Clean Sensitivity Presets based on Device Baseline Multiplier Model:
 * DEFAULT = 1.0X (Baseline phone state)
 * LOW        = 0%   -> 1.0X
 * MEDIUM     = 50%  -> 2.0X
 * HIGH       = 75%  -> 3.0X
 * ULTRA HIGH = 100% -> 5.0X
 */
enum class SensitivityLevel(
    val displayName: String,
    val targetPercent: Int,
    val multiplier: Float,
    val multiplierLabel: String,
    val description: String
) {
    LOW(
        displayName = "LOW",
        targetPercent = 0,
        multiplier = 1.0f,
        multiplierLabel = "1.0X",
        description = "Phone Default baseline (1:1 native input)"
    ),
    MEDIUM(
        displayName = "MEDIUM",
        targetPercent = 50,
        multiplier = 2.0f,
        multiplierLabel = "2.0X",
        description = "Moderate input response boost & reduced drag delay"
    ),
    HIGH(
        displayName = "HIGH",
        targetPercent = 75,
        multiplier = 3.0f,
        multiplierLabel = "3.0X",
        description = "High-precision sampling & fast flick acceleration"
    ),
    ULTRA_HIGH(
        displayName = "ULTRA HIGH",
        targetPercent = 100,
        multiplier = 5.0f,
        multiplierLabel = "5.0X",
        description = "Maximum supported device-level input responsiveness"
    );

    companion object {
        fun fromPercent(percent: Int): SensitivityLevel = when {
            percent < 25 -> LOW
            percent < 65 -> MEDIUM
            percent < 90 -> HIGH
            else -> ULTRA_HIGH
        }

        /**
         * Computes requested multiplier based on slider percent (0% - 100%):
         * 0%   -> 1.0X
         * 50%  -> 2.0X
         * 75%  -> 3.0X
         * 100% -> 5.0X
         */
        fun getMultiplierForPercent(percent: Int): Float {
            val p = percent.coerceIn(0, 100)
            return when {
                p <= 0 -> 1.0f
                p <= 50 -> 1.0f + (p / 50.0f) * 1.0f // 1.0X -> 2.0X
                p <= 75 -> 2.0f + ((p - 50.0f) / 25.0f) * 1.0f // 2.0X -> 3.0X
                else -> 3.0f + ((p - 75.0f) / 25.0f) * 2.0f // 3.0X -> 5.0X
            }
        }
    }
}
