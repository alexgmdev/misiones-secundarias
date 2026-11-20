package com.groovycow.misionessecundarias.ui.profile.publicprofile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.groovycow.misionessecundarias.domain.model.ProfileError
import com.groovycow.misionessecundarias.domain.model.ProfileException
import com.groovycow.misionessecundarias.domain.model.PublicProfile
import com.groovycow.misionessecundarias.domain.usecase.GetPublicProfileUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Estado de la ficha pública de otro usuario (HU-007).
 *
 * Solo contiene campos públicos: nunca expone correo, celular ni nombres completos.
 *
 * @property profile Perfil público cargado, o null mientras no se ha cargado.
 * @property isLoading `true` mientras se consulta el perfil.
 * @property error Error de la última carga, o null.
 */
data class PublicProfileUiState(
    val profile: PublicProfile? = null,
    val isLoading: Boolean = false,
    val error: ProfileError? = null,
)

/**
 * ViewModel de la ficha pública (HU-007), identificado por el usuario consultado.
 *
 * @param getPublicProfile Caso de uso que obtiene el perfil público de otro usuario.
 */
class PublicProfileViewModel(
    private val getPublicProfile: GetPublicProfileUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(PublicProfileUiState())

    /** Estado observable de la ficha. */
    val uiState: StateFlow<PublicProfileUiState> = _uiState.asStateFlow()

    private var currentUserId: String? = null

    /**
     * Carga el perfil de [userId]; si ya está cargado (o cargándose) el mismo usuario no repite la consulta.
     *
     * @param userId Identificador del usuario a mostrar.
     */
    fun load(userId: String) {
        if (userId == currentUserId && _uiState.value.error == null) return
        fetch(userId)
    }

    /** Reintenta la carga del último usuario tras un error. */
    fun retry() {
        currentUserId?.let { fetch(it) }
    }

    private fun fetch(userId: String) {
        currentUserId = userId
        _uiState.value = PublicProfileUiState(isLoading = true)
        viewModelScope.launch {
            getPublicProfile(userId)
                .onSuccess { profile -> _uiState.value = PublicProfileUiState(profile = profile) }
                .onFailure { throwable ->
                    val error = (throwable as? ProfileException)?.error ?: ProfileError.ServerError
                    _uiState.value = PublicProfileUiState(error = error)
                }
        }
    }
}
