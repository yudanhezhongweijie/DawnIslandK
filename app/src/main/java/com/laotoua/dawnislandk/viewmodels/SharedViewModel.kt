package com.laotoua.dawnislandk.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.laotoua.dawnislandk.util.Forum
import com.laotoua.dawnislandk.util.ThreadList

class SharedViewModel : ViewModel() {
    private val TAG = "SharedVM"
    private var _selectedForum = MutableLiveData<Forum>()
    val selectedForum: LiveData<Forum> get() = _selectedForum
    private var _selectedThreadList = MutableLiveData<ThreadList>()
    val selectedThreadList: LiveData<ThreadList> get() = _selectedThreadList

    fun setForum(f: Forum) {
        _selectedForum.postValue(f)
        Log.i(TAG, "set forum to id: ${f.id}")
    }

    fun setThreadList(t: ThreadList) {
        _selectedThreadList.postValue(t)
        Log.i(TAG, "set thread to id: ${t.id}")
    }

}