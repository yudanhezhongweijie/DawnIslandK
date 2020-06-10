package com.laotoua.dawnislandk.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.laotoua.dawnislandk.data.local.LuweiNotice

@Dao
interface LuweiNoticeDao {
    @Query("SELECT * From LuweiNotice ORDER BY id DESC LIMIT 1")
    suspend fun getLatestLuweiNotice(): LuweiNotice?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotice(luweiNotice: LuweiNotice)

    suspend fun insertNoticeWithTimestamp(luweiNotice: LuweiNotice){
        luweiNotice.setUpdatedTimestamp()
        insertNotice(luweiNotice)
    }

    @Query("DELETE FROM LuweiNotice")
    suspend fun nukeTable()
}