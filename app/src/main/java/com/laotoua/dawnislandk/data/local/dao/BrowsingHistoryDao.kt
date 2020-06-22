package com.laotoua.dawnislandk.data.local.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.laotoua.dawnislandk.data.local.entity.BrowsingHistory
import com.laotoua.dawnislandk.data.local.entity.BrowsingHistoryAndPost

@Dao
interface BrowsingHistoryDao {
    @Transaction
    @Query("SELECT * From BrowsingHistory ORDER BY date DESC")
    fun getAllBrowsingHistoryAndPost(): LiveData<List<BrowsingHistoryAndPost>>


    @Query("SELECT * From BrowsingHistory ORDER BY date DESC")
    suspend fun getAllBrowsingHistory(): List<BrowsingHistory>

    @Query("SELECT * From BrowsingHistory WHERE date=:date")
    fun getBrowsingHistoryByDate(date:Long): LiveData<List<BrowsingHistory>>

    @Query("SELECT * From BrowsingHistory WHERE date=:date AND postId=:postId")
    suspend fun getBrowsingHistoryByDateAndIdSync(date:Long, postId:String): BrowsingHistory?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBrowsingHistory(browsingHistory: BrowsingHistory)

    @Query("DELETE FROM BrowsingHistory")
    suspend fun nukeTable()
}