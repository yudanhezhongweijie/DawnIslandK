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
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laotoua.dawnislandk.data.local.entity.Comment
import com.laotoua.dawnislandk.data.repository.CommentRepository
import com.laotoua.dawnislandk.data.repository.QuoteRepository
import com.laotoua.dawnislandk.util.LoadingStatus
import com.laotoua.dawnislandk.util.addOrSet
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

class CommentsViewModel @Inject constructor(
    private val commentRepo: CommentRepository,
    private val quoteRepo: QuoteRepository
) : ViewModel() {
    val currentPostId: String get() = commentRepo.currentPostId
    val currentPostFid: String get() = commentRepo.currentPostFid
    val po get() = commentRepo.po

    private val commentList = mutableListOf<Comment>()

    private val filterIds = mutableListOf<String>()

    val comments = MediatorLiveData<MutableList<Comment>>()

    private val listeningPages = SparseArray<LiveData<List<Comment>>>()
    private val listeningPagesIndices = mutableSetOf<Int>()
    val maxPage get() = commentRepo.maxPage

    val loadingStatus get() = commentRepo.loadingStatus

    val addFeedResponse
        get() = commentRepo.addFeedResponse

    val quoteLoadingStatus = quoteRepo.quoteLoadingStatus
    fun getQuote(id: String): LiveData<Comment> = quoteRepo.getQuote(id)

    fun setPost(id: String, fid: String, targetPage: Int?) {
        if (id != currentPostId) clearCache(true)
        viewModelScope.launch {
            commentRepo.setPost(id, fid)
            if (commentList.isEmpty()) loadLandingPage(targetPage)
        }
    }

    private fun loadLandingPage(targetPage: Int?) {
        getNextPage(false, targetPage ?: commentRepo.getLandingPage())
    }

    fun saveReadingProgress(page: Int) {
        viewModelScope.launch { commentRepo.saveReadingProgress(page) }
    }

    /** sometimes VM is KILLED but repo IS ALIVE, cache in Repo
     *  may show current page is full so should get next page,
     *  but VM still needs the cached current page to display
     */
    fun getNextPage(incrementPage: Boolean = true, readingProgress: Int? = null) {
        var nextPage = readingProgress ?: (commentList.lastOrNull()?.page ?: 1)
        if (incrementPage && commentRepo.checkFullPage(nextPage)) nextPage += 1
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
        if (page == 1 && get(0).id != currentPostId) {
            add(0, commentRepo.getHeaderPost())
        }
        //  insert thread head & Ad below
        commentRepo.getAd(page)?.let { add(if (page == 1) 1 else 0, it) }
    }

    private fun listenToNewPage(page: Int, filterIds: List<String>) {
        val newPage = commentRepo.getLivePage(page)
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
            if (page == 1) {
                comments.removeSource(commentRepo.emptyPage)
                comments.addSource(commentRepo.emptyPage) {
                    if (it == true) {
                        mergeNewPage(emptyList<Comment>().attachHeadAndAd(page), filterIds)
                        comments.value = commentList
                    }
                }
            }
        } else {
            viewModelScope.launch {
                commentRepo.getServerData(page)
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
        if (listeningPagesIndices.contains(1)) comments.removeSource(commentRepo.emptyPage)
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
