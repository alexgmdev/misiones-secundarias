package com.groovycow.misionessecundarias.domain.usecase

import com.groovycow.misionessecundarias.domain.model.Profile
import com.groovycow.misionessecundarias.domain.model.PublicProfile
import com.groovycow.misionessecundarias.domain.repository.PhotoCompressor
import com.groovycow.misionessecundarias.domain.repository.ProfileRepository
import com.groovycow.misionessecundarias.domain.validation.FieldError
import com.groovycow.misionessecundarias.domain.validation.ProfileField

/**
 * Fallo de validación local del formulario de perfil (no se llamó al backend).
 *
 * @property errors Error de validación por campo.
 */
class ProfileValidationException(val errors: Map<ProfileField, FieldError>) : Exception(errors.toString())

/** Consulta si el usuario autenticado completó su perfil (gate de HU-005). */
class IsProfileCompleteUseCase(private val repository: ProfileRepository) {
    /** @return [Result] con verdadero si el perfil está completo. */
    suspend operator fun invoke(): Result<Boolean> = authCatching { repository.isProfileComplete() }
}

/** Obtiene el perfil del usuario autenticado. */
class GetMyProfileUseCase(private val repository: ProfileRepository) {
    /** @return [Result] con el [Profile] propio. */
    suspend operator fun invoke(): Result<Profile> = authCatching { repository.getMyProfile() }
}

/** Obtiene el perfil público de otro usuario. */
class GetPublicProfileUseCase(private val repository: ProfileRepository) {
    /**
     * @param userId Identificador del usuario a consultar.
     * @return [Result] con el [PublicProfile].
     */
    suspend operator fun invoke(userId: String): Result<PublicProfile> = authCatching { repository.getPublicProfile(userId) }
}

/** Sube a Storage una foto ya comprimida a JPEG. */
class UploadProfilePhotoUseCase(private val repository: ProfileRepository) {
    /**
     * @param jpeg Bytes JPEG de máximo 2 MB.
     * @return [Result] con la URL pública de la foto.
     */
    suspend operator fun invoke(jpeg: ByteArray): Result<String> = authCatching { repository.uploadPhoto(jpeg) }
}

/** Valida y comprime la foto elegida en el selector de imágenes. */
class CompressProfilePhotoUseCase(private val compressor: PhotoCompressor) {
    /**
     * @param uri Uri (texto) de la imagen elegida.
     * @return [Result] con los bytes JPEG, o con una [com.groovycow.misionessecundarias.domain.model.ProfileException] tipada.
     */
    suspend operator fun invoke(uri: String): Result<ByteArray> = authCatching { compressor.compressToJpeg(uri) }
}
