package com.laotoua.dawnislandk.data.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.liveData
import com.laotoua.dawnislandk.data.local.Community
import com.laotoua.dawnislandk.data.local.dao.CommunityDao
import com.laotoua.dawnislandk.data.remote.APIErrorDataResponse
import com.laotoua.dawnislandk.data.remote.NMBServiceClient
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

    private val mErrorMsg = MutableLiveData<String>()
    val errorMsg: LiveData<String> get() = mErrorMsg

    private suspend fun getRemoteData(): List<Community> {
        webService.getCommunities().run {
            if (this is APIErrorDataResponse) mErrorMsg.postValue(message)
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
    }

    suspend fun refresh() {
        matchRemoteData(communityList, true)
    }

}