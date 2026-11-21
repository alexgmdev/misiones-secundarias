package com.groovycow.misionessecundarias.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import com.groovycow.misionessecundarias.ui.admin.AdminMenuViewModel
import com.groovycow.misionessecundarias.ui.admin.AdminPanelScreen
import com.groovycow.misionessecundarias.ui.applications.MyApplicationsScreen
import com.groovycow.misionessecundarias.ui.auth.SessionViewModel
import com.groovycow.misionessecundarias.ui.auth.StartDestination
import com.groovycow.misionessecundarias.ui.auth.WelcomeScreen
import com.groovycow.misionessecundarias.ui.auth.login.LoginScreen
import com.groovycow.misionessecundarias.ui.auth.recovery.RequestCodeScreen
import com.groovycow.misionessecundarias.ui.auth.recovery.ResetPasswordScreen
import com.groovycow.misionessecundarias.ui.auth.register.RegisterScreen
import com.groovycow.misionessecundarias.ui.missions.MissionDetailRoute
import com.groovycow.misionessecundarias.ui.missions.MissionListScreen
import com.groovycow.misionessecundarias.ui.missions.PostulantesScreen
import com.groovycow.misionessecundarias.ui.missions.create.CreateMissionScreen
import com.groovycow.misionessecundarias.ui.missions.edit.EditMissionScreen
import com.groovycow.misionessecundarias.ui.missions.live.LiveMissionPanelScreen
import com.groovycow.misionessecundarias.ui.profile.ProfileSummaryViewModel
import com.groovycow.misionessecundarias.ui.profile.components.ProfileAvatar
import com.groovycow.misionessecundarias.ui.profile.setup.InitialProfileScreen
import com.groovycow.misionessecundarias.ui.settings.ChangePasswordScreen
import com.groovycow.misionessecundarias.ui.settings.SettingsScreen
import org.koin.androidx.compose.koinViewModel

/** Destinos de la navegación manual de la aplicación. */
sealed interface AppDestination {
    /** Portada para elegir entre iniciar sesión y crear una cuenta. */
    data object Welcome : AppDestination

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

    /** Configuración inicial de perfil obligatoria, "Crea tu perfil" (HU-005). */
    data object InitialProfile : AppDestination

    /** Transitorio tras iniciar sesión: consulta si el perfil está completo (gate, D10). */
    data object CheckingProfile : AppDestination

    /** Menú principal (`menu-principal`). */
    data object MainMenu : AppDestination

    /** Lista de misiones. */
    data object MissionList : AppDestination

    /** Formulario para crear un borrador de misión (HU-016). */
    data object PublishMission : AppDestination

    /** Detalle de la misión seleccionada. */
    data object MissionDetail : AppDestination

    /** Edición de una misión seleccionada por su publicador. */
    data object EditMission : AppDestination

    /** Misiones creadas por el usuario, alcanzable desde el estado de las misiones. */
    data object MyMissions : AppDestination

    /** Gestión de postulantes de la misión seleccionada (HU-041, HU-044 y HU-045). */
    data object MissionApplicants : AppDestination

    /** Panel de ejecución en vivo con avatares de los solucionadores (HU-051). */
    data object LiveMissionPanel : AppDestination

    /** Postulaciones del usuario. */
    data object MyApplications : AppDestination

    /** Ajustes de la aplicación y cierre de sesión. */
    data object Settings : AppDestination

    /** Cambio de contraseña con la sesión activa, abierto desde Ajustes → Seguridad (HU-010). */
    data object ChangePassword : AppDestination

    /** Panel Administrativo exclusivo para moderadores y administradores. */
    data object AdminPanel : AppDestination

    /**
     * Pantalla de funcionalidad aún no implementada.
     *
     * @property menuDestination Opción del menú que la originó.
     */
    data class Placeholder(val menuDestination: MainMenuDestination) : AppDestination
}

/**
 * Raíz de la navegación: espera a conocer la sesión guardada, fija el destino inicial una sola vez
 * (Welcome sin sesión, MainMenu con sesión) y luego enruta entre destinos.
 */
