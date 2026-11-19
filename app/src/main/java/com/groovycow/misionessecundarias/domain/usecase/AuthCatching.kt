package com.groovycow.misionessecundarias.domain.usecase

import kotlin.coroutines.cancellation.CancellationException

/**
 * Ejecuta [block] y envuelve su resultado en un [Result], conservando las [com.groovycow.misionessecundarias.domain.model.AuthException].
 *
 * @param T Tipo del valor devuelto.
 * @param block Operación suspendible a ejecutar.
 * @return [Result.success] con el valor, o [Result.failure] con la excepción lanzada.
 * @throws CancellationException siempre se relanza para respetar la cancelación de corrutinas.
 */
@Suppress("TooGenericExceptionCaught")
suspend inline fun <T> authCatching(block: () -> T): Result<T> =
    try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }
