package com.laotoua.dawnislandk.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laotoua.dawnislandk.data.entity.Forum
import com.laotoua.dawnislandk.data.entity.Thread
import com.laotoua.dawnislandk.data.network.NMBServiceClient
import kotlinx.coroutines.launch
import timber.log.Timber

class ThreadViewModel : ViewModel() {

    private val threadList = mutableListOf<Thread>()
    private val threadIds = mutableSetOf<String>()
    private var _thread = MutableLiveData<List<Thread>>()
    val thread: LiveData<List<Thread>> get() = _thread
    private var _currentForum: Forum? = null
    val currentForum: Forum? get() = _currentForum
    private var pageCount = 1

    private var _loadingStatus = MutableLiveData<SingleLiveEvent<EventPayload<Nothing>>>()
    val loadingStatus: LiveData<SingleLiveEvent<EventPayload<Nothing>>>
        get() = _loadingStatus

    fun getThreads() {
        viewModelScope.launch {
            _loadingStatus.postValue(SingleLiveEvent.create(LoadingStatus.LOADING))
            val fid = _currentForum?.id ?: "-1"
            Timber.i("Getting threads from $fid ${_currentForum?.name} on page $pageCount")
            DataResource.create(NMBServiceClient.getThreads(fid, pageCount)).run {
                when (this) {
                    is DataResource.Error -> {
                        Timber.e(message)
                        _loadingStatus.postValue(
                            SingleLiveEvent.create(
                                LoadingStatus.FAILED,
                                "无法读取串列表...\n$message"
                            )
                        )
                    }
                    is DataResource.Success -> {
                        convertServerData(data!!, fid)
                    }
                }
            }
        }
    }

    private fun convertServerData(data: List<Thread>, fid: String) {
        // assign fid if not timeline
        if (fid != "-1") data.map { it.fid = fid }
        val noDuplicates = data.filterNot { threadIds.contains(it.id) }
        if (noDuplicates.isNotEmpty()) {
            threadIds.addAll(noDuplicates.map { it.id })
            threadList.addAll(noDuplicates)
            Timber.i(
                "New thread + ads has size of ${noDuplicates.size}, threadIds size ${threadIds.size}, Forum ${currentForum?.name} now have ${threadList.size} threads"
            )
            _thread.postValue(threadList)
            _loadingStatus.postValue(SingleLiveEvent.create(LoadingStatus.SUCCESS))
            pageCount += 1
        } else {
            val message = "Forum ${currentForum?.getDisplayName()} has no new threads."
            Timber.i(message)
            _loadingStatus.postValue(SingleLiveEvent.create(LoadingStatus.FAILED, message))
        }
    }

    fun setForum(f: Forum) {
        if (f == currentForum) return
        Timber.i("Forum has changed. Cleaning old threads...")
        threadList.clear()
        threadIds.clear()
        Timber.i("Setting new forum: ${f.id}")
        _currentForum = f
        pageCount = 1
        getThreads()
    }

    fun refresh() {
        Timber.i("Refreshing forum ${currentForum!!.name}...")
        threadList.clear()
        threadIds.clear()
        pageCount = 1
        getThreads()
    }
}
