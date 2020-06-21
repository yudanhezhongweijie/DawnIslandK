package com.laotoua.dawnislandk.di

import android.content.Context
import com.laotoua.dawnislandk.data.local.DawnDatabase
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
    fun provideCommentDao(dawnDatabase: DawnDatabase): CommentDao {
        return dawnDatabase.commentDao()
    }

    @JvmStatic
    @Provides
    @Singleton
    fun providePostDao(dawnDatabase: DawnDatabase): PostDao {
        return dawnDatabase.postDao()
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
    fun provideNMBNoticeDao(dawnDatabase: DawnDatabase): NMBNoticeDao {
        return dawnDatabase.nmbNoticeDao()
    }

    @JvmStatic
    @Provides
    @Singleton
    fun provideLuweiNoticeDao(dawnDatabase: DawnDatabase): LuweiNoticeDao {
        return dawnDatabase.luweiNoticeDao()
    }

    @JvmStatic
    @Provides
    @Singleton
    fun provideReleaseDao(dawnDatabase: DawnDatabase): ReleaseDao {
        return dawnDatabase.releaseDao()
    }

    @JvmStatic
    @Provides
    @Singleton
    fun provideReadingPageDao(dawnDatabase: DawnDatabase): ReadingPageDao {
        return dawnDatabase.readingPageDao()
    }

    @JvmStatic
    @Provides
    @Singleton
    fun provideBrowsedPostDao(dawnDatabase: DawnDatabase): BrowsedPostDao {
        return dawnDatabase.browsedPostDao()
    }
}