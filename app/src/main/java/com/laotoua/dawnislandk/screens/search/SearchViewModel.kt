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

import androidx.lifecycle.*
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

class SearchViewModel @Inject constructor(private val webNMBServiceClient: NMBServiceClient) :
    ViewModel() {
    var query: String = ""
        private set

    var nextPage = 1
        private set

    private var pageResults = mutableListOf<SearchResult>()

    private val _loadingStatus = MutableLiveData<SingleLiveEvent<EventPayload<Nothing>>>()
    val loadingStatus: LiveData<SingleLiveEvent<EventPayload<Nothing>>> get() = _loadingStatus

    val searchResult = MediatorLiveData<List<SearchResult.Hit>>()

    fun search(q: String) {
        if (q == query) return
        val cookieHash = DawnApp.applicationDataStore.firstCookieHash
        if (cookieHash == null) {
            _loadingStatus.value = SingleLiveEvent.create(LoadingStatus.FAILED, "搜索需要饼干。。")
            return
        }
        viewModelScope.launch {
            webNMBServiceClient.getNMBSearch(q, nextPage, cookieHash).run {
                if (this is APIDataResponse.APISuccessDataResponse) {
                    Timber.d("search success")
                    data.query = q
                    data.page = nextPage
                    pageResults.add(data)
                    combinePagedSearchResults()
                    nextPage += 1
                } else {
                    Timber.d("search failed $message")
                }
            }
        }
    }

    fun combinePagedSearchResults() {
        searchResult.value = pageResults.map { it.hits }.flatten()
    }

}