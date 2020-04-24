package com.laotoua.dawnislandk.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laotoua.dawnislandk.data.entity.Forum
import com.laotoua.dawnislandk.data.entity.Thread
import com.laotoua.dawnislandk.data.network.APIErrorResponse
import com.laotoua.dawnislandk.data.network.APINoDataResponse
import com.laotoua.dawnislandk.data.network.APISuccessResponse
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
    private var _loadFail = MutableLiveData(false)
    val loadFail: LiveData<Boolean>
        get() = _loadFail

    fun getThreads() {
        viewModelScope.launch {
            val fid = _currentForum?.id ?: "-1"
            Timber.i("Getting threads from $fid ${_currentForum?.name} on page $pageCount")
            when (val response = NMBServiceClient.getThreads(fid, pageCount)) {
                // TODO thread deleted
                is APINoDataResponse -> {
                    Timber.e("APINoDataResponse: ${response.message}")
                }
                // TODO mostly network error
                is APIErrorResponse -> {
                    Timber.e("APIErrorResponse: ${response.message}")
                }
                is APISuccessResponse -> {
                    val list = response.data
                    // assign fid if not timeline
                    if (fid != "-1") list.map { it.fid = fid }
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
                }
                else -> {
                    Timber.e("unhandled API type response $response")
                }
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
