package com.groovycow.misionessecundarias.ui.auth.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import com.groovycow.misionessecundarias.ui.auth.toMessage
import org.koin.androidx.compose.koinViewModel

/**
 * Pantalla de inicio de sesión (HU-002) con correo o celular.
 *
 * @param onSignedIn Se invoca una sola vez cuando la sesión quedó iniciada; navega a `menu-principal`.
 * @param onRegister Acción al elegir "Crear cuenta".
 * @param onForgotPassword Acción al elegir "¿Olvidaste tu contraseña?"; si es null el enlace no se muestra.
 * @param viewModel ViewModel del inicio de sesión, obtenido con Koin.
 */
@Composable
fun LoginScreen(
    onSignedIn: () -> Unit,
    onRegister: () -> Unit,
    onForgotPassword: (() -> Unit)? = null,
    viewModel: LoginViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(state.isSignedIn) {
        if (state.isSignedIn) onSignedIn()
    }

    Scaffold { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        ) {
            Text("Iniciar Sesión", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
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
                Text(error.toMessage(), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }
            Button(
                onClick = viewModel::signIn,
                enabled = !state.isLoading,
                modifier = Modifier.fillMaxWidth().height(48.dp),
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text("Iniciar Sesión")
                }
            }
            if (onForgotPassword != null) {
                TextButton(
                    onClick = onForgotPassword,
                    enabled = !state.isLoading,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                ) {
                    Text("¿Olvidaste tu contraseña?")
                }
            }
            TextButton(onClick = onRegister, enabled = !state.isLoading, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text("¿No tienes cuenta? Crear cuenta")
            }
        }
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
                { TextButton(onClick = { visible = !visible }) { Text(if (visible) "Ocultar" else "Mostrar") } }
            } else {
                null
            },
        modifier = Modifier.fillMaxWidth(),
    )
}
