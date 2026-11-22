package com.groovycow.misionessecundarias.ui.navigation

import androidx.compose.runtime.Composable
import com.groovycow.misionessecundarias.ui.onboarding.OnboardingScreen
import com.groovycow.misionessecundarias.ui.profile.edit.EditProfileScreen
import com.groovycow.misionessecundarias.ui.profile.history.MissionHistoryScreen
import com.groovycow.misionessecundarias.ui.profile.me.MyProfileScreen

/**
 * Destinos del flujo de perfil y configuración (D12), enrutados desde una sola rama de [AppNavigation].
 * `ChangePassword` no forma parte de este flujo: permanece como [AppDestination.ChangePassword].
 */
sealed interface ProfileFlowDestination : AppDestination {
    /** Mi perfil (HU-006). */
    data object MyProfile : ProfileFlowDestination

    /** Edición del perfil (HU-008 y HU-009). */
    data object EditProfile : ProfileFlowDestination

    /** Historial de misiones (HU-011). */
    data object MissionHistory : ProfileFlowDestination

    /**
     * Onboarding de bienvenida (HU-013).
     *
     * @property fromSettings `true` si se abrió desde Ajustes (al cerrar vuelve a Ajustes en lugar del menú).
     */
    data class Onboarding(val fromSettings: Boolean) : ProfileFlowDestination
}

/**
 * Enruta los destinos de [ProfileFlowDestination]. Las pantallas se irán conectando en los PR siguientes;
 * mientras tanto muestran el marcador de funcionalidad pendiente.
 *
 * @param destination Destino del flujo de perfil a mostrar.
 * @param onBack Acción al volver desde un marcador.
 * @param onOnboardingFinished Acción al terminar u omitir el onboarding; recibe `fromSettings`.
 * @param onEditProfile Acción del botón "Editar Perfil" de Mi perfil.
 * @param onOpenHistory Acción del botón "Histórico de Misiones" de Mi perfil.
 * @param onHistoryBack Acción al volver desde el historial (vuelve a Mi perfil).
 * @param onProfileEdited Acción al guardar (o cancelar) la edición del perfil: vuelve a Mi perfil.
 */
@Composable
internal fun ProfileFlowRoute(
    destination: ProfileFlowDestination,
    onBack: () -> Unit,
    onOnboardingFinished: (fromSettings: Boolean) -> Unit = {},
    onEditProfile: () -> Unit = {},
    onOpenHistory: () -> Unit = {},
    onProfileEdited: () -> Unit = {},
    onHistoryBack: () -> Unit = onBack,
) {
    when (destination) {
        is ProfileFlowDestination.Onboarding ->
            OnboardingScreen(fromSettings = destination.fromSettings, onFinish = onOnboardingFinished)
        ProfileFlowDestination.MyProfile ->
            MyProfileScreen(onBack = onBack, onEditProfile = onEditProfile, onOpenHistory = onOpenHistory)
        ProfileFlowDestination.EditProfile ->
            EditProfileScreen(onBack = onProfileEdited, onSaved = onProfileEdited)
        ProfileFlowDestination.MissionHistory ->
            MissionHistoryScreen(onBack = onHistoryBack)
    }
}
