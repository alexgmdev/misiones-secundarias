package com.groovycow.misionessecundarias.domain.model

/**
 * Género del perfil público.
 *
 * @property dbValue Valor guardado en la columna `profiles.gender`.
 */
enum class Gender(val dbValue: String) {
    /** Femenino. */
    FEMENINO("femenino"),

    /** Masculino. */
    MASCULINO("masculino"),

    /** Prefiere no decirlo. */
    PREFIERO_NO_DECIR("prefiero_no_decir"),
    ;

    companion object {
        /**
         * Convierte el valor de la base de datos en [Gender].
         *
         * @param value Valor de `profiles.gender`, o null.
         * @return El género correspondiente, o null si es nulo o desconocido.
         */
        fun fromDbValue(value: String?): Gender? = entries.firstOrNull { it.dbValue == value }
    }
}
