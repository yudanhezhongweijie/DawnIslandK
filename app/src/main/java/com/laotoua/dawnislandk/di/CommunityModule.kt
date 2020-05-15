package com.laotoua.dawnislandk.di

import androidx.lifecycle.ViewModel
import com.laotoua.dawnislandk.MainActivity
import com.laotoua.dawnislandk.viewmodel.CommunityViewModel
import dagger.Binds
import dagger.Module
import dagger.android.ContributesAndroidInjector
import dagger.multibindings.IntoMap

@Module
abstract class CommunityModule {

    @ContributesAndroidInjector(
        modules = [
            ViewModelBuilder::class
        ]
    )
    internal abstract fun mainActivity(): MainActivity

    @Binds
    @IntoMap
    @ViewModelKey(CommunityViewModel::class)
    abstract fun bindViewModel(viewModel: CommunityViewModel): ViewModel
}