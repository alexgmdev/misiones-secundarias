package com.groovycow.misionessecundarias.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.groovycow.misionessecundarias.domain.model.SessionState
import com.groovycow.misionessecundarias.domain.usecase.ObserveSessionUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Pantalla con la que debe arrancar la aplicación. */
enum class StartDestination {
    /** Aún se está leyendo la sesión guardada. */
    LOADING,

    /** No hay sesión: ir a inicio de sesión. */
    LOGIN,

    /** Hay sesión: ir al menú principal. */
    MAIN_MENU,
}

/**
 * ViewModel de arranque: decide **una sola vez** la pantalla inicial según la sesión guardada.
 * Los cambios posteriores de sesión (registro, recuperación) no vuelven a mover la navegación.
 *
 * @param observeSession Caso de uso que observa el estado de la sesión.
 */
class SessionViewModel(observeSession: ObserveSessionUseCase) : ViewModel() {
    private val _startDestination = MutableStateFlow(StartDestination.LOADING)

    /** Destino inicial: LOADING hasta conocer el estado de la sesión, luego fijo. */
    val startDestination: StateFlow<StartDestination> = _startDestination.asStateFlow()

    init {
        viewModelScope.launch {
            val state = observeSession().first { it != SessionState.LOADING }
            _startDestination.value =
                if (state == SessionState.AUTHENTICATED) StartDestination.MAIN_MENU else StartDestination.LOGIN
        }
    }
}
