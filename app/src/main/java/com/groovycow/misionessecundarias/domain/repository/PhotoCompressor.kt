package com.groovycow.misionessecundarias.domain.repository

/** Prepara fotos de perfil: valida el formato y las convierte a JPEG de máximo 2 MB. */
interface PhotoCompressor {
    /**
     * Comprime la imagen indicada.
     *
     * @param uri Uri (como texto) de una imagen JPG o PNG.
     * @return Bytes JPEG de máximo 2 MB, con el lado mayor de hasta 1080 px.
     * @throws com.groovycow.misionessecundarias.domain.model.ProfileException con `UnsupportedPhotoFormat`, `PhotoTooLarge` o `ServerError`.
     */
    suspend fun compressToJpeg(uri: String): ByteArray
}
