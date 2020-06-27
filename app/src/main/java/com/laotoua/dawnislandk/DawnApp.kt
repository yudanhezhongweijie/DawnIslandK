package com.laotoua.dawnislandk

import com.laotoua.dawnislandk.data.local.ApplicationDataStore
import com.laotoua.dawnislandk.di.DaggerDawnAppComponent
import com.laotoua.dawnislandk.util.ReadableTime
import com.tencent.mmkv.MMKV
import com.tencent.mmkv.MMKVHandler
import com.tencent.mmkv.MMKVLogLevel
import com.tencent.mmkv.MMKVRecoverStrategic
import dagger.android.AndroidInjector
import dagger.android.DaggerApplication
import timber.log.Timber
import javax.inject.Inject


class DawnApp : DaggerApplication() {

    companion object {
        lateinit var applicationDataStore: ApplicationDataStore
    }

    override fun applicationInjector(): AndroidInjector<out DaggerApplication> {
        return DaggerDawnAppComponent.factory().create(applicationContext)
    }

    @Inject
    lateinit var mApplicationDataStore: ApplicationDataStore

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        applicationDataStore = mApplicationDataStore

        // MMKV
        MMKV.initialize(this)
        MMKV.registerHandler(handler)
        // Time
        ReadableTime.initialize(this)
    }

    private val handler = object : MMKVHandler {
        override fun onMMKVCRCCheckFail(p0: String?): MMKVRecoverStrategic {
            throw Exception("onMMKVCRCCheckFail $p0")
        }

        override fun wantLogRedirecting(): Boolean {
            return true
        }

        override fun mmkvLog(
            level: MMKVLogLevel?,
            file: String?,
            line: Int,
            func: String?,
            message: String?
        ) {
            val log =
                "<" + file.toString() + ":" + line.toString() + "::" + func.toString() + "> " + message
            when (level) {
                MMKVLogLevel.LevelDebug -> {
                    Timber.d(log)
                }
                MMKVLogLevel.LevelInfo -> {
                }
                MMKVLogLevel.LevelWarning -> {
                }
                MMKVLogLevel.LevelError -> {
                    Timber.e(log)
                }
                MMKVLogLevel.LevelNone -> {
                }
            }
        }

        override fun onMMKVFileLengthError(p0: String?): MMKVRecoverStrategic {
            throw Exception("onMMKVFileLengthError $p0")
        }
    }
}

