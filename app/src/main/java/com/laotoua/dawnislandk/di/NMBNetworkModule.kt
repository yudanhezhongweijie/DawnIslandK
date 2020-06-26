package com.laotoua.dawnislandk.di

import com.laotoua.dawnislandk.data.remote.NMBService
import com.laotoua.dawnislandk.data.remote.NMBServiceClient
import com.laotoua.dawnislandk.util.DawnConstants
import dagger.Module
import dagger.Provides
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
object NMBNetworkModule {

    @JvmStatic
    @Provides
    @Singleton
    fun provideNMBService(): NMBService {
        val okHttpClient = OkHttpClient().newBuilder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
        return Retrofit.Builder()
            .baseUrl(DawnConstants.baseCDN)
            .client(okHttpClient)
            .build()
            .create(NMBService::class.java)
    }

    @JvmStatic
    @Provides
    @Singleton
    fun provideNMBServiceClient(service: NMBService): NMBServiceClient {
        return NMBServiceClient(service)
    }
}