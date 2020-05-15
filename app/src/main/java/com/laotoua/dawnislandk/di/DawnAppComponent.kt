package com.laotoua.dawnislandk.di

import android.content.Context
import com.laotoua.dawnislandk.DawnApp
import dagger.BindsInstance
import dagger.Component
import dagger.android.AndroidInjectionModule
import dagger.android.AndroidInjector
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        AndroidInjectionModule::class,
        NMBNetworkModule::class,
        DatabaseModule::class,
        CommunityModule::class
    ]
)
interface DawnAppComponent : AndroidInjector<DawnApp> {
    @Component.Factory
    interface Factory {
        fun create(@BindsInstance applicationContext: Context): DawnAppComponent
    }
}