@Composable
@Suppress("CyclomaticComplexMethod")
fun AppNavigation() {
    val sessionViewModel: SessionViewModel = koinViewModel()
    val startDestination by sessionViewModel.startDestination.collectAsState()
    val isLoggedIn by sessionViewModel.isLoggedIn.collectAsState()
    val isSessionResolved by sessionViewModel.isSessionResolved.collectAsState()
    var selectedMission by remember { mutableStateOf<Mission?>(null) }
    var detailOrigin by remember { mutableStateOf<AppDestination>(AppDestination.MissionList) }
    var destination by remember { mutableStateOf<AppDestination?>(null) }
    var loginNotice by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(startDestination) {
        destination = resolveInitialDestination(destination, startDestination)
    }

    LaunchedEffect(isSessionResolved, isLoggedIn, destination) {
        if (shouldRedirectToWelcome(isSessionResolved, isLoggedIn, destination)) {
            selectedMission = null
            loginNotice =
                if (destination == AppDestination.Settings) {
                    SIGNED_OUT_NOTICE
                } else {
                    SESSION_REQUIRED_NOTICE
                }
            destination = AppDestination.Welcome
        }
    }

    val currentDestination = destination
    if (currentDestination == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    BackHandler(enabled = currentDestination == AppDestination.Login || currentDestination == AppDestination.Register) {
        destination = AppDestination.Welcome
    }
    BackHandler(enabled = currentDestination == AppDestination.ChangePassword) {
        destination = AppDestination.Settings
    }
    BackHandler(enabled = currentDestination == AppDestination.EditMission) {
        destination = AppDestination.MissionDetail
    }
    BackHandler(enabled = currentDestination == AppDestination.RequestCode) {
        destination = AppDestination.Login
    }

    when (currentDestination) {
        AppDestination.Welcome -> {
            WelcomeScreen(
                onLogin = {
                    loginNotice = null
                    destination = AppDestination.Login
                },
                onRegister = { destination = AppDestination.Register },
                notice = loginNotice,
                onNoticeDismissed = { loginNotice = null },
            )
        }

        AppDestination.Login -> {
            LoginScreen(
                onSignedIn = {
                    sessionViewModel.updateLoginState(true)
                    destination = AppDestination.CheckingProfile
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
                onBack = { destination = AppDestination.Welcome },
            )
        }

        AppDestination.CheckingProfile -> {
            LaunchedEffect(Unit) {
                sessionViewModel.checkProfileGate { isComplete ->
                    destination = if (isComplete) AppDestination.MainMenu else AppDestination.InitialProfile
                }
            }
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        AppDestination.InitialProfile -> {
            InitialProfileScreen(
                // Punto único de navegación tras guardar el perfil inicial.
                onProfileCompleted = { destination = ProfileFlowDestination.Onboarding(fromSettings = false) },
            )
        }

        is ProfileFlowDestination -> {
            ProfileFlowRoute(
                destination = currentDestination,
                onBack = { destination = AppDestination.MainMenu },
                onEditProfile = { destination = ProfileFlowDestination.EditProfile },
                onOpenHistory = { destination = ProfileFlowDestination.MissionHistory },
                onProfileEdited = { destination = ProfileFlowDestination.MyProfile },
                onOnboardingFinished = { fromSettings ->
                    destination = if (fromSettings) AppDestination.Settings else AppDestination.MainMenu
                },
            )
        }

        AppDestination.MainMenu ->
            MainMenuDestinationScreen { menuDestination -> destination = menuDestination.toAppDestination() }

        AppDestination.MissionList -> {
            MissionListScreen(
                onMissionClick = { mission ->
                    selectedMission = mission
                    detailOrigin = AppDestination.MissionList
                    destination = AppDestination.MissionDetail
                },
                onMyApplicationsClick = {
                    destination = AppDestination.MyApplications
                },
                onBack = { destination = AppDestination.MainMenu },
            )
        }

        AppDestination.MyMissions -> {
            MissionListScreen(
                onMissionClick = { mission ->
                    selectedMission = mission
                    detailOrigin = AppDestination.MyMissions
                    destination = AppDestination.MissionDetail
                },
                onMyApplicationsClick = {
                    destination = AppDestination.MyApplications
                },
                onBack = { destination = AppDestination.MyApplications },
                startWithMyMissions = true,
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
                    destination = detailOrigin
                },
                onManageApplicants = { destination = AppDestination.MissionApplicants },
                onEditMission = { destination = AppDestination.EditMission },
            )
        }

        AppDestination.EditMission ->
            EditMissionDestination(
                mission = selectedMission,
                onBack = { destination = AppDestination.MissionDetail },
                onMissingMission = { destination = AppDestination.MissionList },
                onSaved = { updatedMission ->
                    selectedMission = updatedMission
                    destination = AppDestination.MissionDetail
                },
            )

        AppDestination.MissionApplicants -> {
            val mission = selectedMission
            if (mission == null) {
                destination = AppDestination.MissionList
            } else {
                PostulantesScreen(
                    missionId = mission.id,
                    missionTitle = mission.title,
                    onBack = { destination = AppDestination.MissionDetail },
                    isMissionInProgress = mission.status == "En progreso",
                    isMissionCompleted = mission.status == "Completada",
                    onOpenLivePanel = { destination = AppDestination.LiveMissionPanel },
                    onMissionStarted = { started ->
                        selectedMission = started
                        destination = AppDestination.LiveMissionPanel
                    },
                )
            }
        }

        AppDestination.LiveMissionPanel -> {
            val mission = selectedMission
            if (mission == null) {
                destination = AppDestination.MissionList
            } else {
                LiveMissionPanelScreen(
                    missionId = mission.id,
                    missionTitle = mission.title,
                    onBack = { destination = AppDestination.MissionApplicants },
                    onMissionCompleted = { completed ->
                        selectedMission = completed
                        destination = AppDestination.MissionApplicants
                    },
                )
            }
        }

        AppDestination.MyApplications -> {
            MyApplicationsScreen(
                onBack = { destination = AppDestination.MainMenu },
                onExploreMissions = { destination = AppDestination.MissionList },
                onMyMissions = { destination = AppDestination.MyMissions },
            )
        }

        AppDestination.Settings -> {
            SettingsScreen(
                onBack = { destination = AppDestination.MainMenu },
                onChangePassword = { destination = AppDestination.ChangePassword },
                onOpenGuide = { destination = ProfileFlowDestination.Onboarding(fromSettings = true) },
                onSignedOut = {
                    sessionViewModel.updateLoginState(false)
                    loginNotice = SIGNED_OUT_NOTICE
                    destination = AppDestination.Welcome
                },
            )
        }

        AppDestination.ChangePassword -> {
            ChangePasswordScreen(onBack = { destination = AppDestination.Settings })
        }

        AppDestination.AdminPanel -> {
            AdminPanelScreen(
                onBack = { destination = AppDestination.MainMenu },
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

@Composable
private fun MainMenuDestinationScreen(onNavigate: (MainMenuDestination) -> Unit) {
    val profileSummaryViewModel: ProfileSummaryViewModel = koinViewModel()
    val summary by profileSummaryViewModel.uiState.collectAsState()
    val adminMenuViewModel: AdminMenuViewModel = koinViewModel()
    val canShowAdminPanel by adminMenuViewModel.canShowAdminPanel.collectAsState()
    LaunchedEffect(Unit) { profileSummaryViewModel.refresh() }
    LaunchedEffect(adminMenuViewModel) { adminMenuViewModel.refreshAccess() }
    DisposableEffect(adminMenuViewModel) {
        onDispose(adminMenuViewModel::hideAdminPanelEntry)
    }
    MainMenuScreen(
        onNavigate = onNavigate,
        username = summary.nickname,
        showAdminPanel = canShowAdminPanel,
        avatar =
            summary.avatarId?.let { avatarId ->
                { ProfileAvatar(avatar = avatarId, size = 148.dp) }
            },
    )
}

/** Mensaje mostrado en el login tras restablecer la contraseña. */
private const val PASSWORD_CHANGED_NOTICE = "Contraseña actualizada. Inicia sesión con tu nueva contraseña"
private const val SESSION_REQUIRED_NOTICE = "Debes iniciar sesión para ingresar a la aplicación"
private const val SIGNED_OUT_NOTICE = "Sesión cerrada correctamente"

private fun resolveInitialDestination(
    current: AppDestination?,
    startDestination: StartDestination,
): AppDestination? = current ?: startDestination.toAppDestination()

private fun shouldRedirectToWelcome(
    isSessionResolved: Boolean,
    isLoggedIn: Boolean,
    destination: AppDestination?,
): Boolean {
    if (!isSessionResolved || isLoggedIn) return false
    return when (destination) {
        null,
        AppDestination.Welcome,
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
        StartDestination.LOGIN -> AppDestination.Welcome
        StartDestination.MAIN_MENU -> AppDestination.MainMenu
        StartDestination.PROFILE_SETUP -> AppDestination.InitialProfile
    }

/** Traduce una opción del menú principal al destino de navegación correspondiente. */
private fun MainMenuDestination.toAppDestination(): AppDestination =
    when (this) {
        MainMenuDestination.SEARCH_MISSIONS -> AppDestination.MissionList
        MainMenuDestination.PUBLISH_MISSION -> AppDestination.PublishMission
        MainMenuDestination.MY_APPLICATIONS -> AppDestination.MyApplications
        MainMenuDestination.SETTINGS -> AppDestination.Settings
        MainMenuDestination.PROFILE -> ProfileFlowDestination.MyProfile
        MainMenuDestination.AVATAR -> ProfileFlowDestination.EditProfile
        MainMenuDestination.ADMIN_PANEL -> AppDestination.AdminPanel
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
    onManageApplicants: () -> Unit,
    onEditMission: () -> Unit,
) {
    if (mission == null) {
        onMissingMission()
        return
    }
    MissionDetailRoute(
        missionId = mission.id,
        publisherId = mission.publisherId,
        missionLocationWkt = mission.location,
        missionTitle = mission.title,
        missionDescription = mission.description,
        missionDateTime = mission.dateTime,
        maxParticipants = mission.maxParticipants,
        modality = mission.modality,
        category = mission.category,
        requirements = mission.requirements,
        missionReward = mission.reward,
        missionStatus = mission.status,
        publisherName = mission.publisherProfile?.fullName ?: "Usuario Desconocido",
        approximateZone = "Barrio San Fernando, Cali",
        onBack = onBack,
        onManageApplicants = onManageApplicants,
        onEditMission = onEditMission,
    )
}

@Composable
private fun EditMissionDestination(
    mission: Mission?,
    onBack: () -> Unit,
    onMissingMission: () -> Unit,
    onSaved: (Mission) -> Unit,
) {
    if (mission == null) {
        onMissingMission()
        return
    }
    EditMissionScreen(
        mission = mission,
        onBack = onBack,
        onSaved = onSaved,
    )
}
