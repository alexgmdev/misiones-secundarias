package com.groovycow.misionessecundarias.domain.model

/** Rol que tuvo el usuario en una misión del historial. */
enum class HistoryRole {
    /** El usuario publicó la misión. */
    PUBLISHER,

    /** El usuario fue aceptado como solucionador. */
    SOLVER,
}

/**
 * Misión de la línea temporal del historial (HU-011).
 *
 * @property missionId Identificador de la misión.
 * @property title Título.
 * @property dateTime Fecha y hora de la misión (ISO 8601).
 * @property status Estado de la misión.
 * @property role Rol del usuario en la misión.
 * @property description Descripción, o null si no se pudo obtener.
 * @property requirements Requerimientos, o null si no hay o no se pudieron obtener.
 */
data class MissionHistoryEntry(
    val missionId: String,
    val title: String,
    val dateTime: String,
    val status: String,
    val role: HistoryRole,
    val description: String? = null,
    val requirements: String? = null,
)

/**
 * Registro de un participante en la bitácora de una misión.
 *
 * @property authorName Nombre del autor.
 * @property text Texto del registro.
 */
data class ParticipantWords(
    val authorName: String,
    val text: String,
)

/**
 * Secciones de bitácora de una misión expandida.
 *
 * @property official Bitácora oficial del publicador, o null si no existe.
 * @property participants Palabras de los participantes (puede estar vacía).
 */
data class MissionLogbookSections(
    val official: String?,
    val participants: List<ParticipantWords>,
)
