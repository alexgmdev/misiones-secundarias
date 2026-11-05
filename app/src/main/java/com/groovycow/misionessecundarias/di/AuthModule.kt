package com.groovycow.misionessecundarias.di

import com.groovycow.misionessecundarias.data.repository.AuthErrorMapper
import com.groovycow.misionessecundarias.data.repository.AuthRepositoryImpl
import com.groovycow.misionessecundarias.domain.repository.AuthRepository
import com.groovycow.misionessecundarias.domain.usecase.ObserveSessionUseCase
import com.groovycow.misionessecundarias.domain.usecase.RegisterUseCase
import com.groovycow.misionessecundarias.domain.validation.BirthDateValidator
import com.groovycow.misionessecundarias.domain.validation.EmailValidator
import com.groovycow.misionessecundarias.domain.validation.NameValidator
import com.groovycow.misionessecundarias.domain.validation.PasswordValidator
import com.groovycow.misionessecundarias.domain.validation.PhoneValidator
import com.groovycow.misionessecundarias.ui.auth.SessionViewModel
import com.groovycow.misionessecundarias.ui.auth.register.RegisterViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import java.time.Clock

/** Módulo Koin de autenticación: reloj, traductor de errores, repositorio, casos de uso y ViewModels. */
val authModule =
    module {
        single<Clock> { Clock.systemDefaultZone() }
        single { AuthErrorMapper() }
        single<AuthRepository> { AuthRepositoryImpl(get(), get()) }

        factory { NameValidator() }
        factory { EmailValidator() }
        factory { PhoneValidator() }
        factory { PasswordValidator() }
        factory { BirthDateValidator(get()) }

        factory { ObserveSessionUseCase(get()) }
        factory { RegisterUseCase(get()) }

        viewModel { SessionViewModel(get()) }
        viewModel { RegisterViewModel(get(), get(), get(), get(), get(), get()) }
    }
