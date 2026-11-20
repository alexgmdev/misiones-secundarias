package com.groovycow.misionessecundarias.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Postulación visible para el publicador de una misión.
 */
@Serializable
data class MissionApplicant(
    val id: String,
    @SerialName("mission_id") val missionId: String,
    @SerialName("solver_id") val solverId: String,
    val status: String,
    @SerialName("applied_at") val appliedAt: String,
    @SerialName("applicant") val profile: ApplicantProfile,
)

/**
 * Datos públicos del usuario postulado.
 */

@Serializable
data class ApplicantProfile(
    @SerialName("full_name")
    val fullName: String,
    val nickname: String? = null,
    @SerialName("avatar_url")
    val avatarUrl: String? = null,
    val gender: String? = null,
    val description: String? = null,
    val level: String? = null,
)

/**
 * Decisiones permitidas sobre una postulación pendiente.
 */
enum class ApplicationDecision(val databaseValue: String) {
    ACCEPT("Aceptado"),
    REJECT("Rechazado"),
}
