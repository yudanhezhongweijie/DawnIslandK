package com.laotoua.dawnislandk.screens

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laotoua.dawnislandk.data.local.Community
import com.laotoua.dawnislandk.data.repository.CommunityRepository
import com.laotoua.dawnislandk.data.repository.DataResource
import com.laotoua.dawnislandk.util.EventPayload
import com.laotoua.dawnislandk.util.LoadingStatus
import com.laotoua.dawnislandk.util.SingleLiveEvent
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

class CommunityViewModel @Inject constructor(private val communityRepository: CommunityRepository) :
    ViewModel() {

    private var _communityList = MutableLiveData<List<Community>>()
    val communityList: LiveData<List<Community>>
        get() = _communityList

    private var _loadingStatus = MutableLiveData<SingleLiveEvent<EventPayload<Nothing>>>()
    val loadingStatus: LiveData<SingleLiveEvent<EventPayload<Nothing>>>
        get() = _loadingStatus

    init {
        getCommunities()
    }

    private fun getCommunities(remoteDataOnly: Boolean = false) {
        viewModelScope.launch {
            communityRepository.getCommunities(remoteDataOnly).collect {
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
                        if (it.data!!.isNotEmpty()) _communityList.postValue(it.data)
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