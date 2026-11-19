package com.groovycow.misionessecundarias.ui.missions.create

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.groovycow.misionessecundarias.domain.model.CreateMissionDraft
import com.groovycow.misionessecundarias.domain.model.Mission
import com.groovycow.misionessecundarias.domain.model.MissionError
import com.groovycow.misionessecundarias.domain.model.MissionField
import com.groovycow.misionessecundarias.domain.model.MissionModality
import com.groovycow.misionessecundarias.domain.model.MissionSessionRequiredException
import com.groovycow.misionessecundarias.domain.usecase.CreateMissionUseCase
import com.groovycow.misionessecundarias.domain.validation.MissionDraftValidator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.IOException
import java.time.LocalDate
import java.time.LocalTime

/** Estado observable del formulario para crear un borrador de misión. */
data class CreateMissionUiState(
    /** Modalidad elegida; nula mientras se muestra el selector inicial. */
    val modality: MissionModality? = null,
    /** Título ingresado. */
    val title: String = "",
    /** Descripción ingresada. */
    val description: String = "",
    /** Requisitos ingresados. */
    val requirements: String = "",
    /** Categoría opcional ingresada. */
    val category: String = "",
    /** Punto de encuentro textual. */
    val meetingPoint: String = "",
    /** Latitud ingresada como texto para permitir edición gradual. */
    val latitude: String = "",
    /** Longitud ingresada como texto para permitir edición gradual. */
    val longitude: String = "",
    /** Cupo máximo ingresado como texto. */
    val maxParticipants: String = "2",
    /** Fecha de inicio seleccionada. */
    val date: LocalDate? = null,
    /** Hora de inicio seleccionada. */
    val time: LocalTime? = null,
    /** Errores de validación por campo. */
    val fieldErrors: Map<MissionField, MissionError> = emptyMap(),
    /** Indica que el guardado está en curso. */
    val isSaving: Boolean = false,
    /** Misión creada exitosamente como borrador. */
    val savedMission: Mission? = null,
    /** Error no asociado a un campo ocurrido al guardar. */
    val submissionError: MissionError? = null,
)

/** Maneja los cambios, validación y persistencia del formulario de creación. */
class CreateMissionViewModel(
    private val createMissionUseCase: CreateMissionUseCase,
    private val validator: MissionDraftValidator,
) : ViewModel() {
    private val _uiState = MutableStateFlow(CreateMissionUiState())

    /** Estado actual del formulario. */
    val uiState: StateFlow<CreateMissionUiState> = _uiState.asStateFlow()

    /** Selecciona el tipo de misión y habilita el formulario. */
    fun onModalitySelected(modality: MissionModality) {
        _uiState.value = _uiState.value.copy(modality = modality, fieldErrors = emptyMap(), submissionError = null)
    }

    /** Vuelve al selector de modalidad sin descartar los demás campos. */
    fun onChangeModality() {
        _uiState.value = _uiState.value.copy(modality = null, fieldErrors = emptyMap(), submissionError = null)
    }

    /** Actualiza el título. */
    fun onTitleChange(value: String) = updateField(MissionField.TITLE) { copy(title = value) }

    /** Actualiza la descripción. */
    fun onDescriptionChange(value: String) = updateField(MissionField.DESCRIPTION) { copy(description = value) }

    /** Actualiza los requisitos. */
    fun onRequirementsChange(value: String) = updateField(MissionField.REQUIREMENTS) { copy(requirements = value) }

    /** Actualiza la categoría opcional. */
    fun onCategoryChange(value: String) = updateField(null) { copy(category = value) }

    /** Actualiza el punto de encuentro. */
    fun onMeetingPointChange(value: String) = updateField(MissionField.MEETING_POINT) { copy(meetingPoint = value) }

    /** Actualiza la latitud. */
    fun onLatitudeChange(value: String) = updateField(MissionField.LATITUDE) { copy(latitude = value) }

    /** Actualiza la longitud. */
    fun onLongitudeChange(value: String) = updateField(MissionField.LONGITUDE) { copy(longitude = value) }

    /** Actualiza el cupo máximo. */
    fun onMaxParticipantsChange(value: String) = updateField(MissionField.MAX_PARTICIPANTS) { copy(maxParticipants = value) }

    /** Actualiza la fecha de inicio. */
    fun onDateChange(value: LocalDate?) = updateField(MissionField.DATE) { copy(date = value) }

    /** Actualiza la hora de inicio. */
    fun onTimeChange(value: LocalTime?) = updateField(MissionField.TIME) { copy(time = value) }

    /** Restablece el formulario para un nuevo borrador. */
    fun reset() {
        _uiState.value = CreateMissionUiState()
    }

    /** Valida y guarda el borrador; ignora llamadas repetidas mientras guarda. */
    @Suppress("ReturnCount")
    fun saveDraft() {
        val currentState = _uiState.value
        if (currentState.isSaving || currentState.savedMission != null) return
        val modality = currentState.modality ?: return
        val draft = currentState.toDraft(modality)
        val validation = validator.validate(draft)

        if (!validation.isValid) {
            _uiState.value = currentState.copy(fieldErrors = validation.fieldErrors, submissionError = null)
            return
        }

        _uiState.value = currentState.copy(isSaving = true, fieldErrors = emptyMap(), submissionError = null)
        viewModelScope.launch {
            createMissionUseCase(draft).fold(
                onSuccess = { mission ->
                    _uiState.value = _uiState.value.copy(isSaving = false, savedMission = mission)
                },
                onFailure = { exception ->
                    _uiState.value = _uiState.value.copy(isSaving = false, submissionError = exception.toMissionError())
                },
            )
        }
    }

    private fun updateField(
        field: MissionField?,
        transform: CreateMissionUiState.() -> CreateMissionUiState,
    ) {
        val current = _uiState.value
        if (current.isSaving || current.savedMission != null) return
        val errors = field?.let { current.fieldErrors - it } ?: current.fieldErrors
        _uiState.value = current.transform().copy(fieldErrors = errors, submissionError = null)
    }

    private fun CreateMissionUiState.toDraft(modality: MissionModality): CreateMissionDraft =
        CreateMissionDraft(
            title = title,
            description = description,
            requirements = requirements,
            category = category,
            modality = modality,
            date = date,
            time = time,
            meetingPoint = meetingPoint,
            latitude = latitude.toCoordinateOrNull(),
            longitude = longitude.toCoordinateOrNull(),
            maxParticipants = maxParticipants.toIntOrNull() ?: 0,
        )

    private fun String.toCoordinateOrNull(): Double? = trim().replace(',', '.').toDoubleOrNull()

    private fun Throwable.toMissionError(): MissionError {
        if (this is MissionSessionRequiredException) return MissionError.SessionRequired
        var current: Throwable? = this
        while (current != null && current !is IOException) current = current.cause
        return if (current != null) MissionError.NoConnection else MissionError.ServerError
    }
}
