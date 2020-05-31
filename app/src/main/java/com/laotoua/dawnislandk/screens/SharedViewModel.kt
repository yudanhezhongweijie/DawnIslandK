package com.laotoua.dawnislandk.screens

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.laotoua.dawnislandk.data.local.Forum
import com.laotoua.dawnislandk.data.local.Thread
import timber.log.Timber

class SharedViewModel : ViewModel() {
    private var _selectedForumId = MutableLiveData<String>()
    val selectedForumId: LiveData<String> get() = _selectedForumId
    private var _selectedThreadId = MutableLiveData<String>()
    val selectedThreadId: LiveData<String> get() = _selectedThreadId
    private var selectedThreadFid: String = "-1"

    private var forumNameMapping = mapOf<String, String>()

    fun setForum(f: Forum) {
        Timber.d("Setting forum to id: ${f.id}")
        _selectedForumId.value = f.id
    }

    fun setThread(t: Thread) {
        Timber.d("Setting thread to ${t.id} and its fid to ${t.fid}")
        selectedThreadFid = t.fid
        _selectedThreadId.value = t.id
    }

    fun setForumNameMapping(map: Map<String, String>) {
        forumNameMapping = map
    }

    fun getForumNameMapping(): Map<String, String> = forumNameMapping

    fun getForumDisplayName(id: String): String = forumNameMapping[id] ?: ""

    fun getSelectedThreadForumName(): String = getForumDisplayName(selectedThreadFid)

    fun getForumIdByName(name: String): String {
        return forumNameMapping.filterValues { it == name }.keys.first()
    }


}