package com.groovycow.misionessecundarias

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import com.groovycow.misionessecundarias.domain.model.Mission
import com.groovycow.misionessecundarias.ui.missions.MissionDetailScreen
import com.groovycow.misionessecundarias.ui.missions.MissionListScreen
import com.groovycow.misionessecundarias.ui.theme.MisionesSecundariasTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MisionesSecundariasTheme {
                // Guardamos el objeto Mission completo en lugar de solo el ID
                var selectedMission by remember { mutableStateOf<Mission?>(null) }

                if (selectedMission == null) {
                    MissionListScreen(
                        // Ahora recibimos el objeto Mission desde la lista
                        onMissionClick = { mission -> selectedMission = mission },
                        onBack = { /* Aquí irá el logout a futuro */ }
                    )
                } else {
                    // Extraemos los datos reales del objeto seleccionado
                    val mission = selectedMission!!
                    MissionDetailScreen(
                        missionTitle = mission.title,
                        missionDescription = mission.description,
                        modality = mission.modality,
                        category = mission.category,
                        requirements = mission.requirements,
                        // Extraemos el nombre del publicador usando el JOIN que hicimos en el repositorio
                        publisherName = mission.publisherProfile?.fullName ?: "Usuario Desconocido",
                        onBack = { selectedMission = null }
                    )
                }
            }
        }
    }
}