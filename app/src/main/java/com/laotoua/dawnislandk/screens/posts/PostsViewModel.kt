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

package com.laotoua.dawnislandk.screens.posts

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laotoua.dawnislandk.data.local.entity.Post
import com.laotoua.dawnislandk.data.remote.APIDataResponse
import com.laotoua.dawnislandk.data.remote.NMBServiceClient
import com.laotoua.dawnislandk.util.EventPayload
import com.laotoua.dawnislandk.util.LoadingStatus
import com.laotoua.dawnislandk.util.SingleLiveEvent
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

class PostsViewModel @Inject constructor(private val webService: NMBServiceClient) : ViewModel() {
    private val postList = mutableListOf<Post>()
    private val postIds = mutableSetOf<String>()
    private var _posts = MutableLiveData<MutableList<Post>>()
    val posts: LiveData<MutableList<Post>> get() = _posts
    private var _currentFid: String? = null
    val currentFid: String? get() = _currentFid
    private var pageCount = 1

    private var _loadingStatus = MutableLiveData<SingleLiveEvent<EventPayload<Nothing>>>()
    val loadingStatus: LiveData<SingleLiveEvent<EventPayload<Nothing>>>
        get() = _loadingStatus

    fun getPosts() {
        viewModelScope.launch {
            _loadingStatus.postValue(SingleLiveEvent.create(LoadingStatus.LOADING))
            val fid = _currentFid ?: "-1"
            Timber.d("Getting threads from $fid on page $pageCount")
            webService.getPosts(fid, pageCount).run {
                if (this is APIDataResponse.APIErrorDataResponse) {
                    Timber.e(message)
                    _loadingStatus.postValue(
                        SingleLiveEvent.create(LoadingStatus.FAILED, "无法读取串列表...\n$message")
                    )
                } else if (this is APIDataResponse.APISuccessDataResponse) {
                    convertServerData(data, fid)
                }
            }
        }
    }

    private fun convertServerData(data: List<Post>, fid: String) {
        // assign fid if not timeline
        if (fid != "-1") data.map { it.fid = fid }
        val noDuplicates = data.filterNot { postIds.contains(it.id) }
        pageCount += 1
        if (noDuplicates.isNotEmpty()) {
            postIds.addAll(noDuplicates.map { it.id })
            postList.addAll(noDuplicates)
            Timber.d(
                "New thread + ads has size of ${noDuplicates.size}, threadIds size ${postIds.size}, Forum $currentFid now have ${postList.size} threads"
            )
            _posts.postValue(postList)
            _loadingStatus.postValue(
                SingleLiveEvent.create(
                    LoadingStatus.SUCCESS
                )
            )
            // possible page X+1's data is identical page X's data when server updates too quickly
        } else {
            Timber.d("Last page were all duplicates. Making new request")
            getPosts()
        }
    }

    fun setForum(fid: String) {
        if (fid == currentFid) return
        Timber.i("Forum has changed. Cleaning old threads...")
        postList.clear()
        postIds.clear()
        Timber.d("Setting new forum: $fid")
        _currentFid = fid
        pageCount = 1
        getPosts()
    }

    fun refresh() {
        Timber.d("Refreshing forum $currentFid...")
        postList.clear()
        postIds.clear()
        pageCount = 1
        getPosts()
    }
}
