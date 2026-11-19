package com.groovycow.misionessecundarias.domain.usecase

import com.groovycow.misionessecundarias.domain.model.ProfileDraft
import com.groovycow.misionessecundarias.domain.model.ProfileError
import com.groovycow.misionessecundarias.domain.model.ProfileException
import com.groovycow.misionessecundarias.domain.model.ProfileUpdate
import com.groovycow.misionessecundarias.domain.repository.ProfileRepository
import com.groovycow.misionessecundarias.domain.validation.ProfileDraftValidator

/**
 * Edita el perfil propio (HU-008/HU-009): valida campos, comprueba el nickname y actualiza.
 *
 * @property repository Repositorio de perfiles.
 * @property validator Validador del formulario.
 */
class UpdateProfileUseCase(
    private val repository: ProfileRepository,
    private val validator: ProfileDraftValidator,
) {
    /**
     * Guarda los cambios.
     *
     * @param draft Datos digitados.
     * @param newPhotoUrl URL de una foto recién subida, o null para conservar la actual.
     * @return [Result.success], o [Result.failure] con [ProfileValidationException] (sin llamar al backend) o
     * [ProfileException] (`NicknameTaken`, `NoConnection`, `ServerError`...). El nickname propio no cuenta como en uso.
     */
    suspend operator fun invoke(
        draft: ProfileDraft,
        newPhotoUrl: String? = null,
    ): Result<Unit> {
        val errors = validator.validate(draft, hasPhoto = true)
        return if (errors.isNotEmpty()) {
            Result.failure(ProfileValidationException(errors))
        } else {
            authCatching {
                val update = draft.toUpdate(newPhotoUrl)
                if (!repository.isNicknameAvailable(update.nickname)) throw ProfileException(ProfileError.NicknameTaken)
                repository.updateProfile(update)
            }
        }
    }
}

internal fun ProfileDraft.toUpdate(photoUrl: String?): ProfileUpdate =
    ProfileUpdate(
        nickname = nickname.trim(),
        gender = checkNotNull(gender),
        description = description.trim().ifEmpty { null },
        avatarId = checkNotNull(avatarId),
        photoUrl = photoUrl,
    )
