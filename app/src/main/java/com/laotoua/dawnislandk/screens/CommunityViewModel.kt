package com.laotoua.dawnislandk.screens

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laotoua.dawnislandk.data.repository.CommunityRepository
import com.laotoua.dawnislandk.util.EventPayload
import com.laotoua.dawnislandk.util.SingleLiveEvent
import kotlinx.coroutines.launch
import javax.inject.Inject

class CommunityViewModel @Inject constructor(private val communityRepository: CommunityRepository) :
    ViewModel() {

    val communityList get() = communityRepository.communityList

    val loadingStatus: LiveData<SingleLiveEvent<EventPayload<Nothing>>> =
        communityRepository.loadingStatus

    fun getForumNameMapping(): Map<String, String> {
        return communityList.value?.flatMap { it.forums }?.associateBy(
            keySelector = { it.id },
            valueTransform = { it.name }) ?: mapOf()
    }

    fun refresh() {
        viewModelScope.launch { communityRepository.refresh() }
    }

}