package com.groovycow.misionessecundarias.domain.model

/** Excepción de negocio con el error tipado que impide publicar una misión. */
class MissionValidationException(
    /** Error de validación que se puede presentar al usuario. */
    val error: MissionError,
) : IllegalArgumentException(error.toString())
