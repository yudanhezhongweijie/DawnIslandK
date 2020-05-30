package com.laotoua.dawnislandk.screens.replys

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laotoua.dawnislandk.data.local.Reply
import com.laotoua.dawnislandk.data.local.Thread
import com.laotoua.dawnislandk.data.repository.ReplyRepository
import com.laotoua.dawnislandk.util.LoadingStatus
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import kotlin.collections.List
import kotlin.collections.MutableList
import kotlin.collections.emptyList
import kotlin.collections.first
import kotlin.collections.firstOrNull
import kotlin.collections.indexOfLast
import kotlin.collections.indices
import kotlin.collections.isNullOrEmpty
import kotlin.collections.last
import kotlin.collections.lastOrNull
import kotlin.collections.map
import kotlin.collections.mutableListOf
import kotlin.collections.mutableMapOf
import kotlin.collections.set

class ReplysViewModel @Inject constructor(private val replyRepo: ReplyRepository) : ViewModel() {
    val currentThreadId: String? get() = replyRepo.currentThreadId
    val po get() = replyRepo.po

    private val replyList = mutableListOf<Reply>()

    // TODO: previous page & next page should be handled the same
    var previousPage: List<Reply> = emptyList()
    val replys = MediatorLiveData<MutableList<Reply>>().apply {
        addSource(replyRepo.emptyPage) {
            if (it == true) {
                val emptyPage = replyRepo.attachAdAndHead(emptyList(), 1)
                handleNewPageData(emptyPage, DIRECTION.NEXT)
                replyRepo.setLoadingStatus(LoadingStatus.NODATA)
            }
        }
    }

    private val listeningPages = mutableMapOf<Int, LiveData<List<Reply>>>()
    val maxPage get() = replyRepo.maxPage

    val loadingStatus get() = replyRepo.loadingStatus

    val addFeedResponse
        get() = replyRepo.addFeedResponse

    enum class DIRECTION {
        NEXT,
        PREVIOUS
    }

    var direction = DIRECTION.NEXT

    fun setThread(f: Thread) {
        if (f.id != currentThreadId) clearCache()
        replyRepo.setThread(f)
        if (replyList.isEmpty()) getNextPage(false)
    }

    /** sometimes VM is KILLED but repo IS ALIVE, cache in Repo
     *  may show current page is full so should get next page,
     *  but VM still needs the cached current page to display
     */
    fun getNextPage(incrementPage: Boolean = true) {
        direction = DIRECTION.NEXT
        var nextPage = replyList.lastOrNull()?.page ?: 1
        if (incrementPage && replyRepo.checkFullPage(nextPage)) nextPage += 1
        listenToNewPage(nextPage, direction)
    }

    fun getPreviousPage() {
        direction = DIRECTION.PREVIOUS
        // Refresh when no data, usually error occurs
        if (replyList.isNullOrEmpty()) {
            getNextPage()
            return
        }
        val lastPage = (replyList.firstOrNull()?.page ?: 1) - 1
        if (lastPage < 1) {
            replyRepo.setLoadingStatus(LoadingStatus.NODATA, "没有上一页了...")
            return
        }
        listenToNewPage(lastPage, direction)
    }


    private fun listenToNewPage(page: Int, direction: DIRECTION) {
        val newPage = replyRepo.getLiveDataOnPage(page)
        if (listeningPages[page] != newPage) {
            listeningPages[page] = newPage
            replys.addSource(newPage) {
                if (!it.isNullOrEmpty()) {
                    handleNewPageData(replyRepo.attachAdAndHead(it, page), direction)
                    replyRepo.setLoadingStatus(LoadingStatus.SUCCESS)
                }
            }
        } else {
            viewModelScope.launch {
                replyRepo.getServerData(page)
            }
        }
    }

    private fun mergeNewPage(list: MutableList<Reply>, direction: DIRECTION) {
        if (list.isNullOrEmpty()) return
        val targetPage = list.first().page
        if (direction == DIRECTION.PREVIOUS) {
            replyList.addAll(0, list)
            previousPage = list
        } else if (replyList.isEmpty() || targetPage > replyList.last().page) {
            replyList.addAll(list)
        } else {
            val ind = replyList.indexOfLast { it.page < targetPage } + 1
            for (i in list.indices) {
                when {
                    (ind + i >= replyList.size || replyList[ind + i].page > list[i].page) -> {
                        replyList.add(ind + i, list[i])
                    }
                    replyList[ind + i].page == list[i].page -> replyList[ind + i] = list[i]
                    else -> throw Exception("replyList insertion error")
                }
            }
        }
    }

    private fun handleNewPageData(list: MutableList<Reply>, direction: DIRECTION) {
        mergeNewPage(list, direction)
        replys.postValue(replyList)
    }

    private fun clearCache() {
        listeningPages.values.map { replys.removeSource(it) }
        listeningPages.clear()
        replyList.clear()
    }

    fun jumpTo(page: Int) {
        Timber.i("Jumping to page $page... Clearing old data")
        direction = DIRECTION.NEXT
        clearCache()
        listenToNewPage(page, direction)
    }

    // TODO: do not send request if subscribe already
    fun addFeed(uuid: String, id: String) {
        viewModelScope.launch {
            replyRepo.addFeed(uuid, id)
        }
    }
}
