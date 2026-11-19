package com.groovycow.misionessecundarias.ui.auth.recovery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.groovycow.misionessecundarias.domain.model.AuthError
import com.groovycow.misionessecundarias.domain.model.AuthException
import com.groovycow.misionessecundarias.domain.usecase.CancelRecoveryUseCase
import com.groovycow.misionessecundarias.domain.usecase.ChangePasswordUseCase
import com.groovycow.misionessecundarias.domain.usecase.RequestPasswordResetUseCase
import com.groovycow.misionessecundarias.domain.usecase.VerifyRecoveryCodeUseCase
import com.groovycow.misionessecundarias.domain.validation.FieldError
import com.groovycow.misionessecundarias.domain.validation.PasswordValidator
import com.groovycow.misionessecundarias.domain.validation.RecoveryCodeValidator
import com.groovycow.misionessecundarias.domain.validation.ValidationResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Campos del formulario de restablecimiento, usados para asociar errores de validación. */
enum class ResetField {
    /** Código recibido por correo. */
    Code,

    /** Nueva contraseña. */
    Password,

    /** Confirmación de la nueva contraseña. */
    Confirmation,
}

/**
 * Estado de la pantalla de restablecimiento de contraseña.
 *
 * @property code Código digitado.
 * @property password Nueva contraseña digitada.
 * @property confirmation Confirmación digitada.
 * @property fieldErrors Errores de validación por campo.
 * @property isLoading `true` mientras hay una solicitud en curso (deshabilita los botones).
 * @property error Error de la última solicitud (código inválido o expirado, clave, red o servidor), o null.
 * @property codeVerified `true` si el código ya se verificó; los reintentos solo repiten el cambio de clave.
 * @property resendAvailableIn Segundos restantes para poder reenviar el código; 0 si ya se puede.
 * @property isPasswordChanged `true` cuando la clave cambió y hay que volver a iniciar sesión.
 */
data class ResetPasswordUiState(
    val code: String = "",
    val password: String = "",
    val confirmation: String = "",
    val fieldErrors: Map<ResetField, FieldError> = emptyMap(),
    val isLoading: Boolean = false,
    val error: AuthError? = null,
    val codeVerified: Boolean = false,
    val resendAvailableIn: Int = 0,
    val isPasswordChanged: Boolean = false,
)

/**
 * ViewModel del restablecimiento de contraseña (HU-004): valida el formulario, verifica el código,
 * cambia la clave y gestiona el reenvío del código con una espera de 60 s.
 *
 * La cuenta regresiva usa `delay` en `viewModelScope`, por lo que en pruebas se controla con el
 * dispatcher de prueba (`advanceTimeBy`).
 *
 * @param email Correo al que se envió el código.
 * @param requestReset Caso de uso para reenviar el código.
 * @param verifyCode Caso de uso que verifica el código.
 * @param changePassword Caso de uso que define la nueva clave y cierra la sesión temporal.
 * @param cancelRecovery Caso de uso que cierra la sesión temporal si se abandona la recuperación.
 * @param codeValidator Validador del formato del código.
 * @param passwordValidator Validador de la contraseña y su confirmación.
 */
