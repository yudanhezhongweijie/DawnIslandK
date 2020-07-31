/*
 *  Copyright 2020 Fishballzzz
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package com.laotoua.dawnislandk

import com.google.firebase.crashlytics.FirebaseCrashlytics
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
            FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(false)
        } else {
            FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true)
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

