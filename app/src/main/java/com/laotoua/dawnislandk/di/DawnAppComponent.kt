package com.laotoua.dawnislandk.di

import android.content.Context
import com.laotoua.dawnislandk.DawnApp
import dagger.BindsInstance
import dagger.Component
import dagger.android.AndroidInjector
import dagger.android.support.AndroidSupportInjectionModule
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        AndroidSupportInjectionModule::class,
        NMBNetworkModule::class,
        DatabaseModule::class,
        ViewModelModule::class
    ]
)
interface DawnAppComponent : AndroidInjector<DawnApp> {
    @Component.Factory
    interface Factory {
        fun create(@BindsInstance applicationContext: Context): DawnAppComponent
    }
}
