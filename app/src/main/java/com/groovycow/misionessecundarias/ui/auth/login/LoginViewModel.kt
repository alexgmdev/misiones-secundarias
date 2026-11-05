package com.groovycow.misionessecundarias.ui.auth.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.groovycow.misionessecundarias.domain.model.AuthError
import com.groovycow.misionessecundarias.domain.model.AuthException
import com.groovycow.misionessecundarias.domain.usecase.SignInUseCase
import com.groovycow.misionessecundarias.domain.validation.FieldError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Campos del formulario de inicio de sesión, usados para asociar errores de validación. */
enum class LoginField {
    /** Correo o celular. */
    Identifier,

    /** Contraseña. */
    Password,
}

/**
 * Estado de la pantalla de inicio de sesión.
 *
 * @property identifier Correo o celular digitado.
 * @property password Contraseña digitada.
 * @property fieldErrors Errores de validación por campo.
 * @property isLoading `true` mientras hay una solicitud en curso (deshabilita el botón).
 * @property error Error de la última solicitud (credenciales, red o servidor), o null.
 * @property isSignedIn `true` cuando la sesión quedó iniciada y hay que navegar a `menu-principal`.
 */
data class LoginUiState(
    val identifier: String = "",
    val password: String = "",
    val fieldErrors: Map<LoginField, FieldError> = emptyMap(),
    val isLoading: Boolean = false,
    val error: AuthError? = null,
    val isSignedIn: Boolean = false,
)

/**
 * ViewModel del inicio de sesión (HU-002): valida que los campos no estén vacíos y delega la autenticación.
 *
 * @param signInUseCase Caso de uso de inicio de sesión por correo o celular.
 */
class LoginViewModel(
    private val signInUseCase: SignInUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(LoginUiState())

    /** Estado observable de la pantalla. */
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    /** Actualiza el correo o celular. @param value Texto digitado. */
    fun onIdentifierChange(value: String) = edit(LoginField.Identifier) { it.copy(identifier = value) }

    /** Actualiza la contraseña. @param value Texto digitado. */
    fun onPasswordChange(value: String) = edit(LoginField.Password) { it.copy(password = value) }

    /**
     * Valida los campos y, si no hay otra solicitud en curso, inicia sesión.
     * Si algún campo está vacío no se llama al backend.
     */
    fun signIn() {
        val current = _uiState.value
        if (current.isLoading) return
        val errors = validate(current)
        if (errors.isNotEmpty()) {
            _uiState.update { it.copy(fieldErrors = errors, error = null) }
            return
        }
        _uiState.update { it.copy(fieldErrors = emptyMap(), error = null, isLoading = true) }
        viewModelScope.launch {
            val result = signInUseCase(current.identifier, current.password)
            _uiState.update {
                it.copy(
                    isLoading = false,
                    isSignedIn = result.isSuccess,
                    error = result.exceptionOrNull()?.toAuthError(),
                )
            }
        }
    }

    private fun edit(
        field: LoginField,
        transform: (LoginUiState) -> LoginUiState,
    ) = _uiState.update { transform(it).copy(fieldErrors = it.fieldErrors - field, error = null) }

    private fun validate(state: LoginUiState): Map<LoginField, FieldError> =
        buildMap {
            if (state.identifier.isBlank()) put(LoginField.Identifier, FieldError.Required)
            if (state.password.isEmpty()) put(LoginField.Password, FieldError.Required)
        }

    private fun Throwable.toAuthError(): AuthError = (this as? AuthException)?.error ?: AuthError.ServerError
}
