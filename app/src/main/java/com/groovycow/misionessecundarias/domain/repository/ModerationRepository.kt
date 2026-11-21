package com.groovycow.misionessecundarias.domain.repository

import com.groovycow.misionessecundarias.domain.model.ModerationReport
import com.groovycow.misionessecundarias.domain.model.ReportPriority
import com.groovycow.misionessecundarias.domain.model.ReportSortOrder
import com.groovycow.misionessecundarias.domain.model.ReportStatus

interface ModerationRepository {
    /** Obtiene el rol del usuario autenticado ('user', 'moderator', 'admin'). */
    suspend fun getUserRole(): String

    /** Consulta la lista de reportes de moderación con filtros y criterio de ordenamiento. */
    suspend fun getReports(
        priorityFilter: ReportPriority? = null,
        statusFilter: ReportStatus? = null,
        targetTypeFilter: String? = null,
        sortOrder: ReportSortOrder = ReportSortOrder.PRIORITY_HIGH_FIRST,
    ): List<ModerationReport>
}
