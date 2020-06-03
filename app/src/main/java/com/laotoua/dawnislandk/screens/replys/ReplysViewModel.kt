package com.laotoua.dawnislandk.screens.replys

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laotoua.dawnislandk.data.local.Reply
import com.laotoua.dawnislandk.data.repository.ReplyRepository
import com.laotoua.dawnislandk.util.LoadingStatus
import com.laotoua.dawnislandk.util.addOrSet
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import kotlin.collections.set

class ReplysViewModel @Inject constructor(private val replyRepo: ReplyRepository) : ViewModel() {
    val currentThreadId: String? get() = replyRepo.currentThreadId
    val po get() = replyRepo.currentThread?.value?.userid ?: ""

    private val replyList = mutableListOf<Reply>()

    private val filterIds = mutableListOf<String>()

    val replys = MediatorLiveData<MutableList<Reply>>()

    private val listeningPages = mutableMapOf<Int, LiveData<List<Reply>>>()
    val maxPage get() = replyRepo.maxPage

    val loadingStatus get() = replyRepo.loadingStatus

    val addFeedResponse
        get() = replyRepo.addFeedResponse

    fun setThreadId(id: String) {
        if (id != currentThreadId) clearCache()
        replyRepo.setThreadId(id)
        viewModelScope.launch { if (replyList.isEmpty()) loadReadingProgress(id) }
    }

    private suspend fun loadReadingProgress(id: String) {
        getNextPage(false, replyRepo.getReadingProgress(id))
    }

    fun saveReadingProgress(page: Int) =
        viewModelScope.launch { replyRepo.saveReadingProgress(page) }

    /** sometimes VM is KILLED but repo IS ALIVE, cache in Repo
     *  may show current page is full so should get next page,
     *  but VM still needs the cached current page to display
     */
    fun getNextPage(incrementPage: Boolean = true, readingProgress: Int? = null) {
        var nextPage = readingProgress ?: (replyList.lastOrNull()?.page ?: 1)
        if (incrementPage && replyRepo.checkFullPage(nextPage)) nextPage += 1
        listenToNewPage(nextPage, filterIds)
    }

    fun getPreviousPage() {
        // Refresh when no data, usually error occurs
        if (replyList.isNullOrEmpty()) {
            getNextPage()
            return
        }
        val lastPage = (replyList.firstOrNull()?.page ?: 1) - 1
        if (lastPage < 1) return
        listenToNewPage(lastPage, filterIds)
    }

    private fun List<Reply>.attachAd(page: Int) = toMutableList().apply {
        //  insert Ad below thread head or as first
        replyRepo.getAd(page)?.let {
            add(if (page == 1) 1 else 0, it)
        }
    }

    private fun listenToNewPage(page: Int, filterIds: List<String>) {
        val newPage = replyRepo.getLiveDataOnPage(page)
        if (listeningPages[page] != newPage) {
            listeningPages[page] = newPage
            replys.addSource(newPage) {
                if (!it.isNullOrEmpty()) {
                    handleNewPageData(it.attachAd(page), filterIds)
                }
            }
        } else {
            viewModelScope.launch {
                replyRepo.getServerData(page)
            }
        }
    }

    private fun mergeNewPage(list: MutableList<Reply>, filterIds: List<String>) {
        if (list.isNullOrEmpty()) return
        val targetPage = list.first().page
        // apply filter
        applyFilterToList(list, filterIds)
        if (replyList.isEmpty() || targetPage > replyList.last().page) {
            replyList.addAll(list)
        } else if (targetPage < replyList.first().page) {
            replyList.addAll(0, list)
        } else {
            val insertInd = replyList.indexOfLast { it.page < targetPage } + 1
            list.mapIndexed { i, reply ->
                replyList.addOrSet(insertInd + i, reply)
            }
        }
    }

    private fun handleNewPageData(list: MutableList<Reply>, filterIds: List<String>) {
        mergeNewPage(list, filterIds)
        replys.postValue(replyList)
        replyRepo.setLoadingStatus(LoadingStatus.SUCCESS)
    }

    private fun clearCache() {
        listeningPages.values.map { replys.removeSource(it) }
        listeningPages.clear()
        replyList.clear()
        clearFilter()
    }

    fun onlyPo() {
        applyFilter(po)
    }

    private fun applyFilter(vararg Ids: String) {
        filterIds.addAll(Ids)
        if (replyList.isNotEmpty()) applyFilterToList(replyList, filterIds)
        replys.postValue(replyList)
    }

    fun clearFilter() {
        filterIds.clear()
        replyList.map { it.visible = true }
        replys.postValue(replyList)
    }

    // keep ad as well
    private fun applyFilterToList(list: MutableList<Reply>, filterIds: List<String>) {
        if (filterIds.isNotEmpty()) list.map {
            it.visible = filterIds.contains(it.userid) || it.isAd()
        }
    }


    fun jumpTo(page: Int) {
        Timber.i("Jumping to page $page... Clearing old data")
        replyList.clear()
        listenToNewPage(page, filterIds)
    }

    // TODO: do not send request if subscribe already
    fun addFeed(uuid: String, id: String) {
        viewModelScope.launch {
            replyRepo.addFeed(uuid, id)
        }
    }
}
