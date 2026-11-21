package com.groovycow.misionessecundarias.data.repository

import com.groovycow.misionessecundarias.domain.model.AdminError
import com.groovycow.misionessecundarias.domain.model.AdminMetrics
import com.groovycow.misionessecundarias.domain.model.AdminValidationException
import com.groovycow.misionessecundarias.domain.repository.AdminRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.logging.Level
import java.util.logging.Logger

class AdminRepositoryImpl(
    private val supabase: SupabaseClient,
) : AdminRepository {
    override suspend fun getUserRole(): String =
        withContext(Dispatchers.IO) {
            val userId = currentUserId()
            val profileRole =
                supabase.from("profiles")
                    .select(columns = Columns.raw("role")) {
                        filter { eq("id", userId) }
                    }
                    .decodeSingleOrNull<UserRoleDto>()
                    ?.role

            profileRole ?: "user"
        }

    override suspend fun getOperationalMetrics(): AdminMetrics =
        withContext(Dispatchers.IO) {
            try {
                fetchMetricsInternal()
            } catch (exception: Exception) {
                LOGGER.log(Level.SEVERE, "Error al consultar métricas operativas", exception)
                throw exception
            }
        }

    private suspend fun fetchMetricsInternal(): AdminMetrics {
        val missions =
            supabase.from("missions")
                .select(columns = Columns.raw("id,status"))
                .decodeList<MissionStatusDto>()

        val totalUsers =
            supabase.from("profiles")
                .select(columns = Columns.raw("id"))
                .decodeList<IdDto>()
                .size

        val acceptedSolvers =
            supabase.from("applications")
                .select(columns = Columns.raw("solver_id")) {
                    filter { eq("status", "Aceptado") }
                }
                .decodeList<SolverIdDto>()
                .map { it.solverId }
                .distinct()
                .size

        val totalMissions = missions.size
        val activeMissions = missions.count { it.status in ACTIVE_STATUSES }
        val completedMissions = missions.count { it.status == "Completada" }

        return AdminMetrics(
            totalMissions = totalMissions,
            activeMissions = activeMissions,
            completedMissions = completedMissions,
            totalUsers = totalUsers,
            activeSolvers = acceptedSolvers,
        )
    }

    private fun currentUserId(): String =
        supabase.auth.currentUserOrNull()?.id
            ?: throw AdminValidationException(AdminError.SessionRequired)

    private companion object {
        val ACTIVE_STATUSES = setOf("Publicada", "Asignada", "En progreso")
        val LOGGER: Logger = Logger.getLogger(AdminRepositoryImpl::class.java.name)
    }
}

@Serializable
private data class UserRoleDto(
    val role: String? = "user",
)

@Serializable
private data class MissionStatusDto(
    val id: String,
    val status: String,
)

@Serializable
private data class IdDto(
    val id: String,
)

@Serializable
private data class SolverIdDto(
    @SerialName("solver_id") val solverId: String,
)
