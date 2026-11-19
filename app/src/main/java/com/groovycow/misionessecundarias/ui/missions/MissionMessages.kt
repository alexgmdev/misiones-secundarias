package com.groovycow.misionessecundarias.ui.missions

import com.groovycow.misionessecundarias.domain.model.MissionError

/**
 * Traduce este error al mensaje en español para la interfaz.
 *
 * @return Texto breve para mostrar en la pantalla.
 */
@Suppress("CyclomaticComplexMethod") // `when` exhaustivo: una rama por tipo de error.
fun MissionError.toMessage(): String =
    when (this) {
        MissionError.Required -> "Este campo es obligatorio"
        MissionError.LocationMustBeInCali -> "La ubicación debe estar en Cali"
        MissionError.MinimumParticipants -> "Debe haber al menos 2 cupos"
        MissionError.DateInPast -> "La fecha no puede ser anterior a hoy"
        MissionError.DateRequired -> "Selecciona la fecha de inicio"
        MissionError.TimeRequired -> "Selecciona la hora de inicio"
        MissionError.NoConnection -> "Sin conexión. Intenta de nuevo"
        MissionError.SessionRequired -> "Debes iniciar sesión para guardar una misión"
        MissionError.ServerError -> "Ocurrió un error. Intenta de nuevo"
        MissionError.DraftIncomplete -> "Completa los datos obligatorios antes de publicar"
        MissionError.MeetingPointRequired -> "Define el punto de encuentro antes de publicar"
        MissionError.InvalidDateTime -> "Revisa la fecha y hora de inicio antes de publicar"
        MissionError.NotDraft -> "Solo se puede publicar una misión en estado de borrador"
        is MissionError.Start -> toStartMessage()
        is MissionError.Complete -> toCompleteMessage()
    }

private fun MissionError.Complete.toCompleteMessage(): String =
    when (this) {
        MissionError.NotCompletable -> "Solo se puede completar una misión que está en progreso"
        MissionError.ProgressIncomplete -> "El progreso debe llegar al 100 % para completar la misión"
        MissionError.MissionNotFound -> "No encontramos la misión"
    }

private fun MissionError.Start.toStartMessage(): String =
    when (this) {
        MissionError.NotStartable -> "Esta misión no se puede iniciar en su estado actual"
        MissionError.StartTooEarly -> "Aún no es la hora programada de la misión"
        MissionError.NotEnoughParticipants -> "Necesitas al menos un participante aceptado para iniciar"
        MissionError.MissionNotFound -> "No encontramos la misión"
    }
