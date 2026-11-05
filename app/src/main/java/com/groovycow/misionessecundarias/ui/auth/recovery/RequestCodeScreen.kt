package com.groovycow.misionessecundarias.ui.auth.recovery

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.groovycow.misionessecundarias.ui.auth.toMessage
import org.koin.androidx.compose.koinViewModel
import java.util.UUID

/**
 * Pantalla para solicitar el código de recuperación de contraseña (HU-003).
 *
 * @param onCodeSent Se invoca una sola vez cuando se envió el código, con el correo (sin espacios) al que se envió.
 * @param onBack Acción al volver (a inicio de sesión).
 * @param viewModel ViewModel de la solicitud, obtenido con Koin; se crea uno nuevo en cada entrada a la pantalla
 * (el store de ViewModels es el de la Activity) para no reutilizar `isCodeSent`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestCodeScreen(
    onCodeSent: (email: String) -> Unit,
    onBack: () -> Unit,
    viewModel: RequestCodeViewModel = koinViewModel(key = rememberSaveable { UUID.randomUUID().toString() }),
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(state.isCodeSent) {
        if (state.isCodeSent) onCodeSent(state.email.trim())
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Recuperar contraseña", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        ) {
            Text("Ingresa tu correo y te enviaremos un código para restablecer tu contraseña.")
            RecoveryTextField(
                label = "Correo electrónico",
                value = state.email,
                onValueChange = viewModel::onEmailChange,
                error = state.emailError,
                keyboardType = KeyboardType.Email,
            )
            state.error?.let { error ->
                Text(error.toMessage(), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }
            Button(
                onClick = viewModel::sendCode,
                enabled = !state.isLoading,
                modifier = Modifier.fillMaxWidth().height(48.dp),
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text("Enviar Código")
                }
            }
        }
    }
}
