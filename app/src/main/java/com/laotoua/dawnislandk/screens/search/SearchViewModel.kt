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

import android.util.SparseArray
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laotoua.dawnislandk.data.local.entity.Post
import com.laotoua.dawnislandk.data.remote.NMBServiceClient
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

class SearchViewModel @Inject constructor(private val webNMBServiceClient: NMBServiceClient) : ViewModel() {
    var query:String = ""
    private set

    var currentPage = 1
    private set

    private var pageResults = SparseArray<List<Post>>()

    fun search(q:String){
        if (q == query) return
        viewModelScope.launch {
            Timber.d("TODO SEARCH: $q")
        }
    }

}