package com.laotoua.dawnislandk.data.local.dao

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.laotoua.dawnislandk.data.local.*
import com.laotoua.dawnislandk.util.Constants
import timber.log.Timber

@Database(
    entities = [Community::class,
        Cookie::class,
        Reply::class,
        Thread::class,
        DailyTrend::class,
        NMBNotice::class,
        LuweiNotice::class,
        Release::class],
    version = 4,
    exportSchema = true
)
@TypeConverters(Converter::class)
abstract class DawnDatabase : RoomDatabase() {
    abstract fun cookieDao(): CookieDao
    abstract fun communityDao(): CommunityDao
    abstract fun replyDao(): ReplyDao
    abstract fun threadDao(): ThreadDao
    abstract fun dailyTrendDao(): DailyTrendDao
    abstract fun nmbNoticeDao(): NMBNoticeDao
    abstract fun luweiNoticeDao(): LuweiNoticeDao
    abstract fun releaseDao(): ReleaseDao

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
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
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
                    put("version", Constants.APP_VERSION)
                    put("downloadUrl", "")
                    put("message", "default entry")
                }
                database.insert("Release", SQLiteDatabase.CONFLICT_REPLACE, contentValue)
            }
        }
    }
}

