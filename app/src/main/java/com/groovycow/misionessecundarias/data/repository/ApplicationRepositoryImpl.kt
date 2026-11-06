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

// gestión de supabase y supabase auth
class ApplicationRepositoryImpl(
    private val supabase: SupabaseClient,
) : ApplicationRepository {
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
