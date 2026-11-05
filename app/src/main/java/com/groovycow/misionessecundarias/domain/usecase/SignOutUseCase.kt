package com.groovycow.misionessecundarias.domain.usecase

import com.groovycow.misionessecundarias.domain.repository.AuthRepository

/** Cierra la sesión activa del usuario. */
class SignOutUseCase(
    private val repository: AuthRepository,
) {
    suspend operator fun invoke(): Result<Unit> = authCatching { repository.signOut() }
}
