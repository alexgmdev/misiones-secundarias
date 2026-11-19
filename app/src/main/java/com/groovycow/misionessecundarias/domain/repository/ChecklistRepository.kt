package com.groovycow.misionessecundarias.domain.repository

import com.groovycow.misionessecundarias.domain.model.ChecklistItem

/** Acceso al checklist de ejecución de una misión propia (HU-053). */
interface ChecklistRepository {
    suspend fun getItems(missionId: String): List<ChecklistItem>

    suspend fun addItem(
        missionId: String,
        title: String,
    ): ChecklistItem

    suspend fun setDone(
        itemId: String,
        isDone: Boolean,
    )

    suspend fun deleteItem(itemId: String)
}
