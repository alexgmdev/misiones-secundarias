package com.groovycow.misionessecundarias.data.repository

import com.groovycow.misionessecundarias.domain.model.ModerationError
import com.groovycow.misionessecundarias.domain.model.ModerationReport
import com.groovycow.misionessecundarias.domain.model.ModerationValidationException
import com.groovycow.misionessecundarias.domain.model.ReportPriority
import com.groovycow.misionessecundarias.domain.model.ReportSortOrder
import com.groovycow.misionessecundarias.domain.model.ReportStatus
import com.groovycow.misionessecundarias.domain.repository.ModerationRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import java.util.logging.Level
import java.util.logging.Logger

class ModerationRepositoryImpl(
    private val supabase: SupabaseClient,
) : ModerationRepository {
    override suspend fun getUserRole(): String =
        withContext(Dispatchers.IO) {
            val userId = currentUserId()
            val profileRole =
                supabase.from("profiles")
                    .select(columns = Columns.raw("role")) {
                        filter { eq("id", userId) }
                    }
                    .decodeSingleOrNull<ModerationUserRoleDto>()
                    ?.role

            profileRole ?: "user"
        }

    override suspend fun getReports(
        priorityFilter: ReportPriority?,
        statusFilter: ReportStatus?,
        targetTypeFilter: String?,
        sortOrder: ReportSortOrder,
    ): List<ModerationReport> =
        withContext(Dispatchers.IO) {
            try {
                fetchReportsInternal(priorityFilter, statusFilter, targetTypeFilter, sortOrder)
            } catch (exception: Exception) {
                LOGGER.log(Level.SEVERE, "Error al consultar los reportes de moderación", exception)
                throw exception
            }
        }

    private suspend fun fetchReportsInternal(
        priorityFilter: ReportPriority?,
        statusFilter: ReportStatus?,
        targetTypeFilter: String?,
        sortOrder: ReportSortOrder,
    ): List<ModerationReport> {
        return supabase.from("moderation_reports")
            .select(columns = Columns.raw("*, reporter_profile:profiles!moderation_reports_reporter_id_fkey(full_name)")) {
                filter {
                    priorityFilter?.let { eq("priority", it.dbValue) }
                    statusFilter?.let { eq("status", it.dbValue) }
                    targetTypeFilter?.let { eq("target_type", it) }
                }
                when (sortOrder) {
                    ReportSortOrder.DATE_NEWEST_FIRST -> order("created_at", Order.DESCENDING)
                    ReportSortOrder.DATE_OLDEST_FIRST -> order("created_at", Order.ASCENDING)
                    else -> order("created_at", Order.DESCENDING)
                }
            }
            .decodeList<ModerationReport>()
    }

    private fun currentUserId(): String =
        supabase.auth.currentUserOrNull()?.id
            ?: throw ModerationValidationException(ModerationError.SessionRequired)

    private companion object {
        val LOGGER: Logger = Logger.getLogger(ModerationRepositoryImpl::class.java.name)
    }
}

@Serializable
private data class ModerationUserRoleDto(val role: String? = "user")
