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
import com.laotoua.dawnislandk.data.local.dao.DailyTrendDao
import com.laotoua.dawnislandk.data.local.entity.DailyTrend
import com.laotoua.dawnislandk.data.local.entity.Post
import com.laotoua.dawnislandk.data.local.entity.Trend
import com.laotoua.dawnislandk.data.remote.APIDataResponse
import com.laotoua.dawnislandk.data.remote.NMBServiceClient
import com.laotoua.dawnislandk.util.DataResource
import com.laotoua.dawnislandk.util.LoadingStatus
import com.laotoua.dawnislandk.util.ReadableTime
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.ceil


@Singleton
class TrendRepository @Inject constructor(
    private val webService: NMBServiceClient,
    private val dailyTrendDao: DailyTrendDao
) {
    private val trendId = "15347469"
    private val po = "m9R9kaD"
    private val trendDelimiter = "\n\u2014\u2014\u2014\u2014\u2014<br />\n<br />\n"

    private val trendLength = 32
    private var cache: DailyTrend? = null

    // Remote only acts as a request status responder, actual data will be emitted by local cache
    fun getLatestTrend(): LiveData<DataResource<DailyTrend>> = liveData {
        var getRemoteData = true
        if (cache == null) cache = dailyTrendDao.findLatestDailyTrendSync()
        var page = 1
        cache?.let {
            emit(DataResource.create(LoadingStatus.SUCCESS, it))
            page = ceil(it.lastReplyCount.toDouble() / 19).toInt()
            // trends updates daily at 1AM
            val diff = ReadableTime.getTimeAgo(System.currentTimeMillis(), it.date, true)
            val dayTime = ReadableTime.HOUR_MILLIS * 24
            if (diff - dayTime < 0) {
                getRemoteData = false
                Timber.d("It's less than 24 hours since Trend last updated. Reusing...")
            }
        }
        if (getRemoteData) {
            emit(DataResource.create())
            getRemoteTrend(page)?.let { emit(it) }
        }
    }

    private suspend fun getRemoteTrend(page: Int): DataResource<DailyTrend>? {
        Timber.d("Getting remote trend on page $page")
        return webService.getComments(trendId, page).run {
            if (this is APIDataResponse.Success) {
                val targetPage = ceil(data!!.replyCount.toDouble() / 19).toInt()
                if (page == 1) {
                    getRemoteTrend(targetPage)
                } else {
                    convertLatestTrend(targetPage, data)
                }
            } else {
                Timber.e(message)
                DataResource.create(LoadingStatus.ERROR, null, "无法读取A岛热榜...\n$message")
            }
        }
    }

    private fun extractLatestTrend(data: Post): DailyTrend? {
        for (reply in data.comments.reversed()) {
            if (reply.userid == po) {
                val list = reply.content.split(trendDelimiter, ignoreCase = true)
                if (list.size == trendLength) {
                    return DailyTrend(
                        trendId,
                        po,
                        ReadableTime.string2Time(reply.now),
                        list.map { convertStringToTrend(it) },
                        data.replyCount.toInt()
                    )
                }
            }
        }
        return null
    }

    private suspend fun convertLatestTrend(targetPage: Int, data: Post): DataResource<DailyTrend>? {
        val newDailyTrend: DailyTrend? = extractLatestTrend(data)
        return when {
            newDailyTrend != null -> {
                if (newDailyTrend.date != cache?.date) {
                    Timber.d("Found new trends. Saving...")
                    cache = newDailyTrend
                    coroutineScope { launch { dailyTrendDao.insertWithTimeStamp(newDailyTrend) } }
                }
                DataResource.create(LoadingStatus.SUCCESS, newDailyTrend)
            }
            targetPage - 1 > 1 -> {
                getRemoteTrend(targetPage - 1)
            }
            else -> {
                DataResource.create(
                    LoadingStatus.ERROR,
                    null,
                    "CANNOT GET LATEST TREND FROM ALL PAGES"
                )
            }
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