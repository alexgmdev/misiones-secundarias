package com.groovycow.misionessecundarias.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/** Portada de acceso mostrada antes de elegir entre iniciar sesión y crear una cuenta. */
@Composable
fun WelcomeScreen(
    onLogin: () -> Unit,
    onRegister: () -> Unit,
    notice: String? = null,
    onNoticeDismissed: () -> Unit = {},
) {
    AuthBackground {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 18.dp)
                    .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(52.dp))
            AuthTitle("Misiones Secundarias")
            Spacer(modifier = Modifier.height(14.dp))
            AuthPanel {
                Column(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "Tu aventura comunitaria comienza aquí",
                        color = GameCream,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium.copy(shadow = SoftTextShadow),
                    )
                    Button(
                        onClick = onLogin,
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GameGold, contentColor = ForestInk),
                    ) {
                        Text("Iniciar Sesión", fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = onRegister,
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(2.dp, GameCream),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = GameCream),
                    ) {
                        Text(
                            text = "Crear cuenta",
                            style = MaterialTheme.typography.labelLarge.copy(shadow = SoftTextShadow),
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    notice?.let { message ->
        AlertDialog(
            onDismissRequest = onNoticeDismissed,
            containerColor = Color(0xFFFFFBEE),
            title = { Text("Aviso", color = ForestInk, fontWeight = FontWeight.Bold) },
            text = { Text(message, color = ForestInk) },
            confirmButton = {
                TextButton(onClick = onNoticeDismissed) {
                    Text("Aceptar", color = ForestInk)
                }
            },
        )
    }
}
