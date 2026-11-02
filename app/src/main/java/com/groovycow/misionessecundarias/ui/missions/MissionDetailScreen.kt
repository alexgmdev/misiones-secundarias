package com.groovycow.misionessecundarias.ui.missions

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun MissionDetailScreen(
    missionTitle: String,
    missionDescription: String,
    modality: String,
    category: String?,
    // En el Sprint 1 mostraremos la zona aproximada (HU-028).
    // En el Sprint 2 aquí irá el mapa real.
    approximateZone: String = "Barrio Granada, Cali",
    onBack: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(text = missionTitle, style = MaterialTheme.typography.headlineLarge)
        Spacer(modifier = Modifier.height(8.dp))

        Text(text = "Modalidad: $modality", style = MaterialTheme.typography.titleMedium)
        if (category != null) {
            Text(text = "Categoría: $category", style = MaterialTheme.typography.titleMedium)
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Descripción:", style = MaterialTheme.typography.titleMedium)
        Text(text = missionDescription, style = MaterialTheme.typography.bodyLarge)

        Spacer(modifier = Modifier.height(16.dp))

        // Zona aproximada (HU-028)
        if (modality == "campo") {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Ubicación Aproximada", style = MaterialTheme.typography.titleSmall)
                    Text(approximateZone, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "(La dirección exacta se revelará al ser aceptado)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))
        Button(
            onClick = { /* Lógica de postulación para Jojhan (HU-039) */ },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Postularme a la Misión")
        }
    }
}