class ResetPasswordViewModel(
    private val email: String,
    private val requestReset: RequestPasswordResetUseCase,
    private val verifyCode: VerifyRecoveryCodeUseCase,
    private val changePassword: ChangePasswordUseCase,
    private val cancelRecovery: CancelRecoveryUseCase,
    private val codeValidator: RecoveryCodeValidator,
    private val passwordValidator: PasswordValidator,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ResetPasswordUiState())

    /** Estado observable de la pantalla. */
    val uiState: StateFlow<ResetPasswordUiState> = _uiState.asStateFlow()

    private var countdownJob: Job? = null

    init {
        startCountdown()
    }

    /** Actualiza el código. @param value Texto digitado. */
    fun onCodeChange(value: String) = edit(ResetField.Code) { it.copy(code = value) }

    /** Actualiza la nueva contraseña. @param value Texto digitado. */
    fun onPasswordChange(value: String) = edit(ResetField.Password) { it.copy(password = value) }

    /** Actualiza la confirmación. @param value Texto digitado. */
    fun onConfirmationChange(value: String) = edit(ResetField.Confirmation) { it.copy(confirmation = value) }

    /**
     * Valida el formulario y, si no hay otra solicitud en curso, verifica el código (solo si aún no se
     * verificó) y cambia la contraseña. Si la verificación ya tuvo éxito, un reintento solo repite el cambio.
     */
    fun save() {
        val current = _uiState.value
        if (current.isLoading) return
        val errors = validate(current)
        if (errors.isNotEmpty()) {
            _uiState.update { it.copy(fieldErrors = errors, error = null) }
            return
        }
        _uiState.update { it.copy(fieldErrors = emptyMap(), error = null, isLoading = true) }
        viewModelScope.launch {
            val verified = current.codeVerified || verifyCode(email, current.code).also { applyVerification(it) }.isSuccess
            if (verified) {
                val result = changePassword(current.password)
                _uiState.update {
                    it.copy(isLoading = false, isPasswordChanged = result.isSuccess, error = result.errorOrNull())
                }
            }
        }
    }

    /** Reenvía el código si terminó la espera de 60 s y no hay otra solicitud en curso; reinicia la cuenta regresiva. */
    fun resend() {
        val current = _uiState.value
        if (current.isLoading || current.resendAvailableIn > 0) return
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            val result = requestReset(email)
            _uiState.update { it.copy(isLoading = false, error = result.errorOrNull()) }
            if (result.isSuccess) startCountdown()
        }
    }

    /**
     * Debe invocarse al salir de la pantalla sin haber cambiado la clave. Si el código ya se verificó
     * (hay una sesión temporal) y la clave no cambió, cierra esa sesión; en otro caso no hace nada.
     * Los fallos del cierre se ignoran.
     */
    fun onAbandon() {
        val current = _uiState.value
        if (!current.codeVerified || current.isPasswordChanged) return
        viewModelScope.launch { cancelRecovery() }
    }

    private fun applyVerification(result: Result<Unit>) {
        _uiState.update {
            if (result.isSuccess) {
                it.copy(codeVerified = true)
            } else {
                it.copy(isLoading = false, error = result.errorOrNull())
            }
        }
    }

    private fun startCountdown() {
        countdownJob?.cancel()
        _uiState.update { it.copy(resendAvailableIn = RESEND_COOLDOWN_SECONDS) }
        countdownJob =
            viewModelScope.launch {
                repeat(RESEND_COOLDOWN_SECONDS) {
                    delay(MILLIS_PER_SECOND)
                    _uiState.update { it.copy(resendAvailableIn = it.resendAvailableIn - 1) }
                }
            }
    }

    private fun edit(
        field: ResetField,
        transform: (ResetPasswordUiState) -> ResetPasswordUiState,
    ) = _uiState.update { transform(it).copy(fieldErrors = it.fieldErrors - field, error = null) }

    private fun validate(state: ResetPasswordUiState): Map<ResetField, FieldError> =
        buildMap {
            if (!state.codeVerified) record(ResetField.Code, codeValidator.validate(state.code))
            record(ResetField.Password, passwordValidator.validate(state.password))
            if (!containsKey(ResetField.Password)) {
                record(ResetField.Confirmation, passwordValidator.validateConfirmation(state.password, state.confirmation))
            }
        }

    private fun MutableMap<ResetField, FieldError>.record(
        field: ResetField,
        result: ValidationResult,
    ) {
        if (result is ValidationResult.Invalid) put(field, result.error)
    }

    private fun Result<Unit>.errorOrNull(): AuthError? = exceptionOrNull()?.let { (it as? AuthException)?.error ?: AuthError.ServerError }

    private companion object {
        const val RESEND_COOLDOWN_SECONDS = 60
        const val MILLIS_PER_SECOND = 1_000L
    }
}
