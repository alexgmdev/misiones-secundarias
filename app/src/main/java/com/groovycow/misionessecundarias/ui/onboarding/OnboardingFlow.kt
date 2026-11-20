package com.groovycow.misionessecundarias.ui.onboarding

import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Página informativa del onboarding.
 *
 * @property title Título de la página.
 * @property body Texto explicativo.
 * @property icon Ícono principal.
 */
internal data class OnboardingPage(
    val title: String,
    val body: String,
    val icon: ImageVector,
)

/** Destinos posibles al cerrar el onboarding. */
internal enum class OnboardingExit {
    /** Menú principal (primer ingreso). */
    MainMenu,

    /** Pantalla de Ajustes (reapertura desde Ayuda). */
    Settings,
}

/** Lógica pura de navegación del onboarding (HU-013), sin estado de Compose. */
internal object OnboardingFlow {
    private val lastIndex get() = ONBOARDING_PAGES.lastIndex

    /** Indica si [index] es la última página. */
    fun isLastPage(index: Int): Boolean = index >= lastIndex

    /** Etiqueta del botón principal: "Empezar" en la última página, "Siguiente" en las demás. */
    fun primaryLabel(index: Int): String = if (isLastPage(index)) "Empezar" else "Siguiente"

    /** "Omitir" se ofrece en todas las páginas excepto la última. */
    fun showSkip(index: Int): Boolean = !isLastPage(index)

    /** Página siguiente a [index], sin pasar de la última. */
    fun nextPage(index: Int): Int = (index + 1).coerceAtMost(lastIndex)

    /** Destino al terminar u omitir según el origen del onboarding. */
    fun exitFor(fromSettings: Boolean): OnboardingExit = if (fromSettings) OnboardingExit.Settings else OnboardingExit.MainMenu
}
