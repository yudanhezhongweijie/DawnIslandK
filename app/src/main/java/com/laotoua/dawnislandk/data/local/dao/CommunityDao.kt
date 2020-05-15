package com.laotoua.dawnislandk.data.local.dao

import androidx.room.*
import com.laotoua.dawnislandk.data.local.Community

@Dao
interface CommunityDao {
    @Query("SELECT * FROM community")
    suspend fun getAll(): List<Community>

    @Query("SELECT * FROM community WHERE id==:id")
    suspend fun getCommunityById(id: String): Community

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(communityList: List<Community>)

    @Delete
    suspend fun delete(community: Community)

    @Query("DELETE FROM community")
    fun nukeTable()
}