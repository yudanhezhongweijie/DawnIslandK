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
        Timeline::class,
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
        Notification::class,
        Emoji::class],
    version = 26
)
@TypeConverters(Converter::class)
abstract class DawnDatabase : RoomDatabase() {
    abstract fun cookieDao(): CookieDao
    abstract fun communityDao(): CommunityDao
    abstract fun timelineDao(): TimelineDao
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
    abstract fun emojiDao(): EmojiDao

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
                    database.execSQL("INSERT OR IGNORE INTO `Post` SELECT id, fid, category, img, ext, now, userid, name, email, title, content, sage, admin, status, replyCount, lastUpdatedAt FROM Thread")
                    database.execSQL("CREATE TABLE IF NOT EXISTS `ReadingPage` (`id` TEXT NOT NULL, `page` INTEGER NOT NULL, `lastUpdatedAt` INTEGER NOT NULL, PRIMARY KEY(`id`))")
                    database.execSQL("INSERT OR IGNORE INTO `ReadingPage` (id,page,lastUpdatedAt) SELECT id,readingProgress,lastUpdatedAt FROM Thread")
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
                    database.execSQL("INSERT OR IGNORE INTO `PostHistory2` SELECT `id`, `newPost`, `postTargetId`, `postTargetFid`, `postTargetPage`, `cookieName`, `content`, `img`, `ext`, `postDate` FROM PostHistory")
                    database.execSQL("DROP TABLE PostHistory")
                    database.execSQL("ALTER TABLE PostHistory2 RENAME TO PostHistory")
                }
            }
            // updates BrowsingHistory Table
            val migrate11To12 = object : Migration(11, 12) {
                override fun migrate(database: SupportSQLiteDatabase) {
                    database.execSQL("CREATE TABLE IF NOT EXISTS `BrowsingHistory2` (`id` INTEGER PRIMARY KEY AUTOINCREMENT, `browsedDate` INTEGER NOT NULL, `postId` TEXT NOT NULL, `postFid` TEXT NOT NULL, `pages` TEXT NOT NULL)")
                    database.execSQL("INSERT OR IGNORE INTO `BrowsingHistory2` SELECT NULL,`date`, `postId`, `postFid`, `pages` FROM BrowsingHistory")
                    database.execSQL("DROP TABLE BrowsingHistory")
                    database.execSQL("ALTER TABLE BrowsingHistory2 RENAME TO BrowsingHistory")
                }
            }

            // adds feeds Table, drop category column in Post
            val migrate12To13 = object : Migration(12, 13) {
                override fun migrate(database: SupportSQLiteDatabase) {
                    database.execSQL("CREATE TABLE IF NOT EXISTS `Feed` (`id` INTEGER NOT NULL, `postId` TEXT NOT NULL, `category` TEXT NOT NULL, `lastUpdatedAt` INTEGER NOT NULL, PRIMARY KEY(`postId`))")
                    database.execSQL("CREATE TABLE IF NOT EXISTS `Post2` (`id` TEXT NOT NULL, `fid` TEXT NOT NULL, `img` TEXT NOT NULL, `ext` TEXT NOT NULL, `now` TEXT NOT NULL, `userid` TEXT NOT NULL, `name` TEXT NOT NULL, `email` TEXT NOT NULL, `title` TEXT NOT NULL, `content` TEXT NOT NULL, `sage` TEXT NOT NULL, `admin` TEXT NOT NULL, `status` TEXT NOT NULL, `replyCount` TEXT NOT NULL, `lastUpdatedAt` INTEGER NOT NULL, PRIMARY KEY(`id`))")
                    database.execSQL("INSERT OR IGNORE INTO `Post2` SELECT `id`, `fid`, `img`, `ext`, `now`, `userid`, `name`, `email`, `title`, `content`, `sage`, `admin`, `status`, `replyCount`, `lastUpdatedAt` FROM Post")
                    database.execSQL("DROP TABLE Post")
                    database.execSQL("ALTER TABLE Post2 RENAME TO Post")
                }
            }
            // updates BrowsingHistory Table
            val migrate13To14 = object : Migration(13, 14) {
                override fun migrate(database: SupportSQLiteDatabase) {
                    database.execSQL("DROP TABLE BrowsingHistory")
                    database.execSQL("CREATE TABLE IF NOT EXISTS `BrowsingHistory` (`browsedDate` INTEGER NOT NULL, `browsedTime` INTEGER NOT NULL, `postId` TEXT NOT NULL, `postFid` TEXT NOT NULL, `pages` TEXT NOT NULL, PRIMARY KEY(`browsedDate`, `postId`))")
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

            // adds timestamp to Cookie to order cookie by last used time
            val migrate17To18 = object : Migration(17, 18) {
                override fun migrate(database: SupportSQLiteDatabase) {
                    try {
                        database.execSQL("CREATE TABLE IF NOT EXISTS `Cookie2` (`cookieHash` TEXT NOT NULL, `cookieName` TEXT NOT NULL, `cookieDisplayName` TEXT NOT NULL, `lastUsedAt` INTEGER NOT NULL, PRIMARY KEY(`cookieHash`))")
                        database.execSQL("INSERT OR REPLACE INTO `Cookie2` SELECT cookieHash, cookieName, cookieDisplayName, 0 FROM Cookie")
                        database.execSQL("DROP TABLE Cookie")
                        database.execSQL("ALTER TABLE Cookie2 RENAME TO Cookie")
                    } catch (e: Exception) {
                        database.execSQL("DROP TABLE Cookie")
                        database.execSQL("CREATE TABLE IF NOT EXISTS `Cookie` (`cookieHash` TEXT NOT NULL, `cookieName` TEXT NOT NULL, `cookieDisplayName` TEXT NOT NULL, `lastUsedAt` INTEGER NOT NULL, PRIMARY KEY(`cookieHash`))")
                    }
                }
            }

            // allow custom Emoji
            val migrate18To19 = object : Migration(18, 19) {
                override fun migrate(database: SupportSQLiteDatabase) {
                    database.execSQL("CREATE TABLE IF NOT EXISTS `Emoji` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `value` TEXT NOT NULL, `userDefined` INTEGER NOT NULL, `lastUsedAt` INTEGER NOT NULL)")
                }
            }

            // now store datetime text instead of epoch
            val migrate19To20 = object : Migration(19, 20) {
                override fun migrate(database: SupportSQLiteDatabase) {
                    // BrowsingHistory
                    database.execSQL("CREATE TABLE IF NOT EXISTS `BrowsingHistory2` (`id` INTEGER NOT NULL, `browsedDateTime` TEXT NOT NULL, `postId` TEXT NOT NULL, `postFid` TEXT NOT NULL, `pages` TEXT NOT NULL, PRIMARY KEY(`id`))")
                    database.execSQL("INSERT OR IGNORE INTO `BrowsingHistory2`(browsedDateTime, postId, postFid, pages) SELECT DATETIME(ROUND((browsedDate + browsedTime)/1000), 'unixepoch'), postId, postFid, pages FROM BrowsingHistory")
                    database.execSQL("DROP TABLE `BrowsingHistory`")
                    database.execSQL("ALTER TABLE BrowsingHistory2 RENAME TO BrowsingHistory")

                    // Comment
                    database.execSQL("CREATE TABLE IF NOT EXISTS `Comment2` (`id` TEXT NOT NULL, `userid` TEXT NOT NULL, `name` TEXT NOT NULL, `sage` TEXT NOT NULL, `admin` TEXT NOT NULL, `status` TEXT NOT NULL, `title` TEXT NOT NULL, `email` TEXT NOT NULL, `now` TEXT NOT NULL, `content` TEXT NOT NULL, `img` TEXT NOT NULL, `ext` TEXT NOT NULL, `page` INTEGER NOT NULL, `parentId` TEXT NOT NULL, `lastUpdatedAt` TEXT NOT NULL, PRIMARY KEY(`id`))")
                    database.execSQL("INSERT OR IGNORE INTO `Comment2`(`id`, `userid`, `name`, `sage`, `admin`, `status`, `title`, `email`, `now`, `content`, `img`, `ext`, `page`, `parentId`, `lastUpdatedAt`) SELECT `id`, `userid`, `name`, `sage`, `admin`, `status`, `title`, `email`, `now`, `content`, `img`, `ext`, `page`, `parentId`, DATETIME(ROUND(lastUpdatedAt/1000), 'unixepoch') FROM Comment")
                    database.execSQL("DROP TABLE `Comment`")
                    database.execSQL("ALTER TABLE Comment2 RENAME TO Comment")

                    // Cookie
                    database.execSQL("CREATE TABLE IF NOT EXISTS `Cookie2` (`cookieHash` TEXT NOT NULL, `cookieName` TEXT NOT NULL, `cookieDisplayName` TEXT NOT NULL, `lastUsedAt` TEXT NOT NULL, PRIMARY KEY(`cookieHash`))")
                    database.execSQL("INSERT OR IGNORE INTO `Cookie2`(`cookieHash`, `cookieName`, `cookieDisplayName`, `lastUsedAt`) SELECT `cookieHash`, `cookieName`, `cookieDisplayName`, DATETIME(ROUND(lastUsedAt/1000), 'unixepoch') FROM Cookie")
                    database.execSQL("DROP TABLE `Cookie`")
                    database.execSQL("ALTER TABLE Cookie2 RENAME TO Cookie")

                    // DailyTrend
                    database.execSQL("CREATE TABLE IF NOT EXISTS `DailyTrend2` (`id` TEXT NOT NULL, `po` TEXT NOT NULL, `date` TEXT NOT NULL, `trends` TEXT NOT NULL, `lastReplyCount` INTEGER NOT NULL, PRIMARY KEY(`id`))")
                    database.execSQL("INSERT OR IGNORE INTO `DailyTrend2`(`id`, `po`, `date`, `trends`, `lastReplyCount`) SELECT `id`, `po`, DATETIME(ROUND(date/1000), 'unixepoch'), `trends`, `lastReplyCount` FROM DailyTrend")
                    database.execSQL("DROP TABLE `DailyTrend`")
                    database.execSQL("ALTER TABLE DailyTrend2 RENAME TO DailyTrend")

                    // Emoji
                    database.execSQL("CREATE TABLE IF NOT EXISTS `Emoji2` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `value` TEXT NOT NULL, `userDefined` INTEGER NOT NULL, `lastUsedAt` TEXT NOT NULL)")
                    database.execSQL("INSERT OR IGNORE INTO `Emoji2`(`id`, `name`, `value`, `userDefined`, `lastUsedAt`) SELECT `id`, `name`, `value`, `userDefined`, DATETIME(ROUND(lastUsedAt/1000), 'unixepoch') FROM Emoji")
                    database.execSQL("DROP TABLE `Emoji`")
                    database.execSQL("ALTER TABLE Emoji2 RENAME TO Emoji")

                    // Feed
                    database.execSQL("CREATE TABLE IF NOT EXISTS `Feed2` (`id` INTEGER NOT NULL, `page` INTEGER NOT NULL, `postId` TEXT NOT NULL, `category` TEXT NOT NULL, `lastUpdatedAt` TEXT NOT NULL, PRIMARY KEY(`postId`))")
                    database.execSQL("INSERT OR IGNORE INTO `Feed2`(`id`, `page`, `postId`, `category`, `lastUpdatedAt`) SELECT `id`, `page`, `postId`, `category`, DATETIME(ROUND(lastUpdatedAt/1000), 'unixepoch') FROM Feed")
                    database.execSQL("DROP TABLE `Feed`")
                    database.execSQL("ALTER TABLE Feed2 RENAME TO Feed")

                    // LuweiNotice
                    database.execSQL("CREATE TABLE IF NOT EXISTS `LuweiNotice2` (`id` INTEGER PRIMARY KEY AUTOINCREMENT, `appVersion` TEXT NOT NULL, `beitaiForums` TEXT NOT NULL, `nmbForums` TEXT NOT NULL, `loadingMsgs` TEXT NOT NULL, `clientsInfo` TEXT NOT NULL, `whitelist` TEXT NOT NULL, `lastUpdatedAt` TEXT NOT NULL)")
                    database.execSQL("INSERT OR IGNORE INTO `LuweiNotice2`(`id`, `appVersion` , `beitaiForums`, `nmbForums`, `loadingMsgs`, `clientsInfo`, `whitelist`, `lastUpdatedAt`) SELECT `id`, `appVersion` , `beitaiForums`, `nmbForums`, `loadingMsgs`, `clientsInfo`, `whitelist`, DATETIME(ROUND(lastUpdatedAt/1000), 'unixepoch') FROM LuweiNotice")
                    database.execSQL("DROP TABLE `LuweiNotice`")
                    database.execSQL("ALTER TABLE LuweiNotice2 RENAME TO LuweiNotice")

                    // NMBNotice
                    database.execSQL("CREATE TABLE IF NOT EXISTS `NMBNotice2` (`id` INTEGER PRIMARY KEY AUTOINCREMENT, `content` TEXT NOT NULL, `date` INTEGER NOT NULL, `enable` INTEGER NOT NULL, `read` INTEGER NOT NULL, `lastUpdatedAt` TEXT NOT NULL)")
                    database.execSQL("INSERT OR IGNORE INTO `NMBNotice2`(`id`, `content`, `date`, `enable`, `read`, `lastUpdatedAt`) SELECT `id`, `content`, `date`, `enable`, `read`, DATETIME(ROUND(lastUpdatedAt/1000), 'unixepoch') FROM NMBNotice")
                    database.execSQL("DROP TABLE `NMBNotice`")
                    database.execSQL("ALTER TABLE NMBNotice2 RENAME TO NMBNotice")

                    // Notification
                    database.execSQL("CREATE TABLE IF NOT EXISTS `Notification2` (`id` TEXT NOT NULL, `fid` TEXT NOT NULL, `newReplyCount` INTEGER NOT NULL, `message` TEXT NOT NULL, `read` INTEGER NOT NULL, `lastUpdatedAt` TEXT NOT NULL, PRIMARY KEY(`id`))")
                    database.execSQL("INSERT OR IGNORE INTO `Notification2`(`id`, `fid`, `newReplyCount`, `message`, `read`, `lastUpdatedAt`) SELECT `id`, `fid`, `newReplyCount`, `message`, `read`, DATETIME(ROUND(lastUpdatedAt/1000), 'unixepoch') FROM Notification")
                    database.execSQL("DROP TABLE `Notification`")
                    database.execSQL("ALTER TABLE Notification2 RENAME TO Notification")

                    // Post
                    database.execSQL("CREATE TABLE IF NOT EXISTS `Post2` (`id` TEXT NOT NULL, `fid` TEXT NOT NULL, `img` TEXT NOT NULL, `ext` TEXT NOT NULL, `now` TEXT NOT NULL, `userid` TEXT NOT NULL, `name` TEXT NOT NULL, `email` TEXT NOT NULL, `title` TEXT NOT NULL, `content` TEXT NOT NULL, `sage` TEXT NOT NULL, `admin` TEXT NOT NULL, `status` TEXT NOT NULL, `replyCount` TEXT NOT NULL, `lastUpdatedAt` TEXT NOT NULL, PRIMARY KEY(`id`))")
                    database.execSQL("INSERT OR IGNORE INTO `Post2`(`id`, `fid`, `img`, `ext`, `now`, `userid`, `name`, `email`, `title`, `content`, `sage`, `admin`, `status`, `replyCount`, `lastUpdatedAt`) SELECT `id`, `fid`, `img`, `ext`, `now`, `userid`, `name`, `email`, `title`, `content`, `sage`, `admin`, `status`, `replyCount`, DATETIME(ROUND(lastUpdatedAt/1000), 'unixepoch') FROM Post")
                    database.execSQL("DROP TABLE `Post`")
                    database.execSQL("ALTER TABLE Post2 RENAME TO Post")

                    // PostHistory
                    database.execSQL("CREATE TABLE IF NOT EXISTS `PostHistory2` (`id` TEXT NOT NULL, `newPost` INTEGER NOT NULL, `postTargetId` TEXT NOT NULL, `postTargetFid` TEXT NOT NULL, `postTargetPage` INTEGER NOT NULL, `cookieName` TEXT NOT NULL, `content` TEXT NOT NULL, `img` TEXT NOT NULL, `ext` TEXT NOT NULL, `postDateTime` TEXT NOT NULL, PRIMARY KEY(`id`))")
                    database.execSQL("INSERT OR IGNORE INTO `PostHistory2`(`id`, `newPost`, `postTargetId`, `postTargetFid`, `postTargetPage`, `cookieName`, `content`, `img`, `ext`, `postDateTime`) SELECT `id`, `newPost`, `postTargetId`, `postTargetFid`, `postTargetPage`, `cookieName`, `content`, `img`, `ext`, DATETIME(ROUND(postDate/1000), 'unixepoch') FROM PostHistory")
                    database.execSQL("DROP TABLE `PostHistory`")
                    database.execSQL("ALTER TABLE PostHistory2 RENAME TO PostHistory")

                    // ReadingPage
                    database.execSQL("CREATE TABLE IF NOT EXISTS `ReadingPage2` (`id` TEXT NOT NULL, `page` INTEGER NOT NULL, `lastUpdatedAt` TEXT NOT NULL, PRIMARY KEY(`id`))")
                    database.execSQL("INSERT OR IGNORE INTO `ReadingPage2`(`id`, `page`, `lastUpdatedAt`) SELECT `id`, `page`, DATETIME(ROUND(lastUpdatedAt/1000), 'unixepoch') FROM ReadingPage")
                    database.execSQL("DROP TABLE `ReadingPage`")
                    database.execSQL("ALTER TABLE ReadingPage2 RENAME TO ReadingPage")

                    // Release
                    database.execSQL("CREATE TABLE IF NOT EXISTS `Release2` (`id` INTEGER NOT NULL, `version` TEXT NOT NULL, `downloadUrl` TEXT NOT NULL, `message` TEXT NOT NULL, `lastUpdatedAt` TEXT NOT NULL, PRIMARY KEY(`id`))")
                    database.execSQL("INSERT OR IGNORE INTO `Release2`(`id`, `version`, `downloadUrl`, `message`, `lastUpdatedAt`) SELECT `id`, `version`, `downloadUrl`, `message`, DATETIME(ROUND(lastUpdatedAt/1000), 'unixepoch') FROM `Release`")
                    database.execSQL("DROP TABLE `Release`")
                    database.execSQL("ALTER TABLE Release2 RENAME TO `Release`")
                }
            }

            // now store datetime text instead of epoch
            val migrate20To21 = object : Migration(20, 21) {
                override fun migrate(database: SupportSQLiteDatabase) {
                    // DailyTrend
                    database.execSQL("CREATE TABLE IF NOT EXISTS `DailyTrend2` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `postId` TEXT NOT NULL, `po` TEXT NOT NULL, `date` TEXT NOT NULL, `trends` TEXT NOT NULL, `lastReplyCount` INTEGER NOT NULL)")
                    database.execSQL("INSERT OR IGNORE INTO `DailyTrend2`(`postId`, `po`, `date`, `trends`, `lastReplyCount`) SELECT `id`, `po`, `date`, `trends`, `lastReplyCount` FROM DailyTrend")
                    database.execSQL("DROP TABLE `DailyTrend`")
                    database.execSQL("ALTER TABLE DailyTrend2 RENAME TO DailyTrend")
                }
            }

            // change trendDB primary key from id to date
            val migrate21To22 = object : Migration(21, 22) {
                override fun migrate(database: SupportSQLiteDatabase) {
                    // DailyTrend
                    database.execSQL("CREATE TABLE IF NOT EXISTS `DailyTrend2` (`postId` TEXT NOT NULL, `po` TEXT NOT NULL, `date` TEXT PRIMARY KEY NOT NULL, `trends` TEXT NOT NULL, `lastReplyCount` INTEGER NOT NULL)")
                    database.execSQL("INSERT OR IGNORE INTO `DailyTrend2`(`postId`, `po`, `date`, `trends`, `lastReplyCount`) SELECT `postId`, `po`, `date`, `trends`, `lastReplyCount` FROM DailyTrend")
                    database.execSQL("DROP TABLE `DailyTrend`")
                    database.execSQL("ALTER TABLE DailyTrend2 RENAME TO DailyTrend")
                }
            }

            // add timelineDb
            val migrate22To23 = object : Migration(22, 23) {
                override fun migrate(database: SupportSQLiteDatabase) {
                    database.execSQL("CREATE TABLE IF NOT EXISTS `Timeline` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `display_name` TEXT NOT NULL, `notice` TEXT NOT NULL, `max_page` INTEGER NOT NULL, PRIMARY KEY(`id`))")
                }
            }

            // add domain flags
            val migrate23To24 = object : Migration(23, 24) {
                override fun migrate(database: SupportSQLiteDatabase) {
                    database.execSQL("ALTER TABLE `BlockedId` ADD COLUMN `domain` TEXT NOT NULL DEFAULT 'nmbxd'")
                    database.execSQL("ALTER TABLE `BrowsingHistory` ADD COLUMN `domain` TEXT NOT NULL DEFAULT 'nmbxd'")
                    database.execSQL("ALTER TABLE `Comment` ADD COLUMN `domain` TEXT NOT NULL DEFAULT 'nmbxd'")
                    database.execSQL("ALTER TABLE `Community` ADD COLUMN `domain` TEXT NOT NULL DEFAULT 'nmbxd'")
                    database.execSQL("ALTER TABLE `Cookie` ADD COLUMN `domain` TEXT NOT NULL DEFAULT 'nmbxd'")
                    database.execSQL("ALTER TABLE `Feed` ADD COLUMN `domain` TEXT NOT NULL DEFAULT 'nmbxd'")
                    database.execSQL("ALTER TABLE `Notification` ADD COLUMN `domain` TEXT NOT NULL DEFAULT 'nmbxd'")
                    database.execSQL("ALTER TABLE `Post` ADD COLUMN `domain` TEXT NOT NULL DEFAULT 'nmbxd'")
                    database.execSQL("ALTER TABLE `PostHistory` ADD COLUMN `domain` TEXT NOT NULL DEFAULT 'nmbxd'")
                    database.execSQL("ALTER TABLE `ReadingPage` ADD COLUMN `domain` TEXT NOT NULL DEFAULT 'nmbxd'")
                }
            }

            // update primaryKeys to include domain
            val migrate24To25 = object : Migration(24, 25) {
                override fun migrate(database: SupportSQLiteDatabase) {
                    database.execSQL("CREATE TABLE IF NOT EXISTS `BlockedId2` (`id` TEXT NOT NULL, `type` INTEGER NOT NULL, `domain` TEXT NOT NULL, PRIMARY KEY(`id`,`domain`))")
                    database.execSQL("INSERT OR IGNORE INTO `BlockedId2`(`id`, `type`, `domain`) SELECT `id`, `type`, `domain` FROM BlockedId")
                    database.execSQL("DROP TABLE `BlockedId`")
                    database.execSQL("ALTER TABLE BlockedId2 RENAME TO BlockedId")


                    database.execSQL("CREATE TABLE IF NOT EXISTS `Comment2` (`id` TEXT NOT NULL, `userid` TEXT NOT NULL, `name` TEXT NOT NULL, `sage` TEXT NOT NULL, `admin` TEXT NOT NULL, `status` TEXT NOT NULL, `title` TEXT NOT NULL, `email` TEXT NOT NULL, `now` TEXT NOT NULL, `content` TEXT NOT NULL, `img` TEXT NOT NULL, `ext` TEXT NOT NULL, `page` INTEGER NOT NULL, `parentId` TEXT NOT NULL, `domain` TEXT NOT NULL, `lastUpdatedAt` TEXT NOT NULL, PRIMARY KEY(`id`, `domain`))")
                    database.execSQL("INSERT OR IGNORE INTO `Comment2`(`id`, `userid`, `name`, `sage`, `admin`, `status`, `title` , `email`, `now`, `content`, `img`, `ext`, `page`, `parentId`, `domain`, `lastUpdatedAt`) SELECT `id`, `userid`, `name`, `sage`, `admin`, `status`, `title` , `email`, `now`, `content`, `img`, `ext`, `page`, `parentId`, `domain`, `lastUpdatedAt` FROM Comment")
                    database.execSQL("DROP TABLE `Comment`")
                    database.execSQL("ALTER TABLE Comment2 RENAME TO Comment")


                    database.execSQL("CREATE TABLE IF NOT EXISTS `Community2` (`id` TEXT NOT NULL, `sort` TEXT NOT NULL, `name` TEXT NOT NULL, `status` TEXT NOT NULL, `forums` TEXT NOT NULL, `domain` TEXT NOT NULL, PRIMARY KEY(`id`, `domain`))")
                    database.execSQL("INSERT OR IGNORE INTO `Community2`(`id`, `sort`, `name`, `forums`, `domain`) SELECT `id`, `sort`, `name`, `forums`, `domain` FROM Community")
                    database.execSQL("DROP TABLE `Community`")
                    database.execSQL("ALTER TABLE Community2 RENAME TO Community")


                    database.execSQL("CREATE TABLE IF NOT EXISTS `Cookie2` (`cookieHash` TEXT NOT NULL, `cookieName` TEXT NOT NULL, `cookieDisplayName` TEXT NOT NULL, `lastUsedAt` TEXT NOT NULL, `domain` TEXT NOT NULL, PRIMARY KEY(`cookieHash`,`domain`))")
                    database.execSQL("INSERT OR IGNORE INTO `Cookie2`(`cookieHash`, `cookieName`, `cookieDisplayName`, `lastUsedAt`, `domain`) SELECT `cookieHash`, `cookieName`, `cookieDisplayName`, `lastUsedAt`, `domain` FROM Cookie")
                    database.execSQL("DROP TABLE `Cookie`")
                    database.execSQL("ALTER TABLE Cookie2 RENAME TO Cookie")


                    database.execSQL("ALTER TABLE `DailyTrend` ADD COLUMN `domain` TEXT NOT NULL DEFAULT 'nmbxd'")
                    database.execSQL("CREATE TABLE IF NOT EXISTS `DailyTrend2` (`postId` TEXT NOT NULL, `po` TEXT NOT NULL, `date` TEXT NOT NULL, `trends` TEXT NOT NULL, `lastReplyCount` INTEGER NOT NULL, `domain` TEXT NOT NULL, PRIMARY KEY(`date`, `domain`))")
                    database.execSQL("INSERT OR IGNORE INTO `DailyTrend2`(`postId`, `po`, `date`, `trends`, `lastReplyCount`, `domain`) SELECT `postId`, `po`, `date`, `trends`, `lastReplyCount`, `domain` FROM DailyTrend")
                    database.execSQL("DROP TABLE `DailyTrend`")
                    database.execSQL("ALTER TABLE DailyTrend2 RENAME TO DailyTrend")


                    database.execSQL("CREATE TABLE IF NOT EXISTS `Feed2` (`id` INTEGER NOT NULL, `page` INTEGER NOT NULL, `postId` TEXT NOT NULL, `category` TEXT NOT NULL, `domain` TEXT NOT NULL, `lastUpdatedAt` TEXT NOT NULL, PRIMARY KEY(`postId`,`domain`))")
                    database.execSQL("INSERT OR IGNORE INTO `Feed2`(`id`, `page`, `postId`, `category`, `domain`, `lastUpdatedAt`) SELECT `id`, `page`, `postId`, `category`, `domain`, `lastUpdatedAt` FROM Feed")
                    database.execSQL("DROP TABLE `Feed`")
                    database.execSQL("ALTER TABLE Feed2 RENAME TO Feed")


                    database.execSQL("CREATE TABLE IF NOT EXISTS `Notification2` (`id` TEXT NOT NULL, `fid` TEXT NOT NULL, `newReplyCount` INTEGER NOT NULL, `message` TEXT NOT NULL, `read` INTEGER NOT NULL, `domain` TEXT NOT NULL, `lastUpdatedAt` TEXT NOT NULL, PRIMARY KEY(`id`, `domain`))")
                    database.execSQL("INSERT OR IGNORE INTO `Notification2`(`id`, `fid`, `newReplyCount`, `message`, `read`, `domain`, `lastUpdatedAt`) SELECT `id`, `fid`, `newReplyCount`, `message`, `read`, `domain`, `lastUpdatedAt` FROM Notification")
                    database.execSQL("DROP TABLE `Notification`")
                    database.execSQL("ALTER TABLE Notification2 RENAME TO Notification")



                    database.execSQL("CREATE TABLE IF NOT EXISTS `Post2` (`id` TEXT NOT NULL, `fid` TEXT NOT NULL, `img` TEXT NOT NULL, `ext` TEXT NOT NULL, `now` TEXT NOT NULL, `userid` TEXT NOT NULL, `name` TEXT NOT NULL, `email` TEXT NOT NULL, `title` TEXT NOT NULL, `content` TEXT NOT NULL, `sage` TEXT NOT NULL, `admin` TEXT NOT NULL, `status` TEXT NOT NULL, `replyCount` TEXT NOT NULL, `domain` TEXT NOT NULL, `lastUpdatedAt` TEXT NOT NULL, PRIMARY KEY(`id`, `domain`))")
                    database.execSQL("INSERT OR IGNORE INTO `Post2`(`id`, `fid`, `img`, `ext`, `now`, `userid`, `name` , `email`, `title`, `content`, `sage`, `admin`, `status`, `replyCount`, `domain`, `lastUpdatedAt`) SELECT `id`, `fid`, `img`, `ext`, `now`, `userid`, `name` , `email`, `title`, `content`, `sage`, `admin`, `status`, `replyCount`, `domain`, `lastUpdatedAt` FROM Post")
                    database.execSQL("DROP TABLE `Post`")
                    database.execSQL("ALTER TABLE Post2 RENAME TO Post")



                    database.execSQL("CREATE TABLE IF NOT EXISTS `PostHistory2` (`id` TEXT NOT NULL, `newPost` INTEGER NOT NULL, `postTargetId` TEXT NOT NULL, `postTargetFid` TEXT NOT NULL, `postTargetPage` INTEGER NOT NULL, `cookieName` TEXT NOT NULL, `content` TEXT NOT NULL, `img` TEXT NOT NULL, `ext` TEXT NOT NULL, `domain` TEXT NOT NULL, `postDateTime` TEXT NOT NULL, PRIMARY KEY(`id`,`domain`))")
                    database.execSQL("INSERT OR IGNORE INTO `PostHistory2`(`id`, `newPost`, `postTargetId`, `postTargetFid`, `postTargetPage`,`cookieName`, `content`, `img`, `ext`, `domain`, `postDateTime`) SELECT `id`, `newPost`, `postTargetId`, `postTargetFid`, `postTargetPage`,`cookieName`, `content`, `img`, `ext`, `domain`, `postDateTime` FROM PostHistory")
                    database.execSQL("DROP TABLE `PostHistory`")
                    database.execSQL("ALTER TABLE PostHistory2 RENAME TO PostHistory")


                    database.execSQL("CREATE TABLE IF NOT EXISTS `ReadingPage2` (`id` TEXT NOT NULL, `page` INTEGER NOT NULL, `domain` TEXT NOT NULL, `lastUpdatedAt` TEXT NOT NULL, PRIMARY KEY(`id`, `domain`))")
                    database.execSQL("INSERT OR IGNORE INTO `ReadingPage2`(`id`, `page`, `domain`, `lastUpdatedAt`) SELECT `id`, `page`, `domain`, `lastUpdatedAt` FROM ReadingPage2")
                    database.execSQL("DROP TABLE `ReadingPage`")
                    database.execSQL("ALTER TABLE ReadingPage2 RENAME TO ReadingPage")


                    database.execSQL("DROP TABLE `Timeline`")
                    database.execSQL("CREATE TABLE IF NOT EXISTS `Timeline` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `display_name` TEXT NOT NULL, `notice` TEXT NOT NULL, `max_page` INTEGER NOT NULL, `domain` TEXT NOT NULL, PRIMARY KEY(`id`, `domain`))")
                }
            }

            val migrate25To26 = object : Migration(25, 26) {
                override fun migrate(database: SupportSQLiteDatabase) {
                    database.execSQL("ALTER TABLE `Post` RENAME COLUMN userid TO userHash;")
                    database.execSQL("ALTER TABLE `Comment` RENAME COLUMN userid TO userHash;")
                }

            }

            synchronized(this) {
                val instance = Room.databaseBuilder(context.applicationContext, DawnDatabase::class.java, "dawnDB")
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
                        migrate16To17,
                        migrate17To18,
                        migrate18To19,
                        migrate19To20,
                        migrate20To21,
                        migrate21To22,
                        migrate22To23,
                        migrate23To24,
                        migrate24To25,
                        migrate25To26
                    )
                    .build()
                INSTANCE = instance
                return instance
            }
        }
    }
}

