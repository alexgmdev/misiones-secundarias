package com.groovycow.misionessecundarias.domain.model

import java.time.LocalDate

/**
 * Datos necesarios para registrar una cuenta nueva (HU-001).
 *
 * @property firstName Nombre, sin espacios sobrantes.
 * @property lastName Apellido, sin espacios sobrantes.
 * @property email Correo electrónico.
 * @property phone Celular ya normalizado (10 dígitos que inician en 3).
 * @property birthDate Fecha de nacimiento.
 * @property password Contraseña en texto plano; nunca se persiste ni se registra en logs.
 */
data class RegistrationData(
    val firstName: String,
    val lastName: String,
    val email: String,
    val phone: String,
    val birthDate: LocalDate,
    val password: String,
)
