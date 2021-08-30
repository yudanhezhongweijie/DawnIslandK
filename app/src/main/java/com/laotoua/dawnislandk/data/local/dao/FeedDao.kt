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
import com.laotoua.dawnislandk.data.local.entity.Feed
import com.laotoua.dawnislandk.data.local.entity.FeedAndPost
import com.laotoua.dawnislandk.data.local.entity.Post
import java.time.LocalDateTime

@Dao
interface FeedDao {

    @Transaction
    @Query("SELECT * From Feed WHERE domain=:domain")
    fun getAllFeedAndPost(domain: String = DawnApp.currentDomain): LiveData<List<FeedAndPost>>

    @Query("SELECT * From Feed where postId=:postId AND domain=:domain")
    suspend fun findFeedByPostId(postId: String, domain: String = DawnApp.currentDomain): Feed?

    @Transaction
    @Query("SELECT * From Feed where date(:targetTime)>date(Feed.lastUpdatedAt) AND domain=:domain ORDER BY Feed.lastUpdatedAt ASC LIMIT 1")
    suspend fun findMostOutdatedFeedAndPost(targetTime: LocalDateTime, domain: String = DawnApp.currentDomain): FeedAndPost?

    @Transaction
    @Query("SELECT * From Feed WHERE page==:page AND domain=:domain ORDER BY id ASC")
    fun getFeedAndPostOnPage(page: Int, domain: String = DawnApp.currentDomain): LiveData<List<FeedAndPost>>

    fun getDistinctFeedAndPostOnPage(page: Int, domain: String = DawnApp.currentDomain) = getFeedAndPostOnPage(page, domain).distinctUntilChanged()

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

    @Transaction
    suspend fun deleteFeedAndDecrementFeedIdsById(id: String) {
        findFeedByPostId(id)?.let {
            decrementFeedIdsAfter(it.id, it.page)
            deleteFeedByPostId(it.postId)
        }
    }

    @Query("UPDATE Feed SET id=id+1 WHERE domain=:domain AND id>=:id AND page=:page")
    suspend fun incrementFeedIdsAfter(id: Int, page: Int, domain: String = DawnApp.currentDomain)

    @Query("UPDATE Feed SET id=id-1 WHERE domain=:domain AND id>:id AND page=:page")
    suspend fun decrementFeedIdsAfter(id: Int, page: Int, domain: String = DawnApp.currentDomain)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFeed(feed: Feed)

    @Query("DELETE FROM Feed WHERE postId=:postId AND domain=:domain")
    suspend fun deleteFeedByPostId(postId: String, domain: String = DawnApp.currentDomain)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllFeed(feedList: List<Feed>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPostIfNotExist(post: Post)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAllPostIfNotExist(postList: List<Post>)


    @Query("DELETE FROM Feed")
    suspend fun nukeTable()
}