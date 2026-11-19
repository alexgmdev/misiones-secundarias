package com.groovycow.misionessecundarias.data.repository

import com.groovycow.misionessecundarias.domain.model.MissionApplicant
import com.groovycow.misionessecundarias.domain.model.MissionLogbook
import com.groovycow.misionessecundarias.domain.model.MissionSessionRequiredException
import com.groovycow.misionessecundarias.domain.repository.LogbookRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.logging.Level
import java.util.logging.Logger

class LogbookRepositoryImpl(
    private val supabase: SupabaseClient,
) : LogbookRepository {
    override suspend fun savePublisherLogbook(
        missionId: String,
        entry: String,
        participants: List<MissionApplicant>,
    ): MissionLogbook =
        withContext(Dispatchers.IO) {
            try {
                savePublisherLogbookInternal(missionId, entry, participants)
            } catch (exception: Exception) {
                LOGGER.log(Level.SEVERE, "Fallo al guardar la bitácora del publicador en Supabase", exception)
                throw exception
            }
        }

    private suspend fun savePublisherLogbookInternal(
        missionId: String,
        entry: String,
        participants: List<MissionApplicant>,
    ): MissionLogbook {
        val authorId = authenticatedUserId()
        val trimmedEntry = entry.trim()
        require(trimmedEntry.isNotEmpty()) { "El relato de la bitácora es obligatorio." }

        val participantIds = participants.map { it.solverId }
        val participantNames = participants.map { it.profile.fullName }

        return supabase.from("mission_logbooks")
            .insert(
                PublisherLogbookDto(
                    missionId = missionId,
                    authorId = authorId,
                    role = "publicador",
                    entry = trimmedEntry,
                    participantIds = participantIds,
                    participantNames = participantNames,
                ),
            ) {
                select(columns = Columns.raw("*, author_profile:profiles!mission_logbooks_author_id_fkey(full_name)"))
            }
            .decodeSingle<MissionLogbook>()
    }

    override suspend fun saveParticipantLogbook(
        missionId: String,
        entry: String,
    ): MissionLogbook =
        withContext(Dispatchers.IO) {
            try {
                saveParticipantLogbookInternal(missionId, entry)
            } catch (exception: Exception) {
                LOGGER.log(Level.SEVERE, "Fallo al guardar la bitácora del participante en Supabase", exception)
                throw exception
            }
        }

    private suspend fun saveParticipantLogbookInternal(
        missionId: String,
        entry: String,
    ): MissionLogbook {
        val authorId = authenticatedUserId()
        val trimmedEntry = entry.trim()
        require(trimmedEntry.isNotEmpty()) { "El relato de la vivencia es obligatorio." }

        return supabase.from("mission_logbooks")
            .insert(
                ParticipantLogbookDto(
                    missionId = missionId,
                    authorId = authorId,
                    role = "participante",
                    entry = trimmedEntry,
                ),
            ) {
                select(columns = Columns.raw("*, author_profile:profiles!mission_logbooks_author_id_fkey(full_name)"))
            }
            .decodeSingle<MissionLogbook>()
    }

    override suspend fun getLogbooksForMission(missionId: String): List<MissionLogbook> =
        withContext(Dispatchers.IO) {
            supabase.from("mission_logbooks")
                .select(columns = Columns.raw("*, author_profile:profiles!mission_logbooks_author_id_fkey(full_name)")) {
                    filter { eq("mission_id", missionId) }
                    order("created_at", Order.ASCENDING)
                }
                .decodeList<MissionLogbook>()
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

    private companion object {
        val LOGGER: Logger = Logger.getLogger(LogbookRepositoryImpl::class.java.name)
    }
}

@Serializable
private data class PublisherLogbookDto(
    @SerialName("mission_id") val missionId: String,
    @SerialName("author_id") val authorId: String,
    val role: String,
    val entry: String,
    @SerialName("participant_ids") val participantIds: List<String>,
    @SerialName("participant_names") val participantNames: List<String>,
)

@Serializable
private data class ParticipantLogbookDto(
    @SerialName("mission_id") val missionId: String,
    @SerialName("author_id") val authorId: String,
    val role: String,
    val entry: String,
)
