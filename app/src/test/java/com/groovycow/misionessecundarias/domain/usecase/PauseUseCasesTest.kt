package com.groovycow.misionessecundarias.domain.usecase

import com.groovycow.misionessecundarias.domain.model.PauseKind
import com.groovycow.misionessecundarias.domain.model.pauseNotice
import com.groovycow.misionessecundarias.domain.repository.PauseRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pruebas de HU-054 y HU-055: pausas de la misión y aviso al postulante. */
class PauseUseCasesTest {
    private val repository = mockk<PauseRepository>()

    @Test
    fun pauseKindsMapToDatabaseValuesAndBack() {
        assertEquals("socializacion", PauseKind.SOCIALIZATION.databaseValue)
        assertEquals("hidratacion", PauseKind.HYDRATION.databaseValue)
        assertEquals("pausa_breve", PauseKind.SHORT_BREAK.databaseValue)
        PauseKind.entries.forEach { assertEquals(it, PauseKind.fromDatabaseValue(it.databaseValue)) }
        assertNull(PauseKind.fromDatabaseValue("desconocida"))
    }

    @Test
    fun noNoticeWhenNothingIsActive() {
        assertNull(pauseNotice(emptySet()))
    }

    @Test
    fun noticeNamesSingleActivePause() {
        assertEquals("Misión en pausa: Hidratación", pauseNotice(setOf(PauseKind.HYDRATION)))
    }

    @Test
    fun noticeListsSeveralPausesInFixedOrder() {
        val notice = pauseNotice(setOf(PauseKind.SHORT_BREAK, PauseKind.SOCIALIZATION))

        assertEquals("Misión en pausa: Socialización, Pausa breve", notice)
    }

    @Test
    fun getActivePausesReturnsRepositoryMap() =
        runTest {
            val expected = mapOf("m1" to setOf(PauseKind.HYDRATION))
            coEvery { repository.getActivePauses(listOf("m1", "m2")) } returns expected

            val result = GetActivePausesUseCase(repository)(listOf("m1", "m2"))

            assertEquals(expected, result.getOrNull())
        }

    @Test
    fun getActivePausesSkipsRepositoryWhenThereAreNoMissions() =
        runTest {
            val result = GetActivePausesUseCase(repository)(emptyList())

            assertTrue(result.getOrNull().orEmpty().isEmpty())
            coVerify(exactly = 0) { repository.getActivePauses(any()) }
        }

    @Test
    fun getActivePausesWrapsFailure() =
        runTest {
            coEvery { repository.getActivePauses(any()) } throws IllegalStateException("sin red")

            assertTrue(GetActivePausesUseCase(repository)(listOf("m1")).isFailure)
        }

    @Test
    fun setPauseDelegatesActivationAndDeactivation() =
        runTest {
            coEvery { repository.setPause("m1", PauseKind.HYDRATION, any()) } returns Unit

            assertTrue(SetMissionPauseUseCase(repository)("m1", PauseKind.HYDRATION, true).isSuccess)
            assertTrue(SetMissionPauseUseCase(repository)("m1", PauseKind.HYDRATION, false).isSuccess)
            coVerify(exactly = 1) { repository.setPause("m1", PauseKind.HYDRATION, true) }
            coVerify(exactly = 1) { repository.setPause("m1", PauseKind.HYDRATION, false) }
        }

    @Test
    fun setPauseWrapsFailure() =
        runTest {
            coEvery { repository.setPause(any(), any(), any()) } throws IllegalStateException("sin permiso")

            assertTrue(SetMissionPauseUseCase(repository)("m1", PauseKind.SOCIALIZATION, true).isFailure)
        }
}
