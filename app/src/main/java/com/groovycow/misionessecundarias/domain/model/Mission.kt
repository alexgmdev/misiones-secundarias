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
    val requirements: String? = null,
    val modality: String,
    @SerialName("date_time") val dateTime: String,
    val location: String? = null,
    @SerialName("max_participants") val maxParticipants: Int,
    val status: String,
    @SerialName("profiles") val publisherProfile: PublisherProfile? = null,
)

@Serializable
data class PublisherProfile(
    @SerialName("full_name") val fullName: String,
)
