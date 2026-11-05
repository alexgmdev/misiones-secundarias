package com.groovycow.misionessecundarias.data.repository

import com.groovycow.misionessecundarias.domain.model.AuthException
import com.groovycow.misionessecundarias.domain.model.SessionState
import com.groovycow.misionessecundarias.domain.repository.AuthRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.gotrue.SessionStatus
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.exception.AuthRestException
import io.github.jan.supabase.gotrue.providers.builtin.Email
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
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

    override suspend fun signIn(
        email: String,
        password: String,
    ) = translatingErrors(AuthOperation.LOGIN) {
        client.auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
    }

    override suspend fun signOut() =
        translatingErrors(AuthOperation.OTHER) {
            client.auth.signOut()
        }

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

    /** Extrae el código de error de GoTrue (`error_code`) cuando la excepción lo trae. */
    private fun RestException.restErrorCode(): String? = (this as? AuthRestException)?.errorCode?.value
}
