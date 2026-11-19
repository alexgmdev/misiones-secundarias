package com.groovycow.misionessecundarias.domain.repository

import com.groovycow.misionessecundarias.domain.model.CreateMissionDraft
import com.groovycow.misionessecundarias.domain.model.Mission

interface MissionRepository {
    /** Guarda un borrador de misión asociado al usuario autenticado. */
    suspend fun createMission(draft: CreateMissionDraft): Mission

    /** Cambia a publicada una misión propia y devuelve el estado resultante. */
    suspend fun publishMission(id: String): Mission

    /** Cambia a "En progreso" una misión propia publicada o asignada y devuelve el estado resultante. */
    suspend fun startMission(id: String): Mission

    /** Cambia a "Completada" una misión propia en progreso y devuelve el estado resultante. */
    suspend fun completeMission(id: String): Mission

    /** Misiones creadas por el usuario autenticado, en cualquier estado (incluye borradores). */
    suspend fun getMyMissions(): List<Mission>

    suspend fun getPublishedMissions(): List<Mission>

    suspend fun getMissionById(id: String): Mission?
}
