package com.groovycow.misionessecundarias.data.repository

import com.groovycow.misionessecundarias.domain.model.Application
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
import com.groovycow.misionessecundarias.domain.model.MissionApplicant

class ApplicationRepositoryImpl(
    private val supabase: SupabaseClient,
) : ApplicationRepository {
    override suspend fun cancelApplication(applicationId: String) =
        withContext(Dispatchers.IO) {
            val solverId =
                supabase.auth.currentUserOrNull()?.id
                    ?: error("Debes iniciar sesión para cancelar una postulación.")

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
            val solverId =
                supabase.auth.currentUserOrNull()?.id
                    ?: error("Debes iniciar sesión para consultar tus postulaciones.")

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
            val solverId =
                supabase.auth.currentUserOrNull()?.id
                    ?: error("Debes iniciar sesión para postularte a una misión.")

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

    // HU-041: Consultar postulantes de una misión propia
    override suspend fun getMissionApplicants(
        missionId: String
    ): List<MissionApplicant> = withContext(Dispatchers.IO) {

        val publisherId =
            supabase.auth.currentUserOrNull()?.id
                ?: error("Debes iniciar sesión.")

        // Comprobar que la misión pertenece al publicador
        val mission = supabase.from("missions")
            .select(columns = Columns.raw("publisher_id")) {
                filter {
                    eq("id", missionId)
                    eq("publisher_id", publisherId)
                }
            }
            .decodeSingleOrNull<MissionOwner>()
            ?: error("La misión no existe o no te pertenece.")

        // Consultar postulaciones y perfiles
        supabase.from("applications")
            .select(
                columns = Columns.raw(
                    "id,mission_id,solver_id,status,applied_at," +
                    "profiles!applications_solver_id_fkey(full_name,avatar_url)"
                )
            ) {
                filter {
                    eq("mission_id", missionId)
                    eq("status", "Postulado")
                }
            }
            .decodeList<MissionApplicant>()
            .sortedBy { it.appliedAt }
    }

    // HU-044: Aceptar postulante
    override suspend fun acceptApplication(
        applicationId: String
    ) = updateApplicationStatus(applicationId, "Aceptado")

    // HU-045: Rechazar postulante
    override suspend fun rejectApplication(
        applicationId: String
    ) = updateApplicationStatus(applicationId, "Rechazado")

    // Actualización común y validada del estado
    private suspend fun updateApplicationStatus(
        applicationId: String,
        newStatus: String
    ) = withContext(Dispatchers.IO) {

        val publisherId =
            supabase.auth.currentUserOrNull()?.id
                ?: error("Debes iniciar sesión.")

        val application = supabase.from("applications")
            .select(
                columns = Columns.raw(
                    "id,status,mission_id"
                )
            ) {
                filter {
                    eq("id", applicationId)
                }
            }
            .decodeSingleOrNull<PendingApplication>()
            ?: error("La postulación no existe.")

        check(application.status == "Postulado") {
            "Esta postulación ya fue procesada."
        }

        val mission = supabase.from("missions")
            .select(columns = Columns.raw("publisher_id")) {
                filter {
                    eq("id", application.missionId)
                    eq("publisher_id", publisherId)
                }
            }
            .decodeSingleOrNull<MissionOwner>()
            ?: error("No eres el publicador de esta misión.")

        // Comprobar cupos antes de aceptar
        if (newStatus == "Aceptado") {

            val accepted = supabase.from("applications")
                .select(columns = Columns.raw("id")) {
                    filter {
                        eq("mission_id", application.missionId)
                        eq("status", "Aceptado")
                    }
                }
                .decodeList<ApplicationId>()

            val missionCapacity = supabase.from("missions")
                .select(columns = Columns.raw("max_participants")) {
                    filter {
                        eq("id", application.missionId)
                    }
                }
                .decodeSingle<MissionCapacity>()

            check(accepted.size < missionCapacity.maxParticipants) {
                "La misión ya alcanzó su cupo máximo."
            }
        }

        // Solo se actualizan postulaciones pendientes
        supabase.from("applications").update(
            {
                set("status", newStatus)
            }
        ) {
            filter {
                eq("id", applicationId)
                eq("status", "Postulado")
            }
        }

        Unit
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
private data class MissionOwner(
    @SerialName("publisher_id")
    val publisherId: String
)

@Serializable
private data class ApplicationId(
    val id: String
)

@Serializable
private data class MissionCapacity(
    @SerialName("max_participants")
    val maxParticipants: Int
)

@Serializable
private data class PendingApplication(
    val id: String,
    @SerialName("mission_id")
    val missionId: String,
    val status: String
)


