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

    var currentPostFid: String = "-1"
        private set

    val po get() = commentRepo.getPo(currentPostId)
    val maxPage get() = commentRepo.getMaxPage(currentPostId)

    private val commentList = mutableListOf<Comment>()

    private val filterIds = mutableListOf<String>()

    val comments = MediatorLiveData<MutableList<Comment>>()

    private val listeningPages = SparseArray<LiveData<DataResource<List<Comment>>>>()
    private val listeningPagesIndices = mutableSetOf<Int>()

    val loadingStatus = MutableLiveData<SingleLiveEvent<EventPayload<Nothing>>>()

    val addFeedResponse = MutableLiveData<SingleLiveEvent<String>>()

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

    fun getNextPage(incrementPage: Boolean = true, readingProgress: Int? = null) {
        var nextPage = readingProgress ?: (commentList.lastOrNull()?.page ?: 1)
        if (incrementPage && commentRepo.checkFullPage(currentPostId, nextPage)) nextPage += 1
        listenToNewPage(nextPage)
    }

    fun getPreviousPage() {
        // Refresh when no data, usually error occurs
        if (commentList.isNullOrEmpty()) {
            getNextPage()
            return
        }
        val lastPage = (commentList.firstOrNull()?.page ?: 1) - 1
        if (lastPage < 1) return
        listenToNewPage(lastPage)
    }

    private fun listenToNewPage(page: Int) {
        val hasCache = listeningPages[page] != null
        if (hasCache) comments.removeSource(listeningPages[page])
        val newPage = commentRepo.getCommentsOnPage(currentPostId, page, hasCache)
        listeningPages.put(page, newPage)
        listeningPagesIndices.add(page)
        comments.addSource(newPage) {
            combineDataResource(it, page)
        }
    }


    private fun combineDataResource(
        dataResource: DataResource<List<Comment>>,
        targetPage: Int
    ) {
        var status = dataResource.status
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
            // inform user latest page is the same as error by setting status to no_data instead of success
            val noAdOldPage = commentList.filter { it.page == targetPage && it.isNotAd() }
            val noAdNewPage = list.filter { it.isNotAd() }
            if (noAdNewPage.size < 19) {
                status = LoadingStatus.NO_DATA
            }
            if (!noAdOldPage.equalsWithServerComments(noAdNewPage)) {
                mergeList(list, targetPage)
            }
        }
        setLoadingStatus(status, dataResource.message)
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

    private fun clearCache(clearFilter: Boolean) {
        listeningPagesIndices.map { i -> listeningPages[i]?.let { s -> comments.removeSource(s) } }
        listeningPages.clear()
        listeningPagesIndices.clear()
        commentList.clear()
        if (clearFilter) filterIds.clear()
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
            it.visible = filterIds.contains(it.userid) || it.isAd()
        }
    }


    fun jumpTo(page: Int) {
        Timber.i("Jumping to page $page... Clearing old data")
        clearCache(false)
        listenToNewPage(page)
    }

    fun getExternalShareContent():String{
        return "${ContentTransformation.htmlToSpanned(commentRepo.getHeaderPost(currentPostId)?.content.toString())}\n\n${DawnConstants.nmbHost}/t/${currentPostId}\n"
    }

    fun addFeed(id: String) {
        viewModelScope.launch {
            addFeedResponse.postValue(commentRepo.addFeed(DawnApp.applicationDataStore.getFeedId(), id))
        }
    }
}
