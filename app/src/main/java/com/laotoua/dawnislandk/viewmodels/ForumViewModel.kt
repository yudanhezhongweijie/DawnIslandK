package com.laotoua.dawnislandk.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laotoua.dawnislandk.util.API
import com.laotoua.dawnislandk.util.Forum
import kotlinx.coroutines.launch

class ForumViewModel : ViewModel() {
    private val TAG = "ForumVM"
    private val api = API()

    private var _forumList = MutableLiveData<List<Forum>>()
    val forumList: LiveData<List<Forum>>
        get() = _forumList

    init {
        getForums()
    }

    fun getForums() {
        viewModelScope.launch {
            val list = api.getForums()
            Log.i(TAG, "forums size ${list.size}")
            _forumList.postValue(list)
        }
    }
}