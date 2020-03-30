package com.laotoua.dawnislandk.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.laotoua.dawnislandk.util.DawnDatabase
import com.laotoua.dawnislandk.util.Forum
import com.laotoua.dawnislandk.util.ThreadList
import timber.log.Timber

class SharedViewModel : ViewModel() {
    private var _selectedForum = MutableLiveData<Forum>()
    val selectedForum: LiveData<Forum> get() = _selectedForum
    private var _selectedThreadList = MutableLiveData<ThreadList>()
    val selectedThreadList: LiveData<ThreadList> get() = _selectedThreadList
    private var db: DawnDatabase? = null

    private var forumNameMapping = mapOf<String, String>()


    fun setForum(f: Forum) {
        _selectedForum.postValue(f)
        Timber.i("set forum to id: ${f.id}")
    }

    fun setThreadList(t: ThreadList) {
        _selectedThreadList.postValue(t)
        Timber.i("set thread to id: ${t.id}")
    }

    fun setDb(db: DawnDatabase) {
        this.db = db
    }

    fun getDb(): DawnDatabase? {
        return db
    }

    fun setForumNameMapping(map: Map<String, String>) {
        this.forumNameMapping = map
    }

    fun getForumDisplayName(id: String): String {
        return forumNameMapping[id] ?: ""
    }

    // TODO: support multiple Po
    fun getPo(): String {
        return selectedThreadList.value!!.userid
    }

}