package com.groovycow.misionessecundarias.di

import com.groovycow.misionessecundarias.data.repository.MissionRepositoryImpl
import com.groovycow.misionessecundarias.domain.repository.MissionRepository
import com.groovycow.misionessecundarias.domain.usecase.GetMissionsUseCase
import com.groovycow.misionessecundarias.ui.missions.MissionViewModel // <-- Asegúrate de tener este import
import org.koin.androidx.viewmodel.dsl.viewModel // <-- Y este import
import org.koin.dsl.module

val repositoryModule = module {
    // Repositorios
    single<MissionRepository> { MissionRepositoryImpl(get()) }

    // Casos de Uso
    factory { GetMissionsUseCase(get()) }

    // ViewModel (ESTA ES LA LÍNEA QUE FALTA)
    viewModel { MissionViewModel(get()) }
}