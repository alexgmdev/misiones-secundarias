package com.groovycow.misionessecundarias.ui.profile.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.groovycow.misionessecundarias.domain.model.AvatarId
import com.groovycow.misionessecundarias.domain.model.Gender
import com.groovycow.misionessecundarias.domain.model.ProfileDraft
import com.groovycow.misionessecundarias.domain.model.ProfileError
import com.groovycow.misionessecundarias.domain.model.ProfileException
import com.groovycow.misionessecundarias.domain.usecase.CompleteInitialProfileUseCase
import com.groovycow.misionessecundarias.domain.usecase.CompressProfilePhotoUseCase
import com.groovycow.misionessecundarias.domain.usecase.ProfileValidationException
import com.groovycow.misionessecundarias.domain.validation.FieldError
import com.groovycow.misionessecundarias.domain.validation.ProfileField
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Estado de la pantalla "Crea tu perfil".
 *
 * @property nickname Apodo digitado.
 * @property gender Género elegido, o null.
 * @property description Descripción digitada (opcional).
 * @property avatarId Avatar elegido, o null.
 * @property photoUri Uri de la foto ya comprimida con éxito (sirve de vista previa), o null.
 * @property photoError Error al preparar la foto elegida (formato o tamaño), o null.
 * @property fieldErrors Errores de validación por campo.
 * @property error Error del guardado (nickname en uso, red o servidor), o null.
 * @property isLoading `true` mientras se guarda (deshabilita el botón).
 * @property isSaved `true` cuando el perfil se guardó y hay que continuar la navegación.
 */
data class InitialProfileUiState(
    val nickname: String = "",
    val gender: Gender? = null,
    val description: String = "",
    val avatarId: AvatarId? = null,
    val photoUri: String? = null,
    val photoError: ProfileError? = null,
    val fieldErrors: Map<ProfileField, FieldError> = emptyMap(),
    val error: ProfileError? = null,
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
)

/**
 * ViewModel del perfil inicial (HU-005): captura el formulario, comprime la foto al elegirla y guarda el perfil.
 *
 * @param completeInitialProfile Caso de uso que valida, sube la foto y guarda el perfil.
 * @param compressPhoto Caso de uso que valida el formato y comprime la foto a JPEG.
 */
class InitialProfileViewModel(
    private val completeInitialProfile: CompleteInitialProfileUseCase,
    private val compressPhoto: CompressProfilePhotoUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(InitialProfileUiState())

    /** Estado observable de la pantalla. */
    val uiState: StateFlow<InitialProfileUiState> = _uiState.asStateFlow()

    private var photoJpeg: ByteArray? = null

    /** Actualiza el apodo. @param value Texto digitado. */
    fun onNicknameChange(value: String) = edit(ProfileField.Nickname) { it.copy(nickname = value) }

    /** Actualiza el género. @param value Género elegido. */
    fun onGenderChange(value: Gender) = edit(ProfileField.Gender) { it.copy(gender = value) }

    /** Actualiza la descripción. @param value Texto digitado. */
    fun onDescriptionChange(value: String) = edit(ProfileField.Description) { it.copy(description = value) }

    /** Actualiza el avatar. @param value Avatar elegido. */
    fun onAvatarChange(value: AvatarId) = edit(ProfileField.Avatar) { it.copy(avatarId = value) }

    /**
     * Comprime la foto elegida en el selector. Si falla se muestra el error bajo la foto y se conserva la foto previa válida.
     *
     * @param uri Uri (texto) de la imagen elegida.
     */
    fun onPhotoPicked(uri: String) {
        viewModelScope.launch {
            compressPhoto(uri)
                .onSuccess { bytes ->
                    photoJpeg = bytes
                    edit(ProfileField.Photo) { it.copy(photoUri = uri, photoError = null) }
                }
                .onFailure { failure ->
                    _uiState.update { it.copy(photoError = failure.toProfileError()) }
                }
        }
    }

    /** Guarda el perfil si no hay otro guardado en curso; los errores de validación no llaman al backend. */
    fun save() {
        val current = _uiState.value
        if (current.isLoading) return
        _uiState.update { it.copy(isLoading = true, error = null, fieldErrors = emptyMap()) }
        viewModelScope.launch {
            val draft = ProfileDraft(current.nickname, current.gender, current.description, current.avatarId)
            completeInitialProfile(draft, photoJpeg)
                .onSuccess { _uiState.update { it.copy(isLoading = false, isSaved = true) } }
                .onFailure { failure ->
                    _uiState.update {
                        if (failure is ProfileValidationException) {
                            it.copy(isLoading = false, fieldErrors = failure.errors)
                        } else {
                            it.copy(isLoading = false, error = failure.toProfileError())
                        }
                    }
                }
        }
    }

    private fun edit(
        field: ProfileField,
        transform: (InitialProfileUiState) -> InitialProfileUiState,
    ) = _uiState.update { transform(it).copy(fieldErrors = it.fieldErrors - field, error = null) }

    private fun Throwable.toProfileError(): ProfileError = (this as? ProfileException)?.error ?: ProfileError.ServerError
}
