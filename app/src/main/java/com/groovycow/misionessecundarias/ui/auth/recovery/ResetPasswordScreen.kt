package com.groovycow.misionessecundarias.ui.auth.recovery

import androidx.activity.compose.BackHandler
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
import androidx.compose.material3.TextButton
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
import com.groovycow.misionessecundarias.ui.auth.RECOVERY_CODE_SENT_MESSAGE
import com.groovycow.misionessecundarias.ui.auth.toMessage
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import java.util.UUID

/**
 * Pantalla para ingresar el código recibido y definir la nueva contraseña (HU-004).
 *
 * @param email Correo al que se envió el código.
 * @param onPasswordChanged Se invoca una sola vez cuando la clave cambió; navega a inicio de sesión.
 * @param onBack Acción al volver (a la solicitud del código); antes se cierra la sesión temporal si el código
 * ya se había verificado.
 * @param viewModel ViewModel del restablecimiento, obtenido con Koin con el correo como parámetro; se crea uno
 * nuevo en cada entrada a la pantalla (el store de ViewModels es el de la Activity).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResetPasswordScreen(
    email: String,
    onPasswordChanged: () -> Unit,
    onBack: () -> Unit,
    viewModel: ResetPasswordViewModel = koinViewModel(key = rememberSaveable { UUID.randomUUID().toString() }) { parametersOf(email) },
) {
    val state by viewModel.uiState.collectAsState()
    val leave = {
        viewModel.onAbandon()
        onBack()
    }

    BackHandler(onBack = leave)

    LaunchedEffect(state.isPasswordChanged) {
        if (state.isPasswordChanged) onPasswordChanged()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nueva contraseña", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = leave) {
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
            Text(RECOVERY_CODE_SENT_MESSAGE, style = MaterialTheme.typography.bodyMedium)
            RecoveryTextField(
                label = "Código",
                value = state.code,
                onValueChange = viewModel::onCodeChange,
                error = state.fieldErrors[ResetField.Code],
                keyboardType = KeyboardType.Number,
            )
            RecoveryTextField(
                label = "Nueva contraseña",
                value = state.password,
                onValueChange = viewModel::onPasswordChange,
                error = state.fieldErrors[ResetField.Password],
                keyboardType = KeyboardType.Password,
                isPassword = true,
            )
            RecoveryTextField(
                label = "Confirmar nueva contraseña",
                value = state.confirmation,
                onValueChange = viewModel::onConfirmationChange,
                error = state.fieldErrors[ResetField.Confirmation],
                keyboardType = KeyboardType.Password,
                isPassword = true,
            )
            state.error?.let { error ->
                Text(error.toMessage(), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }
            Button(
                onClick = viewModel::save,
                enabled = !state.isLoading,
                modifier = Modifier.fillMaxWidth().height(48.dp),
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text("Guardar Nueva Contraseña")
                }
            }
            TextButton(
                onClick = viewModel::resend,
                enabled = !state.isLoading && state.resendAvailableIn == 0,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            ) {
                Text(if (state.resendAvailableIn > 0) "Reenviar código en ${state.resendAvailableIn} s" else "Reenviar código")
            }
        }
    }
}
