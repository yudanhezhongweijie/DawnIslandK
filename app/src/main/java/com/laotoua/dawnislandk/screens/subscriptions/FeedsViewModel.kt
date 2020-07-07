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
import com.laotoua.dawnislandk.data.local.entity.FeedAndPost
import com.laotoua.dawnislandk.data.repository.FeedRepository
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

class FeedsViewModel @Inject constructor(private val feedRepo: FeedRepository) : ViewModel() {
    private val feedPages = SparseArray<LiveData<List<FeedAndPost>>>(10)
    private val feedPageIndices = mutableSetOf<Int>()
    val feeds = MediatorLiveData<MutableList<FeedAndPost>>()
    val loadingStatus get() = feedRepo.loadingStatus
    val delFeedResponse get() = feedRepo.delFeedResponse

    // TODO verify
    fun getNextPage() {
        val nextPage = feeds.value?.lastOrNull()?.feed?.id?.div(10)?.plus(1) ?: 1
        getFeedOnPage(nextPage)
    }

    // TODO verify
    fun refreshOrGetPreviousPage() {
        if (feeds.value.isNullOrEmpty()) {
            getNextPage()
            return
        }
        val lastPage = feeds.value?.firstOrNull()?.feed?.id?.div(10)?.minus(1) ?: 0
        if (lastPage < 1) {
            clearCache()
            getFeedOnPage(1)
        } else {
            getFeedOnPage(lastPage)
        }
    }


    private fun getFeedOnPage(page: Int) {
        Timber.d("getting feed on $page")
        val newPage = feedRepo.getLiveFeedPage(page)
        if (feedPages[page] != newPage) {
            feedPages.put(page, newPage)
            feedPageIndices.add(page)
            feeds.addSource(newPage) {
                combineFeeds()
            }
        } else {
            viewModelScope.launch {
                feedRepo.getServerData(page)
            }
        }
    }

    // 1. filter duplicates
    // 2. filter feeds that do not have post data
    // in the second case, getting remote data should save a copy of post
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
