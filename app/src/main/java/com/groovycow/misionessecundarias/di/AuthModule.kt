package com.groovycow.misionessecundarias.di

import com.groovycow.misionessecundarias.data.repository.AuthErrorMapper
import com.groovycow.misionessecundarias.data.repository.AuthRepositoryImpl
import com.groovycow.misionessecundarias.domain.repository.AuthRepository
import com.groovycow.misionessecundarias.domain.usecase.ObserveSessionUseCase
import com.groovycow.misionessecundarias.ui.auth.SessionViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import java.time.Clock

/** Módulo Koin de autenticación: reloj, traductor de errores, repositorio, casos de uso y ViewModels. */
val authModule =
    module {
        single<Clock> { Clock.systemDefaultZone() }
        single { AuthErrorMapper() }
        single<AuthRepository> { AuthRepositoryImpl(get(), get()) }

        factory { ObserveSessionUseCase(get()) }

        viewModel { SessionViewModel(get()) }
    }
