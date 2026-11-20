package com.groovycow.misionessecundarias.ui.settings

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** Sección informativa de las políticas de privacidad. */
data class PrivacyPolicySection(
    /** Título de la sección. */
    val title: String,
    /** Texto explicativo en español. */
    val body: String,
)

/** Contenido estático de las políticas de privacidad y uso de datos (HU-122). */
val PRIVACY_POLICY_SECTIONS: List<PrivacyPolicySection> =
    listOf(
        PrivacyPolicySection(
            title = "Datos que usamos",
            body =
                "Usamos tu nombre, correo, celular y los datos de tu perfil público para crear tu cuenta, " +
                    "permitir el inicio de sesión y mostrarte misiones acordes a tu participación.",
        ),
        PrivacyPolicySection(
            title = "Ubicación en Cali",
            body =
                "La ubicación de una misión de campo se usa para mostrarla en el mapa y para validar tu llegada " +
                    "al punto de encuentro. La dirección exacta solo se revela cuando el publicador acepta tu postulación.",
        ),
        PrivacyPolicySection(
            title = "Visibilidad del perfil",
            body =
                "Tu perfil público muestra nickname, avatar, género y descripción. El celular, la fecha de nacimiento " +
                    "y los intentos de inicio de sesión se guardan como datos privados y no se muestran a otros usuarios.",
        ),
        PrivacyPolicySection(
            title = "Control de tus datos",
            body =
                "Puedes actualizar tu perfil y tu contraseña desde la aplicación. También puedes solicitar la " +
                    "desactivación lógica de tu cuenta cuando decidas dejar de participar.",
        ),
        PrivacyPolicySection(
            title = "Contacto",
            body =
                "Si tienes dudas sobre el tratamiento de tus datos, escribe al equipo de Groovy Cow de la " +
                    "Universidad del Valle.",
        ),
    )

/**
 * Pantalla informativa de políticas de privacidad y uso de datos (HU-122).
 *
 * @param onBack Acción para volver a Ajustes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyScreen(onBack: () -> Unit) {
    Scaffold(
        containerColor = SandBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Políticas de Privacidad",
                        color = ForestGreen,
                        fontWeight = FontWeight.ExtraBold,
                    )
                },
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
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = "Cómo cuidamos tu información y tu ubicación en la plataforma.",
                color = BarkBrown,
                style = MaterialTheme.typography.bodyMedium,
            )
            PRIVACY_POLICY_SECTIONS.forEach { section ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(2.dp, RiverTeal),
                    colors = CardDefaults.cardColors(containerColor = CreamSurface),
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = section.title,
                            color = ForestGreen,
                            fontWeight = FontWeight.ExtraBold,
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = section.body,
                            color = BarkBrown,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
            }
        }
    }
}

private val ForestGreen = Color(0xFF214E34)
private val RiverTeal = Color(0xFF38A89D)
private val EarthOrange = Color(0xFFE58A3A)
private val SandBackground = Color(0xFFF7F3E8)
private val CreamSurface = Color(0xFFFFFCF3)
private val BarkBrown = Color(0xFF594A3C)
