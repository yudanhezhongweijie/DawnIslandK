/*
 *  Copyright 2020 Fishballzzz
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package com.laotoua.dawnislandk.data.local.dao

import androidx.lifecycle.LiveData
import androidx.lifecycle.distinctUntilChanged
import androidx.room.*
import com.laotoua.dawnislandk.data.local.entity.DailyTrend
import java.util.*

@Dao
interface DailyTrendDao {
    @Query("SELECT * FROM DailyTrend")
    suspend fun getAll(): List<DailyTrend>

    @Query("SELECT * FROM DailyTrend ORDER BY id DESC LIMIT 1")
    suspend fun findLatestDailyTrendSync(): DailyTrend?

    @Query("SELECT * FROM DailyTrend ORDER BY id DESC LIMIT 1")
    fun findLatestDailyTrend(): LiveData<DailyTrend>

    fun findDistinctLatestDailyTrend(): LiveData<DailyTrend> =
        findLatestDailyTrend().distinctUntilChanged()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(dailyTrend: DailyTrend)

    suspend fun insertWithTimeStamp(dailyTrend: DailyTrend) {
        dailyTrend.setUpdatedTimestamp()
        insert(dailyTrend)
    }

    @Update
    suspend fun updateDailyTrends(vararg dailyTrends: DailyTrend)

    @Update
    suspend fun updateDailyTrendsWithTimeStamp(vararg dailyTrends: DailyTrend) {
        val timestamp = Date().time
        dailyTrends.map { it.setUpdatedTimestamp(timestamp) }
        updateDailyTrends(*dailyTrends)
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(dailyTrendList: List<DailyTrend>)

    suspend fun insertAllWithTimeStamp(dailyTrendList: List<DailyTrend>) {
        val timestamp = Date().time
        val listWithTimeStamps = dailyTrendList.apply { map { it.setUpdatedTimestamp(timestamp) } }
        insertAll(listWithTimeStamps)
    }

    @Delete
    suspend fun delete(dailyTrend: DailyTrend)

    @Query("DELETE FROM DailyTrend")
    fun nukeTable()
}