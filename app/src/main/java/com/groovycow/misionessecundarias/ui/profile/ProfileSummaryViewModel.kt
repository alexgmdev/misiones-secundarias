package com.groovycow.misionessecundarias.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.groovycow.misionessecundarias.domain.model.AvatarId
import com.groovycow.misionessecundarias.domain.usecase.GetMyProfileUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Resumen del perfil para el saludo del menú principal.
 *
 * @property nickname Apodo del usuario, o null si no se ha cargado.
 * @property avatarId Avatar elegido, o null.
 * @property photoUrl URL de la foto de perfil, o null.
 */
data class ProfileSummaryUiState(
    val nickname: String? = null,
    val avatarId: AvatarId? = null,
    val photoUrl: String? = null,
)

/**
 * ViewModel del saludo del menú: expone nickname, avatar y foto y se refresca al entrar al menú (D15).
 *
 * @param getMyProfile Caso de uso que obtiene el perfil del usuario autenticado.
 */
class ProfileSummaryViewModel(
    private val getMyProfile: GetMyProfileUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ProfileSummaryUiState())

    /** Resumen observable del perfil. */
    val uiState: StateFlow<ProfileSummaryUiState> = _uiState.asStateFlow()

    /** Vuelve a consultar el perfil; si falla conserva el resumen anterior. */
    fun refresh() {
        viewModelScope.launch {
            getMyProfile().onSuccess { profile ->
                _uiState.value = ProfileSummaryUiState(profile.nickname, profile.avatarId, profile.photoUrl)
            }
        }
    }
}
