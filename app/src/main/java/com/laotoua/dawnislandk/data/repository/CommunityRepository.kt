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
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.liveData
import com.laotoua.dawnislandk.DawnApp
import com.laotoua.dawnislandk.data.local.dao.CommunityDao
import com.laotoua.dawnislandk.data.local.dao.TimelineDao
import com.laotoua.dawnislandk.data.local.entity.Community
import com.laotoua.dawnislandk.data.local.entity.Timeline
import com.laotoua.dawnislandk.data.remote.APIDataResponse
import com.laotoua.dawnislandk.data.remote.NMBServiceClient
import com.laotoua.dawnislandk.util.*
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CommunityRepository @Inject constructor(
    private val webService: NMBServiceClient,
    private val communityDao: CommunityDao,
    private val timelineDao: TimelineDao
) {

    private var currentDomainCommunities: LiveData<DataResource<List<Community>>>? = null
    private var currentDomainTimelines: LiveData<DataResource<List<Timeline>>>? = null

    val communityList  = MediatorLiveData<DataResource<List<Community>>>()
    val timelineList  = MediatorLiveData<DataResource<List<Timeline>>>()

    init {
        refreshCommunitiesAndTimelines()
    }

    fun refreshCommunitiesAndTimelines() {
        Timber.d("Refreshing Communities and Timelines for ${DawnApp.currentDomain}...")
        if (currentDomainCommunities != null) communityList.removeSource(currentDomainCommunities!!)
        currentDomainCommunities = getLiveData(communityDao::getAll, webService::getCommunities)
        communityList.addSource(currentDomainCommunities!!) {
            communityList.value = it
        }
        if (DawnApp.currentDomain == DawnConstants.NMBXDDomain) {
            if (currentDomainTimelines != null) timelineList.removeSource(currentDomainTimelines!!)
            currentDomainTimelines = getLiveData(timelineDao::getAll, webService::getTimeLines)
            timelineList.addSource(currentDomainTimelines!!) {
                timelineList.value = it
            }
        }
    }

    private inline fun <reified T> getLiveData(
        noinline localFetcher: () -> LiveData<List<T>>,
        noinline remoteFetcher: suspend () -> APIDataResponse<List<T>>
    ): LiveData<DataResource<List<T>>> {
        Timber.d("Getting Live ${T::class.simpleName}")
        val cache = getLocalDataSource(localFetcher)
        val remote = getServerDataSource(remoteFetcher)
        return getLocalLiveDataAndRemoteResponse(cache, remote)
    }

    private inline fun <reified T> getLocalDataSource(localFetcher: () -> LiveData<List<T>>): LiveData<DataResource<List<T>>> {
        Timber.d("Querying local ${T::class.simpleName}")
        return getLocalListDataResource(localFetcher())
    }

    private inline fun <reified T> getServerDataSource(noinline remoteFetcher: suspend () -> APIDataResponse<List<T>>): LiveData<DataResource<List<T>>> {
        return liveData {
            Timber.d("Querying remote ${T::class.simpleName}")
            val response = DataResource.create(remoteFetcher())
            if (response.status == LoadingStatus.ERROR) {
                response.message = "无法读取板块列表...\n${response.message}"
            }
            if (response.status == LoadingStatus.SUCCESS) {
                emit(response)
                updateCache(response.data!!, false)
            }
        }
    }

    private suspend inline fun <reified T> updateCache(remote: List<T>, remoteDataOnly: Boolean) {
        val comparator = when (T::class) {
            Community::class -> communityList.value?.data
            Timeline::class -> timelineList.value?.data
            else -> throw Exception("Type not recognized")
        }
        if (remote.isNotEmpty() && (remoteDataOnly || remote != comparator)) {
            Timber.d("Remote ${T::class} differs from local or forced refresh. Updating...")
            @Suppress("UNCHECKED_CAST")
            when (T::class) {
                Community::class -> communityDao.insertAll(remote as List<Community>)
                Timeline::class -> timelineDao.insertAll(remote as List<Timeline>)
                else -> throw Exception("Type not recognized")
            }

        }
    }

    suspend fun saveCommonCommunity(community: Community) {
        communityDao.insert(community)
    }

}