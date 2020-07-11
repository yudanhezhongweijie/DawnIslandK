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
import androidx.lifecycle.Transformations
import androidx.lifecycle.liveData
import com.laotoua.dawnislandk.data.local.dao.CommentDao
import com.laotoua.dawnislandk.data.local.entity.Comment
import com.laotoua.dawnislandk.data.remote.NMBServiceClient
import com.laotoua.dawnislandk.util.DataResource
import com.laotoua.dawnislandk.util.LoadingStatus
import com.laotoua.dawnislandk.util.getCombinedLiveData
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

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
        return getCombinedLiveData(cache, remote)
    }

    private fun getLocalData(id: String): LiveData<DataResource<Comment>> {
        return Transformations.map(commentDao.findCommentById(id)) {
            val status: LoadingStatus =
                if (it == null) LoadingStatus.NO_DATA else LoadingStatus.SUCCESS
            DataResource.create(status, it)
        }
    }

    private fun getServerData(id: String): LiveData<DataResource<Comment>> {
        return liveData<DataResource<Comment>> {
            val response = DataResource.create(webService.getQuote(id))
            if (response.status == LoadingStatus.SUCCESS) {
                emit(convertServerData(id, response.data!!))
            } else {
                emit(DataResource.create(response.status, null, response.message!!))
            }
        }
    }

    private suspend fun convertServerData(id: String, data: Comment): DataResource<Comment> {
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
        return DataResource.create(LoadingStatus.SUCCESS, data)
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