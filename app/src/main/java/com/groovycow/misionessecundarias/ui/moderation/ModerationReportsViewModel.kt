package com.groovycow.misionessecundarias.ui.moderation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.groovycow.misionessecundarias.domain.model.ModerationError
import com.groovycow.misionessecundarias.domain.model.ModerationReport
import com.groovycow.misionessecundarias.domain.model.ModerationValidationException
import com.groovycow.misionessecundarias.domain.model.ReportPriority
import com.groovycow.misionessecundarias.domain.model.ReportSortOrder
import com.groovycow.misionessecundarias.domain.model.ReportStatus
import com.groovycow.misionessecundarias.domain.usecase.GetModerationReportsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.IOException

/** Estado de la interfaz de la cola de moderación. */
sealed interface ModerationReportsUiState {
    /** Cargando la lista de denuncias de moderación. */
    data object Loading : ModerationReportsUiState

    /** Lista de reportes obtenidos y filtrados para el moderador. */
    data class Success(
        val reports: List<ModerationReport>,
        val selectedPriority: ReportPriority? = null,
        val selectedStatus: ReportStatus? = ReportStatus.PENDIENTE,
        val selectedTargetType: String? = null,
        val sortOrder: ReportSortOrder = ReportSortOrder.PRIORITY_HIGH_FIRST,
    ) : ModerationReportsUiState

    /** Acceso denegado: el usuario no tiene rol de moderador. */
    data object AccessDenied : ModerationReportsUiState

    /** Error de conexión o servidor al consultar los reportes. */
    data class Error(val error: ModerationError) : ModerationReportsUiState
}

/** ViewModel de la pantalla de gestión de reportes de moderación. */
class ModerationReportsViewModel(
    private val getModerationReportsUseCase: GetModerationReportsUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow<ModerationReportsUiState>(ModerationReportsUiState.Loading)
    val uiState: StateFlow<ModerationReportsUiState> = _uiState.asStateFlow()

    private var currentPriorityFilter: ReportPriority? = null
    private var currentStatusFilter: ReportStatus? = ReportStatus.PENDIENTE
    private var currentTargetTypeFilter: String? = null
    private var currentSortOrder: ReportSortOrder = ReportSortOrder.PRIORITY_HIGH_FIRST

    init {
        loadReports()
    }

    /** Consulta la cola de reportes aplicando los filtros y ordenamiento activos. */
    fun loadReports() {
        _uiState.value = ModerationReportsUiState.Loading
        viewModelScope.launch {
            getModerationReportsUseCase(
                priorityFilter = currentPriorityFilter,
                statusFilter = currentStatusFilter,
                targetTypeFilter = currentTargetTypeFilter,
                sortOrder = currentSortOrder,
            ).fold(
                onSuccess = { reports ->
                    _uiState.value =
                        ModerationReportsUiState.Success(
                            reports = reports,
                            selectedPriority = currentPriorityFilter,
                            selectedStatus = currentStatusFilter,
                            selectedTargetType = currentTargetTypeFilter,
                            sortOrder = currentSortOrder,
                        )
                },
                onFailure = { exception ->
                    val error = exception.toModerationError()
                    _uiState.value =
                        if (error == ModerationError.AccessDenied) {
                            ModerationReportsUiState.AccessDenied
                        } else {
                            ModerationReportsUiState.Error(error)
                        }
                },
            )
        }
    }

    /** Cambia el criterio de ordenamiento (ej. 'Prioridad Alta' despliega urgentes primero). */
    fun updateSortOrder(newSortOrder: ReportSortOrder) {
        if (currentSortOrder == newSortOrder) return
        currentSortOrder = newSortOrder
        loadReports()
    }

    /** Aplica o limpia el filtro por severidad/prioridad. */
    fun updatePriorityFilter(priority: ReportPriority?) {
        currentPriorityFilter = priority
        loadReports()
    }

    /** Aplica o limpia el filtro por estado. */
    fun updateStatusFilter(status: ReportStatus?) {
        currentStatusFilter = status
        loadReports()
    }

    /** Aplica o limpia el filtro por tipo de contenido. */
    fun updateTargetTypeFilter(targetType: String?) {
        currentTargetTypeFilter = targetType
        loadReports()
    }

    private fun Throwable.toModerationError(): ModerationError =
        when {
            this is ModerationValidationException -> error
            hasIOExceptionCause() -> ModerationError.NoConnection
            else -> ModerationError.ServerError
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
