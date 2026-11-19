package com.groovycow.misionessecundarias.ui.profile.edit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.groovycow.misionessecundarias.domain.model.AvatarId
import com.groovycow.misionessecundarias.domain.model.Gender
import com.groovycow.misionessecundarias.domain.model.ProfileDraft
import com.groovycow.misionessecundarias.domain.model.ProfileError
import com.groovycow.misionessecundarias.domain.model.ProfileException
import com.groovycow.misionessecundarias.domain.usecase.CompressProfilePhotoUseCase
import com.groovycow.misionessecundarias.domain.usecase.GetMyProfileUseCase
import com.groovycow.misionessecundarias.domain.usecase.ProfileValidationException
import com.groovycow.misionessecundarias.domain.usecase.UpdateProfileUseCase
import com.groovycow.misionessecundarias.domain.usecase.UploadProfilePhotoUseCase
import com.groovycow.misionessecundarias.domain.validation.FieldError
import com.groovycow.misionessecundarias.domain.validation.ProfileField
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Estado de la pantalla "Editar perfil".
 *
 * @property isLoaded `true` cuando el perfil actual ya se cargó y el formulario está prellenado.
 * @property loadError Error al cargar el perfil actual, o null.
 * @property nickname Apodo digitado.
 * @property gender Género elegido, o null.
 * @property description Descripción digitada.
 * @property avatarId Avatar elegido, o null.
 * @property photoUrl URL de la foto actual guardada.
 * @property photoPreviewUri Uri de la foto nueva ya comprimida con éxito (vista previa; se sube al guardar), o null.
 * @property photoError Error al preparar la foto elegida (formato o tamaño), o null.
 * @property fieldErrors Errores de validación por campo.
 * @property error Error del guardado (nickname en uso, red o servidor), o null.
 * @property isLoading `true` mientras se guarda (deshabilita el botón).
 * @property isSaved `true` cuando se guardó y hay que volver a Mi perfil.
 */
data class EditProfileUiState(
    val isLoaded: Boolean = false,
    val loadError: ProfileError? = null,
    val nickname: String = "",
    val gender: Gender? = null,
    val description: String = "",
    val avatarId: AvatarId? = null,
    val photoUrl: String? = null,
    val photoPreviewUri: String? = null,
    val photoError: ProfileError? = null,
    val fieldErrors: Map<ProfileField, FieldError> = emptyMap(),
    val error: ProfileError? = null,
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
)

/**
 * ViewModel de Editar perfil (HU-008/HU-009): precarga el perfil propio, valida y guarda los cambios y la foto nueva.
 *
 * @param getMyProfile Caso de uso que obtiene el perfil propio.
 * @param updateProfile Caso de uso que valida y actualiza el perfil (el nickname propio no cuenta como en uso).
 * @param uploadPhoto Caso de uso que sube la foto comprimida y devuelve su URL con `?v=` anti-caché.
 * @param compressPhoto Caso de uso que valida el formato y comprime la foto a JPEG.
 */
class EditProfileViewModel(
    private val getMyProfile: GetMyProfileUseCase,
    private val updateProfile: UpdateProfileUseCase,
    private val uploadPhoto: UploadProfilePhotoUseCase,
    private val compressPhoto: CompressProfilePhotoUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(EditProfileUiState())

    /** Estado observable de la pantalla. */
    val uiState: StateFlow<EditProfileUiState> = _uiState.asStateFlow()

    private var photoJpeg: ByteArray? = null
    private var uploadedPhotoUrl: String? = null

    /** Carga (o recarga) el perfil actual y prellena el formulario, descartando ediciones previas. */
    fun load() {
        viewModelScope.launch {
            getMyProfile()
                .onSuccess { profile ->
                    photoJpeg = null
                    uploadedPhotoUrl = null
                    _uiState.value =
                        EditProfileUiState(
                            isLoaded = true,
                            nickname = profile.nickname.orEmpty(),
                            gender = profile.gender,
                            description = profile.description.orEmpty(),
                            avatarId = profile.avatarId,
                            photoUrl = profile.photoUrl,
                        )
                }
                .onFailure { _uiState.value = EditProfileUiState(loadError = it.toProfileError()) }
        }
    }

    /** Actualiza el apodo. @param value Texto digitado. */
    fun onNicknameChange(value: String) = edit(ProfileField.Nickname) { it.copy(nickname = value) }

    /** Actualiza el género. @param value Género elegido. */
    fun onGenderChange(value: Gender) = edit(ProfileField.Gender) { it.copy(gender = value) }

    /** Actualiza la descripción. @param value Texto digitado. */
    fun onDescriptionChange(value: String) = edit(ProfileField.Description) { it.copy(description = value) }

    /** Actualiza el avatar. @param value Avatar elegido. */
    fun onAvatarChange(value: AvatarId) = edit(ProfileField.Avatar) { it.copy(avatarId = value) }

    /**
     * Comprime la foto elegida. Si falla muestra el error bajo la foto y conserva la foto previa; si funciona,
     * la deja en vista previa y se sube al guardar.
     *
     * @param uri Uri (texto) de la imagen elegida.
     */
    fun onPhotoPicked(uri: String) {
        viewModelScope.launch {
            compressPhoto(uri)
                .onSuccess { bytes ->
                    photoJpeg = bytes
                    uploadedPhotoUrl = null
                    _uiState.update { it.copy(photoPreviewUri = uri, photoError = null, error = null) }
                }
                .onFailure { failure -> _uiState.update { it.copy(photoError = failure.toProfileError()) } }
        }
    }

    /** Guarda los cambios si el formulario está cargado y no hay otro guardado en curso. */
    fun save() {
        val current = _uiState.value
        if (!current.isLoaded || current.isLoading || current.isSaved) return
        _uiState.update { it.copy(isLoading = true, error = null, fieldErrors = emptyMap()) }
        viewModelScope.launch {
            val draft = ProfileDraft(current.nickname, current.gender, current.description, current.avatarId)
            val photoUrl = uploadedPhotoUrl ?: photoJpeg?.let { bytes -> uploadPhoto(bytes).getOrElse { return@launch failSave(it) } }
            uploadedPhotoUrl = photoUrl
            updateProfile(draft, photoUrl)
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

    private fun failSave(failure: Throwable) = _uiState.update { it.copy(isLoading = false, error = failure.toProfileError()) }

    private fun edit(
        field: ProfileField,
        transform: (EditProfileUiState) -> EditProfileUiState,
    ) = _uiState.update { transform(it).copy(fieldErrors = it.fieldErrors - field, error = null) }

    private fun Throwable.toProfileError(): ProfileError = (this as? ProfileException)?.error ?: ProfileError.ServerError
}
