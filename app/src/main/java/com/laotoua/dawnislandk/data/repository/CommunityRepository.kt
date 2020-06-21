package com.laotoua.dawnislandk.data.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.liveData
import com.laotoua.dawnislandk.data.local.entity.Community
import com.laotoua.dawnislandk.data.local.dao.CommunityDao
import com.laotoua.dawnislandk.data.remote.APIDataResponse
import com.laotoua.dawnislandk.data.remote.NMBServiceClient
import com.laotoua.dawnislandk.util.EventPayload
import com.laotoua.dawnislandk.util.LoadingStatus
import com.laotoua.dawnislandk.util.SingleLiveEvent
import kotlinx.coroutines.Dispatchers
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CommunityRepository @Inject constructor(
    private val webService: NMBServiceClient,
    private val dao: CommunityDao
) {
    val communityList: LiveData<List<Community>> = liveData(Dispatchers.IO) {
        Timber.d("Loading communities")
        val local = dao.getAll()
        emitSource(local)
        matchRemoteData(local)
    }

    val reedPictureUrl = MutableLiveData<String>()

    private val _loadingStatus = MutableLiveData<SingleLiveEvent<EventPayload<Nothing>>>()
    val loadingStatus: LiveData<SingleLiveEvent<EventPayload<Nothing>>> get() = _loadingStatus

    private suspend fun getRemoteData(): List<Community> {
        _loadingStatus.postValue(SingleLiveEvent.create(LoadingStatus.LOADING))
        webService.getCommunities().run {
            if (this is APIDataResponse.APIErrorDataResponse) _loadingStatus.postValue(
                SingleLiveEvent.create(
                    LoadingStatus.FAILED,
                    "无法读取板块列表...\n${message}"
                )
            )
            return data ?: emptyList()
        }
    }

    private suspend fun matchRemoteData(
        local: LiveData<List<Community>>,
        remoteDataOnly: Boolean = false
    ) {
        val remote = getRemoteData()
        if (remote.isNotEmpty() && (remote != local.value || remoteDataOnly)) {
            Timber.d("Remote data differs from local data or forced refresh. Updating...")
            dao.insertAll(remote)
        }
        _loadingStatus.postValue(SingleLiveEvent.create(LoadingStatus.SUCCESS))
    }

    suspend fun refresh() {
        matchRemoteData(communityList, true)
    }

    suspend fun getRandomReedPicture() {
        webService.getRandomReedPicture().run {
            if (this is APIDataResponse.APISuccessDataResponse) {
                reedPictureUrl.postValue(data)
            }
        }
    }

}