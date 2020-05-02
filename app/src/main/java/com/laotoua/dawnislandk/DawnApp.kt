package com.laotoua.dawnislandk

import android.app.Application
import androidx.room.Room
import com.laotoua.dawnislandk.data.entity.DawnDatabase
import com.laotoua.dawnislandk.data.state.AppState
import com.laotoua.dawnislandk.ui.util.ReadableTime
import com.tencent.mmkv.MMKV
import timber.log.Timber

class DawnApp : Application() {

    override fun onCreate() {
        super.onCreate()
        Timber.plant(Timber.DebugTree())

        // MMKV
        MMKV.initialize(this)

        // db
        val db = Room.databaseBuilder(
            applicationContext,
            DawnDatabase::class.java, "dawnDB"
        )
            .fallbackToDestructiveMigration()
            .build()
        AppState.setDB(db)

        // Time
        ReadableTime.initialize(this)
    }
}