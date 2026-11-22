package com.groovycow.misionessecundarias.domain.repository

import com.groovycow.misionessecundarias.domain.model.AdminMetrics

interface AdminRepository {
    /** Obtiene el rol del usuario autenticado (ej. 'user', 'moderator', 'admin'). */
    suspend fun getUserRole(): String

    /** Consulta las métricas operativas globales de la aplicación. */
    suspend fun getOperationalMetrics(): AdminMetrics
}
