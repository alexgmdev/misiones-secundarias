package com.groovycow.misionessecundarias.domain.validation

/** Reglas de robustez de la contraseña. */
enum class PasswordRule {
    /** Al menos 8 caracteres. */
    MIN_LENGTH,

    /** Al menos una mayúscula. */
    UPPERCASE,

    /** Al menos una minúscula. */
    LOWERCASE,

    /** Al menos un dígito. */
    DIGIT,
}

/** Valida la contraseña (mínimo 8 caracteres con mayúscula, minúscula y dígito) y su confirmación. */
class PasswordValidator {
    /**
     * Calcula las reglas que la contraseña no cumple.
     *
     * @param password Contraseña digitada.
     * @return Conjunto de [PasswordRule] incumplidas; vacío si es robusta.
     */
    fun unmetRules(password: String): Set<PasswordRule> =
        buildSet {
            if (password.length < MIN_LENGTH) add(PasswordRule.MIN_LENGTH)
            if (password.none { it.isUpperCase() }) add(PasswordRule.UPPERCASE)
            if (password.none { it.isLowerCase() }) add(PasswordRule.LOWERCASE)
            if (password.none { it.isDigit() }) add(PasswordRule.DIGIT)
        }

    /**
     * Valida la contraseña.
     *
     * @param password Contraseña digitada.
     * @return [ValidationResult.Valid] o [ValidationResult.Invalid] con [FieldError.Required],
     * [FieldError.PasswordTooShort] o [FieldError.PasswordMissingClasses].
     */
    fun validate(password: String): ValidationResult {
        val unmet = unmetRules(password)
        return when {
            password.isEmpty() -> ValidationResult.Invalid(FieldError.Required)
            PasswordRule.MIN_LENGTH in unmet -> ValidationResult.Invalid(FieldError.PasswordTooShort)
            unmet.isNotEmpty() -> ValidationResult.Invalid(FieldError.PasswordMissingClasses)
            else -> ValidationResult.Valid
        }
    }

    /**
     * Valida que la confirmación coincida exactamente (sensible a mayúsculas) con la contraseña.
     *
     * @param password Contraseña digitada.
     * @param confirmation Confirmación digitada.
     * @return [ValidationResult.Valid] o [ValidationResult.Invalid] con [FieldError.Required] o [FieldError.PasswordMismatch].
     */
    fun validateConfirmation(
        password: String,
        confirmation: String,
    ): ValidationResult =
        when {
            confirmation.isEmpty() -> ValidationResult.Invalid(FieldError.Required)
            confirmation != password -> ValidationResult.Invalid(FieldError.PasswordMismatch)
            else -> ValidationResult.Valid
        }

    private companion object {
        const val MIN_LENGTH = 8
    }
}
