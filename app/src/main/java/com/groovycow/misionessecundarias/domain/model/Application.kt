package com.groovycow.misionessecundarias.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Application(
    val id: String,
    @SerialName("mission_id") val missionId: String,
    @SerialName("solver_id") val solverId: String,
    val status: String,
    @SerialName("applied_at") val appliedAt: String
)