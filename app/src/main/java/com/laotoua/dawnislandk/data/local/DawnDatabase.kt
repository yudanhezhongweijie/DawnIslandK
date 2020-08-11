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

package com.laotoua.dawnislandk.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.laotoua.dawnislandk.BuildConfig
import com.laotoua.dawnislandk.data.local.dao.*
import com.laotoua.dawnislandk.data.local.entity.*

@Database(
    entities = [
        Community::class,
        Cookie::class,
        Comment::class,
        Post::class,
        DailyTrend::class,
        NMBNotice::class,
        LuweiNotice::class,
        Release::class,
        ReadingPage::class,
        BrowsingHistory::class,
        PostHistory::class,
        Feed::class,
        BlockedId::class,
        Notification::class],
    version = 17
)
@TypeConverters(Converter::class)
abstract class DawnDatabase : RoomDatabase() {
    abstract fun cookieDao(): CookieDao
    abstract fun communityDao(): CommunityDao
    abstract fun commentDao(): CommentDao
    abstract fun postDao(): PostDao
    abstract fun dailyTrendDao(): DailyTrendDao
    abstract fun nmbNoticeDao(): NMBNoticeDao
    abstract fun luweiNoticeDao(): LuweiNoticeDao
    abstract fun releaseDao(): ReleaseDao
    abstract fun readingPageDao(): ReadingPageDao
    abstract fun browsingHistoryDao(): BrowsingHistoryDao
    abstract fun postHistoryDao(): PostHistoryDao
    abstract fun feedDao(): FeedDao
    abstract fun blockedIdDao(): BlockedIdDao
    abstract fun notificationDao(): NotificationDao

