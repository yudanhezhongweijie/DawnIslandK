/*
 *  Copyright 2020 Fishballzzz
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package com.laotoua.dawnislandk.data.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.liveData
import com.laotoua.dawnislandk.data.local.dao.CommunityDao
import com.laotoua.dawnislandk.data.local.entity.Community
import com.laotoua.dawnislandk.data.remote.NMBServiceClient
import com.laotoua.dawnislandk.util.DataResource
import com.laotoua.dawnislandk.util.LoadingStatus
import com.laotoua.dawnislandk.util.getLocalListDataResource
import com.laotoua.dawnislandk.util.getLocalLiveDataAndRemoteResponse
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CommunityRepository @Inject constructor(
    private val webService: NMBServiceClient,
    private val dao: CommunityDao
) {
    val communityList = getLiveCommunities()

    private fun getLiveCommunities(): LiveData<DataResource<List<Community>>> {
        val cache = getLocalData()
        val remote = getServerData()
        return getLocalLiveDataAndRemoteResponse(cache, remote)
    }

    private fun getLocalData(): LiveData<DataResource<List<Community>>> {
        Timber.d("Querying local communities")
        return getLocalListDataResource(dao.getAll())
    }

    private fun getServerData(): LiveData<DataResource<List<Community>>> {
        return liveData {
            Timber.d("Querying remote data communities")
            val response = DataResource.create(webService.getCommunities())
            if (response.status == LoadingStatus.ERROR) {
                response.message = "无法读取板块列表...\n${response.message}"
            }
            emit(response)
            if (response.status == LoadingStatus.SUCCESS) {
                updateCache(response.data!!, false)
            }
        }
    }

    private suspend fun updateCache(remote: List<Community>, remoteDataOnly: Boolean) {
        if (remote.isNotEmpty() && (remoteDataOnly || remote != communityList.value?.data)) {
            Timber.d("Remote data differs from local data or forced refresh. Updating...")
            dao.insertAll(remote)
        }
    }

    suspend fun saveCommonCommunity(community: Community){
        dao.insert(community)
    }

}