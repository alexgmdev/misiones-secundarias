package com.groovycow.misionessecundarias.domain.usecase

import com.groovycow.misionessecundarias.domain.model.HistoryRole
import com.groovycow.misionessecundarias.domain.model.MissionHistoryEntry
import com.groovycow.misionessecundarias.domain.model.MissionLogbookSections
import com.groovycow.misionessecundarias.domain.model.ParticipantWords
import com.groovycow.misionessecundarias.domain.repository.ApplicationRepository
import com.groovycow.misionessecundarias.domain.repository.LogbookRepository
import com.groovycow.misionessecundarias.domain.repository.MissionRepository
import kotlin.coroutines.cancellation.CancellationException

private const val DRAFT_STATUS = "Borrador"
private const val ACCEPTED_STATUS = "Aceptado"
private const val PUBLISHER_ROLE = "publicador"
private const val DEFAULT_AUTHOR = "Participante"

/**
 * Construye el historial de misiones del usuario (HU-011): misiones publicadas (sin borradores) y
 * misiones donde fue aceptado como solucionador, sin repetir y de la más reciente a la más antigua.
 *
 * Reutiliza [MissionRepository.getMyMissions] y [ApplicationRepository.getMyApplications]; para las misiones
 * aceptadas completa descripción y requerimientos con [MissionRepository.getMissionById] (si falla, la entrada
 * se conserva sin esos campos).
 *
 * @param missions Repositorio de misiones.
 * @param applications Repositorio de postulaciones.
 */
class GetMissionHistoryUseCase(
    private val missions: MissionRepository,
    private val applications: ApplicationRepository,
) {
    /** Devuelve el historial ordenado, o el fallo de los repositorios. */
    @Suppress("TooGenericExceptionCaught")
    suspend operator fun invoke(): Result<List<MissionHistoryEntry>> =
        try {
            val own =
                missions.getMyMissions()
                    .filter { it.status != DRAFT_STATUS }
                    .map {
                        MissionHistoryEntry(
                            missionId = it.id,
                            title = it.title,
                            dateTime = it.dateTime,
                            status = it.status,
                            role = HistoryRole.PUBLISHER,
                            description = it.description,
                            requirements = it.requirements,
                        )
                    }
            val ownIds = own.map { it.missionId }.toSet()
            val solver =
                applications.getMyApplications()
                    .filter { it.status == ACCEPTED_STATUS && it.missionId !in ownIds }
                    .distinctBy { it.missionId }
                    .map { app ->
                        val details = runCatchingNonCancel { missions.getMissionById(app.missionId) }
                        MissionHistoryEntry(
                            missionId = app.missionId,
                            title = app.mission.title,
                            dateTime = app.mission.dateTime,
                            status = app.mission.status,
                            role = HistoryRole.SOLVER,
                            description = details?.description,
                            requirements = details?.requirements,
                        )
                    }
            Result.success((own + solver).sortedByDescending { it.dateTime })
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }

    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    private suspend fun <T> runCatchingNonCancel(block: suspend () -> T): T? =
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (ignored: Exception) {
            null
        }
}

/**
 * Obtiene las secciones de bitácora de una misión (HU-011) reutilizando [LogbookRepository.getLogbooksForMission]:
 * la bitácora oficial es la entrada con rol `publicador` y las palabras de participantes las de rol `participante`.
 *
 * @param logbooks Repositorio de bitácoras.
 */
class GetMissionLogbookSectionsUseCase(private val logbooks: LogbookRepository) {
    /**
     * @param missionId Misión consultada.
     * @return Secciones de bitácora, o el fallo de la consulta.
     */
    suspend operator fun invoke(missionId: String): Result<MissionLogbookSections> =
        authCatching {
            val all = logbooks.getLogbooksForMission(missionId)
            MissionLogbookSections(
                official = all.firstOrNull { it.role == PUBLISHER_ROLE }?.entry,
                participants =
                    all.filter { it.role != PUBLISHER_ROLE }.map {
                        ParticipantWords(
                            authorName = it.authorProfile?.fullName ?: DEFAULT_AUTHOR,
                            text = it.entry,
                        )
                    },
            )
        }
}
