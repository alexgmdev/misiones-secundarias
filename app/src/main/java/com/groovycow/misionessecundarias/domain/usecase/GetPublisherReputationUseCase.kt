package com.groovycow.misionessecundarias.domain.usecase

import com.groovycow.misionessecundarias.domain.model.PublisherReputation
import com.groovycow.misionessecundarias.domain.repository.RatingRepository
import kotlin.coroutines.cancellation.CancellationException

/** Consulta la reputación pública acumulada de un publicador (HU-124). */
class GetPublisherReputationUseCase(
    private val ratingRepository: RatingRepository,
) {
    /**
     * @param publisherId Publicador consultado.
     * @return Reputación o `null` cuando todavía no tiene calificaciones.
     * @throws CancellationException Si se cancela la coroutine.
     */
    suspend operator fun invoke(publisherId: String): Result<PublisherReputation?> =
        try {
            Result.success(ratingRepository.getPublisherReputation(publisherId))
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (exception: Exception) {
            Result.failure(exception)
        }
}
