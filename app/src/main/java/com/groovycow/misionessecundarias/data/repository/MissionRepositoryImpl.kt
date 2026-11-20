package com.groovycow.misionessecundarias.data.repository

import com.groovycow.misionessecundarias.domain.model.CreateMissionDraft
import com.groovycow.misionessecundarias.domain.model.Mission
import com.groovycow.misionessecundarias.domain.model.MissionDetailsUpdate
import com.groovycow.misionessecundarias.domain.model.MissionModality
import com.groovycow.misionessecundarias.domain.model.MissionSessionRequiredException
import com.groovycow.misionessecundarias.domain.repository.MissionRepository
import com.groovycow.misionessecundarias.domain.validation.MissionDateTime
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

class MissionRepositoryImpl(private val supabase: SupabaseClient) : MissionRepository {
    override suspend fun createMission(draft: CreateMissionDraft): Mission =
        withContext(Dispatchers.IO) {
            try {
                createMissionInternal(draft)
            } catch (exception: Exception) {
                LOGGER.log(Level.SEVERE, "Fallo al crear el borrador", exception)
                throw exception
            }
        }

    private suspend fun createMissionInternal(draft: CreateMissionDraft): Mission {
        val publisherId = authenticatedUserId("guardar una misión")
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
        val title = draft.title.trim()
        val description = draft.description.trim()
        val category = draft.category?.trim()?.takeIf(String::isNotEmpty)
        val requirements = draft.requirements.trim()
        val reward = draft.reward.trim().takeIf(String::isNotEmpty)
        val dateTime = MissionDateTime.toIsoOffset(date, time)
        val meetingPoint =
            draft.meetingPoint.trim().takeIf {
                draft.modality == MissionModality.CAMPO && it.isNotEmpty()
            }

        val savedId =
            supabase.from("missions")
                .insert(
                    MissionInsertDto(
                        publisherId = publisherId,
                        title = title,
                        description = description,
                        category = category,
                        requirements = requirements,
                        modality = draft.modality.databaseValue,
                        dateTime = dateTime,
                        location = location,
                        meetingPoint = meetingPoint,
                        reward = reward,
                        maxParticipants = draft.maxParticipants,
                        status = "Borrador",
                    ),
                ) {
                    select(columns = Columns.raw("id"))
                }
                .decodeSingle<MissionInsertResponseDto>()
                .id

        return Mission(
            id = savedId,
            publisherId = publisherId,
            title = title,
            description = description,
            category = category,
            requirements = requirements,
            modality = draft.modality.databaseValue,
            dateTime = dateTime,
            location = location,
            meetingPoint = meetingPoint,
            reward = reward,
            maxParticipants = draft.maxParticipants,
            status = "Borrador",
            publisherProfile = null,
        )
    }

    override suspend fun getPublishedMissions(): List<Mission> =
        withContext(Dispatchers.IO) {
            supabase.from("public_missions")
                .select(columns = Columns.raw("*, profiles(full_name)")) { // <-- JOIN con profiles
                    filter {
                        neq("status", "Borrador")
                    }
                }.decodeList<Mission>()
        }

    override suspend fun completeMission(id: String): Mission =
        withContext(Dispatchers.IO) {
            val publisherId = authenticatedUserId("completar una misión")
            val updatedMission =
                supabase.from("missions")
                    .update({ set("status", "Completada") }) {
                        filter {
                            eq("id", id)
                            eq("publisher_id", publisherId)
                            eq("status", "En progreso")
                        }
                        select()
                    }
                    .decodeSingleOrNull<Mission>()

            updatedMission ?: findOwnMission(id, publisherId)?.takeIf { it.status == "Completada" }
                ?: error("La misión no existe, no te pertenece o no está en progreso.")
        }

    override suspend fun updateMissionDetails(
        id: String,
        update: MissionDetailsUpdate,
    ): Mission =
        withContext(Dispatchers.IO) {
            val publisherId = authenticatedUserId("editar una misión")
            val updatedMission =
                supabase.from("missions")
                    .update(
                        {
                            set("title", update.title)
                            set("description", update.description)
                            set("requirements", update.requirements)
                            set("category", update.category)
                            set("reward", update.reward)
                        },
                    ) {
                        filter {
                            eq("id", id)
                            eq("publisher_id", publisherId)
                            eq("status", "Publicada")
                        }
                        select()
                    }
                    .decodeSingleOrNull<Mission>()

            updatedMission ?: error("La misión no existe, no te pertenece o ya no se puede editar.")
        }

