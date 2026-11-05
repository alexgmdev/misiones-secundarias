package com.groovycow.misionessecundarias.ui.auth

import com.groovycow.misionessecundarias.domain.model.AuthError
import com.groovycow.misionessecundarias.domain.validation.FieldError
import org.junit.Assert.assertEquals
import org.junit.Test

class AuthMessagesTest {
    @Test
    fun fieldErrorsUseExactSpecMessages() {
        assertEquals("Este campo es obligatorio", FieldError.Required.toMessage())
        assertEquals("Correo electrónico inválido", FieldError.InvalidEmail.toMessage())
        assertEquals("Celular inválido: debe iniciar en 3 y tener 10 dígitos", FieldError.InvalidPhone.toMessage())
        assertEquals("La contraseña debe tener al menos 8 caracteres", FieldError.PasswordTooShort.toMessage())
        assertEquals("La contraseña debe incluir mayúscula, minúscula y número", FieldError.PasswordMissingClasses.toMessage())
        assertEquals("Las contraseñas no coinciden", FieldError.PasswordMismatch.toMessage())
        assertEquals("Debes ser mayor de 18 años", FieldError.UnderAge.toMessage())
        assertEquals("La fecha de nacimiento no puede ser futura", FieldError.FutureBirthDate.toMessage())
    }

    @Test
    fun everyFieldErrorHasANonBlankMessage() {
        FieldError.entries.forEach { assert(it.toMessage().isNotBlank()) }
    }

    @Test
    fun authErrorsUseExactSpecMessages() {
        assertEquals("El correo ya está en uso", AuthError.EmailInUse.toMessage())
        assertEquals("El celular ya está en uso", AuthError.PhoneInUse.toMessage())
        assertEquals("Sin conexión. Intenta de nuevo", AuthError.NoConnection.toMessage())
        assertEquals("Ocurrió un error. Intenta de nuevo", AuthError.ServerError.toMessage())
        assertEquals("Credenciales incorrectas", AuthError.InvalidCredentials.toMessage())
        assertEquals("Código inválido o expirado", AuthError.InvalidOrExpiredCode.toMessage())
        assertEquals("La nueva contraseña debe ser diferente a la anterior", AuthError.SamePassword.toMessage())
    }
}
