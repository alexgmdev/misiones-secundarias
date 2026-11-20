package com.groovycow.misionessecundarias.domain.repository

import com.groovycow.misionessecundarias.domain.model.PublisherRating
import com.groovycow.misionessecundarias.domain.model.PublisherReputation

/** Persistencia de las calificaciones al publicador (HU-124). */
interface RatingRepository {
    /**
     * Registra una calificación del solucionador autenticado.
     *
     * @param missionId Misión completada que origina la calificación.
     * @param stars Puntuación entre 1 y 5.
     * @return Calificación persistida.
     */
    suspend fun ratePublisher(
        missionId: String,
        stars: Int,
    ): PublisherRating

    /**
     * Reputación pública acumulada de un publicador.
     *
     * @param publisherId Identificador del publicador.
     * @return Promedio y cantidad de calificaciones; `null` si aún no tiene.
     */
    suspend fun getPublisherReputation(publisherId: String): PublisherReputation?
}
