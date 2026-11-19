package com.groovycow.misionessecundarias.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.groovycow.misionessecundarias.domain.model.SessionState
import com.groovycow.misionessecundarias.domain.usecase.ObserveSessionUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
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
 * ViewModel de sesión: decide una sola vez la pantalla inicial y mantiene disponible
 * el estado actual para impedir el acceso a pantallas protegidas cuando no hay sesión.
 *
 * @param observeSession Caso de uso que observa el estado de la sesión.
 */
class SessionViewModel(observeSession: ObserveSessionUseCase) : ViewModel() {
    private val _startDestination = MutableStateFlow(StartDestination.LOADING)
    private val _isLoggedIn = MutableStateFlow(false)
    private val _isSessionResolved = MutableStateFlow(false)

    /** Destino inicial: LOADING hasta conocer el estado de la sesión, luego fijo. */
    val startDestination: StateFlow<StartDestination> = _startDestination.asStateFlow()

    /** `true` únicamente cuando el usuario tiene una sesión válida. */
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    /** Evita decidir la navegación antes de terminar de leer la sesión guardada. */
    val isSessionResolved: StateFlow<Boolean> = _isSessionResolved.asStateFlow()

    init {
        viewModelScope.launch {
            observeSession().collect { state ->
                if (state != SessionState.LOADING) {
                    updateLoginState(state == SessionState.AUTHENTICATED)
                }
                if (_startDestination.value == StartDestination.LOADING && state != SessionState.LOADING) {
                    _startDestination.value =
                        if (state == SessionState.AUTHENTICATED) {
                            StartDestination.MAIN_MENU
                        } else {
                            StartDestination.LOGIN
                        }
                }
            }
        }
    }

    /** Actualiza inmediatamente la validación global después de iniciar o cerrar sesión. */
    fun updateLoginState(isLoggedIn: Boolean) {
        _isLoggedIn.value = isLoggedIn
        _isSessionResolved.value = true
    }
}
