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

import android.util.ArrayMap
import androidx.lifecycle.LiveData
import androidx.lifecycle.liveData
import com.laotoua.dawnislandk.data.local.dao.CommentDao
import com.laotoua.dawnislandk.data.local.entity.Comment
import com.laotoua.dawnislandk.data.remote.NMBServiceClient
import com.laotoua.dawnislandk.util.DataResource
import com.laotoua.dawnislandk.util.LoadingStatus
import com.laotoua.dawnislandk.util.getLocalDataResource
import com.laotoua.dawnislandk.util.getLocalLiveDataAndRemoteResponse
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QuoteRepository @Inject constructor(
    private val webService: NMBServiceClient,
    private val commentDao: CommentDao
) {
    // remember last 30 quote
    private val cacheCap = 30
    private val quoteMap = ArrayMap<String, LiveData<DataResource<Comment>>>(cacheCap)
    private val fifoQuoteList = mutableListOf<String>()

    fun getQuote(id: String): LiveData<DataResource<Comment>> {
        if (quoteMap[id] == null) {
            addQuoteToCache(id, getLiveQuote(id))
        }
        return quoteMap[id]!!
    }

    private fun getLiveQuote(id: String): LiveData<DataResource<Comment>> {
        val cache = getLocalData(id)
        val remote = getServerData(id)
        return getLocalLiveDataAndRemoteResponse(cache, remote)
    }

    private fun getLocalData(id: String): LiveData<DataResource<Comment>> {
        return getLocalDataResource(commentDao.findCommentById(id))
    }

    private fun getServerData(id: String): LiveData<DataResource<Comment>> {
        return liveData<DataResource<Comment>> {
            val response = DataResource.create(webService.getQuote(id))
            if (response.status == LoadingStatus.SUCCESS) {
                convertServerData(id, response.data!!)
            } else {
                val message = if (response.status == LoadingStatus.NO_DATA) "无法获取引用" else response.message
                emit(DataResource.create(LoadingStatus.ERROR, null, message))
            }
        }
    }

    private suspend fun convertServerData(id: String, data: Comment) {
        val cache = quoteMap[id]?.value?.data
        if (!data.equalsWithServerData(cache)) {
            // if local already has cache, update some fields
            // otherwise save new reply cache with default value
            cache?.run {
                data.page = cache.page
                data.parentId = cache.parentId
            }
            coroutineScope {
                launch {
                    commentDao.insertWithTimeStamp(data)
                }
            }
        }
    }

    private fun addQuoteToCache(id: String, quote: LiveData<DataResource<Comment>>) {
        quoteMap[id] = quote
        fifoQuoteList.add(id)
        for (i in 0 until fifoQuoteList.size - cacheCap) {
            quoteMap.remove(fifoQuoteList.first())
            fifoQuoteList.removeAt(0)
        }
    }
}