package com.laotoua.dawnislandk

import android.app.Application
import androidx.room.Room
import com.laotoua.dawnislandk.data.entity.DawnDatabase
import com.laotoua.dawnislandk.data.state.AppState
import com.laotoua.dawnislandk.ui.util.ReadableTime
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