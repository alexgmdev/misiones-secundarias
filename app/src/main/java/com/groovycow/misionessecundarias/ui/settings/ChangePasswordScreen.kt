package com.groovycow.misionessecundarias.ui.settings

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.groovycow.misionessecundarias.domain.model.AuthError
import com.groovycow.misionessecundarias.domain.usecase.ChangePasswordField
import com.groovycow.misionessecundarias.ui.auth.toMessage
import org.koin.androidx.compose.koinViewModel

/**
 * Pantalla de cambio de contraseña desde Ajustes → Seguridad (HU-010).
 *
 * @param onBack Acción al volver a Ajustes (flecha o cierre del diálogo de éxito).
 * @param viewModel ViewModel del formulario.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangePasswordScreen(
    onBack: () -> Unit,
    viewModel: ChangePasswordViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = SandBackground,
        topBar = {
            TopAppBar(
                title = { Text("Cambiar contraseña", color = ForestGreen, fontWeight = FontWeight.ExtraBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = EarthOrange,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SandBackground),
            )
        },
    ) { paddingValues ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(3.dp, EarthOrange),
                colors = CardDefaults.cardColors(containerColor = CreamSurface),
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    PasswordField(
                        label = "Contraseña actual",
                        value = state.current,
                        onValueChange = viewModel::onCurrentChange,
                        errorText =
                            state.fieldErrors[ChangePasswordField.Current]?.toMessage()
                                ?: state.error?.toMessage()?.takeIf { state.error == AuthError.IncorrectCurrentPassword },
                    )
                    PasswordField(
                        label = "Nueva contraseña",
                        value = state.new,
                        onValueChange = viewModel::onNewChange,
                        errorText =
                            state.fieldErrors[ChangePasswordField.New]?.toMessage()
                                ?: state.error?.toMessage()?.takeIf { state.error == AuthError.SamePassword },
                    )
                    PasswordField(
                        label = "Confirmar nueva contraseña",
                        value = state.confirmation,
                        onValueChange = viewModel::onConfirmationChange,
                        errorText = state.fieldErrors[ChangePasswordField.Confirmation]?.toMessage(),
                    )
                    GeneralError(state.error)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = viewModel::submit,
                        enabled = !state.isLoading,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(54.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = ForestGreen,
                                contentColor = CreamSurface,
                            ),
                    ) {
                        if (state.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = CreamSurface,
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Text("Actualizar Contraseña", fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
            }
        }
    }

    if (state.isChanged) {
        AlertDialog(
            onDismissRequest = onBack,
            containerColor = CreamSurface,
            title = { Text("Contraseña actualizada", color = ForestGreen, fontWeight = FontWeight.ExtraBold) },
            text = { Text("Tu contraseña se cambió correctamente.", color = BarkBrown) },
            confirmButton = {
                TextButton(onClick = onBack) {
                    Text("Aceptar", color = ForestGreen, fontWeight = FontWeight.Bold)
                }
            },
        )
    }
}

/** Muestra los errores de red o servidor; los de actual incorrecta y nueva igual se muestran bajo su campo. */
@Composable
private fun GeneralError(error: AuthError?) {
    if (error == null || error == AuthError.IncorrectCurrentPassword || error == AuthError.SamePassword) return
    Text(text = error.toMessage(), color = AlertCoral, fontWeight = FontWeight.Bold)
}

/**
 * Campo de contraseña con alternador "Mostrar"/"Ocultar" y mensaje de error.
 *
 * @param label Etiqueta del campo.
 * @param value Valor actual.
 * @param onValueChange Callback al editar.
 * @param errorText Mensaje de error a mostrar, o null.
 */
@Composable
private fun PasswordField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    errorText: String?,
) {
    var visible by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        isError = errorText != null,
        supportingText = { if (errorText != null) Text(errorText) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            TextButton(
                onClick = { visible = !visible },
                colors = ButtonDefaults.textButtonColors(contentColor = ForestGreen),
            ) {
                Text(if (visible) "Ocultar" else "Mostrar")
            }
        },
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth(),
    )
}

private val ForestGreen = Color(0xFF214E34)
private val EarthOrange = Color(0xFFE58A3A)
private val SandBackground = Color(0xFFF7F3E8)
private val CreamSurface = Color(0xFFFFFCF3)
private val BarkBrown = Color(0xFF594A3C)
private val AlertCoral = Color(0xFFE85D3F)
