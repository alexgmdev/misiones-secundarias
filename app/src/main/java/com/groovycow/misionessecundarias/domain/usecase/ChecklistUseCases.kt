package com.groovycow.misionessecundarias.domain.usecase

import com.groovycow.misionessecundarias.domain.model.ChecklistItem
import com.groovycow.misionessecundarias.domain.repository.ChecklistRepository
import kotlin.coroutines.cancellation.CancellationException

/** Error de negocio con un mensaje listo para mostrar al publicador. */
class ChecklistValidationException(message: String) : IllegalArgumentException(message)

private suspend fun <T> checklistCatching(block: suspend () -> T): Result<T> =
    try {
        Result.success(block())
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (exception: Exception) {
        Result.failure(exception)
    }

/** Obtiene las tareas del checklist de una misión, de la más antigua a la más reciente. */
class GetChecklistUseCase(private val repository: ChecklistRepository) {
    suspend operator fun invoke(missionId: String): Result<List<ChecklistItem>> = checklistCatching { repository.getItems(missionId) }
}

/** Agrega una tarea al checklist; el título se recorta y debe tener entre 1 y 80 caracteres. */
class AddChecklistItemUseCase(private val repository: ChecklistRepository) {
    suspend operator fun invoke(
        missionId: String,
        title: String,
    ): Result<ChecklistItem> =
        checklistCatching {
            val clean = title.trim()
            if (clean.isEmpty()) throw ChecklistValidationException("Escribe el nombre de la tarea.")
            if (clean.length > MAX_TITLE_LENGTH) {
                throw ChecklistValidationException("La tarea admite máximo $MAX_TITLE_LENGTH caracteres.")
            }
            repository.addItem(missionId, clean)
        }

    companion object {
        const val MAX_TITLE_LENGTH = 80
    }
}

/** Marca o desmarca una tarea como cumplida. */
class SetChecklistItemDoneUseCase(private val repository: ChecklistRepository) {
    suspend operator fun invoke(
        itemId: String,
        isDone: Boolean,
    ): Result<Unit> = checklistCatching { repository.setDone(itemId, isDone) }
}

/** Elimina una tarea del checklist. */
class DeleteChecklistItemUseCase(private val repository: ChecklistRepository) {
    suspend operator fun invoke(itemId: String): Result<Unit> = checklistCatching { repository.deleteItem(itemId) }
}
