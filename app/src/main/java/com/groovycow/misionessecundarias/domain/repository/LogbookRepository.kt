package com.groovycow.misionessecundarias.domain.repository

import com.groovycow.misionessecundarias.domain.model.MissionApplicant
import com.groovycow.misionessecundarias.domain.model.MissionLogbook

interface LogbookRepository {
    /** Guarda la bitácora oficial del publicador asociando la lista de participantes. */
    suspend fun savePublisherLogbook(
        missionId: String,
        entry: String,
        participants: List<MissionApplicant>,
    ): MissionLogbook

    /** Guarda las palabras / relato de vivencia de un participante solucionador. */
    suspend fun saveParticipantLogbook(
        missionId: String,
        entry: String,
    ): MissionLogbook

    /** Obtiene el historial completo de bitácoras (publicador y participantes) de la misión. */
    suspend fun getLogbooksForMission(missionId: String): List<MissionLogbook>
}
