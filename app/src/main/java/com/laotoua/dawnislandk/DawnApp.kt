package com.laotoua.dawnislandk

import android.app.Application
import timber.log.Timber

class DawnApp : Application() {

    override fun onCreate() {
        super.onCreate()
        Timber.plant(Timber.DebugTree())
    }
}