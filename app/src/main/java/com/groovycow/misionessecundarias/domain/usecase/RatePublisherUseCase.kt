package com.groovycow.misionessecundarias.domain.usecase

import com.groovycow.misionessecundarias.domain.model.PublisherRating
import com.groovycow.misionessecundarias.domain.model.RatingError
import com.groovycow.misionessecundarias.domain.model.RatingValidationException
import com.groovycow.misionessecundarias.domain.repository.ApplicationRepository
import com.groovycow.misionessecundarias.domain.repository.MissionRepository
import com.groovycow.misionessecundarias.domain.repository.RatingRepository
import kotlin.coroutines.cancellation.CancellationException

/** Registra la calificación de un publicador por parte de un participante aceptado (HU-124). */
class RatePublisherUseCase(
    private val ratingRepository: RatingRepository,
    private val missionRepository: MissionRepository,
    private val applicationRepository: ApplicationRepository,
) {
    /**
     * Valida las precondiciones y guarda la puntuación de 1 a 5 estrellas.
     *
     * @param missionId Misión completada.
     * @param stars Puntuación elegida por el solucionador.
     * @return Calificación persistida o el error de dominio/de repositorio.
     * @throws CancellationException Si se cancela la coroutine.
     */
    suspend operator fun invoke(
        missionId: String,
        stars: Int,
    ): Result<PublisherRating> =
        try {
            if (stars !in MIN_STARS..MAX_STARS) {
                throw RatingValidationException(RatingError.OutOfRange)
            }
            val mission =
                missionRepository.getMissionById(missionId)
                    ?: throw RatingValidationException(RatingError.MissionNotFound)
            if (mission.status != COMPLETED_STATUS) {
                throw RatingValidationException(RatingError.MissionNotCompleted)
            }
            val application = applicationRepository.getMyApplicationStatus(missionId)
            if (application?.status != ACCEPTED_STATUS) {
                throw RatingValidationException(RatingError.NotAcceptedParticipant)
            }
            Result.success(ratingRepository.ratePublisher(missionId, stars))
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (exception: Exception) {
            Result.failure(exception)
        }

    private companion object {
        const val MIN_STARS = 1
        const val MAX_STARS = 5
        const val COMPLETED_STATUS = "Completada"
        const val ACCEPTED_STATUS = "Aceptado"
    }
}
