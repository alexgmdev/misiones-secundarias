package com.groovycow.misionessecundarias.ui.missions

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import org.koin.androidx.compose.koinViewModel

@Composable
fun PublicProfileScreen(
    userId: String,
    onBack: () -> Unit,
    viewModel: PublicProfileViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(userId) {
        viewModel.loadProfile(userId)
    }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
        ) {
            Button(onClick = onBack) {
                Text("Volver")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        when {
            uiState.isLoading -> {
                CircularProgressIndicator()
            }

            uiState.error != null -> {
                Text(
                    text = uiState.error ?: "No fue posible cargar el perfil.",
                    color = MaterialTheme.colorScheme.error,
                )
            }

            uiState.profile != null -> {
                val profile = uiState.profile!!
                val imageUrl =
                    profile.photoUrl?.takeIf { it.isNotBlank() }
                        ?: profile.avatarUrl?.takeIf { it.isNotBlank() }

                if (imageUrl != null) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = "Fotografía de perfil",
                        modifier =
                            Modifier
                                .size(120.dp)
                                .clip(CircleShape),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Box(
                        modifier =
                            Modifier
                                .size(120.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("Sin foto")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text =
                        profile.nickname
                            ?.takeIf { it.isNotBlank() }
                            ?: profile.fullName
                            ?: "Usuario",
                    style = MaterialTheme.typography.headlineSmall,
                )

                Spacer(modifier = Modifier.height(20.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        ProfileDataRow(
                            label = "Nombre",
                            value = profile.fullName,
                        )

                        ProfileDataRow(
                            label = "Género",
                            value = profile.gender,
                        )

                        ProfileDataRow(
                            label = "Nivel",
                            value = profile.level,
                        )

                        Column {
                            Text(
                                text = "Descripción",
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                text =
                                    profile.description
                                        ?.takeIf { it.isNotBlank() }
                                        ?: "Sin descripción disponible.",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileDataRow(
    label: String,
    value: String?,
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = value?.takeIf { it.isNotBlank() } ?: "No especificado",
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
