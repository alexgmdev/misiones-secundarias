package com.groovycow.misionessecundarias.domain.model

/** Errores tipados de la calificación de publicadores (HU-124). */
sealed interface RatingError {
    /** La puntuación debe estar entre 1 y 5 estrellas. */
    data object OutOfRange : RatingError

    /** La misión aún no está completada. */
    data object MissionNotCompleted : RatingError

    /** Solo un participante aceptado puede calificar al publicador. */
    data object NotAcceptedParticipant : RatingError

    /** El usuario ya calificó esta misión. */
    data object AlreadyRated : RatingError

    /** Se necesita una sesión activa para calificar. */
    data object SessionRequired : RatingError

    /** No hay conexión con el servicio. */
    data object NoConnection : RatingError

    /** El servicio rechazó la operación. */
    data object ServerError : RatingError

    /** La misión ya no está disponible. */
    data object MissionNotFound : RatingError
}

/** Excepción de dominio con el error de calificación tipado. */
class RatingValidationException(
    val error: RatingError,
) : IllegalStateException(error.toString())

/**
 * Traduce este error al mensaje en español para la interfaz.
 *
 * @return Texto breve para mostrar en la pantalla.
 */
fun RatingError.toMessage(): String =
    when (this) {
        RatingError.OutOfRange -> "Selecciona entre 1 y 5 estrellas"
        RatingError.MissionNotCompleted -> "Solo puedes calificar cuando la misión esté completada"
        RatingError.NotAcceptedParticipant -> "Solo los participantes aceptados pueden calificar"
        RatingError.AlreadyRated -> "Ya calificaste esta misión"
        RatingError.SessionRequired -> "Debes iniciar sesión para calificar"
        RatingError.NoConnection -> "Sin conexión. Intenta de nuevo"
        RatingError.ServerError -> "Ocurrió un error. Intenta de nuevo"
        RatingError.MissionNotFound -> "No encontramos la misión"
    }
