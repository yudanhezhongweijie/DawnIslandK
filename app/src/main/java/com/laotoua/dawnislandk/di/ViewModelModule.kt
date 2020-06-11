package com.laotoua.dawnislandk.di

import androidx.lifecycle.ViewModel
import com.laotoua.dawnislandk.screens.CommunityViewModel
import com.laotoua.dawnislandk.screens.MainActivity
import com.laotoua.dawnislandk.screens.PagerFragment
import com.laotoua.dawnislandk.screens.SharedViewModel
import com.laotoua.dawnislandk.screens.feeds.FeedsFragment
import com.laotoua.dawnislandk.screens.feeds.FeedsViewModel
import com.laotoua.dawnislandk.screens.replys.ReplysFragment
import com.laotoua.dawnislandk.screens.replys.ReplysViewModel
import com.laotoua.dawnislandk.screens.threads.ThreadsFragment
import com.laotoua.dawnislandk.screens.threads.ThreadsViewModel
import com.laotoua.dawnislandk.screens.trend.TrendsFragment
import com.laotoua.dawnislandk.screens.trend.TrendsViewModel
import dagger.Binds
import dagger.Module
import dagger.android.ContributesAndroidInjector
import dagger.multibindings.IntoMap

@Module
abstract class ViewModelModule {

    @ContributesAndroidInjector(modules = [ViewModelBuilder::class])
    internal abstract fun mainActivity(): MainActivity

    @ContributesAndroidInjector(modules = [ViewModelBuilder::class])
    internal abstract fun pagerFragment(): PagerFragment

    @ContributesAndroidInjector(modules = [ViewModelBuilder::class])
    internal abstract fun threadsFragment(): ThreadsFragment

    @ContributesAndroidInjector(modules = [ViewModelBuilder::class])
    internal abstract fun feedsFragment(): FeedsFragment

    @ContributesAndroidInjector(modules = [ViewModelBuilder::class])
    internal abstract fun replysFragment(): ReplysFragment

    @ContributesAndroidInjector(modules = [ViewModelBuilder::class])
    internal abstract fun trendsFragment(): TrendsFragment

    @Binds
    @IntoMap
    @ViewModelKey(CommunityViewModel::class)
    abstract fun bindCommunityViewModel(viewModel: CommunityViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(ThreadsViewModel::class)
    abstract fun bindThreadsViewModel(viewModel: ThreadsViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(FeedsViewModel::class)
    abstract fun bindFeedsViewModel(viewModel: FeedsViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(ReplysViewModel::class)
    abstract fun bindReplysViewModel(viewModel: ReplysViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(TrendsViewModel::class)
    abstract fun bindTrendsViewModel(viewModel: TrendsViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(SharedViewModel::class)
    abstract fun bindSharedViewModel(viewModel: SharedViewModel): ViewModel

}