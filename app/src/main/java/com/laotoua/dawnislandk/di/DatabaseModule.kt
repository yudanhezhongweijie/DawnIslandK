/*
 *  Copyright 2020 Fishballzzz
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

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
    fun provideBrowsingHistoryDao(dawnDatabase: DawnDatabase): BrowsingHistoryDao {
        return dawnDatabase.browsingHistoryDao()
    }

    @JvmStatic
    @Provides
    @Singleton
    fun providePostHistoryDao(dawnDatabase: DawnDatabase): PostHistoryDao {
        return dawnDatabase.postHistoryDao()
    }

    @JvmStatic
    @Provides
    @Singleton
    fun provideFeedDao(dawnDatabase: DawnDatabase): FeedDao {
        return dawnDatabase.feedDao()
    }

    @JvmStatic
    @Provides
    @Singleton
    fun provideBlockedIdDao(dawnDatabase: DawnDatabase): BlockedIdDao {
        return dawnDatabase.blockedIdDao()
    }

    @JvmStatic
    @Provides
    @Singleton
    fun provideNotificationDao(dawnDatabase: DawnDatabase): NotificationDao {
        return dawnDatabase.notificationDao()
    }
}