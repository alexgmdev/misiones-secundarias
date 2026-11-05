package com.groovycow.misionessecundarias.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.unit.dp
import com.groovycow.misionessecundarias.domain.model.Mission
import com.groovycow.misionessecundarias.ui.applications.MyApplicationsScreen
import com.groovycow.misionessecundarias.ui.auth.SessionViewModel
import com.groovycow.misionessecundarias.ui.auth.StartDestination
import com.groovycow.misionessecundarias.ui.auth.register.RegisterScreen
import com.groovycow.misionessecundarias.ui.missions.MissionDetailRoute
import com.groovycow.misionessecundarias.ui.missions.MissionListScreen
import org.koin.androidx.compose.koinViewModel

/** Destinos de la navegación manual de la aplicación. */
sealed interface AppDestination {
    /** Inicio de sesión (provisional: placeholder hasta HU-002). */
    data object Login : AppDestination

    /** Registro de cuenta (HU-001). */
    data object Register : AppDestination

    /** Configuración inicial de perfil (`perfil-inicial`; placeholder hasta HU-005). */
    data object InitialProfile : AppDestination

    /** Menú principal (`menu-principal`). */
    data object MainMenu : AppDestination

    /** Lista de misiones. */
    data object MissionList : AppDestination

    /** Detalle de la misión seleccionada. */
    data object MissionDetail : AppDestination

    /** Postulaciones del usuario. */
    data object MyApplications : AppDestination

    /**
     * Pantalla de funcionalidad aún no implementada.
     *
     * @property menuDestination Opción del menú que la originó.
     */
    data class Placeholder(val menuDestination: MainMenuDestination) : AppDestination
}

/**
 * Raíz de la navegación: espera a conocer la sesión guardada, fija el destino inicial una sola vez
 * (Login sin sesión, MainMenu con sesión) y luego enruta entre destinos.
 */
@Composable
fun AppNavigation() {
    val sessionViewModel: SessionViewModel = koinViewModel()
    val startDestination by sessionViewModel.startDestination.collectAsState()
    var selectedMission by remember { mutableStateOf<Mission?>(null) }
    var destination by remember { mutableStateOf<AppDestination?>(null) }

    LaunchedEffect(startDestination) {
        if (destination == null) {
            destination = startDestination.toAppDestination()
        }
    }

    val currentDestination = destination
    if (currentDestination == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    BackHandler(enabled = currentDestination == AppDestination.Login || currentDestination == AppDestination.InitialProfile) {
        destination = AppDestination.MainMenu
    }
    BackHandler(enabled = currentDestination == AppDestination.Register) {
        destination = AppDestination.Login
    }

    when (currentDestination) {
        AppDestination.Login -> {
            // TODO(HU-002): reemplazar por LoginScreen. Mientras tanto "Volver" entra al menú para no bloquear el desarrollo.
            LoginPlaceholder(
                onRegister = { destination = AppDestination.Register },
                onBack = { destination = AppDestination.MainMenu },
            )
        }

        AppDestination.Register -> {
            RegisterScreen(
                onRegistered = { destination = AppDestination.InitialProfile },
                onBack = { destination = AppDestination.Login },
            )
        }

        AppDestination.InitialProfile -> {
            FeaturePlaceholderScreen(
                title = "Configuración inicial de perfil",
                onBack = { destination = AppDestination.MainMenu },
            )
        }

        AppDestination.MainMenu -> {
            MainMenuScreen(
                onNavigate = { menuDestination -> destination = menuDestination.toAppDestination() },
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
                onBack = { destination = AppDestination.MainMenu },
            )
        }

        AppDestination.MissionDetail -> {
            MissionDetailDestination(
                mission = selectedMission,
                onMissingMission = { destination = AppDestination.MissionList },
                onBack = {
                    selectedMission = null
                    destination = AppDestination.MissionList
                },
            )
        }

        AppDestination.MyApplications -> {
            MyApplicationsScreen(
                onBack = { destination = AppDestination.MainMenu },
                onExploreMissions = { destination = AppDestination.MissionList },
            )
        }

        is AppDestination.Placeholder -> {
            FeaturePlaceholderScreen(
                title = currentDestination.menuDestination.title,
                onBack = { destination = AppDestination.MainMenu },
            )
        }
    }
}

/**
 * Inicio de sesión provisional (hasta HU-002): placeholder con enlace al registro.
 *
 * @param onRegister Acción al elegir "Crear cuenta".
 * @param onBack Acción de "Volver" (entra al menú para no bloquear el desarrollo).
 */
@Composable
private fun LoginPlaceholder(
    onRegister: () -> Unit,
    onBack: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        FeaturePlaceholderScreen(title = "Inicio de sesión", onBack = onBack)
        TextButton(onClick = onRegister, modifier = Modifier.align(Alignment.BottomCenter).padding(24.dp)) {
            Text("¿No tienes cuenta? Crear cuenta")
        }
    }
}

/** Traduce el destino inicial resuelto por [SessionViewModel] (`null` mientras carga la sesión). */
private fun StartDestination.toAppDestination(): AppDestination? =
    when (this) {
        StartDestination.LOADING -> null
        StartDestination.LOGIN -> AppDestination.Login
        StartDestination.MAIN_MENU -> AppDestination.MainMenu
    }

/** Traduce una opción del menú principal al destino de navegación correspondiente. */
private fun MainMenuDestination.toAppDestination(): AppDestination =
    when (this) {
        MainMenuDestination.SEARCH_MISSIONS -> AppDestination.MissionList
        MainMenuDestination.MY_APPLICATIONS -> AppDestination.MyApplications
        else -> AppDestination.Placeholder(this)
    }

/**
 * Detalle de misión; si no hay misión seleccionada invoca [onMissingMission] para volver a la lista.
 *
 * @param mission Misión seleccionada, o `null` si no hay ninguna.
 * @param onMissingMission Acción cuando no hay misión seleccionada.
 * @param onBack Acción al volver desde el detalle.
 */
@Composable
private fun MissionDetailDestination(
    mission: Mission?,
    onMissingMission: () -> Unit,
    onBack: () -> Unit,
) {
    if (mission == null) {
        onMissingMission()
        return
    }
    MissionDetailRoute(
        missionId = mission.id,
        missionTitle = mission.title,
        missionDescription = mission.description,
        missionDateTime = mission.dateTime,
        maxParticipants = mission.maxParticipants,
        modality = mission.modality,
        category = mission.category,
        requirements = mission.requirements,
        publisherName = mission.publisherProfile?.fullName ?: "Usuario Desconocido",
        approximateZone = "Barrio San Fernando, Cali",
        onBack = onBack,
    )
}
