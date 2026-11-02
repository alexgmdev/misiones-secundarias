package com.groovycow.misionessecundarias.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Mission(
    val id: String,
    @SerialName("publisher_id") val publisherId: String,
    val title: String,
    val description: String,
    val category: String? = null,
    val modality: String,
    @SerialName("date_time") val dateTime: String,
    val location: String? = null,
    @SerialName("max_participants") val maxParticipants: Int,
    val status: String
)