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

import android.util.LongSparseArray
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.liveData
import com.laotoua.dawnislandk.data.local.dao.CommentDao
import com.laotoua.dawnislandk.data.local.dao.PostDao
import com.laotoua.dawnislandk.data.local.entity.Comment
import com.laotoua.dawnislandk.data.remote.APIDataResponse
import com.laotoua.dawnislandk.data.remote.NMBServiceClient
import com.laotoua.dawnislandk.util.EventPayload
import com.laotoua.dawnislandk.util.LoadingStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

class QuoteRepository @Inject constructor(
    private val webService: NMBServiceClient,
    private val commentDao: CommentDao,
    private val postDao: PostDao
) {
    // remember last 30 quote
    private val cacheCap = 30
    private val quoteMap = LongSparseArray<LiveData<Comment>>(cacheCap)
    private val fifoQuoteList = mutableListOf<Long>()
    val quoteLoadingStatus = MutableLiveData<EventPayload<String>>()

    fun getQuote(id: String): LiveData<Comment> {
        val idLong = id.toLong()
        if (quoteMap[idLong] == null) {
            addQuoteToCache(idLong, getLiveQuote(id, idLong))
        }
        return quoteMap[idLong]
    }

    private fun getLiveQuote(id: String, idLong: Long) = liveData<Comment>(Dispatchers.IO) {
        val cache = commentDao.findDistinctCommentById(id)
        addQuoteToCache(idLong, cache)
        emitSource(cache)
        getServerData(id, idLong)
    }

    private suspend fun getServerData(id: String, idLong: Long) = coroutineScope {
        launch {
            webService.getQuote(id).run {
                if (this is APIDataResponse.APISuccessDataResponse) {
                    quoteMap[idLong].value.let {
                        if (!data.equalsWithServerData(it)) {
                            // if local already has cache, update some fields
                            // otherwise save new reply cache with default value
                            it?.let {
                                data.page = it.page
                                data.parentId = it.parentId
                            }
                            commentDao.insertWithTimeStamp(data)
                        }
                    }

                } else {
                    quoteLoadingStatus.postValue(EventPayload(LoadingStatus.FAILED, message, id))
                }
            }
        }
    }

    private fun addQuoteToCache(idLong: Long, quote: LiveData<Comment>) {
        quoteMap.append(idLong, quote)
        fifoQuoteList.add(idLong)
        for (i in 0 until fifoQuoteList.size - cacheCap) {
            quoteMap.delete(fifoQuoteList.first())
            fifoQuoteList.removeAt(0)
        }
    }
}