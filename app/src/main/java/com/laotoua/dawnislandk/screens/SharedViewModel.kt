package com.laotoua.dawnislandk.screens

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.laotoua.dawnislandk.data.local.Forum
import com.laotoua.dawnislandk.data.local.Thread
import timber.log.Timber

class SharedViewModel : ViewModel() {
    private var _selectedForum = MutableLiveData<Forum>()
    val selectedForum: LiveData<Forum> get() = _selectedForum
    private var _selectedThread = MutableLiveData<Thread>()
    val selectedThread: LiveData<Thread> get() = _selectedThread

    private var forumNameMapping = mapOf<String, String>()

    fun setForum(f: Forum) {
        Timber.i("set forum to id: ${f.id}")
        _selectedForum.postValue(f)
    }

    fun setThread(t: Thread) {
        Timber.i("set thread to id: ${t.id}")
        _selectedThread.postValue(t)
    }

    fun setForumNameMapping(map: Map<String, String>) {
        this.forumNameMapping = map
    }

    fun getForumNameMapping(): Map<String, String> {
        return forumNameMapping
    }

    fun getForumDisplayName(id: String): String {
        return forumNameMapping[id] ?: ""
    }

    fun getCurrentForumDisplayName(): String {
        return getForumDisplayName(selectedThread.value?.fid!!)
    }

    fun getForumIdByName(name: String): String {
        return forumNameMapping.filter { (_, value) -> value == name }.keys.first()
    }

}