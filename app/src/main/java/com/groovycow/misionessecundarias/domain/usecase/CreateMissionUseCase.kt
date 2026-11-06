package com.groovycow.misionessecundarias.domain.usecase

import com.groovycow.misionessecundarias.domain.model.CreateMissionDraft
import com.groovycow.misionessecundarias.domain.model.Mission
import com.groovycow.misionessecundarias.domain.repository.MissionRepository
import kotlin.coroutines.cancellation.CancellationException

/** Crea un borrador delegando la persistencia al repositorio. */
class CreateMissionUseCase(
    private val repository: MissionRepository,
) {
    /**
     * Persiste [draft] y devuelve la misión creada.
     *
     * @param draft Datos validados del formulario.
     * @return Éxito con la misión o fallo con la excepción original del repositorio.
     * @throws CancellationException Si se cancela la coroutine.
     */
    suspend operator fun invoke(draft: CreateMissionDraft): Result<Mission> =
        try {
            Result.success(repository.createMission(draft))
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (exception: Exception) {
            Result.failure(exception)
        }
}
