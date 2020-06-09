package com.laotoua.dawnislandk.data.repository

import android.util.SparseArray
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
import com.laotoua.dawnislandk.util.SingleLiveEvent
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
    private val quoteMap = SparseArray<LiveData<Reply>>(cacheCap)
    private val fifoQuoteList = mutableListOf<Int>()
    val quoteLoadingStatus = MutableLiveData<SingleLiveEvent<EventPayload<Nothing>>>()

    fun getQuote(id: String): LiveData<Reply> {
        val idInt = id.toInt()
        if (quoteMap[idInt] == null) {
            addQuoteToCache(idInt, getLiveQuote(id))
        }
        return quoteMap[idInt]
    }

    private fun getLiveQuote(id: String) = liveData<Reply>(Dispatchers.IO) {
        val cache = replyDao.findDistinctReplyById(id)
        addQuoteToCache(id.toInt(), cache)
        emitSource(cache)
        getServerData(id)
    }

    private suspend fun getServerData(id: String) =
        coroutineScope {
            launch {
                webService.getQuote(id).run {
                    when (this) {
                        is APIDataResponse.APISuccessDataResponse -> {
                            if (!data.equalsExceptTimestamp(quoteMap[id.toInt()].value)) {
                                replyDao.insertWithTimeStamp(data)
                            }
                            quoteLoadingStatus.postValue(SingleLiveEvent.create(LoadingStatus.SUCCESS))
                        }
                        else -> {
                            quoteLoadingStatus.postValue(SingleLiveEvent.create(LoadingStatus.FAILED, message))
                        }
                    }
                }
            }

    }

    private fun addQuoteToCache(idInt:Int, quote : LiveData<Reply>){
        quoteMap.append(idInt, quote)
        fifoQuoteList.add(idInt)
        for ( i in 0 until fifoQuoteList.size - cacheCap){
            quoteMap.delete(fifoQuoteList.first())
            fifoQuoteList.removeAt(0)
        }
    }
}