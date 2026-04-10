package com.samsse.streamingapp

import android.app.Application
import com.samsse.streamingapp.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class StreamingApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Configurar Glide para mejor performance
        com.bumptech.glide.Glide.get(this).apply {
            setMemoryCategory(com.bumptech.glide.MemoryCategory.HIGH)
        }

        startKoin {
            androidLogger()
            androidContext(this@StreamingApp)
            modules(appModule)
        }
    }
}