package com.laotoua.dawnislandk.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.laotoua.dawnislandk.data.local.entity.NMBNotice
import java.util.*

@Dao
interface NMBNoticeDao {
    @Query("SELECT * From NMBNotice ORDER BY id DESC LIMIT 1")
    suspend fun getLatestNMBNotice(): NMBNotice?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNMBNotice(notice: NMBNotice)

    suspend fun insertNMBNoticeWithTimestamp(notice: NMBNotice) {
        notice.setUpdatedTimestamp()
        insertNMBNotice(notice)
    }

    @Query("UPDATE NMBNotice SET content=:content, enable=:enable, read=:read,lastUpdatedAt=:lastUpdatedAt WHERE date=:date")
    suspend fun updateNMBNotice(
        content: String,
        enable: Boolean,
        read: Boolean,
        date: Long,
        lastUpdatedAt: Long
    )

    suspend fun updateNMBNoticeWithTimestamp(
        content: String,
        enable: Boolean,
        read: Boolean,
        date: Long,
        lastUpdatedAt: Long? = null
    ) {
        val timestamp = lastUpdatedAt ?: Date().time
        updateNMBNotice(content, enable, read, date, timestamp)
    }

    @Query("DELETE FROM NMBNotice")
    suspend fun nukeTable()

}