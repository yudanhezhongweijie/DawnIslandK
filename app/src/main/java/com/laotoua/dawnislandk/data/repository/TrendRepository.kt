package com.laotoua.dawnislandk.data.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.laotoua.dawnislandk.DawnApp
import com.laotoua.dawnislandk.data.local.DailyTrend
import com.laotoua.dawnislandk.data.local.Post
import com.laotoua.dawnislandk.data.local.Trend
import com.laotoua.dawnislandk.data.local.dao.DailyTrendDao
import com.laotoua.dawnislandk.data.remote.APIDataResponse
import com.laotoua.dawnislandk.data.remote.NMBServiceClient
import com.laotoua.dawnislandk.util.EventPayload
import com.laotoua.dawnislandk.util.LoadingStatus
import com.laotoua.dawnislandk.util.ReadableTime
import com.laotoua.dawnislandk.util.ReadableTime.DATE_FORMAT_WITH_YEAR
import com.laotoua.dawnislandk.util.SingleLiveEvent
import timber.log.Timber
import javax.inject.Inject
import kotlin.math.ceil


class TrendRepository @Inject constructor(
    private val webService: NMBServiceClient,
    private val dailyTrendDao: DailyTrendDao
) {
    private val trendId = "15347469"
    private val po = "m9R9kaD"
    private val trendDelimiter = "\n\u2014\u2014\u2014\u2014\u2014<br />\n<br />\n"

    private val trendLength = 32
    private var page = 1

    private var _loadingStatus = MutableLiveData<SingleLiveEvent<EventPayload<Nothing>>>()
    val loadingStatus: LiveData<SingleLiveEvent<EventPayload<Nothing>>>
        get() = _loadingStatus

    val dailyTrend = MutableLiveData<DailyTrend>()

    suspend fun getLatestTrend() {
        dailyTrendDao.findLatestDailyTrendSync()?.let {
            dailyTrend.value = it
            page = ceil(it.lastReplyCount.toDouble() / 19).toInt()
        }
        // TODO: update only if today is different from cache's data or refresh
        getRemoteTrend()
    }

    private suspend fun getRemoteTrend() {
        webService.getComments(DawnApp.applicationDataStore.firstCookieHash, trendId, page).run {
            if (this is APIDataResponse.APISuccessDataResponse) {
                if (page == 1) {
                    page = ceil(data.replyCount.toDouble() / 19).toInt()
                    getRemoteTrend()
                } else {
                    convertLatestTrend(data)
                }
            } else {
                Timber.e(message)
                _loadingStatus.postValue(
                    SingleLiveEvent.create(
                        LoadingStatus.FAILED,
                        "无法读取A岛热榜...\n$message"
                    )
                )
            }
        }
    }

    private fun extractLatestTrend(data: Post): DailyTrend? {
        for (reply in data.comments.reversed()) {
            if (reply.userid == po) {
                val list = reply.content.split(trendDelimiter, ignoreCase = true)
                if (list.size == trendLength) {
                    // keep only date in DB, e.g 2018-10-18 for "2018-10-18(四)17:55:01"
                    val dateString = data.now.substringBefore("(")
                    return DailyTrend(
                        trendId,
                        po,
                        ReadableTime.string2Time(dateString, DATE_FORMAT_WITH_YEAR),
                        list.map { convertStringToTrend(it) },
                        data.replyCount.toInt()
                    )
                }
            }
        }
        return null
    }

    private suspend fun convertLatestTrend(data: Post) {
        val newDailyTrend: DailyTrend? = extractLatestTrend(data)
        if (newDailyTrend != null) {
            if (newDailyTrend.trends != dailyTrend.value?.trends) {
                dailyTrend.postValue(newDailyTrend)
                dailyTrendDao.insertWithTimeStamp(newDailyTrend)
            }
            _loadingStatus.postValue(SingleLiveEvent.create(LoadingStatus.SUCCESS))
            return
        } else {
            page -= 1
            if (page > 1) getLatestTrend()
            else throw Exception("CANNOT GET LATEST TREND FROM ALL PAGES")
        }
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
        val id = extractQuote(rows[1])
        val content = rows.subList(3, rows.lastIndex).joinToString("<br />")
        return Trend(rank, hits, forum, id, content)
    }

    private fun extractQuote(content: String): String {
        /** api response
        <font color=\"#789922\">&gt;&gt;No.23527403</font>
         */
        val regex = """&gt;&gt;No.\d+""".toRegex()
        return regex.find(content)!!.value.substring(11)

    }
}