package com.groovycow.misionessecundarias.domain.usecase

import com.groovycow.misionessecundarias.domain.model.ModerationError
import com.groovycow.misionessecundarias.domain.model.ModerationReport
import com.groovycow.misionessecundarias.domain.model.ModerationValidationException
import com.groovycow.misionessecundarias.domain.model.ReportPriority
import com.groovycow.misionessecundarias.domain.model.ReportSortOrder
import com.groovycow.misionessecundarias.domain.model.ReportStatus
import com.groovycow.misionessecundarias.domain.repository.ModerationRepository
import kotlin.coroutines.cancellation.CancellationException

/**
 * Consulta y ordena la cola de reportes de moderación (HU - Moderación y Seguridad).
 *
 * Exige que el usuario posea el rol 'moderator'.
 */
class GetModerationReportsUseCase(
    private val moderationRepository: ModerationRepository,
) {
    /**
     * @param priorityFilter Filtro opcional por severidad/prioridad.
     * @param statusFilter Filtro opcional por estado.
     * @param targetTypeFilter Filtro opcional por tipo de contenido.
     * @param sortOrder Criterio de ordenamiento (por defecto 'Prioridad Alta' despliega en primer lugar las denuncias urgentes).
     * @return Lista ordenada de reportes de moderación.
     * @throws CancellationException Si se cancela la coroutine.
     */
    suspend operator fun invoke(
        priorityFilter: ReportPriority? = null,
        statusFilter: ReportStatus? = null,
        targetTypeFilter: String? = null,
        sortOrder: ReportSortOrder = ReportSortOrder.PRIORITY_HIGH_FIRST,
    ): Result<List<ModerationReport>> {
        return try {
            val role = moderationRepository.getUserRole().lowercase()
            if (role != MODERATOR_ROLE) {
                throw ModerationValidationException(ModerationError.AccessDenied)
            }

            val reports =
                moderationRepository.getReports(
                    priorityFilter = priorityFilter,
                    statusFilter = statusFilter,
                    targetTypeFilter = targetTypeFilter,
                    sortOrder = sortOrder,
                )

            val sorted =
                when (sortOrder) {
                    ReportSortOrder.PRIORITY_HIGH_FIRST ->
                        reports.sortedWith(
                            compareBy<ModerationReport> {
                                when (it.reportPriority) {
                                    ReportPriority.ALTA -> 0
                                    ReportPriority.MEDIA -> 1
                                    ReportPriority.BAJA -> 2
                                }
                            }.thenByDescending { it.createdAt.orEmpty() },
                        )
                    ReportSortOrder.DATE_NEWEST_FIRST -> reports.sortedByDescending { it.createdAt.orEmpty() }
                    ReportSortOrder.DATE_OLDEST_FIRST -> reports.sortedBy { it.createdAt.orEmpty() }
                }

            Result.success(sorted)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }

    private companion object {
        const val MODERATOR_ROLE = "moderator"
    }
}
