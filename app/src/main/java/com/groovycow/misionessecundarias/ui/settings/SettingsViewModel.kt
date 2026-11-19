package com.groovycow.misionessecundarias.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.groovycow.misionessecundarias.domain.usecase.SignOutUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SettingsUiState(
    val isSigningOut: Boolean = false,
    val error: String? = null,
)

sealed interface SettingsEvent {
    data object SignedOut : SettingsEvent
}

class SettingsViewModel(
    private val signOutUseCase: SignOutUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<SettingsEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<SettingsEvent> = _events.asSharedFlow()

    fun signOut() {
        if (_uiState.value.isSigningOut) return

        viewModelScope.launch {
            _uiState.value = SettingsUiState(isSigningOut = true)
            signOutUseCase().fold(
                onSuccess = {
                    _uiState.value = SettingsUiState()
                    _events.emit(SettingsEvent.SignedOut)
                },
                onFailure = { exception ->
                    _uiState.value =
                        SettingsUiState(
                            error = exception.message ?: "No fue posible cerrar la sesión.",
                        )
                },
            )
        }
    }

    fun dismissError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
