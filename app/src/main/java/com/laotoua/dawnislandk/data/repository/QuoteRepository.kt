package com.laotoua.dawnislandk.data.repository

import android.util.LongSparseArray
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.liveData
import com.laotoua.dawnislandk.data.local.Reply
import com.laotoua.dawnislandk.data.local.dao.ReplyDao
import com.laotoua.dawnislandk.data.local.dao.ThreadDao
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
    private val replyDao: ReplyDao,
    private val threadDao: ThreadDao
) {
    // remember last 30 quote
    private val cacheCap = 30
    private val quoteMap = LongSparseArray<LiveData<Reply>>(cacheCap)
    private val fifoQuoteList = mutableListOf<Long>()
    val quoteLoadingStatus = MutableLiveData<EventPayload<String>>()

    fun getQuote(id: String): LiveData<Reply> {
        val idLong = id.toLong()
        if (quoteMap[idLong] == null) {
            addQuoteToCache(idLong, getLiveQuote(id, idLong))
        }
        return quoteMap[idLong]
    }

    private fun getLiveQuote(id: String, idLong: Long) = liveData<Reply>(Dispatchers.IO) {
        val cache = replyDao.findDistinctReplyById(id)
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
                            replyDao.insertWithTimeStamp(data)
                        }
                    }

                } else {
                    quoteLoadingStatus.postValue(EventPayload(LoadingStatus.FAILED, message, id))
                }
            }
        }
    }

    private fun addQuoteToCache(idLong: Long, quote: LiveData<Reply>) {
        quoteMap.append(idLong, quote)
        fifoQuoteList.add(idLong)
        for (i in 0 until fifoQuoteList.size - cacheCap) {
            quoteMap.delete(fifoQuoteList.first())
            fifoQuoteList.removeAt(0)
        }
    }
}