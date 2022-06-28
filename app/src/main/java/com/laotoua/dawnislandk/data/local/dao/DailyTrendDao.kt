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
import com.laotoua.dawnislandk.DawnApp
import com.laotoua.dawnislandk.data.local.entity.DailyTrend

@Dao
interface DailyTrendDao {
    @Query("SELECT * FROM DailyTrend WHERE domain=:domain")
    suspend fun getAll(domain: String = DawnApp.currentDomain): List<DailyTrend>

    @Query("SELECT * FROM DailyTrend ORDER BY date DESC LIMIT 7")
    suspend fun findLatestDailyTrendsSync(): List<DailyTrend>

    @Query("SELECT * FROM DailyTrend ORDER BY date DESC LIMIT 7")
    fun findLatestDailyTrends(): LiveData<List<DailyTrend>>

    fun findDistinctLatestDailyTrends(): LiveData<List<DailyTrend>> = findLatestDailyTrends().distinctUntilChanged()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(dailyTrend: DailyTrend)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(dailyTrends: List<DailyTrend>)

    @Delete
    suspend fun delete(dailyTrend: DailyTrend)

    @Query("DELETE FROM DailyTrend")
    suspend fun nukeTable()
}