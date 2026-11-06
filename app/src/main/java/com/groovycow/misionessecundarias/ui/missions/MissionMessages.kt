package com.groovycow.misionessecundarias.ui.missions

import com.groovycow.misionessecundarias.domain.model.MissionError

/**
 * Traduce este error al mensaje en español para la interfaz.
 *
 * @return Texto breve para mostrar en la pantalla.
 */
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
    }
