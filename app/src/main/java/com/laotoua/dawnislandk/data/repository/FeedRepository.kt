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

import android.util.SparseArray
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.liveData
import com.laotoua.dawnislandk.DawnApp
import com.laotoua.dawnislandk.data.local.dao.FeedDao
import com.laotoua.dawnislandk.data.local.entity.Feed
import com.laotoua.dawnislandk.data.local.entity.FeedAndPost
import com.laotoua.dawnislandk.data.local.entity.Post
import com.laotoua.dawnislandk.data.remote.APIDataResponse
import com.laotoua.dawnislandk.data.remote.APIMessageResponse
import com.laotoua.dawnislandk.data.remote.NMBServiceClient
import com.laotoua.dawnislandk.util.EventPayload
import com.laotoua.dawnislandk.util.LoadingStatus
import com.laotoua.dawnislandk.util.SingleLiveEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.*
import javax.inject.Inject

class FeedRepository @Inject constructor(
    private val webService: NMBServiceClient,
    private val feedDao: FeedDao
) {
    private val feedsMap = SparseArray<LiveData<List<FeedAndPost>>>()
    private var _loadingStatus = MutableLiveData<SingleLiveEvent<EventPayload<Nothing>>>()
    val loadingStatus: LiveData<SingleLiveEvent<EventPayload<Nothing>>>
        get() = _loadingStatus

    private val _delFeedResponse = MutableLiveData<SingleLiveEvent<EventPayload<Int>>>()
    val delFeedResponse: LiveData<SingleLiveEvent<EventPayload<Int>>> get() = _delFeedResponse
    private var pageDownloadJob: Job? = null


    fun getLiveFeedPage(page: Int): LiveData<List<FeedAndPost>> {
        if (feedsMap[page] == null) {
            feedsMap.put(page, liveData(Dispatchers.IO) {
                _loadingStatus.postValue(SingleLiveEvent.create(LoadingStatus.LOADING))
                Timber.d("Querying Feed on $page")
                emitSource(getLocalData(page))
                pageDownloadJob = getServerData(page)
            })
        }
        return feedsMap[page]
    }

    private fun getLocalData(page: Int): LiveData<List<FeedAndPost>> =
        feedDao.getDistinctFeedAndPostOnPage(page)

    suspend fun getServerData(page: Int): Job = coroutineScope {
        launch {
            Timber.d("Querying remote Feed on $page")
            webService.getFeeds(DawnApp.applicationDataStore.feedId, page).run {
                if (this is APIDataResponse.APISuccessDataResponse) convertFeedData(data, page)
                else {
                    if (pageDownloadJob?.isCancelled != true) {
                        Timber.e(message)
                        feedsMap.delete(page)
                        _loadingStatus.postValue(
                            SingleLiveEvent.create(LoadingStatus.FAILED, "无法读取订阅...\n$message")
                        )
                    }
                }
            }
        }
    }

    private suspend fun convertFeedData(data: List<Feed.ServerFeed>, page: Int) {
        val feeds = mutableListOf<Feed>()
        val posts = mutableListOf<Post>()
        data.map {
            feeds.add(Feed(null, it.id, it.category, page, Date().time))
            posts.add(it.toPost())
        }
        if (data.isEmpty()) {
            _loadingStatus.postValue(SingleLiveEvent.create(LoadingStatus.NODATA))
        } else {
            _loadingStatus.postValue(SingleLiveEvent.create(LoadingStatus.SUCCESS))
            coroutineScope {
                launch {
                    compareAndUpdateCacheFeeds(feeds, page)
                    feedDao.insertAllPostIfNotExist(posts)
                }
            }
        }
    }

    private suspend fun compareAndUpdateCacheFeeds(list: List<Feed>, page: Int) {
        val cacheFeed = feedsMap[page].value?.map { feedAndPost -> feedAndPost.feed } ?: emptyList()
        val updatedFeed = mutableListOf<Feed>()
        for (i in list.indices) {
            if (i > cacheFeed.lastIndex) {
                updatedFeed.add(list[i])
            } else if (!list[i].equalsExceptIdAndTime(cacheFeed[i])) {
                updatedFeed.add(
                    Feed(
                        cacheFeed[i].id, list[i].postId, list[i].category, list[i].page
                        , list[i].lastUpdatedAt
                    )
                )
            }
        }
        feedDao.insertAllFeed(updatedFeed)
    }

    suspend fun deleteFeed(postId: String, position: Int) {
        Timber.i("Deleting Feed $postId")
        webService.delFeed(DawnApp.applicationDataStore.feedId, postId).run {
            if (this is APIMessageResponse.APISuccessMessageResponse) {
                feedDao.deleteFeed(postId)
                _delFeedResponse.postValue(
                    SingleLiveEvent.create(
                        LoadingStatus.SUCCESS,
                        message,
                        position
                    )
                )
            } else {
                Timber.e("Response type: ${this.javaClass.simpleName}")
                Timber.e(message)
                _delFeedResponse.postValue(
                    SingleLiveEvent.create(
                        LoadingStatus.FAILED,
                        "删除订阅失败"
                    )
                )
            }
        }
    }
}