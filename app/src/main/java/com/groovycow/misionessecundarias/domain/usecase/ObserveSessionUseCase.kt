package com.groovycow.misionessecundarias.domain.usecase

import com.groovycow.misionessecundarias.domain.model.SessionState
import com.groovycow.misionessecundarias.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow

/**
 * Observa el estado de la sesión guardada para decidir la pantalla inicial.
 *
 * @property repository Repositorio de autenticación.
 */
class ObserveSessionUseCase(private val repository: AuthRepository) {
    /**
     * @return Flujo con los estados de sesión ([SessionState]).
     */
    operator fun invoke(): Flow<SessionState> = repository.observeSession()
}
