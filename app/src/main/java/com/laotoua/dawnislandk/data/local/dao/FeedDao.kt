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
    @Query("SELECT * From Feed WHERE page=:page")
    fun getFeedAndPostOnPage(page: Int): LiveData<List<FeedAndPost>>

    fun getDistinctFeedAndPostOnPage(page:Int) = getFeedAndPostOnPage(page).distinctUntilChanged()

    @Query("DELETE FROM Feed WHERE postId=:postId")
    suspend fun deleteFeed(postId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFeed(feed: Feed)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllFeed(feedList: List<Feed>)

    @Query("UPDATE Feed SET page =:page, lastUpdatedAt=:lastUpdatedAt WHERE postId = :postId")
    suspend fun updateFeed(page: Int, lastUpdatedAt: Long, postId: String)

    suspend fun insertOrUpdateFeed(feed: Feed) {
        feed.id.let {
            if (it == null) {
                insertFeed(feed)
            } else {
                updateFeed(feed.page, feed.lastUpdatedAt, feed.postId)
            }
        }
    }

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPostIfNotExist(post: Post)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAllPostIfNotExist(postList: List<Post>)


    @Query("DELETE FROM Feed")
    suspend fun nukeTable()
}