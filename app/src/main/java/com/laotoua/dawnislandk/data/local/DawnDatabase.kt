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
    entities = [Community::class,
        Cookie::class,
        Comment::class,
        Post::class,
        DailyTrend::class,
        NMBNotice::class,
        LuweiNotice::class,
        Release::class,
        ReadingPage::class,
        BrowsingHistory::class],
    version = 6
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
    abstract fun browsedPostDao(): BrowsingHistoryDao

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
            synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    DawnDatabase::class.java,
                    "dawnDB"
                )
                    .addMigrations(
                        MIGRATION_1_2,
                        MIGRATION_2_3,
                        MIGRATION_3_4,
                        MIGRATION_4_5,
                        MIGRATION_5_6
                    )
                    .build()
                INSTANCE = instance
                return instance
            }
        }

        /**
         *  DATABASE MIGRATIONS
         */

        // adds trends
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS `DailyTrend` (`id` TEXT NOT NULL, `po` TEXT NOT NULL, `date` INTEGER NOT NULL, `trends` TEXT NOT NULL,`lastReplyCount` INTEGER NOT NULL, `lastUpdatedAt` INTEGER NOT NULL, PRIMARY KEY(`id`))")
            }
        }

        // adds NMBNotice, LuweiNotice
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS `NMBNotice` (`id` INTEGER PRIMARY KEY AUTOINCREMENT, `content` TEXT NOT NULL, `date` INTEGER NOT NULL, `enable` INTEGER NOT NULL, `read` INTEGER NOT NULL, `lastUpdatedAt` INTEGER NOT NULL)")
                database.execSQL("CREATE TABLE IF NOT EXISTS `LuweiNotice` (`id` INTEGER PRIMARY KEY AUTOINCREMENT, `appVersion` TEXT NOT NULL, `beitaiForums` TEXT NOT NULL, `nmbForums` TEXT NOT NULL, `loadingMsgs` TEXT NOT NULL, `clientsInfo` TEXT NOT NULL, `whitelist` TEXT NOT NULL, `lastUpdatedAt` INTEGER NOT NULL)")
            }
        }

        // adds version checks
        private val MIGRATION_3_4 = object : Migration(3, 4) {
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
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE Reply RENAME TO Comment")
                database.execSQL("CREATE TABLE IF NOT EXISTS Post (`id` TEXT NOT NULL, `fid` TEXT NOT NULL, `category` TEXT NOT NULL, `img` TEXT NOT NULL, `ext` TEXT NOT NULL, `now` TEXT NOT NULL, `userid` TEXT NOT NULL, `name` TEXT NOT NULL, `email` TEXT NOT NULL, `title` TEXT NOT NULL, `content` TEXT NOT NULL, `sage` TEXT NOT NULL, `admin` TEXT NOT NULL, `status` TEXT NOT NULL, `replyCount` TEXT NOT NULL, `lastUpdatedAt` INTEGER NOT NULL, PRIMARY KEY(`id`))")
                database.execSQL("INSERT INTO Post SELECT id, fid, category, img, ext, now, userid, name, email, title, content, sage, admin, status, replyCount, lastUpdatedAt FROM Thread")
                database.execSQL("CREATE TABLE IF NOT EXISTS ReadingPage (`id` TEXT NOT NULL, `page` INTEGER NOT NULL, `lastUpdatedAt` INTEGER NOT NULL, PRIMARY KEY(`id`))")
                database.execSQL("INSERT INTO ReadingPage (id,page,lastUpdatedAt) SELECT id,readingProgress,lastUpdatedAt FROM Thread")
                database.execSQL("DROP Table Thread")
            }
        }

        // adds Browsing History
        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS BrowsingHistory (`date` INTEGER NOT NULL, `postId` TEXT NOT NULL, `postFid` TEXT NOT NULL, `pages` TEXT NOT NULL, PRIMARY KEY(`date`, `postId`))")
            }
        }


    }
}

