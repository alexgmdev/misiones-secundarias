package com.groovycow.misionessecundarias.data.image

import com.groovycow.misionessecundarias.domain.model.ProfileError
import com.groovycow.misionessecundarias.domain.model.ProfileException

/** Reglas puras de compresión de la foto de perfil (D6); separadas de Android para probarlas en JVM. */
object PhotoCompressionPolicy {
    /** Tamaño máximo de la foto subida: 2 MB. */
    const val MAX_BYTES = 2 * 1024 * 1024

    /** Lado mayor máximo en píxeles. */
    const val MAX_SIDE_PX = 1080

    /** Calidades JPEG a probar, de mayor a menor. */
    val QUALITY_STEPS: List<Int> = listOf(85, 75, 65, 55, 45)

    private val SUPPORTED_MIME = setOf("image/jpeg", "image/png")

    /**
     * Indica si el tipo MIME es aceptado como entrada.
     *
     * @param mime Tipo MIME reportado por el `ContentResolver`, o null.
     */
    fun isSupportedMime(mime: String?): Boolean = mime in SUPPORTED_MIME

    /**
     * Calcula el tamaño final: conserva la proporción y limita el lado mayor a [MAX_SIDE_PX].
     *
     * @param width Ancho original.
     * @param height Alto original.
     * @return Par (ancho, alto) de destino.
     */
    fun targetSize(
        width: Int,
        height: Int,
    ): Pair<Int, Int> {
        val longest = maxOf(width, height)
        if (longest <= MAX_SIDE_PX) return width to height
        val scale = MAX_SIDE_PX.toDouble() / longest
        return Math.round(width * scale).toInt().coerceAtLeast(1) to Math.round(height * scale).toInt().coerceAtLeast(1)
    }

    /**
     * Calcula el `inSampleSize` (potencia de 2) más grande que no baja de la resolución de destino.
     *
     * @param width Ancho original.
     * @param height Alto original.
     */
    fun sampleSize(
        width: Int,
        height: Int,
    ): Int {
        val (targetWidth, targetHeight) = targetSize(width, height)
        var sample = 1
        while (width / (sample * 2) >= targetWidth && height / (sample * 2) >= targetHeight) sample *= 2
        return sample
    }

    /**
     * Prueba cada calidad de [QUALITY_STEPS] hasta obtener un resultado que quepa en [maxBytes].
     *
     * @param maxBytes Tamaño máximo permitido.
     * @param encode Codifica la imagen con la calidad indicada.
     * @return Los bytes del primer intento que cabe.
     * @throws ProfileException con `PhotoTooLarge` si ninguna calidad alcanza.
     */
    fun encodeWithinLimit(
        maxBytes: Int = MAX_BYTES,
        encode: (quality: Int) -> ByteArray,
    ): ByteArray {
        for (quality in QUALITY_STEPS) {
            val bytes = encode(quality)
            if (bytes.size <= maxBytes) return bytes
        }
        throw ProfileException(ProfileError.PhotoTooLarge)
    }
}
