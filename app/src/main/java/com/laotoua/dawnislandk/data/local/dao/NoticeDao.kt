package com.laotoua.dawnislandk.data.local.dao

import androidx.room.*
import com.laotoua.dawnislandk.data.local.NMBNotice

@Dao
interface NoticeDao {
    @Query("SELECT * From NMBNotice ORDER BY id DESC LIMIT 1")
    suspend fun getLatestNotice(): NMBNotice?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotice(NMBNotice: NMBNotice)

    @Query("UPDATE NMBNotice SET content=:content, enable=:enable, read=:read WHERE date=:date")
    suspend fun updateNotice(content:String,enable:Boolean, read:Boolean, date: Long)

    @Query("DELETE FROM NMBNotice")
    suspend fun nukeTable()

}