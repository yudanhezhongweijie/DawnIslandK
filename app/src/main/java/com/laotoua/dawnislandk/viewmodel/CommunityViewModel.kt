package com.laotoua.dawnislandk.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laotoua.dawnislandk.entity.Community
import com.laotoua.dawnislandk.entity.CommunityDao
import com.laotoua.dawnislandk.network.NMBServiceClient
import com.laotoua.dawnislandk.util.AppState
import kotlinx.coroutines.launch
import timber.log.Timber

class CommunityViewModel : ViewModel() {
    private val dao: CommunityDao = AppState.DB.communityDao()

    private var _communityList = MutableLiveData<List<Community>>()
    val communityList: LiveData<List<Community>>
        get() = _communityList
    private var _loadFail = MutableLiveData(false)
    val loadFail: LiveData<Boolean>
        get() = _loadFail

    init {
        // TODO added repository
        loadCommunitiesFromDB()
        getCommunitiesFromServer()
    }

    private fun getCommunitiesFromServer() {
        viewModelScope.launch {
            try {
                val list = NMBServiceClient.getCommunities()
                Timber.i("Downloaded communities size ${list.size}")
                if (list.isEmpty()) {
                    Timber.d("Didn't get communities from API")
                    return@launch
                }

                if (list != communityList.value) {
                    Timber.i("Community list has changed. updating...")
                    _communityList.postValue(list)
                    _loadFail.postValue(false)

                    // save to local db
                    saveCommunitiesToDB(list)
                } else {
                    Timber.i("Community list is the same as Db. Reusing...")
                }
            } catch (e: Exception) {
                Timber.e(e, "failed to get communities")
                _loadFail.postValue(true)
            }
        }
    }

    private fun loadCommunitiesFromDB() {
        viewModelScope.launch {
            dao.getAll().let {
                if (it.isNotEmpty()) {
                    Timber.i("Loaded ${it.size} communities from db")
                    _communityList.postValue(it)
                } else {
                    Timber.i("Db has no data about communities")
                }
            }
        }
    }

    private suspend fun saveCommunitiesToDB(list: List<Community>) {
        dao.insertAll(list)
    }

    fun getForumNameMapping(): Map<String, String> {
        return communityList.value?.flatMap { it.forums }?.associateBy(
            keySelector = { it.id },
            valueTransform = { it.name }) ?: mapOf()

    }

    fun refresh() {
        getCommunitiesFromServer()
    }

}