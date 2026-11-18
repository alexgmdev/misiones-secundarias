package com.groovycow.misionessecundarias.ui.missions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.groovycow.misionessecundarias.domain.model.PublicProfile
import com.groovycow.misionessecundarias.domain.usecase.GetPublicProfileUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PublicProfileUiState(
    val isLoading: Boolean = false,
    val profile: PublicProfile? = null,
    val error: String? = null,
)

class PublicProfileViewModel(
    private val getPublicProfile: GetPublicProfileUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(PublicProfileUiState())
    val uiState: StateFlow<PublicProfileUiState> = _uiState.asStateFlow()

    fun loadProfile(userId: String) {
        viewModelScope.launch {
            _uiState.value = PublicProfileUiState(isLoading = true)

            getPublicProfile(userId).fold(
                onSuccess = { profile ->
                    _uiState.value =
                        PublicProfileUiState(
                            profile = profile,
                            error =
                                if (profile == null) {
                                    "No se encontró el perfil."
                                } else {
                                    null
                                },
                        )
                },
                onFailure = {
                    _uiState.value =
                        PublicProfileUiState(
                            error = "No fue posible consultar el perfil público.",
                        )
                },
            )
        }
    }
}
