package com.laotoua.dawnislandk.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laotoua.dawnislandk.data.entity.Community
import com.laotoua.dawnislandk.data.entity.CommunityDao
import com.laotoua.dawnislandk.data.network.NMBServiceClient
import com.laotoua.dawnislandk.data.state.AppState
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
            DataResource.create(NMBServiceClient.getCommunities()).run {
                when (this) {
                    is DataResource.Error -> {
                        Timber.e(message)
                        _loadFail.postValue(true)
                    }
                    is DataResource.Success -> {
                        convertServerData(data!!)
                        _loadFail.postValue(false)
                    }
                }
            }
        }
    }

    private fun convertServerData(data: List<Community>) {
        Timber.i("Downloaded communities size ${data.size}")
        if (data.isEmpty()) {
            Timber.d("API returns empty response")
            return
        }
        if (data != communityList.value) {
            Timber.i("Community list has changed. updating...")
            _communityList.postValue(data)
            _loadFail.postValue(false)

            // save to local db
            viewModelScope.launch { saveCommunitiesToDB(data) }
        } else {
            Timber.i("Community list is the same as Db. Reusing...")
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