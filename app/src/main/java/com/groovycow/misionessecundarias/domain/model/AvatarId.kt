package com.groovycow.misionessecundarias.domain.model

/**
 * Catálogo de avatares predefinidos; la capa de UI los dibuja con Canvas (color e ícono viven allí, no en el dominio).
 *
 * @property key Identificador guardado en `profiles.avatar_id` (formato `^[a-z_]{1,30}$`).
 */
enum class AvatarId(val key: String) {
    /** Explorador de bosques. */
    BOSQUE("bosque"),

    /** Guardián de ríos. */
    RIO("rio"),

    /** Aventurero del sol. */
    SOL("sol"),

    /** Viajero de la tierra. */
    TIERRA("tierra"),

    /** Cazador de estrellas. */
    ESTRELLA("estrella"),

    /** Amante de las flores. */
    FLOR("flor"),

    /** Corazón solidario. */
    CORAZON("corazon"),

    /** Navegante del mapa. */
    BRUJULA("brujula"),
    ;

    companion object {
        /**
         * Busca un avatar por su clave.
         *
         * @param key Valor de `profiles.avatar_id`, o null.
         * @return El avatar, o null si es nulo o no existe en el catálogo.
         */
        fun fromKey(key: String?): AvatarId? = entries.firstOrNull { it.key == key }
    }
}
