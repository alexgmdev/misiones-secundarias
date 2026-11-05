package com.groovycow.misionessecundarias.ui.auth.recovery

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import com.groovycow.misionessecundarias.domain.validation.FieldError
import com.groovycow.misionessecundarias.ui.auth.toMessage

/**
 * Campo de texto de las pantallas de recuperación con mensaje de error y, opcionalmente, alternador de visibilidad.
 *
 * @param label Etiqueta del campo.
 * @param value Valor actual.
 * @param onValueChange Callback al editar.
 * @param error Error de validación a mostrar, o null.
 * @param keyboardType Tipo de teclado.
 * @param isPassword `true` para ocultar el texto con opción "Mostrar"/"Ocultar".
 */
@Composable
internal fun RecoveryTextField(
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
