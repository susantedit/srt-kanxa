package com.srtxcheats.keySystem

data class VerifyKeyRequest(
    val key: String,
    val hwid: String
)

data class VerifyKeyResponse(
    val valid: Boolean = false,
    val message: String? = null,
    val key: String? = null,
    val expiresAt: String? = null,
    val daysLeft: Int? = null,
    val loginCount: Int? = null,
    val loginLimit: Int? = null,
    val type: String? = null
)

sealed class LoginUiState {
    object Idle : LoginUiState()
    object Loading : LoginUiState()
    object AutoLogin : LoginUiState()
    data class Success(val response: VerifyKeyResponse) : LoginUiState()
    data class Error(val message: String) : LoginUiState()
    object ServerOffline : LoginUiState()
    object KeyExpired : LoginUiState()
    object HwidMismatch : LoginUiState()
    object LoginLimitReached : LoginUiState()
    object InvalidKey : LoginUiState()
    data class NetworkError(val error: String) : LoginUiState()
}

data class LicenseSession(
    val key: String,
    val hwid: String,
    val expiresAt: String,
    val daysLeft: Int,
    val type: String,
    val verifiedTimestamp: Long,
    val isValid: Boolean
) {
    companion object {
        val EMPTY = LicenseSession(
            key = "",
            hwid = "",
            expiresAt = "",
            daysLeft = 0,
            type = "FREE",
            verifiedTimestamp = 0L,
            isValid = false
        )
    }
}
