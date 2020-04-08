package com.laotoua.dawnislandk.entities

import android.content.Context
import androidx.room.*


// TODO: export Schema
@Database(entities = [Forum::class], version = 1, exportSchema = false)
abstract class DawnDatabase : RoomDatabase() {
    abstract fun forumDao(): ForumDao

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
                    "dawn_database"
                ).build()
                INSTANCE = instance
                return instance
            }
        }
    }
}

@Dao
interface ForumDao {
    @Query("SELECT * FROM forum")
    suspend fun getAll(): List<Forum>

    @Query("SELECT * FROM forum WHERE id==:fid")
    suspend fun getForumById(fid: String): Forum

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(forumList: List<Forum>)

    @Delete
    suspend fun delete(forum: Forum)

    @Query("DELETE FROM forum")
    fun nukeTable()
}

@Dao
interface cookieDao {
    @Query("SELECT * FROM cookie")
    suspend fun getAll(): List<Cookie>

    @Query("SELECT * FROM cookie WHERE userhash==:userhash")
    suspend fun getForumById(userhash: String): Cookie

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(cookieList: List<Cookie>)

    @Update
    suspend fun updateCookie(cookie: Cookie)

    @Delete
    suspend fun delete(cookie: Cookie)

    @Query("DELETE FROM cookie")
    fun nukeTable()
}