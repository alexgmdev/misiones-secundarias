package com.groovycow.misionessecundarias.di

import com.groovycow.misionessecundarias.data.repository.AuthErrorMapper
import com.groovycow.misionessecundarias.data.repository.AuthRepositoryImpl
import com.groovycow.misionessecundarias.domain.repository.AuthRepository
import com.groovycow.misionessecundarias.domain.usecase.CancelRecoveryUseCase
import com.groovycow.misionessecundarias.domain.usecase.ChangePasswordUseCase
import com.groovycow.misionessecundarias.domain.usecase.ObserveSessionUseCase
import com.groovycow.misionessecundarias.domain.usecase.RegisterUseCase
import com.groovycow.misionessecundarias.domain.usecase.RequestPasswordResetUseCase
import com.groovycow.misionessecundarias.domain.usecase.SignInUseCase
import com.groovycow.misionessecundarias.domain.usecase.VerifyRecoveryCodeUseCase
import com.groovycow.misionessecundarias.domain.validation.BirthDateValidator
import com.groovycow.misionessecundarias.domain.validation.EmailValidator
import com.groovycow.misionessecundarias.domain.validation.NameValidator
import com.groovycow.misionessecundarias.domain.validation.PasswordValidator
import com.groovycow.misionessecundarias.domain.validation.PhoneValidator
import com.groovycow.misionessecundarias.domain.validation.RecoveryCodeValidator
import com.groovycow.misionessecundarias.ui.auth.SessionViewModel
import com.groovycow.misionessecundarias.ui.auth.login.LoginViewModel
import com.groovycow.misionessecundarias.ui.auth.recovery.RequestCodeViewModel
import com.groovycow.misionessecundarias.ui.auth.recovery.ResetPasswordViewModel
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
        factory { RecoveryCodeValidator() }

        factory { ObserveSessionUseCase(get()) }
        factory { RegisterUseCase(get()) }
        factory { SignInUseCase(get(), get()) }
        factory { RequestPasswordResetUseCase(get(), get()) }
        factory { VerifyRecoveryCodeUseCase(get(), get()) }
        factory { ChangePasswordUseCase(get(), get()) }
        factory { CancelRecoveryUseCase(get()) }

        viewModel { SessionViewModel(get()) }
        viewModel { LoginViewModel(get()) }
        viewModel { RequestCodeViewModel(get(), get()) }
        viewModel { (email: String) -> ResetPasswordViewModel(email, get(), get(), get(), get(), get(), get()) }
        viewModel { RegisterViewModel(get(), get(), get(), get(), get(), get()) }
    }