    companion object {
        // Singleton prevents multiple instances of database opening at the
        // same time.
        @Volatile
        private var INSTANCE: DawnDatabase? = null

        fun getDatabase(context: Context): DawnDatabase {
            val tempInstance =
                INSTANCE
            if (tempInstance != null) {
                return tempInstance
            }

            /**
             *  DATABASE MIGRATIONS
             */

            // adds trends
            val migrate1To2 = object : Migration(1, 2) {
                override fun migrate(database: SupportSQLiteDatabase) {
                    database.execSQL("CREATE TABLE IF NOT EXISTS `DailyTrend` (`id` TEXT NOT NULL, `po` TEXT NOT NULL, `date` INTEGER NOT NULL, `trends` TEXT NOT NULL,`lastReplyCount` INTEGER NOT NULL, `lastUpdatedAt` INTEGER NOT NULL, PRIMARY KEY(`id`))")
                }
            }

            // adds NMBNotice, LuweiNotice
            val migrate2To3 = object : Migration(2, 3) {
                override fun migrate(database: SupportSQLiteDatabase) {
                    database.execSQL("CREATE TABLE IF NOT EXISTS `NMBNotice` (`id` INTEGER PRIMARY KEY AUTOINCREMENT, `content` TEXT NOT NULL, `date` INTEGER NOT NULL, `enable` INTEGER NOT NULL, `read` INTEGER NOT NULL, `lastUpdatedAt` INTEGER NOT NULL)")
                    database.execSQL("CREATE TABLE IF NOT EXISTS `LuweiNotice` (`id` INTEGER PRIMARY KEY AUTOINCREMENT, `appVersion` TEXT NOT NULL, `beitaiForums` TEXT NOT NULL, `nmbForums` TEXT NOT NULL, `loadingMsgs` TEXT NOT NULL, `clientsInfo` TEXT NOT NULL, `whitelist` TEXT NOT NULL, `lastUpdatedAt` INTEGER NOT NULL)")
                }
            }

            // adds version checks
            val migrate3To4 = object : Migration(3, 4) {
                override fun migrate(database: SupportSQLiteDatabase) {
                    database.execSQL("CREATE TABLE IF NOT EXISTS `Release` (`id` INTEGER PRIMARY KEY AUTOINCREMENT, `version` TEXT NOT NULL, `downloadUrl` TEXT NOT NULL, `message` TEXT NOT NULL)")
                    val contentValue = ContentValues().apply {
                        put("id", 1)
                        put("version", BuildConfig.VERSION_NAME)
                        put("downloadUrl", "")
                        put("message", "default entry")
                    }
                    database.insert("Release", SQLiteDatabase.CONFLICT_REPLACE, contentValue)
                }
            }

            // renamed Thread,Reply, moved readingProgress to its own table
            val migrate4To5 = object : Migration(4, 5) {
                override fun migrate(database: SupportSQLiteDatabase) {
                    database.execSQL("ALTER TABLE `Reply` RENAME TO Comment")
                    database.execSQL("CREATE TABLE IF NOT EXISTS `Post` (`id` TEXT NOT NULL, `fid` TEXT NOT NULL, `category` TEXT NOT NULL, `img` TEXT NOT NULL, `ext` TEXT NOT NULL, `now` TEXT NOT NULL, `userid` TEXT NOT NULL, `name` TEXT NOT NULL, `email` TEXT NOT NULL, `title` TEXT NOT NULL, `content` TEXT NOT NULL, `sage` TEXT NOT NULL, `admin` TEXT NOT NULL, `status` TEXT NOT NULL, `replyCount` TEXT NOT NULL, `lastUpdatedAt` INTEGER NOT NULL, PRIMARY KEY(`id`))")
                    database.execSQL("INSERT INTO `Post` SELECT id, fid, category, img, ext, now, userid, name, email, title, content, sage, admin, status, replyCount, lastUpdatedAt FROM Thread")
                    database.execSQL("CREATE TABLE IF NOT EXISTS `ReadingPage` (`id` TEXT NOT NULL, `page` INTEGER NOT NULL, `lastUpdatedAt` INTEGER NOT NULL, PRIMARY KEY(`id`))")
                    database.execSQL("INSERT INTO `ReadingPage` (id,page,lastUpdatedAt) SELECT id,readingProgress,lastUpdatedAt FROM Thread")
                    database.execSQL("DROP Table `Thread`")
                }
            }

            // adds Browsing History
            val migrate5To6 = object : Migration(5, 6) {
                override fun migrate(database: SupportSQLiteDatabase) {
                    database.execSQL("CREATE TABLE IF NOT EXISTS `BrowsingHistory` (`date` INTEGER NOT NULL, `postId` TEXT NOT NULL, `postFid` TEXT NOT NULL, `pages` TEXT NOT NULL, PRIMARY KEY(`date`, `postId`))")
                }
            }

            // adds Post History
            val migrate6To7 = object : Migration(6, 7) {
                override fun migrate(database: SupportSQLiteDatabase) {
                    database.execSQL("CREATE TABLE IF NOT EXISTS `PostHistory` (`id` INTEGER PRIMARY KEY AUTOINCREMENT, `postCookieName` TEXT NOT NULL, `postTargetId` TEXT NOT NULL, `postTargetPage` INTEGER NOT NULL, `postTargetFid` TEXT NOT NULL, `newPost` INTEGER NOT NULL, `imgPath` TEXT NOT NULL, `content` TEXT NOT NULL, `postDate` INTEGER NOT NULL)")
                }
            }

            // adds release checks timestamp
            val migrate7To8 = object : Migration(7, 8) {
                override fun migrate(database: SupportSQLiteDatabase) {
                    database.execSQL("ALTER TABLE `Release` ADD COLUMN `lastUpdatedAt` INTEGER NOT NULL DEFAULT 0")
                }
            }

            // update Post History Table
            val migrate8To9 = object : Migration(8, 9) {
                override fun migrate(database: SupportSQLiteDatabase) {
                    database.execSQL("DROP TABLE PostHistory")
                    database.execSQL("CREATE TABLE IF NOT EXISTS `PostHistory` (`id` TEXT NOT NULL, `cookieName` TEXT NOT NULL, `postTargetId` TEXT NOT NULL, `postTargetPage` INTEGER NOT NULL, `postTargetFid` TEXT NOT NULL, `newPost` INTEGER NOT NULL, `content` TEXT NOT NULL, `img` TEXT NOT NULL, `ext` TEXT NOT NULL, `postDate` INTEGER NOT NULL, PRIMARY KEY(`id`))")
                }
            }

            // updates Cookie Table
            val migrate9To10 = object : Migration(9, 10) {
                override fun migrate(database: SupportSQLiteDatabase) {
                    database.execSQL("DROP TABLE Cookie")
                    database.execSQL("CREATE TABLE IF NOT EXISTS `Cookie` (`cookieHash` TEXT NOT NULL, `cookieName` TEXT NOT NULL, `cookieDisplayName` TEXT NOT NULL, PRIMARY KEY(`cookieHash`))")
                }
            }

            // updates PostHistory Table
            val migrate10To11 = object : Migration(10, 11) {
                override fun migrate(database: SupportSQLiteDatabase) {
                    database.execSQL("CREATE TABLE `PostHistory2` (`id` TEXT NOT NULL, `newPost` INTEGER NOT NULL, `postTargetId` TEXT NOT NULL, `postTargetFid` TEXT NOT NULL, `postTargetPage` INTEGER NOT NULL, `cookieName` TEXT NOT NULL, `content` TEXT NOT NULL, `img` TEXT NOT NULL, `ext` TEXT NOT NULL, `postDate` INTEGER NOT NULL, PRIMARY KEY(`id`))")
                    database.execSQL("INSERT INTO `PostHistory2` SELECT `id`, `newPost`, `postTargetId`, `postTargetFid`, `postTargetPage`, `cookieName`, `content`, `img`, `ext`, `postDate` FROM PostHistory")
                    database.execSQL("DROP TABLE PostHistory")
                    database.execSQL("ALTER TABLE PostHistory2 RENAME TO PostHistory")
                }
            }
            // updates BrowsingHistory Table
            val migrate11To12 = object : Migration(11, 12) {
                override fun migrate(database: SupportSQLiteDatabase) {
                    database.execSQL("CREATE TABLE IF NOT EXISTS `BrowsingHistory2` (`id` INTEGER PRIMARY KEY AUTOINCREMENT, `browsedDate` INTEGER NOT NULL, `postId` TEXT NOT NULL, `postFid` TEXT NOT NULL, `pages` TEXT NOT NULL)")
                    database.execSQL("INSERT INTO `BrowsingHistory2` SELECT NULL,`date`, `postId`, `postFid`, `pages` FROM BrowsingHistory")
                    database.execSQL("DROP TABLE BrowsingHistory")
                    database.execSQL("ALTER TABLE BrowsingHistory2 RENAME TO BrowsingHistory")
                }
            }

            // adds feeds Table, drop category column in Post
            val migrate12To13 = object : Migration(12, 13) {
                override fun migrate(database: SupportSQLiteDatabase) {
                    database.execSQL("CREATE TABLE IF NOT EXISTS `Feed` (`id` INTEGER NOT NULL, `postId` TEXT NOT NULL, `category` TEXT NOT NULL, `lastUpdatedAt` INTEGER NOT NULL, PRIMARY KEY(`postId`))")
                    database.execSQL("CREATE TABLE IF NOT EXISTS `Post2` (`id` TEXT NOT NULL, `fid` TEXT NOT NULL, `img` TEXT NOT NULL, `ext` TEXT NOT NULL, `now` TEXT NOT NULL, `userid` TEXT NOT NULL, `name` TEXT NOT NULL, `email` TEXT NOT NULL, `title` TEXT NOT NULL, `content` TEXT NOT NULL, `sage` TEXT NOT NULL, `admin` TEXT NOT NULL, `status` TEXT NOT NULL, `replyCount` TEXT NOT NULL, `lastUpdatedAt` INTEGER NOT NULL, PRIMARY KEY(`id`))")
                    database.execSQL("INSERT INTO `Post2` SELECT `id`, `fid`, `img`, `ext`, `now`, `userid`, `name`, `email`, `title`, `content`, `sage`, `admin`, `status`, `replyCount`, `lastUpdatedAt` FROM Post")
                    database.execSQL("DROP TABLE Post")
                    database.execSQL("ALTER TABLE Post2 RENAME TO Post")
                }
            }
            // updates BrowsingHistory Table
            val migrate13To14 = object : Migration(13, 14) {
                override fun migrate(database: SupportSQLiteDatabase) {
                    database.execSQL("CREATE TABLE IF NOT EXISTS `BrowsingHistory2` (`browsedDate` INTEGER NOT NULL, `browsedTime` INTEGER NOT NULL, `postId` TEXT NOT NULL, `postFid` TEXT NOT NULL, `pages` TEXT NOT NULL, PRIMARY KEY(`browsedDate`, `postId`))")
                    database.execSQL("INSERT OR REPLACE INTO `BrowsingHistory2` VALUES((SELECT browsedDate FROM BrowsingHistory) - (SELECT browsedDate FROM BrowsingHistory) % 86400000, (SELECT browsedDate FROM BrowsingHistory) % 86400000, (SELECT postId FROM BrowsingHistory), (SELECT postFid FROM BrowsingHistory), (SELECT pages FROM BrowsingHistory))")
                    database.execSQL("DROP TABLE BrowsingHistory")
                    database.execSQL("ALTER TABLE BrowsingHistory2 RENAME TO BrowsingHistory")
                }
            }

            // adds page column to Feed
            val migrate14To15 = object : Migration(14, 15) {
                override fun migrate(database: SupportSQLiteDatabase) {
                    database.execSQL("DROP TABLE Feed")
                    database.execSQL("CREATE TABLE IF NOT EXISTS `Feed` (`id` INTEGER NOT NULL, `page` INTEGER NOT NULL, `postId` TEXT NOT NULL, `category` TEXT NOT NULL, `lastUpdatedAt` INTEGER NOT NULL, PRIMARY KEY(`postId`))")
                }
            }

            // adds blockedIds
            val migrate15To16 = object : Migration(15, 16) {
                override fun migrate(database: SupportSQLiteDatabase) {
                    database.execSQL("CREATE TABLE IF NOT EXISTS `BlockedId` (`id` TEXT NOT NULL, `type` INTEGER NOT NULL, PRIMARY KEY(`id`))")
                }
            }

            // adds Notification
            val migrate16To17 = object : Migration(16, 17) {
                override fun migrate(database: SupportSQLiteDatabase) {
                    database.execSQL("CREATE TABLE IF NOT EXISTS `Notification` (`id` TEXT NOT NULL, `fid` TEXT NOT NULL, `newReplyCount` INTEGER NOT NULL, `message` TEXT NOT NULL, `read` INTEGER NOT NULL, `lastUpdatedAt` INTEGER NOT NULL, PRIMARY KEY(`id`))")
                }
            }

            synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    DawnDatabase::class.java,
                    "dawnDB"
                )
                    .addMigrations(
                        migrate1To2,
                        migrate2To3,
                        migrate3To4,
                        migrate4To5,
                        migrate5To6,
                        migrate6To7,
                        migrate7To8,
                        migrate8To9,
                        migrate9To10,
                        migrate10To11,
                        migrate11To12,
                        migrate12To13,
                        migrate13To14,
                        migrate14To15,
                        migrate15To16,
                        migrate16To17
                    )
                    .build()
                INSTANCE = instance
                return instance
            }
        }
    }
}

