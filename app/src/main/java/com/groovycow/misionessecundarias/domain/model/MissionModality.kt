package com.groovycow.misionessecundarias.domain.model

/** Modalidad de una misión y su representación persistida en Supabase. */
enum class MissionModality(
    /** Valor almacenado en la columna `missions.modality`. */
    val databaseValue: String,
) {
    /** Actividad presencial con punto de encuentro y coordenadas. */
    CAMPO("campo"),

    /** Actividad que se realiza de forma remota. */
    REMOTA("remota"),
}
