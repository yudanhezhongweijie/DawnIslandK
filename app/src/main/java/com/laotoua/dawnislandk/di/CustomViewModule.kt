package com.laotoua.dawnislandk.di

import com.laotoua.dawnislandk.screens.replys.QuotePopup
import com.laotoua.dawnislandk.screens.widget.popup.PostPopup
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
internal abstract class CustomViewModule {

    @ContributesAndroidInjector
    abstract fun bindPostPopup(): PostPopup

    @ContributesAndroidInjector
    abstract fun bindQuotePopup(): QuotePopup

}