package com.groovycow.misionessecundarias.domain.repository

import com.groovycow.misionessecundarias.domain.model.CreateMissionDraft
import com.groovycow.misionessecundarias.domain.model.Mission
import com.groovycow.misionessecundarias.domain.model.MissionDetailsUpdate

interface MissionRepository {
    /** Guarda un borrador de misión asociado al usuario autenticado. */
    suspend fun createMission(draft: CreateMissionDraft): Mission

    /** Cambia a publicada una misión propia y devuelve el estado resultante. */
    suspend fun publishMission(id: String): Mission

    /** Cambia a "En progreso" una misión propia publicada o asignada y devuelve el estado resultante. */
    suspend fun startMission(id: String): Mission

    /** Cambia a "Completada" una misión propia en progreso y devuelve el estado resultante. */
    suspend fun completeMission(id: String): Mission

    /** Actualiza los datos descriptivos de una misión propia publicada (HU-032). */
    suspend fun updateMissionDetails(
        id: String,
        update: MissionDetailsUpdate,
    ): Mission

    /** Misiones creadas por el usuario autenticado, en cualquier estado (incluye borradores). */
    suspend fun getMyMissions(): List<Mission>

    suspend fun getPublishedMissions(): List<Mission>

    suspend fun getMissionById(id: String): Mission?

    /** Genera el código único de verificación de cierre para una misión completada o rendida aprobada. */
    suspend fun generateVerificationCode(id: String): String
}
