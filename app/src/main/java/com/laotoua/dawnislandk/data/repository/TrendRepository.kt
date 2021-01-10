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
import java.time.LocalDateTime
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

    private var _maxPage = 1
    val maxPage: Int get() = _maxPage

    // Remote only acts as a request status responder, actual data will be emitted by local cache
    fun getLatestTrend(): LiveData<DataResource<DailyTrend>> = liveData {
        var getRemoteData = true
        if (cache == null) cache = dailyTrendDao.findLatestDailyTrendSync()
        cache?.let {
            emit(DataResource.create(LoadingStatus.SUCCESS, it))
            _maxPage = ceil(it.lastReplyCount.toDouble() / 19).toInt()
            // trends updates daily at 1AM, allows 24 hour CD
            if (ReadableTime.serverDateTimeToUserLocalDateTime(it.date).plusDays(1).isBefore(LocalDateTime.now())) {
                getRemoteData = false
                Timber.d("It's less than 24 hours since Trend last updated. Reusing...")
            }
        }
        if (getRemoteData) {
            emit(DataResource.create())
            getRemoteTrend(_maxPage)?.let { emit(it) }
        }
    }

    private suspend fun getRemoteTrend(page: Int): DataResource<DailyTrend>? {
        Timber.d("Getting remote trend on page $page")
        return webService.getComments(trendId, page).run {
            if (this is APIDataResponse.Success) {
                val targetPage = ceil(data!!.replyCount.toDouble() / 19).toInt()
                if (page == 1 || targetPage != page) {
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

    private suspend fun convertLatestTrend(targetPage: Int, data: Post): DataResource<DailyTrend>? {
        var newDailyTrend: DailyTrend? = null
        for (reply in data.comments.reversed()) {
            if (reply.userid == po) {
                val content = if (reply.content.startsWith("@")) reply.content.substringAfter("<br />\n")
                else reply.content
                val list = content.split(trendDelimiter, ignoreCase = true)
                if (list.size == trendLength) {
                    try {
                        newDailyTrend = DailyTrend(
                            reply.id,
                            reply.userid,
                            ReadableTime.serverTimeStringToServerLocalDateTime(reply.now),
                            list.map { convertStringToTrend(it) },
                            data.replyCount.toInt()
                        )
                        break
                    } catch (e: Exception) {
                        Timber.e(e)
                        return DataResource.create(LoadingStatus.ERROR, null, "无法读取热榜，请尝试更新版本或者联系开发者")
                    }
                }
            }
        }

        return when {
            newDailyTrend != null -> {
                if (cache == null || !newDailyTrend.date.toLocalDate().isEqual(cache!!.date.toLocalDate())) {
                    Timber.d("Found new trends. Saving...")
                    cache = newDailyTrend
                    coroutineScope { launch { dailyTrendDao.insert(newDailyTrend) } }
                }
                DataResource.create(LoadingStatus.SUCCESS, newDailyTrend)
            }
            targetPage - 1 > 1 -> {
                getRemoteTrend(targetPage - 1)
            }
            else -> {
                DataResource.create(LoadingStatus.ERROR, null, "无法读取热榜，请尝试更新版本或者联系开发者")
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