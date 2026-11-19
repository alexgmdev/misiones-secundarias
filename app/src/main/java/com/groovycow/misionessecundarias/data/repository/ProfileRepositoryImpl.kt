package com.groovycow.misionessecundarias.data.repository

import com.groovycow.misionessecundarias.domain.model.AvatarId
import com.groovycow.misionessecundarias.domain.model.Gender
import com.groovycow.misionessecundarias.domain.model.Profile
import com.groovycow.misionessecundarias.domain.model.ProfileError
import com.groovycow.misionessecundarias.domain.model.ProfileException
import com.groovycow.misionessecundarias.domain.model.ProfileUpdate
import com.groovycow.misionessecundarias.domain.model.PublicProfile
import com.groovycow.misionessecundarias.domain.repository.ProfileRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.rpc
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.coroutines.cancellation.CancellationException

/**
 * Implementación de [ProfileRepository] sobre Postgrest y Storage de Supabase.
 *
 * @property supabase Cliente de Supabase.
 * @property errorMapper Traductor de errores del servidor/red a [ProfileError].
 */
class ProfileRepositoryImpl(
    private val supabase: SupabaseClient,
    private val errorMapper: ProfileErrorMapper = ProfileErrorMapper(),
) : ProfileRepository {
    override suspend fun isProfileComplete(): Boolean =
        io {
            supabase.from(TABLE)
                .select(columns = Columns.raw("profile_completed")) { filter { eq("id", currentUserId()) } }
                .decodeSingle<CompletionDto>()
                .profileCompleted
        }

    override suspend fun getMyProfile(): Profile =
        io {
            supabase.from(TABLE)
                .select(columns = Columns.raw("id,nickname,gender,description,avatar_id,photo_url,profile_completed")) {
                    filter { eq("id", currentUserId()) }
                }
                .decodeSingle<ProfileDto>()
                .toProfile()
        }

    override suspend fun isNicknameAvailable(nickname: String): Boolean =
        io {
            supabase.postgrest
                .rpc("nickname_disponible", buildJsonObject { put("p_nickname", nickname) })
                .decodeAs<Boolean>()
        }

    override suspend fun uploadPhoto(jpeg: ByteArray): String =
        io {
            val path = "${currentUserId()}/$PHOTO_FILE"
            val bucket = supabase.storage.from(BUCKET)
            // storage-kt 2.6.0 deduce el Content-Type por la extensión del path (.jpg -> image/jpeg).
            bucket.upload(path, jpeg, upsert = true)
            "${bucket.publicUrl(path)}?v=${System.currentTimeMillis()}"
        }

    override suspend fun updateProfile(update: ProfileUpdate) =
        io {
            val body =
                buildJsonObject {
                    put("nickname", update.nickname)
                    put("gender", update.gender.dbValue)
                    if (update.description == null) put("description", JsonNull) else put("description", update.description)
                    put("avatar_id", update.avatarId.key)
                    update.photoUrl?.let { put("photo_url", it) }
                }
            supabase.from(TABLE).update(body) { filter { eq("id", currentUserId()) } }
            Unit
        }

    override suspend fun getPublicProfile(userId: String): PublicProfile =
        io {
            supabase.from(TABLE)
                .select(columns = Columns.list(PUBLIC_COLUMNS)) { filter { eq("id", userId) } }
                .decodeSingle<ProfileDto>()
                .toPublicProfile()
        }

    private fun currentUserId(): String = supabase.auth.currentUserOrNull()?.id ?: throw ProfileException(ProfileError.SessionRequired)

    /** Ejecuta [block] en IO y convierte cualquier fallo del SDK en [ProfileException]. */
    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    private suspend fun <T> io(block: suspend () -> T): T =
        withContext(Dispatchers.IO) {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: ProfileException) {
                throw e
            } catch (e: RestException) {
                throw ProfileException(errorMapper.mapRest(e.description ?: e.error, e.statusCode))
            } catch (e: Exception) {
                throw ProfileException(errorMapper.mapTransport(e))
            }
        }

    companion object {
        /** Columnas que se consultan de otros usuarios; nunca incluye datos privados (D14). */
        val PUBLIC_COLUMNS: List<String> = listOf("id", "nickname", "gender", "description", "avatar_id", "photo_url")

        private const val TABLE = "profiles"
        private const val BUCKET = "avatars"
        private const val PHOTO_FILE = "foto.jpg"
    }
}

@Serializable
private data class CompletionDto(
    @SerialName("profile_completed") val profileCompleted: Boolean = false,
)

@Serializable
private data class ProfileDto(
    val id: String,
    val nickname: String? = null,
    val gender: String? = null,
    val description: String? = null,
    @SerialName("avatar_id") val avatarId: String? = null,
    @SerialName("photo_url") val photoUrl: String? = null,
    @SerialName("profile_completed") val profileCompleted: Boolean = false,
) {
    fun toProfile() = Profile(id, nickname, Gender.fromDbValue(gender), description, AvatarId.fromKey(avatarId), photoUrl, profileCompleted)

    fun toPublicProfile() = PublicProfile(id, nickname, Gender.fromDbValue(gender), description, AvatarId.fromKey(avatarId), photoUrl)
}
