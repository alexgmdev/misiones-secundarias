package com.groovycow.misionessecundarias.di

import com.groovycow.misionessecundarias.data.repository.ApplicationRepositoryImpl
import com.groovycow.misionessecundarias.data.repository.MissionRepositoryImpl
import com.groovycow.misionessecundarias.domain.repository.ApplicationRepository
import com.groovycow.misionessecundarias.domain.repository.MissionRepository
import com.groovycow.misionessecundarias.domain.usecase.ApplyToMissionUseCase
import com.groovycow.misionessecundarias.domain.usecase.CancelApplicationUseCase
import com.groovycow.misionessecundarias.domain.usecase.CreateMissionUseCase
import com.groovycow.misionessecundarias.domain.usecase.GetMissionsUseCase
import com.groovycow.misionessecundarias.domain.usecase.GetMyApplicationsUseCase
import com.groovycow.misionessecundarias.domain.usecase.PublishMissionUseCase
import com.groovycow.misionessecundarias.domain.validation.MissionDraftValidator
import com.groovycow.misionessecundarias.ui.applications.ApplicationViewModel
import com.groovycow.misionessecundarias.ui.applications.MyApplicationsViewModel
import com.groovycow.misionessecundarias.ui.missions.MissionViewModel // <-- Asegúrate de tener este import
import com.groovycow.misionessecundarias.ui.missions.create.CreateMissionViewModel
import com.groovycow.misionessecundarias.ui.missions.publish.PublishMissionViewModel
import org.koin.androidx.viewmodel.dsl.viewModel // <-- Y este import
import org.koin.dsl.module
import java.time.Clock
import java.time.ZoneId

val repositoryModule =
    module {
        // Repositorios
        single<MissionRepository> { MissionRepositoryImpl(get()) }
        single<ApplicationRepository> { ApplicationRepositoryImpl(get()) }

        // Casos de Uso
        factory { GetMissionsUseCase(get()) }
        factory { CreateMissionUseCase(get()) }
        factory { PublishMissionUseCase(get(), Clock.system(ZoneId.of("America/Bogota"))) }
        factory { ApplyToMissionUseCase(get()) }
        factory { CancelApplicationUseCase(get()) }
        factory { GetMyApplicationsUseCase(get()) }
        factory { MissionDraftValidator(Clock.system(ZoneId.of("America/Bogota"))) }

        // ViewModel (ESTA ES LA LÍNEA QUE FALTA)
        viewModel { MissionViewModel(get()) }
        viewModel { CreateMissionViewModel(get(), get()) }
        viewModel { PublishMissionViewModel(get()) }
        viewModel { ApplicationViewModel(get()) }
        viewModel { MyApplicationsViewModel(get(), get()) }
    }
