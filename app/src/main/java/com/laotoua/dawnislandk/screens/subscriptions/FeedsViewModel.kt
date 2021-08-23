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

package com.laotoua.dawnislandk.screens.subscriptions

import android.util.ArrayMap
import androidx.lifecycle.*
import com.laotoua.dawnislandk.DawnApp
import com.laotoua.dawnislandk.data.local.entity.Feed
import com.laotoua.dawnislandk.data.local.entity.FeedAndPost
import com.laotoua.dawnislandk.data.repository.FeedRepository
import com.laotoua.dawnislandk.util.DataResource
import com.laotoua.dawnislandk.util.EventPayload
import com.laotoua.dawnislandk.util.LoadingStatus
import com.laotoua.dawnislandk.util.SingleLiveEvent
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

class FeedsViewModel @Inject constructor(private val feedRepo: FeedRepository) : ViewModel() {
    private val feedPages = ArrayMap<Int, LiveData<DataResource<List<FeedAndPost>>>>(5)
    private val feedPageIndices = sortedSetOf<Int>()
    val feeds = MediatorLiveData<MutableList<FeedAndPost>>()
    private val _loadingStatus = MutableLiveData<SingleLiveEvent<EventPayload<Nothing>>>()
    val loadingStatus: LiveData<SingleLiveEvent<EventPayload<Nothing>>>
        get() = _loadingStatus
    private val _delFeedResponse = MutableLiveData<SingleLiveEvent<String>>()
    val delFeedResponse: LiveData<SingleLiveEvent<String>> get() = _delFeedResponse

    private var cacheDomain = DawnApp.currentDomain

    // use to flag jumps to a page without any feed
    var lastJumpPage = 0
        private set

    fun changeDomain(domain:String){
        clearCache(true)
        getNextPage()
        cacheDomain = domain
    }

    fun getNextPage() {
        val nextPage = feeds.value?.lastOrNull()?.feed?.page?.plus(1) ?: 1
        getFeedOnPage(nextPage)
    }

    fun refreshOrGetPreviousPage() {
        val lastPage = feeds.value?.firstOrNull()?.feed?.page?.minus(1) ?: 0
        if (lastPage < 1) {
            clearCache()
            getFeedOnPage(1)
        } else {
            getFeedOnPage(lastPage)
        }
    }

    fun jumpToPage(page: Int) {
        Timber.d("Jumping to Feeds on page $page")
        clearCache()
        getFeedOnPage(page)
        lastJumpPage = page
    }

    private fun getFeedOnPage(page: Int) {
        viewModelScope.launch {
            Timber.d("Getting feed on $page")
            val newPage = feedRepo.getLiveFeedPage(page)
            if (feedPages[page] != null) feeds.removeSource(feedPages[page]!!)
            feedPages[page] = newPage
            feedPageIndices.add(page)
            feeds.addSource(newPage) {
                if (it.status == LoadingStatus.SUCCESS || lastJumpPage == page) {
                    combineFeeds()
                }
                _loadingStatus.value = SingleLiveEvent.create(it.status, it.message)
            }
        }
    }

    // 1. filter duplicates
    // 2. filter feeds that do not have post data
    // in the second case, getting remote data should save a copy of post
    // ALSO clears jump page failure flag if data successfully come back
    private fun combineFeeds() {
        val ids = mutableSetOf<String>()
        val noDuplicates = mutableListOf<FeedAndPost>()
        feedPageIndices.map {
            feedPages[it]?.value?.data?.map { feedAndPost ->
                if (!ids.contains(feedAndPost.feed.postId) && feedAndPost.post != null) {
                    ids.add(feedAndPost.feed.postId)
                    noDuplicates.add(feedAndPost)
                }
            }
        }
        if (noDuplicates.isNotEmpty()) {
            lastJumpPage = 0
        }
        feeds.value = noDuplicates
    }

    fun deleteFeed(feed: Feed) {
        viewModelScope.launch {
            _delFeedResponse.postValue(SingleLiveEvent.create(feedRepo.deleteFeed(feed)))
        }
    }

    private fun clearCache(forceClear: Boolean = false) {
        feedPageIndices.map {
            feedPages[it]?.let { s -> feeds.removeSource(s) }
            feedPages.remove(it)
        }
        feedPageIndices.clear()
        if (forceClear) {
            feeds.value = ArrayList()
            lastJumpPage = 0
            feedRepo.clearCache()
        }
    }

}
