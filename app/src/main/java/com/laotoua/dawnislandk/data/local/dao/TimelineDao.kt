package com.laotoua.dawnislandk.data.local.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.laotoua.dawnislandk.DawnApp
import com.laotoua.dawnislandk.data.local.entity.Timeline

@Dao
interface TimelineDao {
    @Query("SELECT * FROM Timeline WHERE domain = :domain ")
    fun getAll(domain: String = DawnApp.currentDomain): LiveData<List<Timeline>>

    @Query("SELECT * FROM Timeline WHERE id=:id")
    suspend fun getTimelineById(id: String): Timeline

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(Timeline: Timeline)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(TimelineList: List<Timeline>)

    @Delete
    suspend fun delete(Timeline: Timeline)

    @Query("DELETE FROM Timeline")
    suspend fun nukeTable()
}