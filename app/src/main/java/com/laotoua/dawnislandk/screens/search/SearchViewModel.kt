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

package com.laotoua.dawnislandk.screens.search

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laotoua.dawnislandk.DawnApp
import com.laotoua.dawnislandk.data.remote.APIDataResponse
import com.laotoua.dawnislandk.data.remote.NMBServiceClient
import com.laotoua.dawnislandk.data.remote.SearchResult
import com.laotoua.dawnislandk.util.EventPayload
import com.laotoua.dawnislandk.util.LoadingStatus
import com.laotoua.dawnislandk.util.SingleLiveEvent
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import kotlin.math.ceil

class SearchViewModel @Inject constructor(private val webNMBServiceClient: NMBServiceClient) :
    ViewModel() {

    var query = ""
        private set
    private var pageResults = mutableListOf<SearchResult>()

    private var nextPage = 1
    var maxPage = 0
        private set
    private var foundHits = 0

    private val _loadingStatus = MutableLiveData<SingleLiveEvent<EventPayload<Nothing>>>()
    val loadingStatus: LiveData<SingleLiveEvent<EventPayload<Nothing>>> get() = _loadingStatus

    private val _searchResult = MutableLiveData<List<SearchResult>>()
    val searchResult: LiveData<List<SearchResult>> get() = _searchResult

    fun search(q: String) {
        if (q == query || q.isBlank()) return
        pageResults.clear()
        foundHits = 0
        query = q
        nextPage = 1
        maxPage = 0
        getNextPage()
    }

    fun getNextPage() {
        val cookieHash = DawnApp.applicationDataStore.firstCookieHash
        if (cookieHash == null) {
            _loadingStatus.value = SingleLiveEvent.create(LoadingStatus.ERROR, "搜索需要饼干。。")
            return
        }
        if (query.isBlank()) return
        _loadingStatus.value = SingleLiveEvent.create(LoadingStatus.LOADING)
        viewModelScope.launch {
            getQueryOnPage(query, nextPage, cookieHash)
        }
    }

    private fun combinePagedSearchResults(page: SearchResult) {
        if (!page.hits.isNullOrEmpty()) {
            pageResults.add(page)
        }
        foundHits += page.hits.size
        if (page.hits.size == 20) nextPage += 1
        if (maxPage == 0) maxPage = ceil(page.queryHits.toDouble() / 20).toInt()
        _loadingStatus.postValue(
            SingleLiveEvent.create(
                LoadingStatus.SUCCESS
            )
        )
        _searchResult.value = pageResults
    }

    private suspend fun getQueryOnPage(query: String, page: Int, cookieHash: String) {
        if (foundHits == pageResults.firstOrNull()?.queryHits) {
            _loadingStatus.value = SingleLiveEvent.create(LoadingStatus.NO_DATA)
            return
        }
        webNMBServiceClient.getNMBSearch(query, page, cookieHash).run {
            if (this is APIDataResponse.APISuccessDataResponse) {
                combinePagedSearchResults(data)
            } else {
                Timber.e(message)
                _loadingStatus.postValue(
                    SingleLiveEvent.create(LoadingStatus.ERROR, "搜索失败...\n$message")
                )
            }
        }
    }
}