package com.laotoua.dawnislandk.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laotoua.dawnislandk.data.entity.Community
import com.laotoua.dawnislandk.data.repository.CommunityRepository
import com.laotoua.dawnislandk.data.repository.DataResource
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import timber.log.Timber

class CommunityViewModel : ViewModel() {

    private var _communityList = MutableLiveData<List<Community>>()
    val communityList: LiveData<List<Community>>
        get() = _communityList

    private var _loadingStatus = MutableLiveData<SingleLiveEvent<EventPayload<Nothing>>>()
    val loadingStatus: LiveData<SingleLiveEvent<EventPayload<Nothing>>>
        get() = _loadingStatus

    private val repository = CommunityRepository()

    init {
        getCommunities()
    }

    private fun getCommunities(remoteDataOnly: Boolean = false) {
        viewModelScope.launch {
            repository.getCommunities(remoteDataOnly).collect {
                Timber.d("collecting flow")
                when (it) {
                    is DataResource.Error -> {
                        Timber.e(it.message)
                        _loadingStatus.postValue(
                            SingleLiveEvent.create(
                                LoadingStatus.FAILED,
                                "无法读取板块列表...\n${it.message}"
                            )
                        )
                    }
                    is DataResource.Success -> {
                        _communityList.postValue(it.data!!)
                    }
                }
            }
        }
    }

    fun getForumNameMapping(): Map<String, String> {
        return communityList.value?.flatMap { it.forums }?.associateBy(
            keySelector = { it.id },
            valueTransform = { it.name }) ?: mapOf()
    }

    fun refresh() {
        getCommunities(true)
    }

}