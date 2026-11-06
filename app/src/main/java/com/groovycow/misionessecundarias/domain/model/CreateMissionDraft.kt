package com.groovycow.misionessecundarias.domain.model

import java.time.LocalDate
import java.time.LocalTime

/** Datos editables para crear un borrador, antes de persistirlo como [Mission]. */
data class CreateMissionDraft(
    /** Título de la misión. */
    val title: String,
    /** Descripción detallada de la actividad. */
    val description: String,
    /** Requisitos para participar. */
    val requirements: String,
    /** Categoría opcional de la misión. */
    val category: String?,
    /** Modalidad seleccionada. */
    val modality: MissionModality,
    /** Día de inicio, nulo mientras no se seleccione. */
    val date: LocalDate?,
    /** Hora de inicio, nula mientras no se seleccione. */
    val time: LocalTime?,
    /** Nombre o dirección del punto de encuentro; solo aplica a misiones de campo. */
    val meetingPoint: String,
    /** Latitud del punto del mapa; solo aplica a misiones de campo. */
    val latitude: Double?,
    /** Longitud del punto del mapa; solo aplica a misiones de campo. */
    val longitude: Double?,
    /** Número máximo de participantes. */
    val maxParticipants: Int,
)
