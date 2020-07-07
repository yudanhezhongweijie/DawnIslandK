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
import com.laotoua.dawnislandk.data.local.entity.Feed
import com.laotoua.dawnislandk.data.local.entity.FeedAndPost
import com.laotoua.dawnislandk.data.local.entity.Post

@Dao
interface FeedDao {
    @Transaction
    @Query("SELECT * From Feed")
    fun getAllFeedAndPost(): LiveData<List<FeedAndPost>>

    @Transaction
    @Query("SELECT * From Feed WHERE id>=:startInd AND id<:endInd ORDER BY id ASC")
    fun getFeedAndPostBetweenIds(startInd: Int, endInd: Int): LiveData<List<FeedAndPost>>

    fun getFeedAndPostOnPage(page: Int) =
        getFeedAndPostBetweenIds((page - 1) * 10 + 1, page * 10 + 1)

    fun getDistinctFeedAndPostOnPage(page: Int) = getFeedAndPostOnPage(page).distinctUntilChanged()

    @Query("DELETE FROM Feed WHERE postId=:postId")
    suspend fun deleteFeed(postId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFeed(feed: Feed)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllFeed(feedList: List<Feed>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPostIfNotExist(post: Post)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAllPostIfNotExist(postList: List<Post>)


    @Query("DELETE FROM Feed")
    suspend fun nukeTable()
}