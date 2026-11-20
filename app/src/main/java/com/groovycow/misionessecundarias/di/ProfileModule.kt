package com.groovycow.misionessecundarias.di

import android.content.Context
import com.groovycow.misionessecundarias.data.image.AndroidPhotoCompressor
import com.groovycow.misionessecundarias.data.repository.ProfileErrorMapper
import com.groovycow.misionessecundarias.data.repository.ProfileRepositoryImpl
import com.groovycow.misionessecundarias.domain.repository.PhotoCompressor
import com.groovycow.misionessecundarias.domain.repository.ProfileRepository
import com.groovycow.misionessecundarias.domain.usecase.CompleteInitialProfileUseCase
import com.groovycow.misionessecundarias.domain.usecase.CompressProfilePhotoUseCase
import com.groovycow.misionessecundarias.domain.usecase.GetMyProfileUseCase
import com.groovycow.misionessecundarias.domain.usecase.GetPublicProfileUseCase
import com.groovycow.misionessecundarias.domain.usecase.IsProfileCompleteUseCase
import com.groovycow.misionessecundarias.domain.usecase.UpdateProfileUseCase
import com.groovycow.misionessecundarias.domain.usecase.UploadProfilePhotoUseCase
import com.groovycow.misionessecundarias.domain.validation.DescriptionValidator
import com.groovycow.misionessecundarias.domain.validation.NicknameValidator
import com.groovycow.misionessecundarias.domain.validation.ProfileDraftValidator
import com.groovycow.misionessecundarias.ui.profile.ProfileSummaryViewModel
import com.groovycow.misionessecundarias.ui.profile.edit.EditProfileViewModel
import com.groovycow.misionessecundarias.ui.profile.me.MyProfileViewModel
import com.groovycow.misionessecundarias.ui.profile.publicprofile.PublicProfileViewModel
import com.groovycow.misionessecundarias.ui.profile.setup.InitialProfileViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

/** Módulo Koin de perfiles: repositorio, compresor de fotos, validadores y casos de uso. */
val profileModule =
    module {
        single { ProfileErrorMapper() }
        single<ProfileRepository> { ProfileRepositoryImpl(get(), get()) }
        single<PhotoCompressor> { AndroidPhotoCompressor(get<Context>()) }

        factory { NicknameValidator() }
        factory { DescriptionValidator() }
        factory { ProfileDraftValidator(get(), get()) }

        factory { IsProfileCompleteUseCase(get()) }
        factory { GetMyProfileUseCase(get()) }
        factory { GetPublicProfileUseCase(get()) }
        factory { CompressProfilePhotoUseCase(get()) }
        factory { UploadProfilePhotoUseCase(get()) }
        factory { UpdateProfileUseCase(get(), get()) }
        factory { CompleteInitialProfileUseCase(get(), get()) }

        viewModel { InitialProfileViewModel(get(), get()) }
        viewModel { MyProfileViewModel(get()) }
        viewModel { EditProfileViewModel(get(), get(), get(), get()) }
        viewModel { ProfileSummaryViewModel(get()) }
        viewModel { PublicProfileViewModel(get()) }
    }
