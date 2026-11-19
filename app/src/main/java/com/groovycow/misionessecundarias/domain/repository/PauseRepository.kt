package com.groovycow.misionessecundarias.domain.repository

import com.groovycow.misionessecundarias.domain.model.PauseKind

/** Acceso a las pausas activas de misiones en ejecución (HU-054 y HU-055). */
interface PauseRepository {
    /** Pausas activas por misión; las misiones sin pausas activas no aparecen en el mapa. */
    suspend fun getActivePauses(missionIds: List<String>): Map<String, Set<PauseKind>>

    /** Activa o desactiva una pausa de una misión propia. */
    suspend fun setPause(
        missionId: String,
        kind: PauseKind,
        isActive: Boolean,
    )
}
