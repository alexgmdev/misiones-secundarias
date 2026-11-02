package com.groovycow.misionessecundarias.domain.repository

interface AuthRepository {
    suspend fun signUp(email: String, password: String, fullName: String, phone: String)
    suspend fun signIn(email: String, password: String)
    suspend fun signOut()
}