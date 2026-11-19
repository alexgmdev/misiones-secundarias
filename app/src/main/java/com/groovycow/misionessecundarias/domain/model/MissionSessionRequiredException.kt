package com.groovycow.misionessecundarias.domain.model

/** Indica que la operación necesita una sesión autenticada válida. */
class MissionSessionRequiredException : IllegalStateException("Debes iniciar sesión para guardar una misión.")
