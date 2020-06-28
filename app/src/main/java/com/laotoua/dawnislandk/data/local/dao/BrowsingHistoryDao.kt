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
import androidx.room.*
import com.laotoua.dawnislandk.data.local.entity.BrowsingHistory
import com.laotoua.dawnislandk.data.local.entity.BrowsingHistoryAndPost

@Dao
interface BrowsingHistoryDao {
    @Transaction
    @Query("SELECT * From BrowsingHistory ORDER BY date DESC")
    fun getAllBrowsingHistoryAndPost(): LiveData<List<BrowsingHistoryAndPost>>

    @Transaction
    @Query("SELECT * From BrowsingHistory WHERE date>=:startDate AND date<=:endDate ORDER BY date DESC ")
    fun getAllBrowsingHistoryAndPostInDateRange(startDate:Long, endDate:Long): LiveData<List<BrowsingHistoryAndPost>>

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