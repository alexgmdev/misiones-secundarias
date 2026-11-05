package com.groovycow.misionessecundarias.data.repository

import com.groovycow.misionessecundarias.domain.model.AuthError
import com.groovycow.misionessecundarias.domain.model.AuthException
import com.groovycow.misionessecundarias.domain.model.RegistrationData
import com.groovycow.misionessecundarias.domain.model.SessionState
import com.groovycow.misionessecundarias.domain.repository.AuthRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.functions.functions
import io.github.jan.supabase.gotrue.SessionStatus
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.exception.AuthRestException
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.gotrue.user.UserSession
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import kotlin.coroutines.cancellation.CancellationException

/**
 * Implementación de [AuthRepository] sobre Supabase Auth (gotrue-kt).
 *
 * @property client Cliente de Supabase.
 * @property errorMapper Traductor de errores del servidor/red a errores tipados.
 */
class AuthRepositoryImpl(
    private val client: SupabaseClient,
    private val errorMapper: AuthErrorMapper,
) : AuthRepository {
    override fun observeSession(): Flow<SessionState> =
        client.auth.sessionStatus.map { status ->
            when (status) {
                is SessionStatus.LoadingFromStorage -> SessionState.LOADING
                is SessionStatus.Authenticated, is SessionStatus.NetworkError -> SessionState.AUTHENTICATED
                is SessionStatus.NotAuthenticated -> SessionState.UNAUTHENTICATED
            }
        }

    override suspend fun isPhoneAvailable(phone: String): Boolean {
        var available = false
        translatingErrors(AuthOperation.OTHER) {
            available =
                client.postgrest
                    .rpc("telefono_disponible", buildJsonObject { put("p_telefono", phone) })
                    .decodeAs<Boolean>()
        }
        return available
    }

    override suspend fun signUp(data: RegistrationData) =
        translatingErrors(AuthOperation.OTHER) {
            client.auth.signUpWith(Email) {
                email = data.email
                password = data.password
                this.data =
                    buildJsonObject {
                        put("first_name", data.firstName)
                        put("last_name", data.lastName)
                        put("phone", data.phone)
                        put("birth_date", data.birthDate.toString())
                    }
            }
            Unit
        }

    override suspend fun signIn(
        email: String,
        password: String,
    ) = translatingErrors(AuthOperation.LOGIN) {
        client.auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
    }

    override suspend fun signInWithPhone(
        phone: String,
        password: String,
    ) = translatingErrors(AuthOperation.LOGIN) {
        val response =
            client.functions.invoke(PHONE_LOGIN_FUNCTION) {
                contentType(ContentType.Application.Json)
                setBody(
                    buildJsonObject {
                        put("telefono", phone)
                        put("clave", password)
                    }.toString(),
                )
            }
        importPhoneSession(response.bodyAsText())
    }

    override suspend fun signOut() =
        translatingErrors(AuthOperation.OTHER) {
            client.auth.signOut()
        }

    /**
     * Importa al cliente de autenticación la sesión devuelta por `login-telefono` (que no incluye `user`):
     * se arma una [UserSession] sin usuario y luego se consulta el usuario real. Si el SDK no puede
     * serializar esa sesión, se recurre a [io.github.jan.supabase.gotrue.Auth.importAuthToken].
     *
     * @param body Cuerpo JSON de la respuesta 200 de la función.
     * @throws AuthException con [AuthError.ServerError] si la respuesta no trae los tokens.
     */
    private suspend fun importPhoneSession(body: String) {
        val json = Json.parseToJsonElement(body).jsonObject
        val accessToken = json.string("access_token")
        val refreshToken = json.string("refresh_token")
        val expiresIn = json["expires_in"]?.jsonPrimitive?.longOrNull ?: DEFAULT_EXPIRES_IN_SECONDS
        try {
            client.auth.importSession(
                UserSession(
                    accessToken = accessToken,
                    refreshToken = refreshToken,
                    expiresIn = expiresIn,
                    tokenType = json["token_type"]?.jsonPrimitive?.contentOrNull ?: "bearer",
                    user = null,
                ),
            )
            client.auth.retrieveUserForCurrentSession(updateSession = true)
        } catch (ignored: SerializationException) {
            client.auth.importAuthToken(accessToken, refreshToken, retrieveUser = true)
        }
    }

    private fun JsonObject.string(key: String): String =
        this[key]?.jsonPrimitive?.contentOrNull ?: throw AuthException(AuthError.ServerError)

    /** Ejecuta [block] y convierte cualquier fallo del SDK en [AuthException]. */
    @Suppress("TooGenericExceptionCaught")
    private suspend fun translatingErrors(
        operation: AuthOperation,
        block: suspend () -> Unit,
    ) {
        try {
            block()
        } catch (e: Exception) {
            throw toAuthFailure(e, operation)
        }
    }

    /** Devuelve la cancelación o el [AuthException] ya tipado tal cual; traduce el resto con [errorMapper]. */
    private fun toAuthFailure(
        e: Exception,
        operation: AuthOperation,
    ): Exception =
        when (e) {
            is CancellationException, is AuthException -> e
            is RestException -> AuthException(errorMapper.mapRest(e.restErrorCode(), e.statusCode, operation))
            else -> AuthException(errorMapper.mapTransport(e))
        }

    private companion object {
        const val PHONE_LOGIN_FUNCTION = "login-telefono"
        const val DEFAULT_EXPIRES_IN_SECONDS = 3600L
    }

    /** Extrae el código de error de GoTrue (`error_code`) cuando la excepción lo trae. */
    private fun RestException.restErrorCode(): String? = (this as? AuthRestException)?.errorCode?.value
}
