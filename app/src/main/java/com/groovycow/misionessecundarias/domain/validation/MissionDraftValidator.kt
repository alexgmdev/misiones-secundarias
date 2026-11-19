package com.groovycow.misionessecundarias.domain.validation

import com.groovycow.misionessecundarias.domain.model.CreateMissionDraft
import com.groovycow.misionessecundarias.domain.model.MissionError
import com.groovycow.misionessecundarias.domain.model.MissionField
import com.groovycow.misionessecundarias.domain.model.MissionModality
import java.time.Clock
import java.time.LocalDate

/** Valida obligatorios, fecha, cupos y ubicación antes de persistir un borrador. */
class MissionDraftValidator(
    /** Reloj inyectable para pruebas deterministas y fecha local de Cali. */
    private val clock: Clock,
) {
    /**
     * Devuelve todos los errores encontrados en [draft].
     *
     * @param draft Datos ingresados en el formulario.
     * @return Errores indexados por campo; vacío cuando el borrador es válido.
     */
    fun validate(draft: CreateMissionDraft): MissionValidationResult {
        val errors = mutableMapOf<MissionField, MissionError>()

        addRequiredError(draft.title, MissionField.TITLE, errors)
        addRequiredError(draft.description, MissionField.DESCRIPTION, errors)
        addRequiredError(draft.requirements, MissionField.REQUIREMENTS, errors)

        if (draft.modality == MissionModality.CAMPO) {
            validateMeetingPoint(draft, errors)
            validateCoordinates(draft, errors)
        }

        if (draft.maxParticipants < MIN_PARTICIPANTS) {
            errors[MissionField.MAX_PARTICIPANTS] = MissionError.MinimumParticipants
        }

        when {
            draft.date == null -> errors[MissionField.DATE] = MissionError.DateRequired
            draft.date.isBefore(LocalDate.now(clock)) -> errors[MissionField.DATE] = MissionError.DateInPast
        }
        if (draft.time == null) errors[MissionField.TIME] = MissionError.TimeRequired

        return MissionValidationResult(errors)
    }

    private fun validateMeetingPoint(
        draft: CreateMissionDraft,
        errors: MutableMap<MissionField, MissionError>,
    ) {
        when {
            draft.meetingPoint.isBlank() -> errors[MissionField.MEETING_POINT] = MissionError.Required
            !draft.meetingPoint.contains(CALI_NAME, ignoreCase = true) -> {
                errors[MissionField.MEETING_POINT] = MissionError.LocationMustBeInCali
            }
        }
    }

    private fun validateCoordinates(
        draft: CreateMissionDraft,
        errors: MutableMap<MissionField, MissionError>,
    ) {
        val latitude = draft.latitude
        val longitude = draft.longitude

        if (latitude == null) {
            errors[MissionField.LATITUDE] = MissionError.Required
        } else if (latitude !in MIN_CALI_LATITUDE..MAX_CALI_LATITUDE) {
            errors[MissionField.LATITUDE] = MissionError.LocationMustBeInCali
        }

        if (longitude == null) {
            errors[MissionField.LONGITUDE] = MissionError.Required
        } else if (longitude !in MIN_CALI_LONGITUDE..MAX_CALI_LONGITUDE) {
            errors[MissionField.LONGITUDE] = MissionError.LocationMustBeInCali
        }
    }

    private fun addRequiredError(
        value: String,
        field: MissionField,
        errors: MutableMap<MissionField, MissionError>,
    ) {
        if (value.isBlank()) errors[field] = MissionError.Required
    }

    private companion object {
        const val MIN_PARTICIPANTS = 2
        const val CALI_NAME = "Cali"
        const val MIN_CALI_LATITUDE = 3.30
        const val MAX_CALI_LATITUDE = 3.60
        const val MIN_CALI_LONGITUDE = -76.65
        const val MAX_CALI_LONGITUDE = -76.35
    }
}
