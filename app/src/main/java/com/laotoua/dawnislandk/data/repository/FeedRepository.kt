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

package com.laotoua.dawnislandk.data.repository

import android.util.ArrayMap
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.liveData
import com.laotoua.dawnislandk.DawnApp
import com.laotoua.dawnislandk.data.local.dao.FeedDao
import com.laotoua.dawnislandk.data.local.entity.Feed
import com.laotoua.dawnislandk.data.local.entity.FeedAndPost
import com.laotoua.dawnislandk.data.local.entity.Post
import com.laotoua.dawnislandk.data.remote.APIMessageResponse
import com.laotoua.dawnislandk.data.remote.NMBServiceClient
import com.laotoua.dawnislandk.util.DataResource
import com.laotoua.dawnislandk.util.LoadingStatus
import com.laotoua.dawnislandk.util.getLocalListDataResource
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FeedRepository @Inject constructor(
    private val webService: NMBServiceClient,
    private val feedDao: FeedDao
) {
    private val feedsMap = ArrayMap<Int, LiveData<DataResource<List<FeedAndPost>>>>()

    fun getLiveFeedPage(page: Int): LiveData<DataResource<List<FeedAndPost>>> {
        feedsMap[page] = getCombinedFeedPage(page)
        return feedsMap[page]!!
    }

    // Remote only acts as a request status responder, actual data will be emitted by local cache
    private fun getCombinedFeedPage(page: Int): LiveData<DataResource<List<FeedAndPost>>> {
        val result = MediatorLiveData<DataResource<List<FeedAndPost>>>()
        val cache = getLocalData(page)
        val remote = getServerData(page)
        result.addSource(cache) {
            if (it.status == LoadingStatus.SUCCESS) {
                result.value = it
            }
        }
        result.addSource(remote) {
            if (it.status == LoadingStatus.NO_DATA || it.status == LoadingStatus.ERROR) {
                result.value = it
            }
        }
        return result
    }

    private fun getLocalData(page: Int): LiveData<DataResource<List<FeedAndPost>>> {
        Timber.d("Querying local Feed on $page")
        return getLocalListDataResource(feedDao.getDistinctFeedAndPostOnPage(page))
    }

    private fun getServerData(page: Int): LiveData<DataResource<List<FeedAndPost>>> {
        return liveData {
            Timber.d("Querying remote Feed on $page")
            val response =
                DataResource.create(webService.getFeeds(DawnApp.applicationDataStore.getFeedId(), page))
            if (response.status == LoadingStatus.SUCCESS) {
                emit(DataResource.create(convertFeedData(response.data!!, page), emptyList()))
            } else {
                emit(
                    DataResource.create(
                        response.status,
                        emptyList(),
                        "无法读取订阅...\n${response.message}"
                    )
                )
            }
        }

    }

    // Note only return request status
    private suspend fun convertFeedData(data: List<Feed.ServerFeed>, page: Int): LoadingStatus {
        if (data.isEmpty()) {
            return LoadingStatus.NO_DATA
        }
        val feeds = mutableListOf<Feed>()
        val posts = mutableListOf<Post>()
        val baseIndex = (page - 1) * 10 + 1
        val timestamp = Date().time
        data.mapIndexed { index, serverFeed ->
            feeds.add(Feed(baseIndex + index, page, serverFeed.id, serverFeed.category, timestamp))
            posts.add(serverFeed.toPost())
        }

        val cacheFeed = feedsMap[page]?.value?.data?.map { it.feed } ?: emptyList()
        if (cacheFeed != feeds) {
            coroutineScope {
                launch {
                    feedDao.insertAllFeed(feeds)
                    feedDao.insertAllPostIfNotExist(posts)
                }
            }
        }
        return LoadingStatus.SUCCESS
    }

    suspend fun deleteFeed(feed: Feed): String {
        Timber.d("Deleting Feed ${feed.postId}")
        return webService.delFeed(DawnApp.applicationDataStore.getFeedId(), feed.postId).run {
            if (this is APIMessageResponse.Success) {
                coroutineScope { launch { feedDao.deleteFeedAndDecrementFeedIds(feed) } }
                message
            } else {
                Timber.e(message)
                "删除订阅失败"
            }
        }
    }
}