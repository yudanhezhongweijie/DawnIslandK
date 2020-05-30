package com.laotoua.dawnislandk.di

import android.content.Context
import androidx.room.Room
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
        return Room.databaseBuilder(
            applicationContext,
            DawnDatabase::class.java, "dawnDB"
        )
            .fallbackToDestructiveMigration()
            .build()
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
}