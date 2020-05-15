package com.laotoua.dawnislandk.di

import com.laotoua.dawnislandk.data.remote.NMBService
import com.laotoua.dawnislandk.data.remote.NMBServiceClient
import com.laotoua.dawnislandk.util.Constants
import dagger.Module
import dagger.Provides
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
class NMBNetworkModule {

    @Provides
    @Singleton
    fun provideNMBService(): NMBService = Retrofit.Builder()
        .baseUrl(Constants.baseCDN)
        .build()
        .create(NMBService::class.java)

    @Provides
    @Singleton
    fun provideNMBServiceClient(service: NMBService): NMBServiceClient {
        return NMBServiceClient(service)
    }
}