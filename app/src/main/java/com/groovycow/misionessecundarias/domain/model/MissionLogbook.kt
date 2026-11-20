package com.groovycow.misionessecundarias.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Entrada de la bitácora oficial de la vivencia de una misión (HU - Evaluación y Cierre: Publicador + Participantes).
 */
@Serializable
data class MissionLogbook(
    val id: String,
    @SerialName("mission_id") val missionId: String,
    @SerialName("author_id") val authorId: String,
    val role: String,
    val entry: String,
    @SerialName("participant_ids") val participantIds: List<String> = emptyList(),
    @SerialName("participant_names") val participantNames: List<String> = emptyList(),
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("author_profile") val authorProfile: LogbookAuthorProfile? = null,
)

@Serializable
data class LogbookAuthorProfile(
    @SerialName("full_name") val fullName: String,
)
