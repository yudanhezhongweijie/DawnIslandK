package com.laotoua.dawnislandk.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.laotoua.dawnislandk.data.local.entity.ReadingPage

@Dao
interface ReadingPageDao {
    @Query("SELECT * From ReadingPage WHERE id=:id")
    suspend fun getReadingPageById(id:String): ReadingPage?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReadingPage(readingPage: ReadingPage)

    suspend fun insertReadingPageWithTimeStamp(readingPage: ReadingPage){
        readingPage.setUpdatedTimestamp()
        insertReadingPage(readingPage)
    }

    @Query("DELETE FROM ReadingPage")
    suspend fun nukeTable()
}