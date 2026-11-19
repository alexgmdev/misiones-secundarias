package com.groovycow.misionessecundarias.ui.onboarding

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OnboardingFlowTest {
    @Test
    fun `tiene cuatro paginas con titulos esperados`() {
        assertEquals(4, ONBOARDING_PAGES.size)
        assertEquals(
            listOf("Misiones de campo", "Misiones remotas", "Postulaciones", "Moderación y convivencia"),
            ONBOARDING_PAGES.map { it.title },
        )
    }

    @Test
    fun `primera pagina no es la ultima y muestra Siguiente`() {
        assertFalse(OnboardingFlow.isLastPage(0))
        assertEquals("Siguiente", OnboardingFlow.primaryLabel(0))
        assertTrue(OnboardingFlow.showSkip(0))
    }

    @Test
    fun `ultima pagina muestra Empezar y oculta Omitir`() {
        assertTrue(OnboardingFlow.isLastPage(3))
        assertEquals("Empezar", OnboardingFlow.primaryLabel(3))
        assertFalse(OnboardingFlow.showSkip(3))
    }

    @Test
    fun `siguiente avanza y se detiene en la ultima`() {
        assertEquals(1, OnboardingFlow.nextPage(0))
        assertEquals(3, OnboardingFlow.nextPage(3))
    }

    @Test
    fun `cierre va al menu o a Ajustes segun origen`() {
        assertEquals(OnboardingExit.MainMenu, OnboardingFlow.exitFor(fromSettings = false))
        assertEquals(OnboardingExit.Settings, OnboardingFlow.exitFor(fromSettings = true))
    }
}
