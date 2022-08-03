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

package com.laotoua.dawnislandk.screens.comments

import android.util.SparseArray
import androidx.lifecycle.*
import com.laotoua.dawnislandk.DawnApp
import com.laotoua.dawnislandk.data.local.entity.Comment
import com.laotoua.dawnislandk.data.repository.CommentRepository
import com.laotoua.dawnislandk.data.repository.QuoteRepository
import com.laotoua.dawnislandk.screens.util.ContentTransformation
import com.laotoua.dawnislandk.util.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

class CommentsViewModel @Inject constructor(
    private val commentRepo: CommentRepository,
    private val quoteRepo: QuoteRepository
) : ViewModel() {
    var currentPostId: String = "0"
        private set

    var currentPostFid: String = DawnConstants.TIMELINE_COMMUNITY_ID
        private set

    val po get() = commentRepo.getPo(currentPostId)
    val maxPage get() = commentRepo.getMaxPage(currentPostId)
    private var cacheDomain = DawnApp.currentDomain

    private val commentList = mutableListOf<Comment>()

    private val filterIds = mutableListOf<String>()

    val comments = MediatorLiveData<MutableList<Comment>>()

    // use to indicate whether post deleted message should be shown
    private var postDeleted = false

    private val listeningPages = SparseArray<LiveData<DataResource<List<Comment>>>>()
    private val listeningPagesIndices = mutableSetOf<Int>()

    val loadingStatus = MutableLiveData<SingleLiveEvent<EventPayload<Nothing>>>()

    val feedResponse = MutableLiveData<SingleLiveEvent<String>>()

    fun changeDomain(domain:String){
        if (cacheDomain != domain) {
            clearCache(true)
            cacheDomain = domain
        }
    }

    fun getQuote(id: String): LiveData<DataResource<Comment>> = liveData {
        // try to find quote in current post, if not then in local cache or remote data
        val result = if (id == currentPostId) commentRepo.getHeaderPost(id) else commentList.find { it.id == id }
        if (result != null) emit(DataResource.create(LoadingStatus.SUCCESS, result))
        else emitSource(quoteRepo.getQuote(id))
    }

    private fun setLoadingStatus(status: LoadingStatus, message: String? = null) {
        loadingStatus.postValue(SingleLiveEvent.create(status, message))
    }

    fun setPost(id: String, fid: String, targetPage: Int) {
        if (id == currentPostId) return
        clearCache(true)
        currentPostId = id
        currentPostFid = fid
        viewModelScope.launch {
            commentRepo.setPost(id, fid)
            loadLandingPage(targetPage)
            // catch for jumps without fid or without server updates
            if (fid.isBlank()) {
                currentPostFid = commentRepo.getFid(id)
            }
        }
    }

    private fun loadLandingPage(targetPage: Int) {
        getNextPage(
            false,
            if (targetPage > 0) targetPage else commentRepo.getLandingPage(currentPostId)
        )
    }

    fun saveReadingProgress(page: Int) {
        viewModelScope.launch { commentRepo.saveReadingProgress(currentPostId, page) }
    }

    fun getNextPage(incrementPage: Boolean = true, readingProgress: Int? = null, forceUpdate: Boolean = true) {
        var nextPage = readingProgress ?: (commentList.lastOrNull()?.page ?: 1)
        if (incrementPage && maxPage > nextPage) nextPage += 1
        listenToNewPage(nextPage, forceUpdate)
    }

    fun getPreviousPage(forceUpdate: Boolean = true) {
        // Refresh when no data, usually error occurs
        if (commentList.isEmpty()) {
            getNextPage()
            return
        }
        val lastPage = (commentList.firstOrNull()?.page ?: 1) - 1
        if (lastPage < 1) return
        listenToNewPage(lastPage, forceUpdate)
    }

    private fun listenToNewPage(page: Int, forceUpdate: Boolean) {
        val hasCache = listeningPages[page] != null
        if (hasCache && !forceUpdate) return
        if (hasCache) comments.removeSource(listeningPages[page])
        val newPage = commentRepo.getCommentsOnPage(currentPostId, page, hasCache, postDeleted)
        listeningPages.put(page, newPage)
        listeningPagesIndices.add(page)
        comments.addSource(newPage) {
            combineDataResource(it, page)
        }
    }


    private fun combineDataResource(dataResource: DataResource<List<Comment>>, targetPage: Int) {
        var status = dataResource.status
        var message = dataResource.message
        if (dataResource.status == LoadingStatus.SUCCESS || dataResource.status == LoadingStatus.NO_DATA) {
            // assign fid if missing
            if (currentPostFid.isBlank()) {
                currentPostFid = commentRepo.getFid(currentPostId)
            }
            val list = mutableListOf<Comment>()
            /**
             * By default, a post is only stored in the post table, but not stored in the comment table.
             * However when requesting references, all references are stored as comment in comment table.
             * Therefore, the first page can have or not have the header post
             */
            if (targetPage == 1 && (dataResource.data.isNullOrEmpty() || (dataResource.data.isNotEmpty() && dataResource.data[0].id != currentPostId))) {
                commentRepo.getHeaderPost(currentPostId)?.let { list.add(0, it) }
            }
            dataResource.data?.let { list.addAll(it) }
            /** inform user this page is the end by setting status to no_data instead of success,
             *  if this page reach page max count: 20 if has Ad, 19 otherwise
             */
            if (list.size < 19 + if (list.firstOrNull()?.isAd() == true) 1 else 0) {
                status = LoadingStatus.NO_DATA
            }

            val noAdOldPage = commentList.filter { it.page == targetPage && it.isNotAd() }
            if (!noAdOldPage.equalsWithServerComments(list)) {
                mergeList(list, targetPage)
            }
        }
        if (postDeleted && targetPage >= maxPage - 1) message =
            "\u8be5\u4e3b\u9898\u4e0d\u5b58\u5728"
        setLoadingStatus(status, message)
        if (!postDeleted && status == LoadingStatus.SUCCESS && dataResource.message == "\u8be5\u4e3b\u9898\u4e0d\u5b58\u5728") {
            postDeleted = true
        }
    }

    private fun mergeList(
        list: List<Comment>,
        targetPage: Int
    ) {
        if (list.isEmpty()) {
            Timber.d("Page $targetPage is empty. List still has size of ${comments.value?.size}")
            return
        }
        Timber.d("Merging ${list.size} comments on $targetPage")
        // apply filter
        applyFilterToList(list)
        if (commentList.isEmpty() || targetPage > commentList.last().page) {
            commentList.addAll(list)
        } else if (targetPage < commentList.first().page) {
            commentList.addAll(0, list)
        } else {
            commentList.removeAll { it.page == targetPage }
            val insertInd = commentList.indexOfLast { it.page < targetPage } + 1
            commentList.addAll(insertInd, list)
        }
        comments.value = commentList
        Timber.d("Got ${comments.value?.size} after merging on $targetPage")
    }

    private fun clearCache(clearFilter: Boolean, clearEverything: Boolean = false) {
        listeningPagesIndices.map { i -> listeningPages[i]?.let { s -> comments.removeSource(s) } }
        listeningPages.clear()
        listeningPagesIndices.clear()
        commentList.clear()
        if (clearFilter) filterIds.clear()
        if (clearEverything) {
            quoteRepo.clearCache()
            commentRepo.clearCache()
            currentPostFid = DawnConstants.TIMELINE_COMMUNITY_ID
            currentPostId = "0"
        }
    }

    fun onlyPo() {
        applyFilter(po)
    }

    private fun applyFilter(vararg Ids: String) {
        filterIds.addAll(Ids)
        if (commentList.isNotEmpty()) applyFilterToList(commentList)
        comments.postValue(commentList)
    }

    fun clearFilter() {
        filterIds.clear()
        commentList.map { it.visible = true }
        comments.postValue(commentList)
    }

    // keep ad as well
    private fun applyFilterToList(list: List<Comment>) {
        if (filterIds.isNotEmpty()) list.map {
            it.visible = filterIds.contains(it.userHash) || it.isAd()
        }
    }


    fun jumpTo(page: Int) {
        Timber.i("Jumping to page $page... Clearing old data")
        clearCache(false)
        listenToNewPage(page, true)
    }

    fun getExternalShareContent(): String {
        return "${ContentTransformation.htmlToSpanned(commentRepo.getHeaderPost(currentPostId)?.content.toString())}\n\n${DawnConstants.NMBXDHost}/t/${currentPostId}\n"
    }

    fun addFeed(id: String) {
        viewModelScope.launch {
            feedResponse.postValue(commentRepo.addFeed(id))
        }
    }

    fun delFeed(id: String) {
        viewModelScope.launch {
            feedResponse.postValue(commentRepo.deleteFeed(id))
        }
    }
}
