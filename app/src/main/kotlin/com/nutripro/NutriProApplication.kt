package com.nutripro

import android.app.Application
import timber.log.Timber

class NutriProApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Timber.plant(Timber.DebugTree())
    }
}
