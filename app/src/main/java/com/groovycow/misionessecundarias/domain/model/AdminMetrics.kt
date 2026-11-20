package com.groovycow.misionessecundarias.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Métricas operativas globales de la aplicación para el Panel Administrativo.
 *
 * @property totalMissions Número total de misiones registradas.
 * @property activeMissions Misiones publicadas, asignadas o en progreso.
 * @property completedMissions Misiones en estado completada.
 * @property totalUsers Usuarios totales registrados en la plataforma.
 * @property activeSolvers Solucionadores con al menos una postulación aceptada.
 */
@Serializable
data class AdminMetrics(
    @SerialName("total_missions") val totalMissions: Int = 0,
    @SerialName("active_missions") val activeMissions: Int = 0,
    @SerialName("completed_missions") val completedMissions: Int = 0,
    @SerialName("total_users") val totalUsers: Int = 0,
    @SerialName("active_solvers") val activeSolvers: Int = 0,
)
