
package com.groovycow.misionessecundarias.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MissionApplicant(
    val id: String,
    @SerialName("mission_id")
    val missionId: String,
    @SerialName("solver_id")
    val solverId: String,
    val status: String,
    @SerialName("applied_at")
    val appliedAt: String,
    val profiles: ApplicantProfile? = null
)

@Serializable
data class ApplicantProfile(
    @SerialName("full_name")
    val fullName: String,
    @SerialName("avatar_url")
    val avatarUrl: String? = null
)
