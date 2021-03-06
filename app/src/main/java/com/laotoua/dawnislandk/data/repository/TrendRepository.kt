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
import com.laotoua.dawnislandk.data.local.dao.DailyTrendDao
import com.laotoua.dawnislandk.data.local.entity.DailyTrend
import com.laotoua.dawnislandk.data.local.entity.Post
import com.laotoua.dawnislandk.data.local.entity.Trend
import com.laotoua.dawnislandk.data.remote.APIDataResponse
import com.laotoua.dawnislandk.data.remote.NMBServiceClient
import com.laotoua.dawnislandk.util.DataResource
import com.laotoua.dawnislandk.util.LoadingStatus
import com.laotoua.dawnislandk.util.ReadableTime
import com.laotoua.dawnislandk.util.getLocalListDataResource
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

    val latestTrends = MediatorLiveData<DataResource<List<DailyTrend>>>()
    private val cache: LiveData<DataResource<List<DailyTrend>>> = getLocalListDataResource(dailyTrendDao.findDistinctLatestDailyTrends())
    private lateinit var remote: LiveData<DataResource<List<DailyTrend>>>
    private var trendsCount = 0


    var maxPage = 1
        private set

    init {
        subscribeToCache()
    }


    private fun subscribeToCache() {
        latestTrends.addSource(cache) {
            if (it.status == LoadingStatus.SUCCESS && trendsCount > 0 && trendsCount < 7){
                latestTrends.value = DataResource.create(data = it.data)
            } else if (it.status == LoadingStatus.SUCCESS) {
                latestTrends.value = it
            }
            maxPage = it.data?.first()?.page ?: 1
            if (!this::remote.isInitialized) subscribeToRemote()
        }
    }


    fun subscribeToRemote() {
        if (this::remote.isInitialized) latestTrends.removeSource(remote)
        remote = liveData<DataResource<List<DailyTrend>>> {
            val lastDate: LocalDateTime? = cache.value?.data?.firstOrNull()?.date
            if (lastDate != null && ReadableTime.serverDateTimeToUserLocalDateTime(lastDate).plusDays(1).isAfter(LocalDateTime.now())) {
                Timber.d("It's less than 24 hours since Trend last updated. Reusing...")
                emit(DataResource.create(LoadingStatus.SUCCESS, cache.value?.data))
            } else {
                trendsCount = 0
                emit(DataResource.create())
                val result = getRemoteTrend(maxPage)
                emit(result)
            }
        }
        latestTrends.addSource(remote) {
            latestTrends.value = DataResource.create(it.status, cache.value?.data, it.message)
        }
    }

    // Remote only acts as a request status responder, actual data will be emitted by local cache
    private suspend fun getRemoteTrend(page: Int): DataResource<List<DailyTrend>> {
        Timber.d("Getting remote trends on page $page")
        return webService.getComments(trendId, page).run {
            if (this is APIDataResponse.Success) {
                val newMaxPage = ceil(data!!.replyCount.toDouble() / 19).toInt()
                if (maxPage < newMaxPage) {
                    maxPage = newMaxPage
                    extractLatestTrends(data)
                    getRemoteTrend(newMaxPage)
                } else {
                    trendsCount += extractLatestTrends(data)
                    if (trendsCount < 7) {
                        Timber.d("Only got $trendsCount/7 Daily Trend from last Page. Need more...")
                        getRemoteTrend(page - 1)
                    }
                    else DataResource.create(LoadingStatus.SUCCESS, emptyList())
                }
            } else {
                Timber.e(message)
                DataResource.create(LoadingStatus.ERROR, null, "无法读取A岛热榜...\n$message")
            }
        }
    }

    suspend fun extractLatestTrends(data: Post): Int {
        val foundTrends = mutableListOf<DailyTrend>()
        for (reply in data.comments) {
            if (reply.userid == po) {
                val content = if (reply.content.startsWith("@")) reply.content.substringAfter("<br />\n") else reply.content
                val list = content.split(trendDelimiter, ignoreCase = true)
                if (list.size == trendLength) {
                    try {
                        val newDailyTrend = DailyTrend(
                            reply.id,
                            reply.userid,
                            ReadableTime.serverTimeStringToServerLocalDateTime(reply.now),
                            list.map { convertStringToTrend(it) },
                            data.replyCount.toInt()
                        )
                        foundTrends.add(newDailyTrend)
                    } catch (e: Exception) {
                        Timber.e(e)
                    }
                }
            }
        }
        if (foundTrends.isNotEmpty()) {
            Timber.d("Found ${foundTrends.size} Daily Trend. Saving...")
            coroutineScope { launch { dailyTrendDao.insertAll(foundTrends) } }
        }
        return foundTrends.size
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