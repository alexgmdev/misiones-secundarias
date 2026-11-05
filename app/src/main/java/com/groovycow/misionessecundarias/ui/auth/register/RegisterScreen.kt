package com.groovycow.misionessecundarias.ui.auth.register

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.groovycow.misionessecundarias.domain.validation.FieldError
import com.groovycow.misionessecundarias.ui.auth.toMessage
import org.koin.androidx.compose.koinViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * Pantalla de registro (HU-001).
 *
 * @param onRegistered Se invoca una sola vez cuando la cuenta se creó; navega a `perfil-inicial`.
 * @param onBack Acción al volver (a inicio de sesión).
 * @param viewModel ViewModel del registro, obtenido con Koin.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    onRegistered: () -> Unit,
    onBack: () -> Unit,
    viewModel: RegisterViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(state.isRegistered) {
        if (state.isRegistered) onRegistered()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Crear cuenta", fontWeight = FontWeight.Bold) },
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
                    .padding(padding)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            RegisterFields(state, viewModel)
            state.error?.let { error ->
                Text(error.toMessage(), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }
            Button(
                onClick = viewModel::register,
                enabled = !state.isLoading,
                modifier = Modifier.fillMaxWidth().height(48.dp),
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text("Registrarse")
                }
            }
            OutlinedButton(onClick = onBack, enabled = !state.isLoading, modifier = Modifier.fillMaxWidth()) {
                Text("Ya tengo cuenta")
            }
        }
    }
}

/** Campos del formulario de registro. */
@Composable
private fun RegisterFields(
    state: RegisterUiState,
    viewModel: RegisterViewModel,
) {
    val errors = state.fieldErrors
    FormField("Nombre", state.firstName, viewModel::onFirstNameChange, errors[RegisterField.FirstName], KeyboardType.Text)
    FormField("Apellido", state.lastName, viewModel::onLastNameChange, errors[RegisterField.LastName], KeyboardType.Text)
    FormField("Correo electrónico", state.email, viewModel::onEmailChange, errors[RegisterField.Email], KeyboardType.Email)
    FormField("Celular", state.phone, viewModel::onPhoneChange, errors[RegisterField.Phone], KeyboardType.Phone)
    BirthDateField(state.birthDate, errors[RegisterField.BirthDate], viewModel::onBirthDateChange)
    FormField(
        "Contraseña",
        state.password,
        viewModel::onPasswordChange,
        errors[RegisterField.Password],
        KeyboardType.Password,
        isPassword = true,
    )
    FormField(
        "Confirmar contraseña",
        state.confirmation,
        viewModel::onConfirmationChange,
        errors[RegisterField.Confirmation],
        KeyboardType.Password,
        isPassword = true,
    )
}

/**
 * Campo de texto del formulario con mensaje de error y, opcionalmente, alternador de visibilidad de contraseña.
 *
 * @param label Etiqueta del campo.
 * @param value Valor actual.
 * @param onValueChange Callback al editar.
 * @param error Error de validación a mostrar, o null.
 * @param keyboardType Tipo de teclado.
 * @param isPassword `true` para ocultar el texto con opción "Mostrar"/"Ocultar".
 */
@Composable
private fun FormField(
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

/**
 * Campo de fecha de nacimiento que abre un selector Material 3 (no se escribe a mano).
 *
 * @param value Fecha elegida, o null.
 * @param error Error de validación, o null.
 * @param onValueChange Callback con la fecha elegida.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BirthDateField(
    value: LocalDate?,
    error: FieldError?,
    onValueChange: (LocalDate) -> Unit,
) {
    var showPicker by remember { mutableStateOf(false) }
    Column {
        OutlinedButton(onClick = { showPicker = true }, modifier = Modifier.fillMaxWidth()) {
            Text(value?.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) ?: "Fecha de nacimiento")
        }
        if (error != null) {
            Text(error.toMessage(), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
    }
    if (showPicker) {
        val pickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { millis ->
                            onValueChange(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate())
                        }
                        showPicker = false
                    },
                ) { Text("Aceptar") }
            },
            dismissButton = { TextButton(onClick = { showPicker = false }) { Text("Cancelar") } },
        ) {
            DatePicker(state = pickerState)
        }
    }
}