    override suspend fun getMyMissions(): List<Mission> =
        withContext(Dispatchers.IO) {
            val publisherId = authenticatedUserId("consultar mis misiones")
            supabase.from("missions")
                .select(columns = Columns.raw("*, profiles(full_name)")) {
                    filter { eq("publisher_id", publisherId) }
                    order("date_time", Order.DESCENDING)
                }.decodeList<Mission>()
        }

    override suspend fun getMissionById(id: String): Mission? =
        withContext(Dispatchers.IO) {
            supabase.from("public_missions")
                .select(columns = Columns.raw("*, profiles(full_name)")) { // <-- JOIN con profiles
                    filter {
                        eq("id", id)
                    }
                }.decodeSingleOrNull<Mission>()
        }

    override suspend fun generateVerificationCode(id: String): String =
        withContext(Dispatchers.IO) {
            val publisherId = authenticatedUserId("generar el código de verificación")
            val mission =
                findOwnMission(id, publisherId)
                    ?: error("La misión no existe o no te pertenece.")

            check(
                mission.status == "En progreso" ||
                    mission.status == "Completada" ||
                    mission.status == "Rendida aprobada",
            ) {
                "La misión debe estar en progreso, completada o rendida aprobada para generar el código."
            }

            generateRandomVerificationCode()
        }

    override suspend fun publishMission(id: String): Mission =
        withContext(Dispatchers.IO) {
            val publisherId = authenticatedUserId("publicar una misión")
            val updatedMission =
                supabase.from("missions")
                    .update({ set("status", "Publicada") }) {
                        filter {
                            eq("id", id)
                            eq("publisher_id", publisherId)
                            eq("status", "Borrador")
                        }
                        select()
                    }
                    .decodeSingleOrNull<Mission>()

            updatedMission ?: findOwnMission(id, publisherId)?.takeIf { it.status == "Publicada" }
                ?: error("La misión no existe, no te pertenece o no está en borrador.")
        }

    override suspend fun startMission(id: String): Mission =
        withContext(Dispatchers.IO) {
            val publisherId = authenticatedUserId("iniciar una misión")
            val updatedMission =
                supabase.from("missions")
                    .update({ set("status", "En progreso") }) {
                        filter {
                            eq("id", id)
                            eq("publisher_id", publisherId)
                            isIn("status", listOf("Publicada", "Asignada"))
                        }
                        select()
                    }
                    .decodeSingleOrNull<Mission>()

            updatedMission ?: findOwnMission(id, publisherId)?.takeIf { it.status == "En progreso" }
                ?: error("La misión no existe, no te pertenece o no se puede iniciar en su estado actual.")
        }

    private suspend fun findOwnMission(
        id: String,
        publisherId: String,
    ): Mission? =
        supabase.from("missions")
            .select {
                filter {
                    eq("id", id)
                    eq("publisher_id", publisherId)
                }
            }
            .decodeSingleOrNull<Mission>()

    @Suppress("UnusedParameter")
    private suspend fun authenticatedUserId(action: String): String {
        val currentUser = supabase.auth.currentUserOrNull()
        if (currentUser != null) return currentUser.id

        val recoveredUser =
            runCatching {
                supabase.auth.retrieveUserForCurrentSession(updateSession = true)
            }.getOrNull()

        return recoveredUser?.id ?: throw MissionSessionRequiredException()
    }

    private fun generateRandomVerificationCode(): String {
        val allowedChars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        val randomPart =
            (1..CODE_RANDOM_LENGTH)
                .map { allowedChars.random() }
                .joinToString("")
        return "CALI-$randomPart"
    }

    private companion object {
        private const val CODE_RANDOM_LENGTH = 4
        val LOGGER: Logger = Logger.getLogger(MissionRepositoryImpl::class.java.name)
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
    val reward: String?,
    @SerialName("max_participants") val maxParticipants: Int,
    val status: String,
)

@Serializable
private data class MissionInsertResponseDto(val id: String)
