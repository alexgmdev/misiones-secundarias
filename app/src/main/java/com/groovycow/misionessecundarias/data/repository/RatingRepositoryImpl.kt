package com.groovycow.misionessecundarias.data.repository

import com.groovycow.misionessecundarias.domain.model.MissionSessionRequiredException
import com.groovycow.misionessecundarias.domain.model.PublisherRating
import com.groovycow.misionessecundarias.domain.model.PublisherReputation
import com.groovycow.misionessecundarias.domain.model.RatingError
import com.groovycow.misionessecundarias.domain.model.RatingValidationException
import com.groovycow.misionessecundarias.domain.repository.RatingRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.math.round

/** Implementación Supabase de las calificaciones al publicador (HU-124). */
class RatingRepositoryImpl(
    private val supabase: SupabaseClient,
) : RatingRepository {
    override suspend fun ratePublisher(
        missionId: String,
        stars: Int,
    ): PublisherRating =
        withContext(Dispatchers.IO) {
            val solverId = authenticatedUserId()
            val mission =
                supabase.from("missions")
                    .select(columns = Columns.raw("publisher_id")) {
                        filter { eq("id", missionId) }
                    }
                    .decodeSingleOrNull<MissionPublisherDto>()
                    ?: throw RatingValidationException(RatingError.MissionNotFound)

            val existing =
                supabase.from("publisher_ratings")
                    .select(columns = Columns.raw("id")) {
                        filter {
                            eq("mission_id", missionId)
                            eq("solver_id", solverId)
                        }
                    }
                    .decodeList<RatingIdDto>()
            if (existing.isNotEmpty()) {
                throw RatingValidationException(RatingError.AlreadyRated)
            }

            supabase.from("publisher_ratings")
                .insert(
                    RatingInsertDto(
                        missionId = missionId,
                        solverId = solverId,
                        publisherId = mission.publisherId,
                        stars = stars,
                    ),
                ) {
                    select()
                }
                .decodeSingle<PublisherRating>()
        }

    override suspend fun getPublisherReputation(publisherId: String): PublisherReputation? =
        withContext(Dispatchers.IO) {
            val rows =
                supabase.from("publisher_ratings")
                    .select(columns = Columns.raw("stars")) {
                        filter { eq("publisher_id", publisherId) }
                    }
                    .decodeList<RatingStarsDto>()

            if (rows.isEmpty()) {
                null
            } else {
                val average = rows.map { it.stars }.average()
                PublisherReputation(average = round(average * 10) / 10, total = rows.size)
            }
        }

    private suspend fun authenticatedUserId(): String {
        val currentUser = supabase.auth.currentUserOrNull()
        if (currentUser != null) return currentUser.id

        val recoveredUser =
            runCatching {
                supabase.auth.retrieveUserForCurrentSession(updateSession = true)
            }.getOrNull()

        return recoveredUser?.id ?: throw MissionSessionRequiredException()
    }
}

@Serializable
private data class MissionPublisherDto(
    @SerialName("publisher_id") val publisherId: String,
)

@Serializable
private data class RatingIdDto(val id: String)

@Serializable
private data class RatingStarsDto(val stars: Int)

@Serializable
private data class RatingInsertDto(
    @SerialName("mission_id") val missionId: String,
    @SerialName("solver_id") val solverId: String,
    @SerialName("publisher_id") val publisherId: String,
    val stars: Int,
)
