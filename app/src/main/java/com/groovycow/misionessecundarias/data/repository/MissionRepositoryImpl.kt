package com.groovycow.misionessecundarias.data.repository

import com.groovycow.misionessecundarias.domain.model.Mission
import com.groovycow.misionessecundarias.domain.repository.MissionRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MissionRepositoryImpl(private val supabase: SupabaseClient) : MissionRepository {

    override suspend fun getPublishedMissions(): List<Mission> = withContext(Dispatchers.IO) {
        supabase.from("missions")
            .select(columns = Columns.raw("*, profiles(full_name)")) { // <-- JOIN con profiles
                filter {
                    neq("status", "Borrador")
                }
            }.decodeList<Mission>()
    }

    override suspend fun getMissionById(id: String): Mission? = withContext(Dispatchers.IO) {
        supabase.from("missions")
            .select(columns = Columns.raw("*, profiles(full_name)")) { // <-- JOIN con profiles
                filter {
                    eq("id", id)
                }
            }.decodeSingleOrNull<Mission>()
    }
}