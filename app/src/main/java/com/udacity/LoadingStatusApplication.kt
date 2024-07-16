package com.udacity

import android.app.Application
import timber.log.Timber

class LoadingStatusApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }
}