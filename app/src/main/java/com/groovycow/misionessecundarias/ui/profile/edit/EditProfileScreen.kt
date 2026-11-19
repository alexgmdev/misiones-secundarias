package com.groovycow.misionessecundarias.ui.profile.edit

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.groovycow.misionessecundarias.domain.model.ProfileError
import com.groovycow.misionessecundarias.domain.validation.ProfileField
import com.groovycow.misionessecundarias.ui.profile.ProfileMessages
import com.groovycow.misionessecundarias.ui.profile.components.AvatarPicker
import com.groovycow.misionessecundarias.ui.profile.components.GenderSelector
import com.groovycow.misionessecundarias.ui.profile.components.ProfilePhoto
import com.groovycow.misionessecundarias.ui.theme.NaturePalette
import org.koin.androidx.compose.koinViewModel

private const val DESCRIPTION_MAX = 280

/**
 * Pantalla "Editar Perfil" (HU-008). Carga el perfil al entrar y vuelve a Mi perfil al guardar.
 *
 * @param onBack Acción al volver (flecha) sin guardar.
 * @param onSaved Acción cuando los cambios se guardaron.
 * @param viewModel ViewModel de la pantalla.
 */
@Composable
fun EditProfileScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: EditProfileViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(Unit) { viewModel.load() }
    LaunchedEffect(state.isSaved) { if (state.isSaved) onSaved() }
    EditProfileContent(state = state, viewModel = viewModel, onBack = onBack)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditProfileContent(
    state: EditProfileUiState,
    viewModel: EditProfileViewModel,
    onBack: () -> Unit,
) {
    Scaffold(
        containerColor = NaturePalette.SandBackground,
        topBar = {
            TopAppBar(
                title = { Text("Editar Perfil", fontWeight = FontWeight.ExtraBold, color = NaturePalette.ForestGreen) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = NaturePalette.EarthOrange,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NaturePalette.SandBackground),
            )
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            when {
                state.isLoaded -> EditForm(state = state, viewModel = viewModel)
                state.loadError != null -> LoadErrorCard(error = state.loadError, onRetry = viewModel::load)
                else -> CircularProgressIndicator(color = NaturePalette.RiverTeal)
            }
        }
    }
}

@Composable
private fun EditForm(
    state: EditProfileUiState,
    viewModel: EditProfileViewModel,
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Sección de foto: HU-009 añadirá aquí la acción "Cambiar foto".
        PhotoSection(photoUrl = state.photoUrl)
        DataSection(state = state, viewModel = viewModel)
        AvatarSection(state = state, viewModel = viewModel)
        if (state.error != null) {
            Text(
                text = ProfileMessages.error(state.error),
                color = NaturePalette.AlertCoral,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Button(
            onClick = viewModel::save,
            enabled = !state.isLoading,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(14.dp),
            colors =
                ButtonDefaults.buttonColors(
                    containerColor = NaturePalette.LeafGreen,
                    contentColor = NaturePalette.CreamSurface,
                ),
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = NaturePalette.CreamSurface,
                    strokeWidth = 2.dp,
                )
            } else {
                Text("Guardar Cambios", fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}

@Composable
private fun PhotoSection(photoUrl: String?) {
    SectionCard(title = "Foto") {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            ProfilePhoto(photoUrl = photoUrl, size = 96.dp)
        }
    }
}

@Composable
private fun DataSection(
    state: EditProfileUiState,
    viewModel: EditProfileViewModel,
) {
    val errors = state.fieldErrors
    val nicknameError =
        if (state.error == ProfileError.NicknameTaken) {
            ProfileMessages.error(ProfileError.NicknameTaken)
        } else {
            errors[ProfileField.Nickname]?.let { ProfileMessages.fieldError(ProfileField.Nickname, it) }
        }
    val descriptionError = errors[ProfileField.Description]?.let { ProfileMessages.fieldError(ProfileField.Description, it) }
    SectionCard(title = "Datos") {
        OutlinedTextField(
            value = state.nickname,
            onValueChange = viewModel::onNicknameChange,
            label = { Text("Nickname") },
            isError = nicknameError != null,
            supportingText = { if (nicknameError != null) Text(nicknameError) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = fieldColors(),
            modifier = Modifier.fillMaxWidth(),
        )
        Text("Género", fontWeight = FontWeight.Bold, color = NaturePalette.BarkBrown)
        GenderSelector(
            selected = state.gender,
            onSelect = viewModel::onGenderChange,
            errorText = errors[ProfileField.Gender]?.let { ProfileMessages.fieldError(ProfileField.Gender, it) },
        )
        OutlinedTextField(
            value = state.description,
            onValueChange = viewModel::onDescriptionChange,
            label = { Text("Descripción (opcional)") },
            isError = descriptionError != null,
            supportingText = { Text(descriptionError ?: "${state.description.length}/$DESCRIPTION_MAX") },
            minLines = 3,
            shape = RoundedCornerShape(12.dp),
            colors = fieldColors(),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun AvatarSection(
    state: EditProfileUiState,
    viewModel: EditProfileViewModel,
) {
    SectionCard(title = "Avatar") {
        AvatarPicker(
            selected = state.avatarId,
            onSelect = viewModel::onAvatarChange,
            errorText = state.fieldErrors[ProfileField.Avatar]?.let { ProfileMessages.fieldError(ProfileField.Avatar, it) },
        )
    }
}

@Composable
private fun SectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = NaturePalette.CreamSurface),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(3.dp, NaturePalette.LeafGreen),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(text = title, fontWeight = FontWeight.ExtraBold, color = NaturePalette.ForestGreen)
            content()
        }
    }
}

@Composable
private fun fieldColors() =
    OutlinedTextFieldDefaults.colors(
        focusedBorderColor = NaturePalette.LeafGreen,
        unfocusedBorderColor = NaturePalette.MapLine,
        focusedLabelColor = NaturePalette.ForestGreen,
        cursorColor = NaturePalette.ForestGreen,
    )

@Composable
private fun LoadErrorCard(
    error: ProfileError,
    onRetry: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = NaturePalette.LightCoral),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(3.dp, NaturePalette.AlertCoral),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = ProfileMessages.error(error),
                color = NaturePalette.BarkBrown,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Button(
                onClick = onRetry,
                shape = RoundedCornerShape(14.dp),
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = NaturePalette.EarthOrange,
                        contentColor = NaturePalette.CreamSurface,
                    ),
            ) { Text("Reintentar", fontWeight = FontWeight.ExtraBold) }
        }
    }
}
