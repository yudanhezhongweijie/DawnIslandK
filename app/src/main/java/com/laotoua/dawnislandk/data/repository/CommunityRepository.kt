package com.laotoua.dawnislandk.data.repository

import com.laotoua.dawnislandk.data.local.Community
import com.laotoua.dawnislandk.data.local.dao.CommunityDao
import com.laotoua.dawnislandk.data.remote.NMBServiceClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import timber.log.Timber
import javax.inject.Inject

class CommunityRepository @Inject constructor(
    private val webService: NMBServiceClient,
    private val dao: CommunityDao
) {

    fun getCommunities(remoteDataOnly: Boolean): Flow<DataResource<List<Community>>> = flow {
        var localList: List<Community>? = null
        if (!remoteDataOnly) {
            localList = dao.getAll()
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