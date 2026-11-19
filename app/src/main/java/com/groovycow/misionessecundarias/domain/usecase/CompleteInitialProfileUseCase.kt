package com.groovycow.misionessecundarias.domain.usecase

import com.groovycow.misionessecundarias.domain.model.ProfileDraft
import com.groovycow.misionessecundarias.domain.model.ProfileError
import com.groovycow.misionessecundarias.domain.model.ProfileException
import com.groovycow.misionessecundarias.domain.repository.ProfileRepository
import com.groovycow.misionessecundarias.domain.validation.ProfileDraftValidator

/**
 * Completa el perfil inicial (HU-005): valida, comprueba el nickname, sube la foto y guarda el perfil.
 *
 * @property repository Repositorio de perfiles.
 * @property validator Validador del formulario.
 */
class CompleteInitialProfileUseCase(
    private val repository: ProfileRepository,
    private val validator: ProfileDraftValidator,
) {
    /**
     * Guarda el perfil inicial.
     *
     * @param draft Datos digitados.
     * @param photoJpeg Bytes JPEG ya comprimidos, o null si no se eligió foto.
     * @return [Result.success], o [Result.failure] con [ProfileValidationException] (nada se envía al backend) o
     * [ProfileException]. Orden: validar, `nickname_disponible`, subir foto y actualizar el perfil.
     */
    suspend operator fun invoke(
        draft: ProfileDraft,
        photoJpeg: ByteArray?,
    ): Result<Unit> {
        val errors = validator.validate(draft, hasPhoto = photoJpeg != null)
        return if (errors.isNotEmpty() || photoJpeg == null) {
            Result.failure(ProfileValidationException(errors))
        } else {
            authCatching {
                val nickname = draft.nickname.trim()
                if (!repository.isNicknameAvailable(nickname)) throw ProfileException(ProfileError.NicknameTaken)
                val photoUrl = repository.uploadPhoto(photoJpeg)
                repository.updateProfile(draft.toUpdate(photoUrl))
            }
        }
    }
}
