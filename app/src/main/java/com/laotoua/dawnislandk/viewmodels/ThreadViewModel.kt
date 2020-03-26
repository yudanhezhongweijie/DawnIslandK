package com.laotoua.dawnislandk.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laotoua.dawnislandk.util.API
import com.laotoua.dawnislandk.util.ThreadList
import kotlinx.coroutines.launch

class ThreadViewModel : ViewModel() {
    private val TAG = "ThreadVM"
    private val api = API()

    private val threadList = mutableListOf<ThreadList>()
    private val threadIds = mutableSetOf<String>()
    private var _newPage = MutableLiveData<List<ThreadList>>()
    val newPage: LiveData<List<ThreadList>>
        get() = _newPage

    private var forumId = "4"
    private var pageCount = 1


    init {
        getThreads()
    }

    fun getThreads() {
        viewModelScope.launch {
            val newthreads = api.getThreads("id=" + forumId + "&page=${pageCount}")
            val noDuplicates = newthreads.filterNot { threadIds.contains(it.id) }
            threadIds.addAll(noDuplicates.map { it.id })
            Log.i(
                TAG,
                "no duplicate thread size ${noDuplicates.size}, threadIds size ${threadIds.size}"
            )
            threadList.addAll(noDuplicates)
            _newPage.postValue(noDuplicates)
            pageCount += 1
        }
    }

}
