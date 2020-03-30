package com.laotoua.dawnislandk.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laotoua.dawnislandk.util.API
import com.laotoua.dawnislandk.util.Forum
import com.laotoua.dawnislandk.util.ForumDao
import com.laotoua.dawnislandk.util.ThreadList
import kotlinx.coroutines.launch
import timber.log.Timber

class ThreadViewModel : ViewModel() {
    private val api = API()

    private val threadList = mutableListOf<ThreadList>()
    private val threadIds = mutableSetOf<String>()
    private var _newPage = MutableLiveData<List<ThreadList>>()
    val newPage: LiveData<List<ThreadList>>
        get() = _newPage
    private var _currentForum: Forum? = null
    val currentForum: Forum? get() = _currentForum
    private var pageCount = 1
    private var _loadFail = MutableLiveData(false)
    val loadFail: LiveData<Boolean>
        get() = _loadFail

    private var forumDao: ForumDao? = null

    init {
        getThreads()
    }

    fun getThreads() {
        viewModelScope.launch {
            try {
                val fid = _currentForum?.id ?: "-1"
                Timber.i("getting threads from $fid ${_currentForum?.name ?: "综合版1(default)"}")
                val list = api.getThreads("id=" + fid + "&page=${pageCount}", fid.equals("-1"), fid)
                val noDuplicates = list.filterNot { threadIds.contains(it.id) }
                if (noDuplicates.isNotEmpty()) {
                    threadIds.addAll(noDuplicates.map { it.id })
                    Timber.i(
                        "no duplicate thread size ${noDuplicates.size}, threadIds size ${threadIds.size}"
                    )
                    threadList.addAll(noDuplicates)
                    _newPage.postValue(noDuplicates)
                    _loadFail.postValue(false)
                    pageCount += 1
                } else {
                    Timber.i("Forum ${_currentForum!!.id} has no new threads.")
                    _loadFail.postValue(true)
                }
            } catch (e: Exception) {
                Timber.e(e, "failed to get threads")
                _loadFail.postValue(true)
            }
        }
    }

    fun setForum(f: Forum) {
        Timber.i("Cleaning old threads...")
        threadList.clear()
        threadIds.clear()
        Timber.i("Setting new forum: ${f.id}")
        _currentForum = f
        pageCount = 1
        getThreads()
    }

    fun setForumDao(dao: ForumDao?) {
        this.forumDao = dao
    }

}
