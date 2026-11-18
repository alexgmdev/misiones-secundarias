package com.groovycow.misionessecundarias.data.repository

import com.groovycow.misionessecundarias.domain.model.ChecklistItem
import com.groovycow.misionessecundarias.domain.repository.ChecklistRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Checklist de ejecución sobre Supabase; las políticas RLS limitan el acceso al publicador. */
class ChecklistRepositoryImpl(
    private val supabase: SupabaseClient,
) : ChecklistRepository {
    override suspend fun getItems(missionId: String): List<ChecklistItem> =
        withContext(Dispatchers.IO) {
            supabase.from(TABLE)
                .select {
                    filter { eq("mission_id", missionId) }
                    order("created_at", Order.ASCENDING)
                }
                .decodeList<ChecklistItem>()
        }

    override suspend fun addItem(
        missionId: String,
        title: String,
    ): ChecklistItem =
        withContext(Dispatchers.IO) {
            supabase.from(TABLE)
                .insert(NewChecklistItem(missionId = missionId, title = title)) { select() }
                .decodeSingle<ChecklistItem>()
        }

    override suspend fun setDone(
        itemId: String,
        isDone: Boolean,
    ) = withContext(Dispatchers.IO) {
        supabase.from(TABLE).update({ set("is_done", isDone) }) {
            filter { eq("id", itemId) }
        }
        Unit
    }

    override suspend fun deleteItem(itemId: String) =
        withContext(Dispatchers.IO) {
            supabase.from(TABLE).delete { filter { eq("id", itemId) } }
            Unit
        }

    @Serializable
    private data class NewChecklistItem(
        @SerialName("mission_id") val missionId: String,
        val title: String,
    )

    private companion object {
        const val TABLE = "mission_checklist_items"
    }
}
