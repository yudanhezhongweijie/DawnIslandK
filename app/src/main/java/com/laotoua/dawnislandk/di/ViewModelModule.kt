package com.laotoua.dawnislandk.di

import androidx.lifecycle.ViewModel
import com.laotoua.dawnislandk.screens.CommunityViewModel
import com.laotoua.dawnislandk.screens.MainActivity
import com.laotoua.dawnislandk.screens.PagerFragment
import com.laotoua.dawnislandk.screens.SharedViewModel
import com.laotoua.dawnislandk.screens.comments.CommentsFragment
import com.laotoua.dawnislandk.screens.comments.CommentsViewModel
import com.laotoua.dawnislandk.screens.feeds.FeedsFragment
import com.laotoua.dawnislandk.screens.feeds.FeedsViewModel
import com.laotoua.dawnislandk.screens.history.BrowsingHistoryFragment
import com.laotoua.dawnislandk.screens.history.BrowsingHistoryViewModel
import com.laotoua.dawnislandk.screens.posts.PostsFragment
import com.laotoua.dawnislandk.screens.posts.PostsViewModel
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
    internal abstract fun postsFragment(): PostsFragment

    @ContributesAndroidInjector(modules = [ViewModelBuilder::class])
    internal abstract fun feedsFragment(): FeedsFragment

    @ContributesAndroidInjector(modules = [ViewModelBuilder::class])
    internal abstract fun commentsFragment(): CommentsFragment

    @ContributesAndroidInjector(modules = [ViewModelBuilder::class])
    internal abstract fun trendsFragment(): TrendsFragment

    @ContributesAndroidInjector(modules = [ViewModelBuilder::class])
    internal abstract fun historyFragment(): BrowsingHistoryFragment

    @Binds
    @IntoMap
    @ViewModelKey(CommunityViewModel::class)
    abstract fun bindCommunityViewModel(viewModel: CommunityViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(PostsViewModel::class)
    abstract fun bindPostsViewModel(viewModel: PostsViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(FeedsViewModel::class)
    abstract fun bindFeedsViewModel(viewModel: FeedsViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(CommentsViewModel::class)
    abstract fun bindCommentsViewModel(viewModel: CommentsViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(TrendsViewModel::class)
    abstract fun bindTrendsViewModel(viewModel: TrendsViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(BrowsingHistoryViewModel::class)
    abstract fun bindHistoryViewModel(viewModelBrowsing: BrowsingHistoryViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(SharedViewModel::class)
    abstract fun bindSharedViewModel(viewModel: SharedViewModel): ViewModel
}