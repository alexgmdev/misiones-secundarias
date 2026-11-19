package com.groovycow.misionessecundarias.ui.profile.setup

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.groovycow.misionessecundarias.domain.model.ProfileError
import com.groovycow.misionessecundarias.domain.validation.ProfileField
import com.groovycow.misionessecundarias.ui.auth.AuthBackground
import com.groovycow.misionessecundarias.ui.auth.AuthPanel
import com.groovycow.misionessecundarias.ui.auth.AuthTitle
import com.groovycow.misionessecundarias.ui.auth.ErrorCream
import com.groovycow.misionessecundarias.ui.auth.ForestInk
import com.groovycow.misionessecundarias.ui.auth.GameCream
import com.groovycow.misionessecundarias.ui.auth.GameGold
import com.groovycow.misionessecundarias.ui.auth.SoftTextShadow
import com.groovycow.misionessecundarias.ui.auth.authTextFieldColors
import com.groovycow.misionessecundarias.ui.profile.ProfileMessages
import com.groovycow.misionessecundarias.ui.profile.components.AvatarPicker
import com.groovycow.misionessecundarias.ui.profile.components.GenderSelector
import com.groovycow.misionessecundarias.ui.profile.components.ProfilePhoto
import org.koin.androidx.compose.koinViewModel

private const val DESCRIPTION_MAX = 280

/**
 * Pantalla "Crea tu perfil" (HU-005): nickname, género, descripción, avatar y foto obligatorios (salvo la descripción).
 *
 * @param onProfileCompleted Se invoca una sola vez cuando el perfil se guardó.
 * @param viewModel ViewModel del perfil inicial, obtenido con Koin.
 */
@Composable
fun InitialProfileScreen(
    onProfileCompleted: () -> Unit,
    viewModel: InitialProfileViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val photoPicker =
        rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            uri?.let { viewModel.onPhotoPicked(it.toString()) }
        }

    LaunchedEffect(state.isSaved) {
        if (state.isSaved) onProfileCompleted()
    }

    AuthBackground {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 18.dp)
                    .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(40.dp))
            AuthTitle("Crea tu perfil")
            Spacer(modifier = Modifier.height(12.dp))
            AuthPanel {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    ProfileFormFields(state, viewModel)
                    PhotoSection(
                        state = state,
                        onPick = {
                            photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                    )
                    val generalError = state.error?.takeIf { it != ProfileError.NicknameTaken }
                    if (generalError != null) {
                        Text(
                            text = ProfileMessages.error(generalError),
                            color = ErrorCream,
                            style = MaterialTheme.typography.bodyMedium.copy(shadow = SoftTextShadow),
                        )
                    }
                    SaveButton(isLoading = state.isLoading, onClick = viewModel::save)
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun ProfileFormFields(
    state: InitialProfileUiState,
    viewModel: InitialProfileViewModel,
) {
    val errors = state.fieldErrors
    val nicknameError =
        if (state.error == ProfileError.NicknameTaken) {
            ProfileMessages.error(ProfileError.NicknameTaken)
        } else {
            errors[ProfileField.Nickname]?.let { ProfileMessages.fieldError(ProfileField.Nickname, it) }
        }
    OutlinedTextField(
        value = state.nickname,
        onValueChange = viewModel::onNicknameChange,
        label = { Text("Nickname") },
        isError = nicknameError != null,
        supportingText = { if (nicknameError != null) Text(nicknameError) },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        colors = authTextFieldColors(),
        modifier = Modifier.fillMaxWidth(),
    )
    SectionLabel("Género")
    GenderSelector(
        selected = state.gender,
        onSelect = viewModel::onGenderChange,
        errorText = errors[ProfileField.Gender]?.let { ProfileMessages.fieldError(ProfileField.Gender, it) },
        chipColors =
            FilterChipDefaults.filterChipColors(
                containerColor = GameCream.copy(alpha = 0.92f),
                labelColor = ForestInk,
                selectedContainerColor = GameGold,
                selectedLabelColor = ForestInk,
            ),
        errorColor = ErrorCream,
    )
    val descriptionError = errors[ProfileField.Description]?.let { ProfileMessages.fieldError(ProfileField.Description, it) }
    OutlinedTextField(
        value = state.description,
        onValueChange = viewModel::onDescriptionChange,
        label = { Text("Descripción (opcional)") },
        isError = descriptionError != null,
        supportingText = { Text(descriptionError ?: "${state.description.length}/$DESCRIPTION_MAX") },
        minLines = 3,
        shape = RoundedCornerShape(14.dp),
        colors = authTextFieldColors(),
        modifier = Modifier.fillMaxWidth(),
    )
    SectionLabel("Avatar")
    AvatarPicker(
        selected = state.avatarId,
        onSelect = viewModel::onAvatarChange,
        errorText = errors[ProfileField.Avatar]?.let { ProfileMessages.fieldError(ProfileField.Avatar, it) },
        selectedBorderColor = GameGold,
        errorColor = ErrorCream,
    )
}

@Composable
private fun PhotoSection(
    state: InitialProfileUiState,
    onPick: () -> Unit,
) {
    SectionLabel("Foto de perfil")
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ProfilePhoto(photoUrl = state.photoUri, size = 96.dp)
        OutlinedButton(
            onClick = onPick,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(2.dp, GameCream),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = GameCream),
        ) {
            Text(if (state.photoUri == null) "Elegir foto" else "Cambiar foto")
        }
        val photoError =
            state.photoError?.let { ProfileMessages.error(it) }
                ?: state.fieldErrors[ProfileField.Photo]?.let { ProfileMessages.fieldError(ProfileField.Photo, it) }
        if (photoError != null) {
            Text(
                text = photoError,
                color = ErrorCream,
                style = MaterialTheme.typography.bodySmall.copy(shadow = SoftTextShadow),
            )
        }
    }
}

@Composable
private fun SaveButton(
    isLoading: Boolean,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        enabled = !isLoading,
        modifier = Modifier.fillMaxWidth().height(52.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = GameGold, contentColor = ForestInk),
    ) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = ForestInk, strokeWidth = 2.dp)
        } else {
            Text("Guardar Perfil", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        color = GameCream,
        style = MaterialTheme.typography.titleSmall.copy(shadow = SoftTextShadow),
        fontWeight = FontWeight.Bold,
    )
}
