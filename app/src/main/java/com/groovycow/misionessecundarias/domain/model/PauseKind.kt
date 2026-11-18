package com.groovycow.misionessecundarias.domain.model

/** Pausas que el publicador puede activar durante la ejecución de una misión (HU-054 y HU-055). */
enum class PauseKind(
    /** Valor guardado en la columna `kind` de `mission_pauses`. */
    val databaseValue: String,
    /** Texto del apartado de pausas del publicador. */
    val label: String,
    /** Nombre corto para el aviso que ve el postulante. */
    val shortName: String,
) {
    /** Pausa para socializar con el grupo. */
    SOCIALIZATION("socializacion", "Socialización de la misión", "Socialización"),

    /** Pausa para tomar agua. */
    HYDRATION("hidratacion", "Hidratación", "Hidratación"),

    /** Descanso breve del grupo. */
    SHORT_BREAK("pausa_breve", "Pausa breve", "Pausa breve"),
    ;

    companion object {
        /** Convierte el valor de base de datos; devuelve null si no se reconoce. */
        fun fromDatabaseValue(value: String): PauseKind? = entries.firstOrNull { it.databaseValue == value }
    }
}

/** Texto del aviso que ve el postulante cuando hay pausas activas, o null si no hay ninguna. */
fun pauseNotice(active: Set<PauseKind>): String? {
    if (active.isEmpty()) return null
    val names = PauseKind.entries.filter { it in active }.joinToString(", ") { it.shortName }
    return "Misión en pausa: $names"
}
