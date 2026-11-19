package com.groovycow.misionessecundarias.domain.model

/**
 * Datos descriptivos editables de una misión publicada (HU-032).
 *
 * @property title Nuevo título de la misión.
 * @property description Nueva descripción de la actividad.
 * @property requirements Nuevos requisitos para participar.
 * @property category Categoría opcional; `null` cuando se deja vacía.
 * @property reward Recompensa informativa opcional; `null` cuando se deja vacía.
 */
data class MissionDetailsUpdate(
    val title: String,
    val description: String,
    val requirements: String,
    val category: String? = null,
    val reward: String? = null,
)
