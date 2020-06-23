package com.laotoua.dawnislandk.data.local.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.laotoua.dawnislandk.data.local.entity.PostHistory

@Dao
interface PostHistoryDao {
    @Query("SELECT * From PostHistory ORDER BY postDate DESC")
    fun getAllPostHistory(): LiveData<List<PostHistory>>

    @Transaction
    @Query("SELECT * From PostHistory WHERE postDate>=:startDate AND postDate<=:endDate ORDER BY postDate DESC ")
    fun getAllPostHistoryInDateRange(startDate:Long, endDate:Long): LiveData<List<PostHistory>>

    @Query("SELECT * From PostHistory WHERE postDate=:date")
    fun getPostHistoryByDate(date:Long): LiveData<List<PostHistory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPostHistory(browsingHistory: PostHistory)

    @Query("DELETE FROM PostHistory")
    suspend fun nukeTable()
}