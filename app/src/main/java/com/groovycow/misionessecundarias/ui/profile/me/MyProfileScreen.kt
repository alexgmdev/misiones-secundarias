package com.groovycow.misionessecundarias.ui.profile.me

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.material3.MaterialTheme
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
import com.groovycow.misionessecundarias.domain.model.Profile
import com.groovycow.misionessecundarias.domain.model.ProfileError
import com.groovycow.misionessecundarias.ui.profile.ProfileMessages
import com.groovycow.misionessecundarias.ui.profile.components.ProfileAvatar
import com.groovycow.misionessecundarias.ui.profile.components.ProfilePhoto
import com.groovycow.misionessecundarias.ui.theme.NaturePalette
import org.koin.androidx.compose.koinViewModel

/**
 * Pantalla "Mi perfil" (HU-006). Recarga el perfil cada vez que se entra a ella.
 *
 * @param onBack Acción al volver al menú.
 * @param onEditProfile Acción del botón "Editar Perfil".
 * @param onOpenHistory Acción del botón "Histórico de Misiones".
 * @param viewModel ViewModel de la pantalla.
 */
@Composable
fun MyProfileScreen(
    onBack: () -> Unit,
    onEditProfile: () -> Unit,
    onOpenHistory: () -> Unit,
    viewModel: MyProfileViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(Unit) { viewModel.load() }
    MyProfileContent(
        state = state,
        onBack = onBack,
        onEditProfile = onEditProfile,
        onOpenHistory = onOpenHistory,
        onRetry = viewModel::retry,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MyProfileContent(
    state: MyProfileUiState,
    onBack: () -> Unit,
    onEditProfile: () -> Unit,
    onOpenHistory: () -> Unit,
    onRetry: () -> Unit,
) {
    Scaffold(
        containerColor = NaturePalette.SandBackground,
        topBar = {
            TopAppBar(
                title = { Text("Mi Perfil", fontWeight = FontWeight.ExtraBold, color = NaturePalette.ForestGreen) },
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
            val profile = state.profile
            when {
                profile != null ->
                    ProfileBody(profile = profile, onEditProfile = onEditProfile, onOpenHistory = onOpenHistory)
                state.error != null -> ErrorCard(error = state.error, onRetry = onRetry)
                else -> CircularProgressIndicator(color = NaturePalette.RiverTeal)
            }
        }
    }
}

@Composable
private fun ProfileBody(
    profile: Profile,
    onEditProfile: () -> Unit,
    onOpenHistory: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        HeaderCard(profile)
        profile.gender?.let { InfoCard(title = "Género", body = ProfileMessages.genderLabel(it)) }
        InfoCard(title = "Descripción", body = profile.description?.takeIf { it.isNotBlank() })
        Spacer(modifier = Modifier.height(2.dp))
        ActionButton(text = "Editar Perfil", color = NaturePalette.LeafGreen, onClick = onEditProfile)
        ActionButton(text = "Histórico de Misiones", color = NaturePalette.RiverTeal, onClick = onOpenHistory)
    }
}

@Composable
private fun HeaderCard(profile: Profile) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = NaturePalette.CreamSurface),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(3.dp, NaturePalette.LeafGreen),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                ProfilePhoto(photoUrl = profile.photoUrl, size = 96.dp)
                profile.avatarId?.let { ProfileAvatar(avatar = it, size = 96.dp) }
            }
            Text(
                text = profile.nickname.orEmpty(),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = NaturePalette.ForestGreen,
                textAlign = TextAlign.Center,
            )
            Text(
                text = MyProfileUiState.LEVEL_LABEL,
                color = NaturePalette.CreamSurface,
                fontWeight = FontWeight.ExtraBold,
                modifier =
                    Modifier
                        .background(NaturePalette.EarthOrange, CircleShape)
                        .padding(horizontal = 14.dp, vertical = 5.dp),
            )
        }
    }
}

@Composable
private fun InfoCard(
    title: String,
    body: String?,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = NaturePalette.CreamSurface),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(2.dp, NaturePalette.MapLine),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(text = title, fontWeight = FontWeight.ExtraBold, color = NaturePalette.ForestGreen)
            if (body != null) {
                Text(text = body, color = NaturePalette.BarkBrown, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}

@Composable
private fun ActionButton(
    text: String,
    color: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(56.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color, contentColor = NaturePalette.CreamSurface),
    ) {
        Text(text = text, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
private fun ErrorCard(
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
            ActionButton(text = "Reintentar", color = NaturePalette.EarthOrange, onClick = onRetry)
        }
    }
}
