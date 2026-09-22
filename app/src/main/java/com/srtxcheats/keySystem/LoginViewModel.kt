package com.srtxcheats.keySystem

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.srtxcheats.utils.AppLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LoginViewModel(private val repository: KeyRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val _currentSession = MutableStateFlow(LicenseSession.EMPTY)
    val currentSession: StateFlow<LicenseSession> = _currentSession.asStateFlow()

    init {
        checkSavedLicense()
    }

    fun checkSavedLicense() {
        viewModelScope.launch {
            _uiState.value = LoginUiState.AutoLogin
            val cached = repository.getCachedSession()
            _currentSession.value = cached

            if (cached.isValid && cached.key.isNotBlank()) {
                // If we have days left recorded as negative or 0 and an expiry string, check expiry
                if (cached.daysLeft < 0) {
                    _uiState.value = LoginUiState.KeyExpired
                    return@launch
                }

                // Verify with server in background to confirm validity
                val result = repository.verifyKeyOnline(cached.key)
                result.fold(
                    onSuccess = { resp ->
                        if (resp.valid) {
                            _uiState.value = LoginUiState.Success(resp)
                        } else {
                            val msg = resp.message.orEmpty().lowercase()
                            when {
                                msg.contains("expired") -> _uiState.value = LoginUiState.KeyExpired
                                msg.contains("hwid") || msg.contains("device") -> _uiState.value = LoginUiState.HwidMismatch
                                msg.contains("limit") -> _uiState.value = LoginUiState.LoginLimitReached
                                else -> _uiState.value = LoginUiState.InvalidKey
                            }
                        }
                    },
                    onFailure = { err ->
                        // If network offline or timed out, allow temporary grace if cached was recently verified
                        val isRecent = (System.currentTimeMillis() - cached.verifiedTimestamp) < 24 * 60 * 60 * 1000L
                        if (isRecent) {
                            AppLogger.i("Offline grace session active for valid cached key")
                            _uiState.value = LoginUiState.Success(
                                VerifyKeyResponse(
                                    valid = true,
                                    message = "Authorized (Offline Mode)",
                                    key = cached.key,
                                    expiresAt = cached.expiresAt,
                                    daysLeft = cached.daysLeft,
                                    type = cached.type
                                )
                            )
                        } else {
                            when (err.message) {
                                "SERVER_OFFLINE" -> _uiState.value = LoginUiState.ServerOffline
                                "OFFLINE", "TIMEOUT" -> _uiState.value = LoginUiState.NetworkError("Network offline or timeout. Please check your connection.")
                                else -> _uiState.value = LoginUiState.Error(err.message ?: "Authentication failed")
                            }
                        }
                    }
                )
            } else {
                _uiState.value = LoginUiState.Idle
            }
        }
    }

    fun activateKey(rawKey: String) {
        val trimmed = rawKey.trim()
        if (trimmed.isBlank()) {
            _uiState.value = LoginUiState.Error("Please enter your license key.")
            return
        }

        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading

            val result = repository.verifyKeyOnline(trimmed)
            result.fold(
                onSuccess = { resp ->
                    if (resp.valid) {
                        _uiState.value = LoginUiState.Success(resp)
                    } else {
                        val msg = resp.message.orEmpty().lowercase()
                        when {
                            msg.contains("expired") -> _uiState.value = LoginUiState.KeyExpired
                            msg.contains("hwid") || msg.contains("device") -> _uiState.value = LoginUiState.HwidMismatch
                            msg.contains("limit") -> _uiState.value = LoginUiState.LoginLimitReached
                            else -> _uiState.value = LoginUiState.InvalidKey
                        }
                    }
                },
                onFailure = { err ->
                    when (err.message) {
                        "SERVER_OFFLINE" -> _uiState.value = LoginUiState.ServerOffline
                        "OFFLINE" -> _uiState.value = LoginUiState.NetworkError("No internet connection. Please connect to activate your key.")
                        "TIMEOUT" -> _uiState.value = LoginUiState.NetworkError("Connection timed out. Render backend may be waking up, please retry.")
                        else -> _uiState.value = LoginUiState.Error("Verification error: ${err.message}")
                    }
                }
            )
        }
    }

    fun logout() {
        viewModelScope.launch {
            repository.clearSession()
            _currentSession.value = LicenseSession.EMPTY
            _uiState.value = LoginUiState.Idle
        }
    }
}
