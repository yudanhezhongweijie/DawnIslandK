package com.laotoua.dawnislandk.di

import android.content.Context
import com.laotoua.dawnislandk.data.local.ApplicationDataStore
import com.laotoua.dawnislandk.data.local.dao.*
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
object DatabaseModule {

    @JvmStatic
    @Provides
    @Singleton
    fun provideDawnDB(applicationContext: Context): DawnDatabase {
        return DawnDatabase.getDatabase(applicationContext)
    }

    @JvmStatic
    @Provides
    @Singleton
    fun provideCommunityDao(dawnDatabase: DawnDatabase): CommunityDao {
        return dawnDatabase.communityDao()
    }

    @JvmStatic
    @Provides
    @Singleton
    fun provideCookieDao(dawnDatabase: DawnDatabase): CookieDao {
        return dawnDatabase.cookieDao()
    }

    @JvmStatic
    @Provides
    @Singleton
    fun provideReplyDao(dawnDatabase: DawnDatabase): ReplyDao {
        return dawnDatabase.replyDao()
    }

    @JvmStatic
    @Provides
    @Singleton
    fun provideThreadDao(dawnDatabase: DawnDatabase): ThreadDao {
        return dawnDatabase.threadDao()
    }

    @JvmStatic
    @Provides
    @Singleton
    fun provideDailyTrendDao(dawnDatabase: DawnDatabase): DailyTrendDao {
        return dawnDatabase.dailyTrendDao()
    }

    @JvmStatic
    @Provides
    @Singleton
    fun provideNoticeDao(dawnDatabase: DawnDatabase): NoticeDao {
        return dawnDatabase.noticeDao()
    }

}