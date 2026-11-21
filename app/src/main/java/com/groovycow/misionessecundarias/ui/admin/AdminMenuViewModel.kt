package com.groovycow.misionessecundarias.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.groovycow.misionessecundarias.domain.usecase.CanAccessAdminPanelUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Resuelve si la opción del panel debe aparecer en el menú de la sesión actual. */
class AdminMenuViewModel(
    private val canAccessAdminPanelUseCase: CanAccessAdminPanelUseCase,
) : ViewModel() {
    private val _canShowAdminPanel = MutableStateFlow(false)
    val canShowAdminPanel: StateFlow<Boolean> = _canShowAdminPanel.asStateFlow()

    fun refreshAccess() {
        _canShowAdminPanel.value = false
        viewModelScope.launch {
            _canShowAdminPanel.value = canAccessAdminPanelUseCase().getOrDefault(false)
        }
    }

    fun hideAdminPanelEntry() {
        _canShowAdminPanel.value = false
    }
}
