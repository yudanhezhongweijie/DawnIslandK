package com.laotoua.dawnislandk.data.local.dao

import androidx.lifecycle.LiveData
import androidx.lifecycle.distinctUntilChanged
import androidx.room.*
import com.laotoua.dawnislandk.data.local.Thread
import java.util.*

@Dao
interface ThreadDao {
    @Query("SELECT * FROM Thread")
    suspend fun getAll(): List<Thread>

    @Query("SELECT * FROM Thread WHERE id=:id")
    fun findThreadById(id: String): LiveData<Thread>

    @Query("SELECT readingProgress FROM Thread WHERE id=:id")
    suspend fun findThreadReadingProgressByIdSync(id: String): Int?

    fun findDistinctThreadById(id: String): LiveData<Thread> =
        findThreadById(id).distinctUntilChanged()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(thread: Thread)

    suspend fun insertWithTimeStamp(thread: Thread) {
        thread.setUpdatedTimestamp()
        insert(thread)
    }

    @Update
    suspend fun updateThreads(vararg threads: Thread)

    @Update
    suspend fun updateThreadsWithTimeStamp(vararg threads: Thread) {
        val timestamp = Date().time
        threads.map { it.setUpdatedTimestamp(timestamp) }
        updateThreads(*threads)
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(threadList: List<Thread>)

    suspend fun insertAllWithTimeStamp(threadList: List<Thread>) {
        val timestamp = Date().time
        val listWithTimeStamps = threadList.apply { map { it.setUpdatedTimestamp(timestamp) } }
        insertAll(listWithTimeStamps)
    }

    @Delete
    suspend fun delete(thread: Thread)

    @Query("DELETE FROM Thread")
    fun nukeTable()
}