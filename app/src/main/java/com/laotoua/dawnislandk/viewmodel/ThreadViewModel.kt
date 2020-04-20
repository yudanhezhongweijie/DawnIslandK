package com.laotoua.dawnislandk.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laotoua.dawnislandk.entity.Forum
import com.laotoua.dawnislandk.entity.Thread
import com.laotoua.dawnislandk.network.NMBServiceClient
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
    private var _loadFail = MutableLiveData(false)
    val loadFail: LiveData<Boolean>
        get() = _loadFail

    fun getThreads() {
        viewModelScope.launch {
            try {
                val fid = _currentForum?.id ?: "-1"
                Timber.i("Getting threads from $fid ${_currentForum?.name ?: "时间线(default)"}")
                val list = NMBServiceClient.getThreads(fid, pageCount)
                val noDuplicates = list.filterNot { threadIds.contains(it.id) }
                if (noDuplicates.isNotEmpty()) {
                    threadIds.addAll(noDuplicates.map { it.id })
                    Timber.i(
                        "New thread + ads has size of ${noDuplicates.size}, threadIds size ${threadIds.size}"
                    )
                    threadList.addAll(noDuplicates)
                    Timber.i(
                        "Forum ${currentForum?.name} now have ${threadList.size} threads"
                    )
                    _thread.postValue(threadList)
                    _loadFail.postValue(false)
                    pageCount += 1
                } else {
                    Timber.i("Forum ${currentForum?.getDisplayName()} has no new threads.")
                    _loadFail.postValue(true)
                }
            } catch (e: Exception) {
                Timber.e(e, "failed to get threads")
                _loadFail.postValue(true)
            }
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

}
