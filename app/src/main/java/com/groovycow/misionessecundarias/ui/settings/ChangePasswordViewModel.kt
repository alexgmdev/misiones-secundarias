package com.groovycow.misionessecundarias.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.groovycow.misionessecundarias.domain.model.AuthError
import com.groovycow.misionessecundarias.domain.model.AuthException
import com.groovycow.misionessecundarias.domain.usecase.ChangeCurrentPasswordUseCase
import com.groovycow.misionessecundarias.domain.usecase.ChangePasswordField
import com.groovycow.misionessecundarias.domain.usecase.ChangePasswordValidationException
import com.groovycow.misionessecundarias.domain.validation.FieldError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Estado de la pantalla de cambio de contraseña.
 *
 * @property current Contraseña actual digitada.
 * @property new Nueva contraseña digitada.
 * @property confirmation Confirmación digitada.
 * @property fieldErrors Errores de validación por campo.
 * @property isLoading `true` mientras hay una solicitud en curso (deshabilita el botón).
 * @property error Error de la última solicitud (actual incorrecta, igual a la anterior, red o servidor), o null.
 * @property isChanged `true` cuando la contraseña cambió y se debe mostrar el diálogo de confirmación.
 */
data class ChangePasswordUiState(
    val current: String = "",
    val new: String = "",
    val confirmation: String = "",
    val fieldErrors: Map<ChangePasswordField, FieldError> = emptyMap(),
    val isLoading: Boolean = false,
    val error: AuthError? = null,
    val isChanged: Boolean = false,
)

/**
 * ViewModel del cambio de contraseña desde Ajustes (HU-010): valida por campo sin llamar al backend y,
 * si el formulario es válido, reautentica y actualiza la clave sin cerrar la sesión.
 *
 * @param changeCurrentPassword Caso de uso que cambia la contraseña.
 */
class ChangePasswordViewModel(
    private val changeCurrentPassword: ChangeCurrentPasswordUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ChangePasswordUiState())

    /** Estado observable de la pantalla. */
    val uiState: StateFlow<ChangePasswordUiState> = _uiState.asStateFlow()

    /** Actualiza la contraseña actual. @param value Texto digitado. */
    fun onCurrentChange(value: String) = edit(ChangePasswordField.Current) { it.copy(current = value) }

    /** Actualiza la nueva contraseña. @param value Texto digitado. */
    fun onNewChange(value: String) = edit(ChangePasswordField.New) { it.copy(new = value) }

    /** Actualiza la confirmación. @param value Texto digitado. */
    fun onConfirmationChange(value: String) = edit(ChangePasswordField.Confirmation) { it.copy(confirmation = value) }

    /** Valida el formulario y, si no hay otra solicitud en curso, solicita el cambio de contraseña. */
    fun submit() {
        val current = _uiState.value
        if (current.isLoading) return
        val errors = changeCurrentPassword.validate(current.current, current.new, current.confirmation)
        if (errors.isNotEmpty()) {
            _uiState.update { it.copy(fieldErrors = errors, error = null) }
            return
        }
        _uiState.update { it.copy(fieldErrors = emptyMap(), error = null, isLoading = true) }
        viewModelScope.launch {
            val result = changeCurrentPassword(current.current, current.new, current.confirmation)
            _uiState.update { state ->
                val failure = result.exceptionOrNull()
                state.copy(
                    isLoading = false,
                    isChanged = result.isSuccess,
                    fieldErrors = (failure as? ChangePasswordValidationException)?.errors.orEmpty(),
                    error = failure?.takeUnless { it is ChangePasswordValidationException }?.toAuthError(),
                )
            }
        }
    }

    private fun edit(
        field: ChangePasswordField,
        transform: (ChangePasswordUiState) -> ChangePasswordUiState,
    ) = _uiState.update { transform(it).copy(fieldErrors = it.fieldErrors - field, error = null) }

    private fun Throwable.toAuthError(): AuthError = (this as? AuthException)?.error ?: AuthError.ServerError
}
