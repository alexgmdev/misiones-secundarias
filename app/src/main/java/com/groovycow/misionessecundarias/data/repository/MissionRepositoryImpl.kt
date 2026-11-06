package com.groovycow.misionessecundarias.data.repository

import com.groovycow.misionessecundarias.domain.model.CreateMissionDraft
import com.groovycow.misionessecundarias.domain.model.Mission
import com.groovycow.misionessecundarias.domain.model.MissionModality
import com.groovycow.misionessecundarias.domain.repository.MissionRepository
import com.groovycow.misionessecundarias.domain.validation.MissionDateTime
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class MissionRepositoryImpl(private val supabase: SupabaseClient) : MissionRepository {
    override suspend fun createMission(draft: CreateMissionDraft): Mission =
        withContext(Dispatchers.IO) {
            val publisherId = authenticatedUserId()
            val location =
                if (draft.modality == MissionModality.CAMPO) {
                    val latitude = requireNotNull(draft.latitude) { "La latitud es obligatoria para una misión de campo." }
                    val longitude = requireNotNull(draft.longitude) { "La longitud es obligatoria para una misión de campo." }
                    "SRID=4326;POINT($longitude $latitude)"
                } else {
                    null
                }
            val date = requireNotNull(draft.date) { "La fecha de inicio es obligatoria." }
            val time = requireNotNull(draft.time) { "La hora de inicio es obligatoria." }

            supabase.from("missions")
                .insert(
                    MissionInsertDto(
                        publisherId = publisherId,
                        title = draft.title.trim(),
                        description = draft.description.trim(),
                        category = draft.category?.trim()?.takeIf(String::isNotEmpty),
                        requirements = draft.requirements.trim(),
                        modality = draft.modality.databaseValue,
                        dateTime = MissionDateTime.toIsoOffset(date, time),
                        location = location,
                        meetingPoint =
                            draft.meetingPoint.trim().takeIf {
                                draft.modality == MissionModality.CAMPO && it.isNotEmpty()
                            },
                        maxParticipants = draft.maxParticipants,
                        status = "Borrador",
                    ),
                ) {
                    select()
                }
                .decodeSingle<Mission>()
        }

    override suspend fun getPublishedMissions(): List<Mission> =
        withContext(Dispatchers.IO) {
            supabase.from("missions")
                .select(columns = Columns.raw("*, profiles(full_name)")) { // <-- JOIN con profiles
                    filter {
                        neq("status", "Borrador")
                    }
                }.decodeList<Mission>()
        }

    override suspend fun getMissionById(id: String): Mission? =
        withContext(Dispatchers.IO) {
            supabase.from("missions")
                .select(columns = Columns.raw("*, profiles(full_name)")) { // <-- JOIN con profiles
                    filter {
                        eq("id", id)
                    }
                }.decodeSingleOrNull<Mission>()
        }

    private suspend fun authenticatedUserId(): String {
        val currentUser = supabase.auth.currentUserOrNull()
        if (currentUser != null) return currentUser.id

        val recoveredUser =
            runCatching {
                supabase.auth.retrieveUserForCurrentSession(updateSession = true)
            }.getOrNull()

        return recoveredUser?.id ?: error("Debes iniciar sesión para guardar una misión.")
    }
}

@Serializable
private data class MissionInsertDto(
    @SerialName("publisher_id") val publisherId: String,
    val title: String,
    val description: String,
    val category: String?,
    val requirements: String,
    val modality: String,
    @SerialName("date_time") val dateTime: String,
    val location: String?,
    @SerialName("meeting_point") val meetingPoint: String?,
    @SerialName("max_participants") val maxParticipants: Int,
    val status: String,
)
