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
import com.laotoua.dawnislandk.data.local.entity.BrowsingHistory
import com.laotoua.dawnislandk.data.local.entity.BrowsingHistoryAndPost
import java.time.LocalDateTime

@Dao
interface BrowsingHistoryDao {
    @Transaction
    @Query("SELECT * From BrowsingHistory WHERE domain = :domain ORDER BY browsedDateTime DESC")
    fun getAllBrowsingHistoryAndPost(domain: String = DawnApp.currentDomain): LiveData<List<BrowsingHistoryAndPost>>

    @Transaction
    @Query("SELECT * From BrowsingHistory WHERE domain = :domain AND browsedDateTime>=date(:startDate) AND browsedDateTime<date(:endDate, '+1 day') ORDER BY browsedDateTime DESC")
    fun getAllBrowsingHistoryAndPostInDateRange(
        startDate: LocalDateTime,
        endDate: LocalDateTime,
        domain: String = DawnApp.currentDomain
    ): LiveData<List<BrowsingHistoryAndPost>>

    @Query("SELECT * From BrowsingHistory WHERE domain = :domain  ORDER BY browsedDateTime DESC")
    suspend fun getAllBrowsingHistory(domain: String = DawnApp.currentDomain): List<BrowsingHistory>

    @Query("SELECT * From BrowsingHistory WHERE domain = :domain AND date(browsedDateTime)=date(:date) ORDER BY browsedDateTime DESC")
    fun getBrowsingHistoryByDate(date: LocalDateTime, domain: String = DawnApp.currentDomain): LiveData<List<BrowsingHistory>>

    @Query("SELECT * From BrowsingHistory WHERE domain = :domain AND date(browsedDateTime)=date(:today) AND postId=:postId ORDER BY browsedDateTime DESC LIMIT 1")
    suspend fun getBrowsingHistoryByTodayAndIdSync(today: LocalDateTime, postId: String, domain: String = DawnApp.currentDomain): BrowsingHistory?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBrowsingHistory(browsingHistory: BrowsingHistory)

    suspend fun insertOrUpdateBrowsingHistory(browsingHistory: BrowsingHistory) {
        browsingHistory.browsedDateTime = LocalDateTime.now()
        val cache = getBrowsingHistoryByTodayAndIdSync(browsingHistory.browsedDateTime, browsingHistory.postId)
        if (cache != null) {
            browsingHistory.pages.apply { addAll(cache.pages) }
            browsingHistory.id = cache.id
        }
        insertBrowsingHistory(browsingHistory)
    }

    @Query("DELETE FROM BrowsingHistory")
    suspend fun nukeTable()
}