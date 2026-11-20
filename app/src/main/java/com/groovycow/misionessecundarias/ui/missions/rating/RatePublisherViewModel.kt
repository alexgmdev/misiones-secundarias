package com.groovycow.misionessecundarias.ui.missions.rating

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.groovycow.misionessecundarias.domain.model.MissionSessionRequiredException
import com.groovycow.misionessecundarias.domain.model.RatingError
import com.groovycow.misionessecundarias.domain.model.RatingValidationException
import com.groovycow.misionessecundarias.domain.model.toMessage
import com.groovycow.misionessecundarias.domain.usecase.RatePublisherUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.IOException

/** Estado del formulario de calificación al publicador (HU-124). */
data class RatePublisherUiState(
    /** Misión completada que se está calificando; `null` cuando el diálogo está cerrado. */
    val missionId: String? = null,
    /** Puntuación seleccionada entre 1 y 5. */
    val stars: Int = 5,
    /** Indica que la calificación se está enviando. */
    val isSubmitting: Boolean = false,
    /** La calificación se guardó correctamente. */
    val isCompleted: Boolean = false,
    /** Mensaje de éxito o error para el diálogo. */
    val message: String? = null,
    /** Indica que [message] es un error. */
    val isError: Boolean = false,
)

/** Gestiona el envío de la calificación de un publicador. */
class RatePublisherViewModel(
    private val ratePublisherUseCase: RatePublisherUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(RatePublisherUiState())

    /** Estado actual del diálogo de calificación. */
    val uiState: StateFlow<RatePublisherUiState> = _uiState.asStateFlow()

    /** Abre el diálogo para la misión completada. */
    fun open(missionId: String) {
        _uiState.value = RatePublisherUiState(missionId = missionId)
    }

    /** Actualiza la puntuación elegida. */
    fun selectStars(stars: Int) {
        val current = _uiState.value
        if (current.isSubmitting || current.isCompleted) return
        _uiState.value = current.copy(stars = stars.coerceIn(MIN_STARS, MAX_STARS), message = null, isError = false)
    }

    /** Envía la puntuación y notifica el resultado. */
    fun submit() {
        val current = _uiState.value
        val missionId = current.missionId ?: return
        if (current.isSubmitting || current.isCompleted) return

        _uiState.value = current.copy(isSubmitting = true, message = null, isError = false)
        viewModelScope.launch {
            ratePublisherUseCase(missionId, current.stars).fold(
                onSuccess = {
                    _uiState.value =
                        _uiState.value.copy(
                            isSubmitting = false,
                            isCompleted = true,
                            message = "¡Gracias! Tu calificación quedó registrada.",
                        )
                },
                onFailure = { exception ->
                    _uiState.value =
                        _uiState.value.copy(
                            isSubmitting = false,
                            message = exception.toRatingMessage(),
                            isError = true,
                        )
                },
            )
        }
    }

    /** Cierra el diálogo y descarta el estado. */
    fun dismiss() {
        _uiState.value = RatePublisherUiState()
    }

    private fun Throwable.toRatingMessage(): String =
        when (this) {
            is RatingValidationException -> error.toMessage()
            is MissionSessionRequiredException -> RatingError.SessionRequired.toMessage()
            else -> {
                if (hasIOExceptionCause()) {
                    RatingError.NoConnection.toMessage()
                } else {
                    RatingError.ServerError.toMessage()
                }
            }
        }

    private fun Throwable.hasIOExceptionCause(): Boolean {
        var current: Throwable? = this
        while (current != null) {
            if (current is IOException) return true
            current = current.cause
        }
        return false
    }

    private companion object {
        const val MIN_STARS = 1
        const val MAX_STARS = 5
    }
}
