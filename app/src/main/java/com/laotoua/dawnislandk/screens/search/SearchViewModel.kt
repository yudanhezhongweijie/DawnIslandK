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

class SearchViewModel @Inject constructor(private val webNMBServiceClient: NMBServiceClient) :
    ViewModel() {
    var query: String = ""
        private set


    private var pageResults = mutableListOf<SearchResult>()

    val nextPage get() = 1.coerceAtLeast(pageResults.size)

    private val _loadingStatus = MutableLiveData<SingleLiveEvent<EventPayload<Nothing>>>()
    val loadingStatus: LiveData<SingleLiveEvent<EventPayload<Nothing>>> get() = _loadingStatus

    private val _searchResult = MutableLiveData<List<SearchResult>>()
    val searchResult: LiveData<List<SearchResult>> get() = _searchResult

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
                    combinePagedSearchResults(data)
                } else {
                    Timber.e(message)
                    _loadingStatus.postValue(
                        SingleLiveEvent.create(LoadingStatus.FAILED, "搜索失败...\n$message")
                    )
                }
            }
        }
    }

    private fun combinePagedSearchResults(page : SearchResult) {
        pageResults.add(page)
        _searchResult.value = pageResults
    }

}