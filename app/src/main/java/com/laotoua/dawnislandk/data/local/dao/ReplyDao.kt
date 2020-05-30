package com.laotoua.dawnislandk.data.local.dao

import androidx.lifecycle.LiveData
import androidx.lifecycle.distinctUntilChanged
import androidx.room.*
import com.laotoua.dawnislandk.data.local.Reply
import java.util.*

@Dao
interface ReplyDao {

    @Query("SELECT * FROM Reply WHERE parentId=:parentId")
    suspend fun findAllByParentId(parentId: String): List<Reply>

    @Query("SELECT * FROM Reply WHERE parentId=:parentId AND page<=:page")
    suspend fun findByParentIdUntilPage(parentId: String, page: Int): List<Reply>

    @Query("SELECT * FROM Reply WHERE parentId=:parentId AND page=:page")
    fun findPageByParentId(parentId: String, page: Int): LiveData<List<Reply>>

    fun findDistinctPageByParentId(parentId: String, page: Int):
            LiveData<List<Reply>> = findPageByParentId(parentId, page).distinctUntilChanged()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(replyList: List<Reply>)

    suspend fun insertAllWithTimeStamp(replyList: List<Reply>) {
        val timestamp = Date().time
        val listWithTimeStamps = replyList.apply { map { it.lastUpdatedAt = timestamp } }
        insertAll(listWithTimeStamps)
    }

    @Delete
    suspend fun delete(reply: Reply)

    @Query("DELETE FROM Reply")
    fun nukeTable()
}