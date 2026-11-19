package com.groovycow.misionessecundarias.ui.auth.register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.groovycow.misionessecundarias.domain.model.AuthError
import com.groovycow.misionessecundarias.domain.model.AuthException
import com.groovycow.misionessecundarias.domain.model.RegistrationData
import com.groovycow.misionessecundarias.domain.usecase.RegisterUseCase
import com.groovycow.misionessecundarias.domain.validation.BirthDateValidator
import com.groovycow.misionessecundarias.domain.validation.EmailValidator
import com.groovycow.misionessecundarias.domain.validation.FieldError
import com.groovycow.misionessecundarias.domain.validation.NameValidator
import com.groovycow.misionessecundarias.domain.validation.PasswordValidator
import com.groovycow.misionessecundarias.domain.validation.PhoneValidator
import com.groovycow.misionessecundarias.domain.validation.ValidationResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Campos del formulario de registro, usados para asociar errores de validación. */
enum class RegisterField {
    /** Nombre. */
    FirstName,

    /** Apellido. */
    LastName,

    /** Correo electrónico. */
    Email,

    /** Celular. */
    Phone,

    /** Fecha de nacimiento. */
    BirthDate,

    /** Contraseña. */
    Password,

    /** Confirmación de contraseña. */
    Confirmation,
}

/**
 * Estado de la pantalla de registro.
 *
 * @property firstName Nombre digitado.
 * @property lastName Apellido digitado.
 * @property email Correo digitado.
 * @property phone Celular digitado (formato libre).
 * @property birthDate Fecha de nacimiento elegida, o null.
 * @property password Contraseña digitada.
 * @property confirmation Confirmación de la contraseña.
 * @property fieldErrors Errores de validación por campo.
 * @property isLoading `true` mientras hay una solicitud en curso (deshabilita el botón).
 * @property error Error del servidor o de red de la última solicitud, o null.
 * @property isRegistered `true` cuando la cuenta se creó y hay que navegar a `perfil-inicial`.
 */
data class RegisterUiState(
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val phone: String = "",
    val birthDate: LocalDate? = null,
    val password: String = "",
    val confirmation: String = "",
    val fieldErrors: Map<RegisterField, FieldError> = emptyMap(),
    val isLoading: Boolean = false,
    val error: AuthError? = null,
    val isRegistered: Boolean = false,
)

/**
 * ViewModel del registro (HU-001): valida el formulario y delega la creación de la cuenta.
 *
 * @param registerUseCase Caso de uso de registro.
 * @param nameValidator Validador de nombre y apellido.
 * @param emailValidator Validador de correo.
 * @param phoneValidator Validador y normalizador de celular.
 * @param birthDateValidator Validador de fecha de nacimiento.
 * @param passwordValidator Validador de contraseña y confirmación.
 */
class RegisterViewModel(
    private val registerUseCase: RegisterUseCase,
    private val nameValidator: NameValidator,
    private val emailValidator: EmailValidator,
    private val phoneValidator: PhoneValidator,
    private val birthDateValidator: BirthDateValidator,
    private val passwordValidator: PasswordValidator,
) : ViewModel() {
    private val _uiState = MutableStateFlow(RegisterUiState())

    /** Estado observable de la pantalla. */
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    /** Actualiza el nombre. @param value Texto digitado. */
    fun onFirstNameChange(value: String) = edit(RegisterField.FirstName) { it.copy(firstName = value) }

    /** Actualiza el apellido. @param value Texto digitado. */
    fun onLastNameChange(value: String) = edit(RegisterField.LastName) { it.copy(lastName = value) }

    /** Actualiza el correo. @param value Texto digitado. */
    fun onEmailChange(value: String) = edit(RegisterField.Email) { it.copy(email = value) }

    /** Actualiza el celular. @param value Texto digitado. */
    fun onPhoneChange(value: String) = edit(RegisterField.Phone) { it.copy(phone = value) }

    /** Actualiza la fecha de nacimiento. @param value Fecha elegida en el selector. */
    fun onBirthDateChange(value: LocalDate) = edit(RegisterField.BirthDate) { it.copy(birthDate = value) }

    /** Actualiza la contraseña. @param value Texto digitado. */
    fun onPasswordChange(value: String) = edit(RegisterField.Password) { it.copy(password = value) }

    /** Actualiza la confirmación. @param value Texto digitado. */
    fun onConfirmationChange(value: String) = edit(RegisterField.Confirmation) { it.copy(confirmation = value) }

    /**
     * Valida el formulario y, si es correcto y no hay otra solicitud en curso, envía el registro.
     * Si hay errores de validación no se llama al backend.
     */
    fun register() {
        val current = _uiState.value
        if (current.isLoading) return
        val errors = validate(current)
        if (errors.isNotEmpty()) {
            _uiState.update { it.copy(fieldErrors = errors, error = null) }
            return
        }
        _uiState.update { it.copy(fieldErrors = emptyMap(), error = null, isLoading = true) }
        viewModelScope.launch {
            val result = registerUseCase(current.toRegistrationData())
            _uiState.update {
                it.copy(
                    isLoading = false,
                    isRegistered = result.isSuccess,
                    error = result.exceptionOrNull()?.toAuthError(),
                )
            }
        }
    }

    private fun edit(
        field: RegisterField,
        transform: (RegisterUiState) -> RegisterUiState,
    ) = _uiState.update { transform(it).copy(fieldErrors = it.fieldErrors - field, error = null) }

    private fun validate(state: RegisterUiState): Map<RegisterField, FieldError> {
        val results =
            mapOf(
                RegisterField.FirstName to nameValidator.validate(state.firstName),
                RegisterField.LastName to nameValidator.validate(state.lastName),
                RegisterField.Email to emailValidator.validate(state.email),
                RegisterField.Phone to phoneValidator.validate(state.phone),
                RegisterField.BirthDate to birthDateValidator.validate(state.birthDate),
                RegisterField.Password to passwordValidator.validate(state.password),
                RegisterField.Confirmation to passwordValidator.validateConfirmation(state.password, state.confirmation),
            )
        return results.mapNotNull { (field, result) -> (result as? ValidationResult.Invalid)?.let { field to it.error } }.toMap()
    }

    private fun RegisterUiState.toRegistrationData() =
        RegistrationData(
            firstName = firstName.trim(),
            lastName = lastName.trim(),
            email = emailValidator.normalize(email),
            phone = requireNotNull(phoneValidator.normalize(phone)),
            birthDate = requireNotNull(birthDate),
            password = password,
        )

    private fun Throwable.toAuthError(): AuthError = (this as? AuthException)?.error ?: AuthError.ServerError
}
