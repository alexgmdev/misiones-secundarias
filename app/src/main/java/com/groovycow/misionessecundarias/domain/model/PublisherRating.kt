package com.groovycow.misionessecundarias.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Calificación que un solucionador deja al publicador tras completar una misión (HU-124). */
@Serializable
data class PublisherRating(
    val id: String,
    @SerialName("mission_id") val missionId: String,
    @SerialName("solver_id") val solverId: String,
    @SerialName("publisher_id") val publisherId: String,
    /** Puntuación de 1 a 5 estrellas. */
    val stars: Int,
    @SerialName("created_at") val createdAt: String? = null,
)

/**
 * Reputación pública de un publicador.
 *
 * @property average Promedio de estrellas, redondeado a un decimal.
 * @property total Cantidad de calificaciones recibidas.
 */
data class PublisherReputation(
    val average: Double,
    val total: Int,
)
