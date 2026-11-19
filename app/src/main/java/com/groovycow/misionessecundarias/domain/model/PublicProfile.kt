
package com.groovycow.misionessecundarias.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PublicProfile(
    val id: String,
    @SerialName("full_name")
    val fullName: String? = null,
    val nickname: String? = null,
    @SerialName("avatar_url")
    val avatarUrl: String? = null,
    val gender: String? = null,
    val description: String? = null,
    val level: String? = null,
)
