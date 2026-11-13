package com.groovycow.misionessecundarias.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Postulación visible para el publicador de una misión. */
@Serializable
data class MissionApplicant(
    val id: String,
    @SerialName("mission_id") val missionId: String,
    @SerialName("solver_id") val solverId: String,
    val status: String,
    @SerialName("applied_at") val appliedAt: String,
    @SerialName("applicant") val profile: ApplicantProfile,
)

/** Datos públicos mínimos del usuario postulado. */
@Serializable
data class ApplicantProfile(
    @SerialName("full_name") val fullName: String,
)

/** Decisiones permitidas sobre una postulación pendiente. */
enum class ApplicationDecision(val databaseValue: String) {
    /** Incluye al usuario entre los participantes aceptados. */
    ACCEPT("Aceptado"),

    /** Informa que el usuario no fue seleccionado. */
    REJECT("Rechazado"),
}
