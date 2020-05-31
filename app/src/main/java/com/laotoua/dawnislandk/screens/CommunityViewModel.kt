package com.laotoua.dawnislandk.screens

import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laotoua.dawnislandk.data.repository.CommunityRepository
import com.laotoua.dawnislandk.util.EventPayload
import com.laotoua.dawnislandk.util.LoadingStatus
import com.laotoua.dawnislandk.util.SingleLiveEvent
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

class CommunityViewModel @Inject constructor(private val communityRepository: CommunityRepository) :
    ViewModel() {

    val communityList get() = communityRepository.communityList

    val loadingStatus: LiveData<SingleLiveEvent<EventPayload<Nothing>>> =
        Transformations.map(communityRepository.errorMsg) { message ->
            Timber.e(message)
            SingleLiveEvent.create(
                LoadingStatus.FAILED,
                "无法读取板块列表...\n${message}",
                null
            )
        }

    fun getForumNameMapping(): Map<String, String> {
        return communityList.value?.flatMap { it.forums }?.associateBy(
            keySelector = { it.id },
            valueTransform = { it.name }) ?: mapOf()
    }

    fun refresh() {
        viewModelScope.launch { communityRepository.refresh() }
    }

}