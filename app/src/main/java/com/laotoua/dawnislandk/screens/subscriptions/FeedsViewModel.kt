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

import android.util.SparseArray
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laotoua.dawnislandk.data.local.entity.Feed
import com.laotoua.dawnislandk.data.local.entity.FeedAndPost
import com.laotoua.dawnislandk.data.repository.FeedRepository
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

class FeedsViewModel @Inject constructor(private val feedRepo: FeedRepository) : ViewModel() {
    private val feedPages = SparseArray<LiveData<List<FeedAndPost>>>(5)
    private val feedPageIndices = mutableSetOf<Int>()
    val feeds = MediatorLiveData<MutableList<FeedAndPost>>()
    val loadingStatus get() = feedRepo.loadingStatus
    val delFeedResponse get() = feedRepo.delFeedResponse

    // use to flag jumps to a page without any feed
    var lastJumpPage = 0
        private set

    fun getNextPage() {
        val lastFeed: Feed? = feeds.value?.lastOrNull()?.feed
        var nextPage = lastFeed?.page ?: 1
        if (lastFeed?.id?.rem(10) == 0) nextPage += 1
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
            if (feedPages[page] != newPage) {
                feedPages.put(page, newPage)
                feedPageIndices.add(page)
                feeds.addSource(newPage) {
                    combineFeeds()
                }
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
            feedPages[it].value?.map { feedAndPost ->
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

    fun deleteFeed(id: String, position: Int) {
        viewModelScope.launch {
            feedRepo.deleteFeed(id, position)
        }
    }

    private fun clearCache() {
        feedPageIndices.map {
            feedPages[it]?.let { s -> feeds.removeSource(s) }
            feedPages.delete(it)
        }
        feedPageIndices.clear()
    }

}
