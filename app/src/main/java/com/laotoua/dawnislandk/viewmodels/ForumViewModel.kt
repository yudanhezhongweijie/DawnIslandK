package com.laotoua.dawnislandk.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laotoua.dawnislandk.entities.Forum
import com.laotoua.dawnislandk.entities.ForumDao
import com.laotoua.dawnislandk.util.API
import com.laotoua.dawnislandk.util.AppState
import kotlinx.coroutines.launch
import timber.log.Timber

class ForumViewModel : ViewModel() {
    private val api = API()
    private val dao: ForumDao = AppState.DB.forumDao()

    private var _forumList = MutableLiveData<List<Forum>>()
    val forumList: LiveData<List<Forum>>
        get() = _forumList
    private var _loadFail = MutableLiveData(false)
    val loadFail: LiveData<Boolean>
        get() = _loadFail


    fun getForums() {
        viewModelScope.launch {
            try {
                val list = api.getForums()
                Timber.i("Downloaded forums size ${list.size}")
                if (list != forumList.value) {
                    Timber.i("Forum list is the same as Db. Reusing...")
                    Timber.i("Forum list has changed. updating...")
                    _forumList.postValue(list)
                    _loadFail.postValue(false)

                    // save to local db
                    saveToDB(list)
                } else {
                    Timber.i("Forum list is the same as Db. Reusing...")
                }
            } catch (e: Exception) {
                Timber.e(e, "failed to get forums")
                _loadFail.postValue(true)
            }
        }
    }

    fun loadFromDB() {
        viewModelScope.launch {
            dao.getAll().let {
                if (it.size ?: 0 > 0) {
                    Timber.i("Loaded ${it.size} forums from db")
                    _forumList.postValue(it)
                } else {
                    Timber.i("Db has no data about forums")
                }
            }
        }
    }

    private suspend fun saveToDB(list: List<Forum>) {
        dao.insertAll(list)
    }

    fun getForumNameMapping(): Map<String, String> {
        forumList.value?.let { list ->
            return list.associateBy(
                keySelector = { it.id },
                valueTransform = { it.name })
        }
        return mapOf()
    }

}