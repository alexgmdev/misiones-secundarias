package com.groovycow.misionessecundarias.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.groovycow.misionessecundarias.domain.model.AdminError
import com.groovycow.misionessecundarias.domain.model.AdminMetrics
import com.groovycow.misionessecundarias.domain.model.AdminValidationException
import com.groovycow.misionessecundarias.domain.usecase.GetAdminMetricsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.IOException

/** Estado de la interfaz del Panel Administrativo de Moderador. */
sealed interface AdminPanelUiState {
    /** Se están consultando el rol y las métricas operativas. */
    data object Loading : AdminPanelUiState

    /** El usuario es un moderador/admin autorizado y se muestran sus métricas. */
    data class Success(val metrics: AdminMetrics) : AdminPanelUiState

    /** Acceso denegado porque el usuario no posee rol de moderador ni administrador. */
    data object AccessDenied : AdminPanelUiState

    /** Error de conexión o servidor al consultar el panel. */
    data class Error(val error: AdminError) : AdminPanelUiState
}

/** ViewModel del Panel Administrativo (Monitoreo y Métricas). */
class AdminPanelViewModel(
    private val getAdminMetricsUseCase: GetAdminMetricsUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow<AdminPanelUiState>(AdminPanelUiState.Loading)
    val uiState: StateFlow<AdminPanelUiState> = _uiState.asStateFlow()

    init {
        loadMetrics()
    }

    /** Consulta las métricas y valida el rol de acceso. */
    fun loadMetrics() {
        _uiState.value = AdminPanelUiState.Loading
        viewModelScope.launch {
            getAdminMetricsUseCase().fold(
                onSuccess = { metrics -> _uiState.value = AdminPanelUiState.Success(metrics) },
                onFailure = { exception ->
                    val adminError = exception.toAdminError()
                    _uiState.value =
                        if (adminError == AdminError.AccessDenied) {
                            AdminPanelUiState.AccessDenied
                        } else {
                            AdminPanelUiState.Error(adminError)
                        }
                },
            )
        }
    }

    private fun Throwable.toAdminError(): AdminError =
        when {
            this is AdminValidationException -> error
            hasIOExceptionCause() -> AdminError.NoConnection
            else -> AdminError.ServerError
        }

    private fun Throwable.hasIOExceptionCause(): Boolean {
        var current: Throwable? = this
        while (current != null) {
            if (current is IOException) return true
            current = current.cause
        }
        return false
    }
}
