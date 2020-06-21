package com.laotoua.dawnislandk.screens

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laotoua.dawnislandk.data.local.entity.Forum
import com.laotoua.dawnislandk.data.repository.CommunityRepository
import com.laotoua.dawnislandk.util.EventPayload
import com.laotoua.dawnislandk.util.SingleLiveEvent
import kotlinx.coroutines.launch
import javax.inject.Inject

class CommunityViewModel @Inject constructor(private val communityRepository: CommunityRepository) :
    ViewModel() {

    val communityList get() = communityRepository.communityList
    val reedPictureUrl: LiveData<String> get() = communityRepository.reedPictureUrl
    val loadingStatus: LiveData<SingleLiveEvent<EventPayload<Nothing>>> =
        communityRepository.loadingStatus

    fun getForums(): List<Forum> {
        return communityList.value?.flatMap { it.forums } ?: emptyList()
    }

    fun refresh() {
        viewModelScope.launch { communityRepository.refresh() }
    }


    init {
        getRandomReedPicture()
    }

    fun getRandomReedPicture() {
        viewModelScope.launch { communityRepository.getRandomReedPicture() }
    }

}