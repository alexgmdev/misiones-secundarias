package com.groovycow.misionessecundarias.data.repository

import com.groovycow.misionessecundarias.domain.model.Application
import com.groovycow.misionessecundarias.domain.model.ApplicationDecision
import com.groovycow.misionessecundarias.domain.model.MissionApplicant
import com.groovycow.misionessecundarias.domain.model.MyApplication
import com.groovycow.misionessecundarias.domain.repository.ApplicationRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.OffsetDateTime

class ApplicationRepositoryImpl(
    private val supabase: SupabaseClient,
) : ApplicationRepository {
    override suspend fun isCurrentUser(userId: String): Boolean =
        withContext(Dispatchers.IO) {
            authenticatedUserId("consultar la misión") == userId
        }

    override suspend fun getMissionApplicants(missionId: String): List<MissionApplicant> =
        withContext(Dispatchers.IO) {
            val publisherId = authenticatedUserId("consultar los postulantes")
            requireMissionOwner(missionId, publisherId)

            supabase.from("applications")
                .select(
                    columns =
                        Columns.raw(
                            "id,mission_id,solver_id,status,applied_at," +
                                "applicant:profiles!applications_solver_id_fkey(full_name)",
                        ),
                ) {
                    filter { eq("mission_id", missionId) }
                }
                .decodeList<MissionApplicant>()
                .sortedByDescending { it.appliedAt }
        }

    override suspend fun updateApplicationStatus(
        applicationId: String,
        decision: ApplicationDecision,
    ) = withContext(Dispatchers.IO) {
        val publisherId = authenticatedUserId("gestionar una postulación")
        val target =
            supabase.from("applications")
                .select(
                    columns =
                        Columns.raw(
                            "id,mission_id,status," +
                                "missions(publisher_id,max_participants,status)",
                        ),
                ) {
                    filter { eq("id", applicationId) }
                }
                .decodeSingleOrNull<ApplicationDecisionTarget>()
                ?: error("La postulación ya no existe.")

        check(target.mission.publisherId == publisherId) {
            "Solo el publicador de la misión puede gestionar sus postulantes."
        }
        check(target.status == "Postulado") {
            "Esta postulación ya fue gestionada."
        }
        val missionAllowsDecision =
            target.mission.status == "Publicada" ||
                (decision == ApplicationDecision.REJECT && target.mission.status == "Asignada")
        check(missionAllowsDecision) {
            "La misión ya no admite esta decisión sobre postulantes."
        }

        val acceptedCount =
            if (decision == ApplicationDecision.ACCEPT) {
                supabase.from("applications")
                    .select(columns = Columns.raw("id")) {
                        filter {
                            eq("mission_id", target.missionId)
                            eq("status", "Aceptado")
                        }
                    }
                    .decodeList<ApplicationId>()
                    .size
            } else {
                0
            }

        if (decision == ApplicationDecision.ACCEPT) {
            check(acceptedCount < target.mission.maxParticipants) {
                "La misión ya alcanzó el máximo de participantes."
            }
        }

        val updated =
            supabase.from("applications")
                .update({ set("status", decision.databaseValue) }) {
                    filter {
                        eq("id", applicationId)
                        eq("status", "Postulado")
                    }
                    select(columns = Columns.raw("id"))
                }
                .decodeSingleOrNull<ApplicationId>()

        check(updated != null) { "La postulación cambió antes de guardar la decisión." }

        if (
            decision == ApplicationDecision.ACCEPT &&
            acceptedCount + 1 >= target.mission.maxParticipants
        ) {
            supabase.from("missions").update({ set("status", "Asignada") }) {
                filter {
                    eq("id", target.missionId)
                    eq("publisher_id", publisherId)
                    eq("status", "Publicada")
                }
            }
        }
    }

    override suspend fun cancelApplication(applicationId: String) =
        withContext(Dispatchers.IO) {
            val solverId = authenticatedUserId("cancelar una postulación")

            val application =
                supabase.from("applications")
                    .select(
                        columns =
                            Columns.raw(
                                "id,status,missions(date_time,status)",
                            ),
                    ) {
                        filter {
                            eq("id", applicationId)
                            eq("solver_id", solverId)
                        }
                    }
                    .decodeSingleOrNull<CancellableApplication>()
                    ?: error("La postulación ya no existe o no te pertenece.")

            check(application.status == "Postulado" || application.status == "Aceptado") {
                "Esta postulación ya no puede cancelarse."
            }
            check(application.mission.status == "Publicada" || application.mission.status == "Asignada") {
                "No puedes retirarte porque la misión ya inició o finalizó."
            }

            val missionStart =
                runCatching {
                    OffsetDateTime.parse(application.mission.dateTime)
                }.getOrElse {
                    error("No fue posible validar la fecha de inicio de la misión.")
                }

            check(missionStart.isAfter(OffsetDateTime.now())) {
                "No puedes retirarte después de la hora de inicio de la misión."
            }

            supabase.from("applications").delete {
                filter {
                    eq("id", applicationId)
                    eq("solver_id", solverId)
                }
            }

            Unit
        }

    override suspend fun getMyApplications(): List<MyApplication> =
        withContext(Dispatchers.IO) {
            val solverId = authenticatedUserId("consultar tus postulaciones")

            supabase.from("applications")
                .select(
                    columns =
                        Columns.raw(
                            "id,mission_id,status,applied_at,missions(id,title,date_time,status)",
                        ),
                ) {
                    filter { eq("solver_id", solverId) }
                }
                .decodeList<MyApplication>()
                .sortedByDescending { it.appliedAt }
        }

    override suspend fun applyToMission(missionId: String): Application =
        withContext(Dispatchers.IO) {
            val solverId = authenticatedUserId("postularte a una misión")

            val mission =
                supabase.from("missions")
                    .select(columns = Columns.raw("publisher_id,status,date_time")) {
                        filter { eq("id", missionId) }
                    }
                    .decodeSingleOrNull<MissionApplicationInfo>()
                    ?: error("La misión ya no está disponible.")

            check(mission.publisherId != solverId) {
                "No puedes postularte a una misión publicada por ti."
            }
            check(mission.status == "Publicada") {
                "Solo puedes postularte a misiones publicadas."
            }

            val existingApplication =
                supabase.from("applications")
                    .select {
                        filter {
                            eq("mission_id", missionId)
                            eq("solver_id", solverId)
                        }
                    }
                    .decodeSingleOrNull<Application>()

            check(existingApplication == null) {
                "Ya tienes una postulación registrada para esta misión."
            }

            val acceptedSchedules =
                supabase.from("applications")
                    .select(columns = Columns.raw("missions(date_time)")) {
                        filter {
                            eq("solver_id", solverId)
                            eq("status", "Aceptado")
                        }
                    }
                    .decodeList<AcceptedApplicationSchedule>()

            check(acceptedSchedules.none { sameInstant(it.mission.dateTime, mission.dateTime) }) {
                "Ya tienes una misión aceptada en la misma fecha y hora."
            }

            supabase.from("applications")
                .insert(
                    ApplicationInsert(
                        missionId = missionId,
                        solverId = solverId,
                    ),
                ) {
                    select()
                }
                .decodeSingle<Application>()
        }

    private suspend fun authenticatedUserId(action: String): String {
        val currentUser = supabase.auth.currentUserOrNull()
        if (currentUser != null) return currentUser.id

        val recoveredUser =
            runCatching {
                supabase.auth.retrieveUserForCurrentSession(updateSession = true)
            }.getOrNull()

        return recoveredUser?.id ?: error("Debes iniciar sesión para $action.")
    }

    private suspend fun requireMissionOwner(
        missionId: String,
        publisherId: String,
    ) {
        val mission =
            supabase.from("missions")
                .select(columns = Columns.raw("id")) {
                    filter {
                        eq("id", missionId)
                        eq("publisher_id", publisherId)
                    }
                }
                .decodeSingleOrNull<ApplicationId>()
        check(mission != null) { "Solo el publicador puede consultar los postulantes de esta misión." }
    }

    private fun sameInstant(
        first: String,
        second: String,
    ): Boolean =
        runCatching {
            OffsetDateTime.parse(first).toInstant() == OffsetDateTime.parse(second).toInstant()
        }.getOrElse {
            first == second
        }
}

@Serializable
private data class ApplicationInsert(
    @SerialName("mission_id") val missionId: String,
    @SerialName("solver_id") val solverId: String,
)

@Serializable
private data class MissionApplicationInfo(
    @SerialName("publisher_id") val publisherId: String,
    val status: String,
    @SerialName("date_time") val dateTime: String,
)

@Serializable
private data class AcceptedApplicationSchedule(
    @SerialName("missions") val mission: MissionSchedule,
)

@Serializable
private data class MissionSchedule(
    @SerialName("date_time") val dateTime: String,
)

@Serializable
private data class CancellableApplication(
    val id: String,
    val status: String,
    @SerialName("missions") val mission: CancellableMission,
)

@Serializable
private data class CancellableMission(
    @SerialName("date_time") val dateTime: String,
    val status: String,
)

@Serializable
private data class ApplicationId(val id: String)

@Serializable
private data class ApplicationDecisionTarget(
    val id: String,
    @SerialName("mission_id") val missionId: String,
    val status: String,
    @SerialName("missions") val mission: ApplicationDecisionMission,
)

@Serializable
private data class ApplicationDecisionMission(
    @SerialName("publisher_id") val publisherId: String,
    @SerialName("max_participants") val maxParticipants: Int,
    val status: String,
)
