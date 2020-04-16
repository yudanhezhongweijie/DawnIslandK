package com.laotoua.dawnislandk

import android.app.Application
import androidx.room.Room
import com.laotoua.dawnislandk.entities.DawnDatabase
import com.laotoua.dawnislandk.util.AppState
import com.laotoua.dawnislandk.util.ReadableTime
import timber.log.Timber

class DawnApp : Application() {

    override fun onCreate() {
        super.onCreate()
        Timber.plant(Timber.DebugTree())

        // Time
        ReadableTime.initialize(this)
        val db = Room.databaseBuilder(
            applicationContext,
            DawnDatabase::class.java, "dawnDB"
        )
            .fallbackToDestructiveMigration()
            .build()

        AppState.setDB(db)

    }
}