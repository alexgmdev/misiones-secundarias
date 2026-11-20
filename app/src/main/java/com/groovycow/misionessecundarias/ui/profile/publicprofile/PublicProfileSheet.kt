package com.groovycow.misionessecundarias.ui.profile.publicprofile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import com.groovycow.misionessecundarias.domain.model.PublicProfile
import com.groovycow.misionessecundarias.ui.profile.ProfileMessages
import com.groovycow.misionessecundarias.ui.profile.components.ProfileAvatar
import com.groovycow.misionessecundarias.ui.profile.components.ProfilePhoto
import com.groovycow.misionessecundarias.ui.profile.me.MyProfileUiState
import com.groovycow.misionessecundarias.ui.theme.NaturePalette
import org.koin.androidx.compose.koinViewModel

/** Mensaje cuando el usuario aún no completó su perfil público. */
private const val UNAVAILABLE_MESSAGE = "Perfil no disponible"

/**
 * Ficha pública de solo lectura de otro usuario (HU-007), como hoja inferior modal.
 *
 * Muestra únicamente foto, nickname, género, descripción y avatar; se cierra al deslizar, con Atrás
 * o al tocar fuera, volviendo a la pantalla de origen.
 *
 * @param userId Identificador del usuario cuya ficha se muestra.
 * @param onDismiss Acción al cerrar la ficha.
 * @param viewModel ViewModel de la ficha.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PublicProfileSheet(
    userId: String,
    onDismiss: () -> Unit,
    viewModel: PublicProfileViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(userId) { viewModel.load(userId) }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = NaturePalette.CreamSurface,
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().heightIn(min = 240.dp).navigationBarsPadding().padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            val profile = state.profile
            when {
                profile != null -> PublicProfileBody(profile)
                state.error != null -> SheetError(error = state.error!!, onRetry = viewModel::retry)
                else -> CircularProgressIndicator(color = NaturePalette.RiverTeal)
            }
        }
    }
}

@Composable
private fun PublicProfileBody(profile: PublicProfile) {
    if (profile.nickname.isNullOrBlank()) {
        Text(UNAVAILABLE_MESSAGE, color = NaturePalette.BarkBrown, fontWeight = FontWeight.Bold)
        return
    }
    Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            ProfilePhoto(photoUrl = profile.photoUrl, size = 96.dp)
            profile.avatarId?.let { ProfileAvatar(avatar = it, size = 96.dp) }
        }
        Text(
            text = profile.nickname,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
            color = NaturePalette.ForestGreen,
            textAlign = TextAlign.Center,
        )
        Text(
            text = MyProfileUiState.LEVEL_LABEL,
            color = NaturePalette.CreamSurface,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.background(NaturePalette.EarthOrange, CircleShape).padding(horizontal = 14.dp, vertical = 5.dp),
        )
        profile.gender?.let {
            Text(ProfileMessages.genderLabel(it), color = NaturePalette.BarkBrown, fontWeight = FontWeight.Bold)
        }
        profile.description?.takeIf { it.isNotBlank() }?.let {
            Text(it, color = NaturePalette.BarkBrown, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun SheetError(
    error: ProfileError,
    onRetry: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = ProfileMessages.error(error),
            color = NaturePalette.BarkBrown,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = NaturePalette.LeafGreen)) {
            Text("Reintentar", fontWeight = FontWeight.ExtraBold)
        }
    }
}
