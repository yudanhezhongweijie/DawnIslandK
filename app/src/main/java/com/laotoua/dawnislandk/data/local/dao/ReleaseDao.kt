package com.laotoua.dawnislandk.data.local.dao

import androidx.room.*
import com.laotoua.dawnislandk.data.local.Release

@Dao
interface ReleaseDao {
    @Query("SELECT * From 'Release' ORDER BY id DESC LIMIT 1")
    suspend fun getLatestRelease(): Release?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRelease(release: Release)

    @Query("DELETE FROM 'Release'")
    suspend fun nukeTable()
}