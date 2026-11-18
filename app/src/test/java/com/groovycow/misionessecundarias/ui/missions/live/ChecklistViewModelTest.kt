package com.groovycow.misionessecundarias.ui.missions.live

import com.groovycow.misionessecundarias.domain.model.ChecklistItem
import com.groovycow.misionessecundarias.domain.repository.ChecklistRepository
import com.groovycow.misionessecundarias.domain.usecase.AddChecklistItemUseCase
import com.groovycow.misionessecundarias.domain.usecase.DeleteChecklistItemUseCase
import com.groovycow.misionessecundarias.domain.usecase.GetChecklistUseCase
import com.groovycow.misionessecundarias.domain.usecase.SetChecklistItemDoneUseCase
import com.groovycow.misionessecundarias.testutil.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Pruebas de HU-053: estado del checklist y porcentaje de progreso en el panel. */
class ChecklistViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = mockk<ChecklistRepository>()
    private val first = item("1", false)
    private val second = item("2", false)

    private fun viewModel() =
        ChecklistViewModel(
            GetChecklistUseCase(repository),
            AddChecklistItemUseCase(repository),
            SetChecklistItemDoneUseCase(repository),
            DeleteChecklistItemUseCase(repository),
        )

    private fun loaded(vararg items: ChecklistItem): ChecklistViewModel {
        coEvery { repository.getItems("m1") } returns items.toList()
        return viewModel().also { it.load("m1") }
    }

    @Test
    fun loadExposesItemsAndZeroProgress() {
        val viewModel = loaded(first, second)

        assertEquals(listOf(first, second), viewModel.uiState.value.items)
        assertEquals(0, viewModel.uiState.value.progressPercent)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun loadFailureShowsError() {
        coEvery { repository.getItems("m1") } throws IllegalStateException("sin red")
        val viewModel = viewModel()

        viewModel.load("m1")

        assertNotNull(viewModel.uiState.value.error)
        assertTrue(viewModel.uiState.value.items.isEmpty())
    }

    @Test
    fun togglingItemUpdatesProgress() {
        val viewModel = loaded(first, second)
        coEvery { repository.setDone("1", true) } returns Unit

        viewModel.toggle(first)

        assertEquals(50, viewModel.uiState.value.progressPercent)
        assertTrue(viewModel.uiState.value.items.first { it.id == "1" }.isDone)
    }

    @Test
    fun uncheckingItemLowersProgress() {
        val done = item("1", true)
        val viewModel = loaded(done, second)
        coEvery { repository.setDone("1", false) } returns Unit

        viewModel.toggle(done)

        assertEquals(0, viewModel.uiState.value.progressPercent)
    }

    @Test
    fun failedToggleKeepsItemAndShowsError() {
        val viewModel = loaded(first, second)
        coEvery { repository.setDone("1", true) } throws IllegalStateException("sin permiso")

        viewModel.toggle(first)

        assertFalse(viewModel.uiState.value.items.first { it.id == "1" }.isDone)
        assertNotNull(viewModel.uiState.value.actionError)
        assertFalse(viewModel.uiState.value.isBusy)
    }

    @Test
    fun addingItemAppendsAndCallsCallback() {
        val viewModel = loaded(first)
        val added = item("3", false, "Hidratarse")
        coEvery { repository.addItem("m1", "Hidratarse") } returns added
        var callbackCalled = false

        viewModel.add("m1", "Hidratarse") { callbackCalled = true }

        assertEquals(listOf(first, added), viewModel.uiState.value.items)
        assertTrue(callbackCalled)
    }

    @Test
    fun addingBlankItemShowsValidationMessageAndSkipsCallback() {
        val viewModel = loaded(first)
        var callbackCalled = false

        viewModel.add("m1", "  ") { callbackCalled = true }

        assertEquals("Escribe el nombre de la tarea.", viewModel.uiState.value.actionError)
        assertFalse(callbackCalled)
        assertEquals(listOf(first), viewModel.uiState.value.items)
    }

    @Test
    fun deletingItemRemovesItAndRecalculatesProgress() {
        val done = item("1", true)
        val viewModel = loaded(done, second)
        coEvery { repository.deleteItem("2") } returns Unit

        viewModel.delete(second)

        assertEquals(listOf(done), viewModel.uiState.value.items)
        assertEquals(100, viewModel.uiState.value.progressPercent)
    }

    @Test
    fun refreshPicksUpExternalChanges() {
        val viewModel = loaded(first)
        coEvery { repository.getItems("m1") } returns listOf(item("1", true))

        viewModel.refresh("m1")

        assertEquals(100, viewModel.uiState.value.progressPercent)
    }

    @Test
    fun failedRefreshKeepsVisibleItemsWithoutError() {
        val viewModel = loaded(first)
        coEvery { repository.getItems("m1") } throws IllegalStateException("sin red")

        viewModel.refresh("m1")

        assertEquals(listOf(first), viewModel.uiState.value.items)
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun dismissActionErrorClearsMessage() {
        val viewModel = loaded(first)
        viewModel.add("m1", "")

        viewModel.dismissActionError()

        assertNull(viewModel.uiState.value.actionError)
    }

    private fun item(
        id: String,
        done: Boolean,
        title: String = "Tarea $id",
    ) = ChecklistItem(id = id, missionId = "m1", title = title, isDone = done)
}
