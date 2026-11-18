package com.groovycow.misionessecundarias.data.repository

import com.groovycow.misionessecundarias.domain.model.PauseKind
import com.groovycow.misionessecundarias.domain.repository.PauseRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Pausas sobre Supabase; las políticas RLS limitan la escritura al publicador y la lectura a los aceptados. */
class PauseRepositoryImpl(
    private val supabase: SupabaseClient,
) : PauseRepository {
    override suspend fun getActivePauses(missionIds: List<String>): Map<String, Set<PauseKind>> =
        withContext(Dispatchers.IO) {
            supabase.from(TABLE)
                .select {
                    filter {
                        isIn("mission_id", missionIds)
                        eq("is_active", true)
                    }
                }
                .decodeList<PauseRow>()
                .mapNotNull { row -> PauseKind.fromDatabaseValue(row.kind)?.let { row.missionId to it } }
                .groupBy({ it.first }, { it.second })
                .mapValues { (_, kinds) -> kinds.toSet() }
        }

    override suspend fun setPause(
        missionId: String,
        kind: PauseKind,
        isActive: Boolean,
    ) = withContext(Dispatchers.IO) {
        // Se actualiza solo `is_active` (único permiso de UPDATE); si la fila no existe, se inserta.
        val updated =
            supabase.from(TABLE)
                .update({ set("is_active", isActive) }) {
                    filter {
                        eq("mission_id", missionId)
                        eq("kind", kind.databaseValue)
                    }
                    select()
                }
                .decodeList<PauseRow>()
        if (updated.isEmpty()) {
            supabase.from(TABLE).insert(
                PauseRow(missionId = missionId, kind = kind.databaseValue, isActive = isActive),
            )
        }
        Unit
    }

    @Serializable
    private data class PauseRow(
        @SerialName("mission_id") val missionId: String,
        val kind: String,
        @SerialName("is_active") val isActive: Boolean = true,
    )

    private companion object {
        const val TABLE = "mission_pauses"
    }
}
