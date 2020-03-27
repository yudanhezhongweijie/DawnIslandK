package com.laotoua.dawnislandk.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laotoua.dawnislandk.util.API
import com.laotoua.dawnislandk.util.Forum
import com.laotoua.dawnislandk.util.ForumDao
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
    private var currentForum: Forum? = null
    private var pageCount = 1

    private var forumDao: ForumDao? = null

    init {
        getThreads()
    }

    fun getThreads() {
        viewModelScope.launch {
            val fid = currentForum?.id ?: "4"
            Log.i(TAG, "getting threads from $fid ${currentForum?.name ?: "综合版1(default)"}")
            val list = api.getThreads("id=" + fid + "&page=${pageCount}", fid.equals("-1"), fid)
            val noDuplicates = list.filterNot { threadIds.contains(it.id) }
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

    fun setForum(f: Forum) {
        Log.i(TAG, "Cleaning old threads...")
        threadList.clear()
        threadIds.clear()
        Log.i(TAG, "Setting new forum: ${f.id}")
        currentForum = f
        pageCount = 1
        getThreads()
    }

    fun setForumDao(dao: ForumDao?) {
        this.forumDao = dao
    }

}
