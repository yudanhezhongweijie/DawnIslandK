package com.laotoua.dawnislandk.data.repository

import com.laotoua.dawnislandk.data.entity.Community
import com.laotoua.dawnislandk.data.entity.CommunityDao
import com.laotoua.dawnislandk.data.network.NMBServiceClient
import com.laotoua.dawnislandk.data.state.AppState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import timber.log.Timber

class CommunityRepository {
    // TODO: Injection
    private val webService = NMBServiceClient
    private val dao: CommunityDao = AppState.DB.communityDao()

    fun getCommunities(remoteDataOnly: Boolean): Flow<DataResource<List<Community>>> = flow {
        var localList: List<Community>? = null
        if (!remoteDataOnly) {
            localList = dao.getCommunities()
            Timber.i("Loaded ${localList.size} communities from db")
            emit(DataResource.create(localList))
        }

        DataResource.create(webService.getCommunities()).run {
            if (this is DataResource.Success) {
                if (!data.isNullOrEmpty() && data != localList) {
                    Timber.i("Community list has changed. updating...")
                    emit(this)
                    dao.insertAll(data)
                } else {
                    Timber.i("Community list is the same as Db. Reusing...")
                }
            }
        }
    }
}