package com.groovycow.misionessecundarias.ui.missions.live

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.groovycow.misionessecundarias.domain.model.ChecklistItem
import com.groovycow.misionessecundarias.domain.model.checklistProgressPercent
import com.groovycow.misionessecundarias.domain.usecase.AddChecklistItemUseCase
import com.groovycow.misionessecundarias.domain.usecase.ChecklistValidationException
import com.groovycow.misionessecundarias.domain.usecase.DeleteChecklistItemUseCase
import com.groovycow.misionessecundarias.domain.usecase.GetChecklistUseCase
import com.groovycow.misionessecundarias.domain.usecase.SetChecklistItemDoneUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Estado del checklist de ejecución y del progreso que se deriva de él (HU-053). */
data class ChecklistUiState(
    val isLoading: Boolean = false,
    val items: List<ChecklistItem> = emptyList(),
    val error: String? = null,
    val actionError: String? = null,
    val isBusy: Boolean = false,
) {
    /** Porcentaje de tareas cumplidas, de 0 a 100. */
    val progressPercent: Int get() = checklistProgressPercent(items)
}

/** Gestiona las tareas del checklist y calcula el porcentaje de progreso de la misión. */
class ChecklistViewModel(
    private val getChecklist: GetChecklistUseCase,
    private val addChecklistItem: AddChecklistItemUseCase,
    private val setItemDone: SetChecklistItemDoneUseCase,
    private val deleteChecklistItem: DeleteChecklistItemUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ChecklistUiState())
    val uiState: StateFlow<ChecklistUiState> = _uiState.asStateFlow()

    /** Primera carga con indicador de progreso. */
    fun load(missionId: String) {
        viewModelScope.launch {
            _uiState.value = ChecklistUiState(isLoading = true)
            getChecklist(missionId).fold(
                onSuccess = { _uiState.value = ChecklistUiState(items = it) },
                onFailure = { _uiState.value = ChecklistUiState(error = LOAD_ERROR) },
            )
        }
    }

    /** Actualización silenciosa; conserva lo visible si falla o si hay una acción en curso. */
    fun refresh(missionId: String) {
        viewModelScope.launch {
            if (_uiState.value.isBusy) return@launch
            getChecklist(missionId).onSuccess { items ->
                if (!_uiState.value.isBusy) _uiState.value = _uiState.value.copy(items = items, error = null)
            }
        }
    }

    /** Agrega una tarea; [onAdded] se llama solo si se guardó. */
    fun add(
        missionId: String,
        title: String,
        onAdded: () -> Unit = {},
    ) {
        runAction(
            action = { addChecklistItem(missionId, title).map { item -> { items: List<ChecklistItem> -> items + item } } },
            onSuccess = onAdded,
        )
    }

    /** Marca o desmarca [item] como cumplida. */
    fun toggle(item: ChecklistItem) {
        val done = !item.isDone
        runAction(
            action = {
                setItemDone(item.id, done).map {
                    { items: List<ChecklistItem> -> items.map { if (it.id == item.id) it.copy(isDone = done) else it } }
                }
            },
        )
    }

    /** Elimina [item] del checklist. */
    fun delete(item: ChecklistItem) {
        runAction(
            action = { deleteChecklistItem(item.id).map { { items: List<ChecklistItem> -> items.filterNot { it.id == item.id } } } },
        )
    }

    fun dismissActionError() {
        _uiState.value = _uiState.value.copy(actionError = null)
    }

    private fun runAction(
        action: suspend () -> Result<(List<ChecklistItem>) -> List<ChecklistItem>>,
        onSuccess: () -> Unit = {},
    ) {
        if (_uiState.value.isBusy) return
        _uiState.value = _uiState.value.copy(isBusy = true, actionError = null)
        viewModelScope.launch {
            action().fold(
                onSuccess = { transform ->
                    _uiState.value = _uiState.value.copy(items = transform(_uiState.value.items), isBusy = false)
                    onSuccess()
                },
                onFailure = { exception ->
                    _uiState.value = _uiState.value.copy(isBusy = false, actionError = exception.toActionMessage())
                },
            )
        }
    }

    private fun Throwable.toActionMessage(): String = if (this is ChecklistValidationException) message.orEmpty() else ACTION_ERROR

    private companion object {
        const val LOAD_ERROR = "No fue posible cargar el checklist. Inténtalo nuevamente."
        const val ACTION_ERROR = "No fue posible guardar el cambio. Inténtalo nuevamente."
    }
}
