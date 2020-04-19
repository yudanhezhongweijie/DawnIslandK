package com.laotoua.dawnislandk.viewmodels

import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.laotoua.dawnislandk.entities.Forum
import com.laotoua.dawnislandk.entities.Thread
import timber.log.Timber

class SharedViewModel : ViewModel() {
    private var _selectedForum = MutableLiveData<Forum>()
    val selectedForum: LiveData<Forum> get() = _selectedForum
    private var _selectedThreadList = MutableLiveData<Thread>()
    val selectedThread: LiveData<Thread> get() = _selectedThreadList

    private var forumNameMapping = mapOf<String, String>()

    private var _currentFragment = MutableLiveData<Fragment>()
    val currentFragment: MutableLiveData<Fragment> get() = _currentFragment

    fun setFragment(fragment: Fragment) {
        _currentFragment.postValue(fragment)
    }


    fun setForum(f: Forum) {
        Timber.i("set forum to id: ${f.id}")
        _selectedForum.postValue(f)
    }

    fun setThreadList(t: Thread) {
        Timber.i("set thread to id: ${t.id}")
        _selectedThreadList.postValue(t)
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

    // TODO: support multiple Po
    fun getPo(): String {
        return selectedThread.value!!.userid
    }

    fun generateAppbarTitle(): String {
        return when (currentFragment.value?.javaClass?.simpleName) {
            // TODO: default forumName
            "ThreadFragment" -> "A岛 • ${selectedForum.value?.name ?: "时间线"}"
            "ReplyFragment" -> "A岛 • ${selectedThread.value?.fid?.let { getForumDisplayName(it) }} • ${selectedThread.value?.id}"
            "SettingsFragment" -> "设置"
            "SizeCustomizationFragment" -> "设置串卡片布局"
            else -> {
                Timber.e("Need to set title in $currentFragment")
                "Need to set title in $currentFragment"
            }
        }

    }

}