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
import com.laotoua.dawnislandk.data.local.entity.Feed
import com.laotoua.dawnislandk.data.local.entity.FeedAndPost

@Dao
interface FeedDao {
    @Transaction
    @Query("SELECT * From Feed")
    fun getAllFeedAndPost(): LiveData<List<FeedAndPost>>

    @Transaction
    @Query("SELECT * From Feed WHERE page=:page")
    fun getAllFeedAndPostOnPage(        page: Int    ): LiveData<List<FeedAndPost>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFeed(feed: Feed)

    @Query("UPDATE Feed SET page =:page, lastUpdatedAt=:lastUpdatedAt WHERE id = :id")
    suspend fun updateFeed(page: Int, lastUpdatedAt: Long, id: Int)

    suspend fun insertOrUpdateFeed(feed: Feed) {
        feed.id.let {
            if (it == null) {
                insertFeed(feed)
            } else {
                updateFeed(feed.page, feed.lastUpdatedAt, it)
            }
        }
    }

    @Query("DELETE FROM Feed")
    suspend fun nukeTable()
}