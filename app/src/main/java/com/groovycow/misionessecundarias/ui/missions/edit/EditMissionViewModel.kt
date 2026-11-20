package com.groovycow.misionessecundarias.ui.missions.edit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.groovycow.misionessecundarias.domain.model.Mission
import com.groovycow.misionessecundarias.domain.model.MissionDetailsUpdate
import com.groovycow.misionessecundarias.domain.model.MissionError
import com.groovycow.misionessecundarias.domain.model.MissionValidationException
import com.groovycow.misionessecundarias.domain.usecase.UpdateMissionDetailsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.IOException

/** Estado del formulario de edición de una misión publicada (HU-032). */
data class EditMissionUiState(
    /** Identificador de la misión cargada. */
    val missionId: String = "",
    /** Título editable. */
    val title: String = "",
    /** Descripción editable. */
    val description: String = "",
    /** Requisitos editables. */
    val requirements: String = "",
    /** Categoría opcional. */
    val category: String = "",
    /** Recompensa informativa opcional. */
    val reward: String = "",
    /** Indica que la actualización está en curso. */
    val isSaving: Boolean = false,
    /** Misión actualizada correctamente. */
    val savedMission: Mission? = null,
    /** Error de validación, red o servidor. */
    val error: MissionError? = null,
)

/** Maneja la edición de los datos descriptivos de una misión publicada sin participantes. */
class EditMissionViewModel(
    private val updateMissionDetailsUseCase: UpdateMissionDetailsUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(EditMissionUiState())

    /** Estado actual del formulario. */
    val uiState: StateFlow<EditMissionUiState> = _uiState.asStateFlow()

    /** Carga los valores actuales de [mission]. */
    fun load(mission: Mission) {
        if (_uiState.value.missionId == mission.id && _uiState.value.savedMission == null) return
        _uiState.value =
            EditMissionUiState(
                missionId = mission.id,
                title = mission.title,
                description = mission.description,
                requirements = mission.requirements.orEmpty(),
                category = mission.category.orEmpty(),
                reward = mission.reward.orEmpty(),
            )
    }

    /** Actualiza el título. */
    fun onTitleChange(value: String) = update { copy(title = value) }

    /** Actualiza la descripción. */
    fun onDescriptionChange(value: String) = update { copy(description = value) }

    /** Actualiza los requisitos. */
    fun onRequirementsChange(value: String) = update { copy(requirements = value) }

    /** Actualiza la categoría opcional. */
    fun onCategoryChange(value: String) = update { copy(category = value) }

    /** Actualiza la recompensa informativa opcional. */
    fun onRewardChange(value: String) = update { copy(reward = value) }

    /** Valida y guarda los cambios; ignora envíos repetidos. */
    fun save() {
        val current = _uiState.value
        if (current.isSaving || current.savedMission != null) return
        val update =
            MissionDetailsUpdate(
                title = current.title,
                description = current.description,
                requirements = current.requirements,
                category = current.category.takeIf { it.isNotBlank() },
                reward = current.reward.takeIf { it.isNotBlank() },
            )
        if (update.title.isBlank() || update.description.isBlank() || update.requirements.isBlank()) {
            _uiState.value = current.copy(error = MissionError.InvalidDetails)
            return
        }

        _uiState.value = current.copy(isSaving = true, error = null)
        viewModelScope.launch {
            updateMissionDetailsUseCase(current.missionId, update).fold(
                onSuccess = { mission ->
                    _uiState.value = _uiState.value.copy(isSaving = false, savedMission = mission)
                },
                onFailure = { exception ->
                    _uiState.value =
                        _uiState.value.copy(isSaving = false, error = exception.toMissionError())
                },
            )
        }
    }

    private fun update(transform: EditMissionUiState.() -> EditMissionUiState) {
        val current = _uiState.value
        if (current.isSaving || current.savedMission != null) return
        _uiState.value = current.transform().copy(error = null)
    }

    private fun Throwable.toMissionError(): MissionError =
        when (this) {
            is MissionValidationException -> error
            else -> if (hasIOExceptionCause()) MissionError.NoConnection else MissionError.ServerError
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
