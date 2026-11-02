package com.groovycow.misionessecundarias

import android.app.Application
import com.groovycow.misionessecundarias.di.repositoryModule
import com.groovycow.misionessecundarias.di.supabaseModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class MisionesApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@MisionesApp)
            modules(supabaseModule, repositoryModule)
        }
    }
}