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

    @Query("SELECT * From Feed where postId=:postId")
    suspend fun findFeedByPostId(postId: String): Feed?

    @Transaction
    @Query("SELECT * From Feed,Post where Feed.postId == Post.id And :targetTime>Post.lastUpdatedAt ORDER BY Feed.lastUpdatedAt ASC, Post.lastUpdatedAt ASC LIMIT 1")
    suspend fun findMostOutdatedFeedAndPost(targetTime:Long): FeedAndPost?

    @Transaction
    @Query("SELECT * From Feed WHERE page==:page ORDER BY id ASC")
    fun getFeedAndPostOnPage(page: Int): LiveData<List<FeedAndPost>>

    fun getDistinctFeedAndPostOnPage(page: Int) = getFeedAndPostOnPage(page).distinctUntilChanged()

    @Transaction
    suspend fun addFeedToTopAndIncrementFeedIds(feed: Feed) {
        incrementFeedIdsAfter(feed.id, feed.page)
        insertFeed(feed)
    }

    @Transaction
    suspend fun deleteFeedAndDecrementFeedIds(feed: Feed){
        decrementFeedIdsAfter(feed.id, feed.page)
        deleteFeedByPostId(feed.postId)
    }

    @Query("UPDATE Feed SET id=id+1 WHERE id>=:id AND page=:page")
    suspend fun incrementFeedIdsAfter(id: Int, page:Int)

    @Query("UPDATE Feed SET id=id-1 WHERE id>:id AND page=:page")
    suspend fun decrementFeedIdsAfter(id: Int, page:Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFeed(feed: Feed)

    @Query("DELETE FROM Feed WHERE postId=:postId")
    suspend fun deleteFeedByPostId(postId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllFeed(feedList: List<Feed>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPostIfNotExist(post: Post)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAllPostIfNotExist(postList: List<Post>)


    @Query("DELETE FROM Feed")
    suspend fun nukeTable()
}