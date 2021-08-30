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
import com.laotoua.dawnislandk.DawnApp
import com.laotoua.dawnislandk.data.local.entity.PostHistory
import java.time.LocalDateTime

@Dao
interface PostHistoryDao {
    @Query("SELECT * From PostHistory WHERE domain=:domain ORDER BY postDateTime DESC")
    fun getAllPostHistory(domain: String = DawnApp.currentDomain): LiveData<List<PostHistory>>

    @Transaction
    @Query("SELECT * From PostHistory WHERE postDateTime>=:startDate AND postDateTime<=:endDate AND domain=:domain ORDER BY postDateTime DESC ")
    fun getAllPostHistoryInDateRange(startDate: LocalDateTime, endDate: LocalDateTime, domain: String = DawnApp.currentDomain): LiveData<List<PostHistory>>

    @Query("SELECT * From PostHistory WHERE postDateTime=:date AND domain=:domain")
    fun getPostHistoryByDate(date: LocalDateTime, domain: String = DawnApp.currentDomain): LiveData<List<PostHistory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPostHistory(browsingHistory: PostHistory)

    @Query("DELETE FROM PostHistory")
    suspend fun nukeTable()
}