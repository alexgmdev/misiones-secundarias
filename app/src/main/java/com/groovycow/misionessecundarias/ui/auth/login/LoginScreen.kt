package com.groovycow.misionessecundarias.ui.auth.login

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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.groovycow.misionessecundarias.domain.validation.FieldError
import com.groovycow.misionessecundarias.ui.auth.AuthBackground
import com.groovycow.misionessecundarias.ui.auth.AuthPanel
import com.groovycow.misionessecundarias.ui.auth.AuthTitle
import com.groovycow.misionessecundarias.ui.auth.ButtonTextShadow
import com.groovycow.misionessecundarias.ui.auth.ErrorCream
import com.groovycow.misionessecundarias.ui.auth.ForestInk
import com.groovycow.misionessecundarias.ui.auth.GameCream
import com.groovycow.misionessecundarias.ui.auth.GameGold
import com.groovycow.misionessecundarias.ui.auth.SoftTextShadow
import com.groovycow.misionessecundarias.ui.auth.authTextFieldColors
import com.groovycow.misionessecundarias.ui.auth.toMessage
import org.koin.androidx.compose.koinViewModel

/**
 * Pantalla de inicio de sesión (HU-002) con correo o celular.
 *
 * @param onSignedIn Se invoca una sola vez cuando la sesión quedó iniciada; navega a `menu-principal`.
 * @param onRegister Acción al elegir "Crear cuenta".
 * @param onForgotPassword Acción al elegir "¿Olvidaste tu contraseña?"; si es null el enlace no se muestra.
 * @param notice Mensaje informativo opcional sobre el formulario (por ejemplo, tras cambiar la contraseña).
 * @param onNoticeDismissed Acción al cerrar el aviso informativo.
 * @param viewModel ViewModel del inicio de sesión, obtenido con Koin.
 */
@Composable
fun LoginScreen(
    onSignedIn: () -> Unit,
    onRegister: () -> Unit,
    onForgotPassword: (() -> Unit)? = null,
    notice: String? = null,
    onNoticeDismissed: () -> Unit = {},
    viewModel: LoginViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(state.isSignedIn) {
        if (state.isSignedIn) {
            viewModel.onSignInHandled()
            onSignedIn()
        }
    }

    AuthBackground {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 18.dp)
                    .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(42.dp))
            AuthTitle("Iniciar Sesión")
            Spacer(modifier = Modifier.height(14.dp))
            AuthPanel {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    LoginTextField(
                        label = "Correo o celular",
                        value = state.identifier,
                        onValueChange = viewModel::onIdentifierChange,
                        error = state.fieldErrors[LoginField.Identifier],
                        keyboardType = KeyboardType.Email,
                    )
                    LoginTextField(
                        label = "Contraseña",
                        value = state.password,
                        onValueChange = viewModel::onPasswordChange,
                        error = state.fieldErrors[LoginField.Password],
                        keyboardType = KeyboardType.Password,
                        isPassword = true,
                    )
                    state.error?.let { error ->
                        Text(
                            text = error.toMessage(),
                            color = ErrorCream,
                            style = MaterialTheme.typography.bodyMedium.copy(shadow = SoftTextShadow),
                        )
                    }
                    Button(
                        onClick = viewModel::signIn,
                        enabled = !state.isLoading,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = GameGold,
                                contentColor = ForestInk,
                            ),
                    ) {
                        if (state.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = ForestInk,
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Text(
                                text = "Iniciar Sesión",
                                style = MaterialTheme.typography.labelLarge.copy(shadow = ButtonTextShadow),
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                    if (onForgotPassword != null) {
                        LoginLink(
                            text = "¿Olvidaste tu contraseña?",
                            onClick = onForgotPassword,
                            enabled = !state.isLoading,
                        )
                    }
                    LoginLink(
                        text = "¿No tienes cuenta? Crear cuenta",
                        onClick = onRegister,
                        enabled = !state.isLoading,
                    )
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    notice?.let { message ->
        AlertDialog(
            onDismissRequest = onNoticeDismissed,
            title = { Text("Aviso", fontWeight = FontWeight.Bold) },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = onNoticeDismissed) {
                    Text("Aceptar")
                }
            },
        )
    }
}

@Composable
private fun LoginLink(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean,
) {
    TextButton(onClick = onClick, enabled = enabled) {
        Text(
            text = text,
            color = GameCream,
            style = MaterialTheme.typography.labelLarge.copy(shadow = SoftTextShadow),
        )
    }
}

/**
 * Campo de texto del formulario de login con mensaje de error y, opcionalmente, alternador de visibilidad.
 *
 * @param label Etiqueta del campo.
 * @param value Valor actual.
 * @param onValueChange Callback al editar.
 * @param error Error de validación a mostrar, o null.
 * @param keyboardType Tipo de teclado.
 * @param isPassword `true` para ocultar el texto con opción "Mostrar"/"Ocultar".
 */
@Composable
private fun LoginTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    error: FieldError?,
    keyboardType: KeyboardType,
    isPassword: Boolean = false,
) {
    var visible by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        isError = error != null,
        supportingText = { if (error != null) Text(error.toMessage()) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        visualTransformation = if (isPassword && !visible) PasswordVisualTransformation() else VisualTransformation.None,
        trailingIcon =
            if (isPassword) {
                {
                    TextButton(
                        onClick = { visible = !visible },
                        colors = ButtonDefaults.textButtonColors(contentColor = ForestInk),
                    ) {
                        Text(if (visible) "Ocultar" else "Mostrar")
                    }
                }
            } else {
                null
            },
        shape = RoundedCornerShape(14.dp),
        colors =
            authTextFieldColors(),
        modifier = Modifier.fillMaxWidth(),
    )
}
