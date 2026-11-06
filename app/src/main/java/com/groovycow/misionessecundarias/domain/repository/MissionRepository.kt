package com.groovycow.misionessecundarias.domain.repository

import com.groovycow.misionessecundarias.domain.model.CreateMissionDraft
import com.groovycow.misionessecundarias.domain.model.Mission

interface MissionRepository {
    /** Guarda un borrador de misión asociado al usuario autenticado. */
    suspend fun createMission(draft: CreateMissionDraft): Mission

    /** Cambia a publicada una misión propia y devuelve el estado resultante. */
    suspend fun publishMission(id: String): Mission

    suspend fun getPublishedMissions(): List<Mission>

    suspend fun getMissionById(id: String): Mission?
}
