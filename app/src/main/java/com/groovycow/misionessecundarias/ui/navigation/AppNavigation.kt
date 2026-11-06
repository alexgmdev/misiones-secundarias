package com.groovycow.misionessecundarias.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.groovycow.misionessecundarias.domain.model.Mission
import com.groovycow.misionessecundarias.ui.applications.MyApplicationsScreen
import com.groovycow.misionessecundarias.ui.auth.SessionViewModel
import com.groovycow.misionessecundarias.ui.auth.StartDestination
import com.groovycow.misionessecundarias.ui.auth.login.LoginScreen
import com.groovycow.misionessecundarias.ui.auth.recovery.RequestCodeScreen
import com.groovycow.misionessecundarias.ui.auth.recovery.ResetPasswordScreen
import com.groovycow.misionessecundarias.ui.auth.register.RegisterScreen
import com.groovycow.misionessecundarias.ui.missions.MissionDetailRoute
import com.groovycow.misionessecundarias.ui.missions.MissionListScreen
import com.groovycow.misionessecundarias.ui.missions.create.CreateMissionScreen
import com.groovycow.misionessecundarias.ui.settings.SettingsScreen
import org.koin.androidx.compose.koinViewModel

/** Destinos de la navegación manual de la aplicación. */
sealed interface AppDestination {
    /** Inicio de sesión (HU-002). */
    data object Login : AppDestination

    /** Registro de cuenta (HU-001). */
    data object Register : AppDestination

    /** Solicitud del código de recuperación de contraseña (HU-003). */
    data object RequestCode : AppDestination

    /**
     * Ingreso del código y nueva contraseña (HU-004).
     *
     * @property email Correo al que se envió el código.
     */
    data class ResetPassword(val email: String) : AppDestination

    /** Configuración inicial de perfil (`perfil-inicial`; placeholder hasta HU-005). */
    data object InitialProfile : AppDestination

    /** Menú principal (`menu-principal`). */
    data object MainMenu : AppDestination

    /** Lista de misiones. */
    data object MissionList : AppDestination

    /** Formulario para crear un borrador de misión (HU-016). */
    data object PublishMission : AppDestination

    /** Detalle de la misión seleccionada. */
    data object MissionDetail : AppDestination

    /** Postulaciones del usuario. */
    data object MyApplications : AppDestination

    /** Ajustes de la aplicación y cierre de sesión. */
    data object Settings : AppDestination

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
@Suppress("CyclomaticComplexMethod")
fun AppNavigation() {
    val sessionViewModel: SessionViewModel = koinViewModel()
    val startDestination by sessionViewModel.startDestination.collectAsState()
    val isLoggedIn by sessionViewModel.isLoggedIn.collectAsState()
    val isSessionResolved by sessionViewModel.isSessionResolved.collectAsState()
    var selectedMission by remember { mutableStateOf<Mission?>(null) }
    var destination by remember { mutableStateOf<AppDestination?>(null) }
    var loginNotice by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(startDestination) {
        destination = resolveInitialDestination(destination, startDestination)
    }

    LaunchedEffect(isSessionResolved, isLoggedIn, destination) {
        if (shouldRedirectToLogin(isSessionResolved, isLoggedIn, destination)) {
            selectedMission = null
            loginNotice =
                if (destination == AppDestination.Settings) {
                    SIGNED_OUT_NOTICE
                } else {
                    SESSION_REQUIRED_NOTICE
                }
            destination = AppDestination.Login
        }
    }

    val currentDestination = destination
    if (currentDestination == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    BackHandler(enabled = currentDestination == AppDestination.InitialProfile) {
        destination = AppDestination.MainMenu
    }
    BackHandler(enabled = currentDestination == AppDestination.Register || currentDestination == AppDestination.RequestCode) {
        destination = AppDestination.Login
    }

    when (currentDestination) {
        AppDestination.Login -> {
            LoginScreen(
                onSignedIn = {
                    sessionViewModel.updateLoginState(true)
                    destination = AppDestination.MainMenu
                },
                onRegister = { destination = AppDestination.Register },
                onForgotPassword = {
                    loginNotice = null
                    destination = AppDestination.RequestCode
                },
                notice = loginNotice,
                onNoticeDismissed = { loginNotice = null },
            )
        }

        AppDestination.RequestCode -> {
            RequestCodeScreen(
                onCodeSent = { email -> destination = AppDestination.ResetPassword(email) },
                onBack = { destination = AppDestination.Login },
            )
        }

        is AppDestination.ResetPassword -> {
            ResetPasswordScreen(
                email = currentDestination.email,
                onPasswordChanged = {
                    loginNotice = PASSWORD_CHANGED_NOTICE
                    destination = AppDestination.Login
                },
                onBack = { destination = AppDestination.RequestCode },
            )
        }

        AppDestination.Register -> {
            RegisterScreen(
                onRegistered = {
                    sessionViewModel.updateLoginState(true)
                    destination = AppDestination.InitialProfile
                },
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

        AppDestination.PublishMission -> {
            CreateMissionScreen(
                onBack = { destination = AppDestination.MainMenu },
                onDraftSaved = { destination = AppDestination.MainMenu },
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

        AppDestination.Settings -> {
            SettingsScreen(
                onBack = { destination = AppDestination.MainMenu },
                onSignedOut = {
                    sessionViewModel.updateLoginState(false)
                    loginNotice = SIGNED_OUT_NOTICE
                    destination = AppDestination.Login
                },
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

/** Mensaje mostrado en el login tras restablecer la contraseña. */
private const val PASSWORD_CHANGED_NOTICE = "Contraseña actualizada. Inicia sesión con tu nueva contraseña"
private const val SESSION_REQUIRED_NOTICE = "Debes iniciar sesión para ingresar a la aplicación"
private const val SIGNED_OUT_NOTICE = "Sesión cerrada correctamente"

private fun resolveInitialDestination(
    current: AppDestination?,
    startDestination: StartDestination,
): AppDestination? = current ?: startDestination.toAppDestination()

private fun shouldRedirectToLogin(
    isSessionResolved: Boolean,
    isLoggedIn: Boolean,
    destination: AppDestination?,
): Boolean {
    if (!isSessionResolved || isLoggedIn) return false
    return when (destination) {
        null,
        AppDestination.Login,
        AppDestination.Register,
        AppDestination.RequestCode,
        is AppDestination.ResetPassword,
        -> false

        else -> true
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
        MainMenuDestination.PUBLISH_MISSION -> AppDestination.PublishMission
        MainMenuDestination.MY_APPLICATIONS -> AppDestination.MyApplications
        MainMenuDestination.SETTINGS -> AppDestination.Settings
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
