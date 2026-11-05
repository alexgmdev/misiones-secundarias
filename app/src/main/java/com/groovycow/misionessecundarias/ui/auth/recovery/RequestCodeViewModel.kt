package com.groovycow.misionessecundarias.ui.auth.recovery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.groovycow.misionessecundarias.domain.model.AuthError
import com.groovycow.misionessecundarias.domain.model.AuthException
import com.groovycow.misionessecundarias.domain.usecase.RequestPasswordResetUseCase
import com.groovycow.misionessecundarias.domain.validation.EmailValidator
import com.groovycow.misionessecundarias.domain.validation.FieldError
import com.groovycow.misionessecundarias.domain.validation.ValidationResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Estado de la pantalla de solicitud del código de recuperación.
 *
 * @property email Correo digitado.
 * @property emailError Error de validación del correo, o null.
 * @property isLoading `true` mientras hay una solicitud en curso (deshabilita el botón).
 * @property error Error de la última solicitud (solo falta de conexión o error inesperado), o null.
 * @property isCodeSent `true` cuando la solicitud terminó y hay que navegar a la pantalla del código.
 */
data class RequestCodeUiState(
    val email: String = "",
    val emailError: FieldError? = null,
    val isLoading: Boolean = false,
    val error: AuthError? = null,
    val isCodeSent: Boolean = false,
)

/**
 * ViewModel de la solicitud del código de recuperación (HU-003): valida el correo y delega el envío.
 *
 * @param requestReset Caso de uso que solicita el código sin revelar si la cuenta existe.
 * @param emailValidator Validador del formato del correo.
 */
class RequestCodeViewModel(
    private val requestReset: RequestPasswordResetUseCase,
    private val emailValidator: EmailValidator,
) : ViewModel() {
    private val _uiState = MutableStateFlow(RequestCodeUiState())

    /** Estado observable de la pantalla. */
    val uiState: StateFlow<RequestCodeUiState> = _uiState.asStateFlow()

    /** Actualiza el correo y limpia errores. @param value Texto digitado. */
    fun onEmailChange(value: String) = _uiState.update { it.copy(email = value, emailError = null, error = null) }

    /**
     * Valida el correo y, si no hay otra solicitud en curso, solicita el código.
     * Con correo vacío o mal formado no se llama al backend.
     */
    fun sendCode() {
        val current = _uiState.value
        if (current.isLoading) return
        val validation = emailValidator.validate(current.email)
        if (validation is ValidationResult.Invalid) {
            _uiState.update { it.copy(emailError = validation.error, error = null) }
            return
        }
        _uiState.update { it.copy(emailError = null, error = null, isLoading = true) }
        viewModelScope.launch {
            val result = requestReset(emailValidator.normalize(current.email))
            _uiState.update {
                it.copy(
                    isLoading = false,
                    isCodeSent = result.isSuccess,
                    error = result.exceptionOrNull()?.let { failure -> (failure as? AuthException)?.error ?: AuthError.ServerError },
                )
            }
        }
    }
}
