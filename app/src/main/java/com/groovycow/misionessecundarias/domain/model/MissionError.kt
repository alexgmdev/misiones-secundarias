package com.groovycow.misionessecundarias.domain.model

/** Errores tipados que puede producir la validación o el guardado de una misión. */
sealed interface MissionError {
    /** Falta completar un dato obligatorio. */
    data object Required : MissionError

    /** El punto indicado no corresponde a Cali. */
    data object LocationMustBeInCali : MissionError

    /** El número de cupos debe ser al menos dos. */
    data object MinimumParticipants : MissionError

    /** La fecha seleccionada ya pasó. */
    data object DateInPast : MissionError

    /** No se seleccionó una fecha de inicio. */
    data object DateRequired : MissionError

    /** No se seleccionó una hora de inicio. */
    data object TimeRequired : MissionError

    /** No hay conexión con el servicio. */
    data object NoConnection : MissionError

    /** Se necesita iniciar sesión para guardar una misión. */
    data object SessionRequired : MissionError

    /** El servicio rechazó el guardado o devolvió un error inesperado. */
    data object ServerError : MissionError

    /** El borrador no tiene todos los datos obligatorios para publicarse. */
    data object DraftIncomplete : MissionError

    /** El punto de encuentro debe definirse antes de publicar. */
    data object MeetingPointRequired : MissionError

    /** La fecha y hora del borrador no se pueden interpretar. */
    data object InvalidDateTime : MissionError

    /** La misión no está en estado de borrador. */
    data object NotDraft : MissionError

    /** Errores que impiden iniciar la ejecución de una misión (HU-050). */
    sealed interface Start : MissionError

    /** La misión no está en un estado desde el que pueda iniciarse. */
    data object NotStartable : Start

    /** Aún no llega la fecha y hora programadas de la misión. */
    data object StartTooEarly : Start

    /** No hay suficientes participantes aceptados para iniciar. */
    data object NotEnoughParticipants : Start

    /** La misión no existe o no es visible para el usuario. */
    data object MissionNotFound : Start, Complete

    /** Errores que impiden completar una misión en ejecución (HU-064). */
    sealed interface Complete : MissionError

    /** La misión no está en progreso, así que no puede completarse. */
    data object NotCompletable : Complete

    /** El progreso aún no llegó al 100 %. */
    data object ProgressIncomplete : Complete
}

/** Campo del formulario asociado a un error de validación. */
enum class MissionField {
    /** Título. */
    TITLE,

    /** Descripción. */
    DESCRIPTION,

    /** Requisitos. */
    REQUIREMENTS,

    /** Punto de encuentro. */
    MEETING_POINT,

    /** Latitud del punto de encuentro. */
    LATITUDE,

    /** Longitud del punto de encuentro. */
    LONGITUDE,

    /** Fecha de inicio. */
    DATE,

    /** Hora de inicio. */
    TIME,

    /** Cupo máximo. */
    MAX_PARTICIPANTS,
}
