package com.laotoua.dawnislandk.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laotoua.dawnislandk.util.API
import com.laotoua.dawnislandk.util.Forum
import com.laotoua.dawnislandk.util.ForumDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ForumViewModel : ViewModel() {
    private val TAG = "ForumVM"
    private val api = API()
    private var dao: ForumDao? = null

    private var _forumList = MutableLiveData<List<Forum>>()
    val forumList: LiveData<List<Forum>>
        get() = _forumList


    fun getForums() {
        viewModelScope.launch {
            val list = api.getForums()
            Log.i(TAG, "Downloaded forums size ${list.size}")
            if (list != forumList.value) {
                Log.i(TAG, "Forum list has changed. updating...")
                _forumList.postValue(list)
                // save to local db
                withContext(Dispatchers.IO) {
                    dao?.insertAll(list)
                }
            } else {
                Log.i(TAG, "Forum list is the same as Db. Reusing...")
            }
        }
    }

    fun loadFromDB() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                dao?.getAll().let {
                    if (it?.size ?: 0 > 0) {
                        Log.i(TAG, "Loaded ${it?.size} forums from db")
                        _forumList.postValue(it)
                    } else {
                        Log.i(TAG, "Db has no data about forums")
                    }
                }
            }
        }
    }

    fun getForumNameMapping(): Map<String, String> {
        forumList.value?.let { list ->
            return list.associateBy(
                keySelector = { it.id },
                valueTransform = { it.name })
        }
        return mapOf()
    }

    fun setDb(dao: ForumDao) {
        this.dao = dao
        Log.i(TAG, "Forum DAO set!!!")
    }
}