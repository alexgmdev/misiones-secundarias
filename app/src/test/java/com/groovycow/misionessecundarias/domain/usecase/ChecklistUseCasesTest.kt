package com.groovycow.misionessecundarias.domain.usecase

import com.groovycow.misionessecundarias.domain.model.ChecklistItem
import com.groovycow.misionessecundarias.domain.model.checklistProgressPercent
import com.groovycow.misionessecundarias.domain.repository.ChecklistRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pruebas de HU-053: checklist de ejecución y cálculo del porcentaje de progreso. */
class ChecklistUseCasesTest {
    private val repository = mockk<ChecklistRepository>()

    @Test
    fun progressIsZeroWithoutItems() {
        assertEquals(0, checklistProgressPercent(emptyList()))
    }

    @Test
    fun progressIsZeroWhenNothingIsDone() {
        assertEquals(0, checklistProgressPercent(listOf(item("1", false), item("2", false))))
    }

    @Test
    fun progressIsFiftyWithHalfDone() {
        assertEquals(50, checklistProgressPercent(listOf(item("1", true), item("2", false))))
    }

    @Test
    fun progressRoundsToNearestInteger() {
        val oneOfThree = listOf(item("1", true), item("2", false), item("3", false))
        val twoOfThree = listOf(item("1", true), item("2", true), item("3", false))

        assertEquals(33, checklistProgressPercent(oneOfThree))
        assertEquals(67, checklistProgressPercent(twoOfThree))
    }

    @Test
    fun progressIsSixtyWithThreeOfFiveDone() {
        val items = listOf(true, true, true, false, false).mapIndexed { i, done -> item("$i", done) }

        assertEquals(60, checklistProgressPercent(items))
    }

    @Test
    fun progressIsHundredWhenEverythingIsDone() {
        assertEquals(100, checklistProgressPercent(listOf(item("1", true), item("2", true))))
    }

    @Test
    fun getChecklistReturnsRepositoryItems() =
        runTest {
            val items = listOf(item("1", true))
            coEvery { repository.getItems("m1") } returns items

            assertEquals(items, GetChecklistUseCase(repository)("m1").getOrNull())
        }

    @Test
    fun getChecklistWrapsFailure() =
        runTest {
            coEvery { repository.getItems("m1") } throws IllegalStateException("sin red")

            assertTrue(GetChecklistUseCase(repository)("m1").isFailure)
        }

    @Test
    fun addTrimsTitleBeforeSaving() =
        runTest {
            val saved = item("1", false, "Socializar")
            coEvery { repository.addItem("m1", "Socializar") } returns saved

            val result = AddChecklistItemUseCase(repository)("m1", "  Socializar  ")

            assertEquals(saved, result.getOrNull())
        }

    @Test
    fun addRejectsBlankTitle() =
        runTest {
            val result = AddChecklistItemUseCase(repository)("m1", "   ")

            assertTrue(result.exceptionOrNull() is ChecklistValidationException)
            coVerify(exactly = 0) { repository.addItem(any(), any()) }
        }

    @Test
    fun addRejectsTitleLongerThanLimit() =
        runTest {
            val tooLong = "a".repeat(AddChecklistItemUseCase.MAX_TITLE_LENGTH + 1)

            val result = AddChecklistItemUseCase(repository)("m1", tooLong)

            assertTrue(result.exceptionOrNull() is ChecklistValidationException)
            coVerify(exactly = 0) { repository.addItem(any(), any()) }
        }

    @Test
    fun addAcceptsTitleAtLimit() =
        runTest {
            val title = "a".repeat(AddChecklistItemUseCase.MAX_TITLE_LENGTH)
            coEvery { repository.addItem("m1", title) } returns item("1", false, title)

            assertTrue(AddChecklistItemUseCase(repository)("m1", title).isSuccess)
        }

    @Test
    fun setDoneDelegatesToRepository() =
        runTest {
            coEvery { repository.setDone("i1", true) } returns Unit

            val result = SetChecklistItemDoneUseCase(repository)("i1", true)

            assertTrue(result.isSuccess)
            coVerify(exactly = 1) { repository.setDone("i1", true) }
        }

    @Test
    fun deleteDelegatesToRepositoryAndWrapsFailure() =
        runTest {
            coEvery { repository.deleteItem("i1") } returns Unit
            coEvery { repository.deleteItem("i2") } throws IllegalStateException("sin permiso")

            assertTrue(DeleteChecklistItemUseCase(repository)("i1").isSuccess)
            assertTrue(DeleteChecklistItemUseCase(repository)("i2").isFailure)
        }

    private fun item(
        id: String,
        done: Boolean,
        title: String = "Tarea $id",
    ) = ChecklistItem(id = id, missionId = "m1", title = title, isDone = done)
}
