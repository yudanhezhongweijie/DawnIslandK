package com.laotoua.dawnislandk.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laotoua.dawnislandk.data.entity.Thread
import com.laotoua.dawnislandk.data.entity.Trend
import com.laotoua.dawnislandk.data.network.NMBServiceClient
import com.laotoua.dawnislandk.ui.util.extractQuote
import kotlinx.coroutines.launch
import timber.log.Timber

class TrendViewModel : ViewModel() {

    private val trendId = "15347469"
    private val po = "m9R9kaD"
    private val trendDelimiter = "\n\u2014\u2014\u2014\u2014\u2014<br />\n<br />\n"

    private var _trendList = MutableLiveData<List<Trend>>()
    val trendList: LiveData<List<Trend>> get() = _trendList

    private val trendLength = 32
    var page = 1

    private var _loadingStatus = MutableLiveData<SingleLiveEvent<EventPayload<Nothing>>>()
    val loadingStatus: LiveData<SingleLiveEvent<EventPayload<Nothing>>>
        get() = _loadingStatus

    fun getLatestTrend() {
        viewModelScope.launch {
            getLatestTrendPage()
        }
    }

    private suspend fun getLatestTrendPage() {
        DataResource.create(NMBServiceClient.getReplys(trendId, page)).run {
            when (this) {
                is DataResource.Success -> {
                    val count = data!!.replyCount?.toInt() ?: 0
                    if (page == 1) {
                        page = kotlin.math.ceil(count.toDouble() / 19).toInt()
                        getLatestTrendPage()
                    } else {
                        val list = extractLatestTrend(data)
                        if (list.size == trendLength) {
                            convertTrendData(list)
                        } else {
                            page -= 1
                            getLatestTrendPage()
                        }
                    }
                }
                is DataResource.Error -> {
                    Timber.e(message)
                    _loadingStatus.postValue(
                        SingleLiveEvent.create(
                            LoadingStatus.FAILED, "无法读取A岛热榜...\n$message"
                        )
                    )
                }
            }
        }
    }

    private fun extractLatestTrend(data: Thread): List<String> {
        for (reply in data.replys!!.reversed()) {
            if (reply.userid == po) {
                val list = reply.content.split(trendDelimiter, ignoreCase = true)
                if (list.size == 32) {
                    return list
                }
            }
        }
        return emptyList()
    }


    private fun convertTrendData(trends: List<String>) {
        _trendList.postValue(trends.map { convertStringToTrend(it) })
    }

    /**
     * sample
     *    01. Trend 367 [欢乐恶搞] <br />
     *    <font color="#789922">&gt;&gt;No.26117594</font><br />
     *    <br />
     *    谈一谈我们兄弟几个人的婚事...<br />
     *    <br />
     *    先说老王吧...<br />
     */
    private fun convertStringToTrend(string: String): Trend {
        val rows = string.split("<br />")
        val headers = rows[0].split(" ")
        val rank = headers[0].removeSuffix(".")
        val hits = headers[2]
        val forum = headers[3].removeSurrounding("[", "]")
        val id = extractQuote(rows[1]).first()
        val content = rows.subList(3, rows.lastIndex).joinToString("<br />")
        return Trend(rank, hits, forum, id, content)
    }

}
