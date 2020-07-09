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
import com.laotoua.dawnislandk.data.local.entity.Comment
import com.laotoua.dawnislandk.data.repository.CommentRepository
import com.laotoua.dawnislandk.data.repository.QuoteRepository
import com.laotoua.dawnislandk.util.EventPayload
import com.laotoua.dawnislandk.util.LoadingStatus
import com.laotoua.dawnislandk.util.SingleLiveEvent
import com.laotoua.dawnislandk.util.addOrSet
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

class CommentsViewModel @Inject constructor(
    private val commentRepo: CommentRepository,
    private val quoteRepo: QuoteRepository
) : ViewModel() {
    var currentPostId: String = "0"
    private set
    // TODO: updates fragment when fid comes back
    var currentPostFid: String = "-1"
        private set

    // TODO: handle empty Page

    val po get() = commentRepo.getPo(currentPostId)
    val maxPage get() = commentRepo.getMaxPage(currentPostId)

    private val commentList = mutableListOf<Comment>()

    private val filterIds = mutableListOf<String>()

    val comments = MediatorLiveData<MutableList<Comment>>()

    private val listeningPages = SparseArray<LiveData<List<Comment>>>()
    private val listeningPagesIndices = mutableSetOf<Int>()

    val loadingStatus = MutableLiveData<SingleLiveEvent<EventPayload<Nothing>>>()

    val addFeedResponse
        get() = commentRepo.addFeedResponse

    val quoteLoadingStatus = quoteRepo.quoteLoadingStatus
    fun getQuote(id: String): LiveData<Comment> = quoteRepo.getQuote(id)

    fun setLoadingStatus(status: LoadingStatus, message: String? = null) =
        loadingStatus.postValue(SingleLiveEvent.create(status, message))

    fun setPost(id: String, fid: String, targetPage: Int?) {
//        if (id == currentPostId) return
//        setLoadingStatus(LoadingStatus.LOADING)
        if (id != currentPostId) clearCache(true)
        currentPostId = id
        currentPostFid = fid
        viewModelScope.launch {
            commentRepo.setPost(id, fid)
            if (commentList.isEmpty()) loadLandingPage(targetPage)
        }
        // catch for jumps without fid or without server updates
//        if (fid.isBlank()) {
//            postMap[idInt]?.fid?.let {
//                _currentPostFid = it
//            }
//        }
    }

    private fun loadLandingPage(targetPage: Int?) {
        getNextPage(false, targetPage ?: commentRepo.getLandingPage(currentPostId))
    }

    fun saveReadingProgress(page: Int) {
        viewModelScope.launch { commentRepo.saveReadingProgress(currentPostId, page) }
    }

    /** sometimes VM is KILLED but repo IS ALIVE, cache in Repo
     *  may show current page is full so should get next page,
     *  but VM still needs the cached current page to display
     */
    fun getNextPage(incrementPage: Boolean = true, readingProgress: Int? = null) {
        var nextPage = readingProgress ?: (commentList.lastOrNull()?.page ?: 1)
        if (incrementPage && commentRepo.checkFullPage(currentPostId, nextPage)) nextPage += 1
        listenToNewPage(nextPage, filterIds)
    }

    fun getPreviousPage() {
        // Refresh when no data, usually error occurs
        if (commentList.isNullOrEmpty()) {
            getNextPage()
            return
        }
        val lastPage = (commentList.firstOrNull()?.page ?: 1) - 1
        if (lastPage < 1) return
        listenToNewPage(lastPage, filterIds)
    }

    /**
     * By default, a post is only stored in the post table, but not stored in the comment table.
     * However when requesting references, all references are stored as comment in comment table.
     * Therefore, the first page can have or not have the header post
     */
    private fun List<Comment>.attachHeadAndAd(page: Int) = toMutableList().apply {
        // first page can have
        if (page == 1) {
            val header = commentRepo.getHeaderPost(currentPostId)
            if (header != null && (isEmpty() || get(0).id != header.id)) {
                add(0, header)
            }
        }
        //  insert thread head & Ad below
        commentRepo.getAd(currentPostId, page)?.let { add(if (page == 1) 1 else 0, it) }
    }

    private fun listenToNewPage(page: Int, filterIds: List<String>) {
        val newPage = commentRepo.getLivePage(currentPostId, page)
        if (listeningPages[page] != newPage) {
            listeningPages.put(page, newPage)
            listeningPagesIndices.add(page)
            comments.addSource(newPage) {
                if (!it.isNullOrEmpty()) {
                    mergeNewPage(it.attachHeadAndAd(page), filterIds)
                    comments.value = commentList
                    commentRepo.setLoadingStatus(LoadingStatus.SUCCESS)
                }
            }
//            if (page == 1) {
//                comments.removeSource(commentRepo.emptyPage)
//                comments.addSource(commentRepo.emptyPage) {
//                    if (it == true) {
//                        mergeNewPage(emptyList<Comment>().attachHeadAndAd(page), filterIds)
//                        comments.value = commentList
//                    }
//                }
//            }
        } else {
            viewModelScope.launch {
                commentRepo.getServerData(currentPostId, page)
            }
        }
    }

    private fun mergeNewPage(list: MutableList<Comment>, filterIds: List<String>) {
        if (list.isNullOrEmpty()) return
        val targetPage = list.first().page
        // apply filter
        applyFilterToList(list, filterIds)
        if (commentList.isEmpty() || targetPage > commentList.last().page) {
            commentList.addAll(list)
        } else if (targetPage < commentList.first().page) {
            commentList.addAll(0, list)
        } else {
            val insertInd = commentList.indexOfLast { it.page < targetPage } + 1
            list.mapIndexed { i, reply ->
                commentList.addOrSet(insertInd + i, reply)
            }
        }
    }

    private fun clearCache(clearFilter: Boolean) {
        listeningPagesIndices.map { i -> listeningPages[i]?.let { s -> comments.removeSource(s) } }
//        if (listeningPagesIndices.contains(1)) comments.removeSource(commentRepo.emptyPage)
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
        if (commentList.isNotEmpty()) applyFilterToList(commentList, filterIds)
        comments.postValue(commentList)
    }

    fun clearFilter() {
        filterIds.clear()
        commentList.map { it.visible = true }
        comments.postValue(commentList)
    }

    // keep ad as well
    private fun applyFilterToList(list: MutableList<Comment>, filterIds: List<String>) {
        if (filterIds.isNotEmpty()) list.map {
            it.visible = filterIds.contains(it.userid) || it.isAd()
        }
    }


    fun jumpTo(page: Int) {
        Timber.i("Jumping to page $page... Clearing old data")
        clearCache(false)
        listenToNewPage(page, filterIds)
    }

    // TODO: do not send request if subscribe already
    fun addFeed(uuid: String, id: String) {
        viewModelScope.launch {
            commentRepo.addFeed(uuid, id)
        }
    }
}
