package com.groovycow.misionessecundarias.ui.profile.history

import com.groovycow.misionessecundarias.domain.model.HistoryRole
import com.groovycow.misionessecundarias.domain.model.MissionHistoryEntry
import com.groovycow.misionessecundarias.domain.model.MissionLogbookSections
import com.groovycow.misionessecundarias.domain.model.ParticipantWords
import com.groovycow.misionessecundarias.domain.model.ProfileError
import com.groovycow.misionessecundarias.domain.usecase.GetMissionHistoryUseCase
import com.groovycow.misionessecundarias.domain.usecase.GetMissionLogbookSectionsUseCase
import com.groovycow.misionessecundarias.testutil.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException

class MissionHistoryViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val getHistory = mockk<GetMissionHistoryUseCase>()
    private val getLogbooks = mockk<GetMissionLogbookSectionsUseCase>()

    private val entry = MissionHistoryEntry("m1", "Limpieza", "2026-03-01T10:00:00Z", "Completada", HistoryRole.PUBLISHER, "d", "r")

    private fun vm(): MissionHistoryViewModel = MissionHistoryViewModel(getHistory, getLogbooks)

    @Test
    fun loadShowsEntries() {
        coEvery { getHistory() } returns Result.success(listOf(entry))
        val vm = vm()

        vm.load()

        val state = vm.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.error)
        assertEquals(listOf(entry), state.entries)
        assertNull(state.expandedId)
    }

    @Test
    fun emptyHistoryHasNoEntriesNorError() {
        coEvery { getHistory() } returns Result.success(emptyList())
        val vm = vm()

        vm.load()

        assertTrue(vm.uiState.value.entries.isEmpty())
        assertNull(vm.uiState.value.error)
    }

    @Test
    fun toggleExpandsLoadsLogbookAndCollapses() {
        coEvery { getHistory() } returns Result.success(listOf(entry))
        coEvery { getLogbooks("m1") } returns
            Result.success(MissionLogbookSections("Oficial", listOf(ParticipantWords("Luis", "Genial"))))
        val vm = vm()
        vm.load()

        vm.toggle("m1")

        assertEquals("m1", vm.uiState.value.expandedId)
        assertEquals("Oficial", vm.uiState.value.logbooks["m1"]?.sections?.official)

        vm.toggle("m1")
        assertNull(vm.uiState.value.expandedId)

        vm.toggle("m1")
        coVerify(exactly = 1) { getLogbooks("m1") }
    }

    @Test
    fun logbookFailureDoesNotBreakTimelineAndAllowsRetryOnReExpand() {
        coEvery { getHistory() } returns Result.success(listOf(entry))
        coEvery { getLogbooks("m1") } returns Result.failure(IOException("x"))
        val vm = vm()
        vm.load()

        vm.toggle("m1")

        val logbook = vm.uiState.value.logbooks["m1"]
        assertTrue(logbook?.failed == true)
        assertNull(logbook?.sections)
        assertEquals(listOf(entry), vm.uiState.value.entries)
        assertNull(vm.uiState.value.error)

        coEvery { getLogbooks("m1") } returns Result.success(MissionLogbookSections(null, emptyList()))
        vm.toggle("m1")
        vm.toggle("m1")
        assertTrue(vm.uiState.value.logbooks["m1"]?.failed == false)
    }

    @Test
    fun loadErrorThenRetry() {
        coEvery { getHistory() } returns Result.failure(IllegalStateException("x"))
        val vm = vm()
        vm.load()
        assertEquals(ProfileError.ServerError, vm.uiState.value.error)
        assertTrue(vm.uiState.value.entries.isEmpty())

        coEvery { getHistory() } returns Result.success(listOf(entry))
        vm.retry()

        assertNull(vm.uiState.value.error)
        assertEquals(1, vm.uiState.value.entries.size)
    }

    @Test
    fun noConnectionError() {
        coEvery { getHistory() } returns Result.failure(IOException("sin red"))
        val vm = vm()

        vm.load()

        assertEquals(ProfileError.NoConnection, vm.uiState.value.error)
    }
}
