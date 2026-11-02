package com.groovycow.misionessecundarias.di

import com.groovycow.misionessecundarias.data.repository.ApplicationRepositoryImpl
import com.groovycow.misionessecundarias.data.repository.MissionRepositoryImpl
import com.groovycow.misionessecundarias.domain.repository.ApplicationRepository
import com.groovycow.misionessecundarias.domain.repository.MissionRepository
import com.groovycow.misionessecundarias.domain.usecase.ApplyToMissionUseCase
import com.groovycow.misionessecundarias.domain.usecase.CancelApplicationUseCase
import com.groovycow.misionessecundarias.domain.usecase.GetMissionsUseCase
import com.groovycow.misionessecundarias.domain.usecase.GetMyApplicationsUseCase
import com.groovycow.misionessecundarias.ui.applications.ApplicationViewModel
import com.groovycow.misionessecundarias.ui.applications.MyApplicationsViewModel
import com.groovycow.misionessecundarias.ui.missions.MissionViewModel // <-- Asegúrate de tener este import
import org.koin.androidx.viewmodel.dsl.viewModel // <-- Y este import
import org.koin.dsl.module

val repositoryModule = module {
    // Repositorios
    single<MissionRepository> { MissionRepositoryImpl(get()) }
    single<ApplicationRepository> { ApplicationRepositoryImpl(get()) }

    // Casos de Uso
    factory { GetMissionsUseCase(get()) }
    factory { ApplyToMissionUseCase(get()) }
    factory { CancelApplicationUseCase(get()) }
    factory { GetMyApplicationsUseCase(get()) }

    // ViewModel (ESTA ES LA LÍNEA QUE FALTA)
    viewModel { MissionViewModel(get()) }
    viewModel { ApplicationViewModel(get()) }
    viewModel { MyApplicationsViewModel(get(), get()) }
}
