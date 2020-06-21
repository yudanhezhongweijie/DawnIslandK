package com.laotoua.dawnislandk.data.local.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.laotoua.dawnislandk.data.local.entity.BrowsedPost

@Dao
interface BrowsedPostDao {
    @Query("SELECT * From BrowsedPost")
    suspend fun getAllBrowsedPost(): List<BrowsedPost>

    @Query("SELECT * From BrowsedPost WHERE date=:date")
    fun getBrowsedPostByDate(date:Long): LiveData<List<BrowsedPost>>

    @Query("SELECT * From BrowsedPost WHERE date=:date AND postId=:postId")
    suspend fun getBrowsedPostByDateAndIdSync(date:Long, postId:String): BrowsedPost?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBrowsedPost(browsedPost: BrowsedPost)

    @Query("DELETE FROM BrowsedPost")
    suspend fun nukeTable()
}