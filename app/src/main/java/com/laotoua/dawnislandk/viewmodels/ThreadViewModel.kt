package com.laotoua.dawnislandk.viewmodels

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

    private lateinit var threadList: List<ThreadList>
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
            threadList = api.getThreads("id=" + forumId + "&page=${pageCount}")
            _newPage.postValue(threadList)
            pageCount += 1
        }
    }

}
