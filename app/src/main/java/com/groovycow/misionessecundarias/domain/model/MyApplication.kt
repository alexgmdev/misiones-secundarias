package com.groovycow.misionessecundarias.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MyApplication(
    val id: String,
    @SerialName("mission_id") val missionId: String,
    val status: String,
    @SerialName("applied_at") val appliedAt: String,
    @SerialName("missions") val mission: AppliedMission
)

@Serializable
data class AppliedMission(
    val id: String,
    val title: String,
    @SerialName("date_time") val dateTime: String,
    val status: String
)
