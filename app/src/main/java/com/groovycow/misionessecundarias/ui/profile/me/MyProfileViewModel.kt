package com.groovycow.misionessecundarias.ui.profile.me

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.groovycow.misionessecundarias.domain.model.Profile
import com.groovycow.misionessecundarias.domain.model.ProfileError
import com.groovycow.misionessecundarias.domain.model.ProfileException
import com.groovycow.misionessecundarias.domain.usecase.GetMyProfileUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Estado de la pantalla "Mi perfil".
 *
 * @property profile Perfil propio cargado, o null mientras no se ha cargado.
 * @property isLoading `true` mientras se consulta el perfil.
 * @property error Error de la última carga, o null.
 */
data class MyProfileUiState(
    val profile: Profile? = null,
    val isLoading: Boolean = false,
    val error: ProfileError? = null,
) {
    companion object {
        /** Nivel fijo mostrado en el perfil (no existe aún un modelo de gamificación). */
        const val LEVEL_LABEL = "Nivel 1"
    }
}

/**
 * ViewModel de Mi perfil (HU-006): consulta el perfil propio cada vez que se entra a la pantalla.
 *
 * @param getMyProfile Caso de uso que obtiene el perfil del usuario autenticado.
 */
class MyProfileViewModel(
    private val getMyProfile: GetMyProfileUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(MyProfileUiState())

    /** Estado observable de la pantalla. */
    val uiState: StateFlow<MyProfileUiState> = _uiState.asStateFlow()

    /** Carga (o recarga) el perfil; conserva el perfil previo mientras llega el nuevo. */
    fun load() {
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            getMyProfile()
                .onSuccess { profile -> _uiState.value = MyProfileUiState(profile = profile) }
                .onFailure { throwable ->
                    val error = (throwable as? ProfileException)?.error ?: ProfileError.ServerError
                    _uiState.value = MyProfileUiState(error = error)
                }
        }
    }

    /** Reintenta la carga tras un error. */
    fun retry() = load()
}
