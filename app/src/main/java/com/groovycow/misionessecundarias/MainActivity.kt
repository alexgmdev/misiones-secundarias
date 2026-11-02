package com.groovycow.misionessecundarias

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.groovycow.misionessecundarias.domain.model.Mission
import com.groovycow.misionessecundarias.ui.applications.MyApplicationsScreen
import com.groovycow.misionessecundarias.ui.missions.MissionDetailRoute
import com.groovycow.misionessecundarias.ui.missions.MissionListScreen
import com.groovycow.misionessecundarias.ui.navigation.FeaturePlaceholderScreen
import com.groovycow.misionessecundarias.ui.navigation.MainMenuDestination
import com.groovycow.misionessecundarias.ui.navigation.MainMenuScreen
import com.groovycow.misionessecundarias.ui.theme.MisionesSecundariasTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MisionesSecundariasTheme {
                var selectedMission by remember { mutableStateOf<Mission?>(null) }
                var destination by remember { mutableStateOf<AppDestination>(AppDestination.MainMenu) }

                when (val currentDestination = destination) {
                    AppDestination.MainMenu -> {
                        MainMenuScreen(
                            onNavigate = { menuDestination ->
                                destination = when (menuDestination) {
                                    MainMenuDestination.SEARCH_MISSIONS -> AppDestination.MissionList
                                    MainMenuDestination.MY_APPLICATIONS -> AppDestination.MyApplications
                                    else -> AppDestination.Placeholder(menuDestination)
                                }
                            }
                        )
                    }

                    AppDestination.MissionList -> {
                        MissionListScreen(
                            onMissionClick = { mission ->
                                selectedMission = mission
                                destination = AppDestination.MissionDetail
                            },
                            onMyApplicationsClick = {
                                destination = AppDestination.MyApplications
                            },
                            onBack = { destination = AppDestination.MainMenu }
                        )
                    }

                    AppDestination.MissionDetail -> {
                        val mission = selectedMission
                        if (mission == null) {
                            destination = AppDestination.MissionList
                        } else {
                            MissionDetailRoute(
                                missionId = mission.id,
                                missionTitle = mission.title,
                                missionDescription = mission.description,
                                missionDateTime = mission.dateTime,
                                maxParticipants = mission.maxParticipants,
                                modality = mission.modality,
                                category = mission.category,
                                requirements = mission.requirements,
                                publisherName = mission.publisherProfile?.fullName
                                    ?: "Usuario Desconocido",
                                approximateZone = "Barrio San Fernando, Cali", // <-- Aquí pasamos tu HU-028
                                onBack = {
                                    selectedMission = null
                                    destination = AppDestination.MissionList
                                }
                            )
                        }
                    }

                    AppDestination.MyApplications -> {
                        MyApplicationsScreen(
                            onBack = { destination = AppDestination.MainMenu },
                            onExploreMissions = { destination = AppDestination.MissionList }
                        )
                    }

                    is AppDestination.Placeholder -> {
                        FeaturePlaceholderScreen(
                            title = currentDestination.menuDestination.title,
                            onBack = { destination = AppDestination.MainMenu }
                        )
                    }
                }
            }
        }
    }
}

private sealed interface AppDestination {
    data object MainMenu : AppDestination
    data object MissionList : AppDestination
    data object MissionDetail : AppDestination
    data object MyApplications : AppDestination
    data class Placeholder(val menuDestination: MainMenuDestination) : AppDestination
}