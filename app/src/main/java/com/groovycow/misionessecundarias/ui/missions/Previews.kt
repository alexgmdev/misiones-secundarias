package com.groovycow.misionessecundarias.ui.missions

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.runtime.Composable
import com.groovycow.misionessecundarias.domain.model.Mission
import com.groovycow.misionessecundarias.ui.theme.MisionesSecundariasTheme

// --- PREVIEW DEL DETALLE ---
@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, name = "Detalle - Campo")
@Preview(showBackground = true, name = "Detalle - Remota")
@Composable
fun MissionDetailScreenPreview() {
    MisionesSecundariasTheme {
        MissionDetailScreen(
            missionTitle = "Limpieza del Río Cali",
            missionDescription = "Necesitamos ayuda para recoger basura en la orilla del río este sábado.",
            missionDateTime = "2026-11-04T20:04:37.93485+00:00",
            maxParticipants = 5,
            modality = "campo",
            category = "Ecológica",
            requirements = "Guantes, ropa cómoda, agua.",
            publisherName = "prueba@misiones.com",
            onBack = {}
        )
    }
}

// --- PREVIEW DE LA TARJETA ---
@Preview(showBackground = true, name = "Tarjeta de Misión")
@Composable
fun MissionCardPreview() {
    MisionesSecundariasTheme {
        MissionCard(
            mission = Mission(
                id = "123",
                publisherId = "456",
                title = "Limpieza del Río Cali",
                description = "Desc",
                category = "Ecológica",
                requirements = null,
                modality = "campo",
                dateTime = "2026-11-04T20:04:37.93485+00:00",
                location = null,
                maxParticipants = 5,
                status = "Publicada",
                publisherProfile = null
            ),
            onClick = {}
        )
    }
}
