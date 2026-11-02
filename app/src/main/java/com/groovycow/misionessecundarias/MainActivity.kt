package com.groovycow.misionessecundarias

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import com.groovycow.misionessecundarias.ui.missions.MissionDetailScreen
import com.groovycow.misionessecundarias.ui.missions.MissionListScreen
import com.groovycow.misionessecundarias.ui.theme.MisionesSecundariasTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MisionesSecundariasTheme {
                var selectedMissionId by remember { mutableStateOf<String?>(null) }

                if (selectedMissionId == null) {
                    MissionListScreen(
                        onMissionClick = { id -> selectedMissionId = id }
                    )
                } else {
                    // Datos simulados para probar el Detalle (HU-027)
                    MissionDetailScreen(
                        missionTitle = "Prueba de Misión",
                        missionDescription = "Esta es una descripción de prueba para verificar la pantalla de detalle.",
                        modality = "campo",
                        category = "Social",
                        approximateZone = "Barrio San Fernando",
                        onBack = { selectedMissionId = null }
                    )
                }
            }
        }
    }
}