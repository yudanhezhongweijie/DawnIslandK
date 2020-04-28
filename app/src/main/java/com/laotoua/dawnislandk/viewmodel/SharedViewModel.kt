package com.laotoua.dawnislandk.viewmodel

import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.laotoua.dawnislandk.data.entity.Forum
import com.laotoua.dawnislandk.data.entity.Thread
import com.laotoua.dawnislandk.ui.fragment.*
import timber.log.Timber

class SharedViewModel : ViewModel() {
    private var _selectedForum = MutableLiveData<Forum>()
    val selectedForum: LiveData<Forum> get() = _selectedForum
    private var _selectedThread = MutableLiveData<Thread>()
    val selectedThread: LiveData<Thread> get() = _selectedThread

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

    fun getForumIdByName(name: String): String {
        return forumNameMapping.filter { (_, value) -> value == name }.keys.first()
    }

    fun generateAppbarTitle(): String {
        return when (currentFragment.value) {
            // TODO: default forumName
            is ThreadFragment -> "A岛 • ${selectedForum.value?.name ?: "时间线"}"
            is ReplyFragment -> "A岛 • ${selectedThread.value?.fid?.let { getForumDisplayName(it) }}"
            is FeedFragment -> "我的订阅"
            is SettingsFragment -> "设置"
            is SizeCustomizationFragment -> "设置串卡片布局"
            is TrendFragment -> "A岛热榜"
            else -> {
                Timber.e("Need to set title in Frag ${currentFragment.value}, currently using default...")
                "A岛 • ${selectedForum.value?.name ?: "时间线"}"
            }
        }

    }

    fun generateAppBarSubtitle(): String? {
        return when (currentFragment.value) {
            is ReplyFragment -> ">> No.${selectedThread.value?.id} • adnmb.com"
            else -> null
        }
    }

}