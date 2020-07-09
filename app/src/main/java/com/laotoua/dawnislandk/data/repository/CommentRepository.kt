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
import android.util.SparseArray
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.liveData
import com.laotoua.dawnislandk.DawnApp
import com.laotoua.dawnislandk.data.local.dao.*
import com.laotoua.dawnislandk.data.local.entity.*
import com.laotoua.dawnislandk.data.remote.APIMessageResponse
import com.laotoua.dawnislandk.data.remote.NMBServiceClient
import com.laotoua.dawnislandk.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.collections.set

@Singleton
class CommentRepository @Inject constructor(
    private val webService: NMBServiceClient,
    private val commentDao: CommentDao,
    private val postDao: PostDao,
    private val readingPageDao: ReadingPageDao,
    private val browsingHistoryDao: BrowsingHistoryDao,
    private val feedDao: FeedDao
) {

    /** remember all pages for last 15 posts, using threadId and page as index
     *  pop posts by fifo
     */
    private val cacheCap = 15
    private val postMap = ArrayMap<String, Post>(cacheCap)
    private val commentsMap =
        ArrayMap<String, SparseArray<LiveData<DataResource<List<Comment>>>>>(cacheCap)
    private val readingPageMap = ArrayMap<String, ReadingPage>(cacheCap)
    private val browsingHistoryMap = ArrayMap<String, BrowsingHistory>(cacheCap)
    private val fifoPostList = mutableListOf<String>()

    val addFeedResponse = MutableLiveData<SingleLiveEvent<EventPayload<Nothing>>>()
    private val todayDateLong = ReadableTime.getTodayDateLong()

    fun getPo(id: String) = postMap[id]?.userid ?: ""
    fun getMaxPage(id: String) = postMap[id]?.getMaxPage() ?: 1
    fun getFid(id: String) = postMap[id]?.fid ?: ""

    private fun getTimeElapsedToday(): Long = Date().time - todayDateLong

    suspend fun setPost(id: String, fid: String) {
        clearCachedPages()
        Timber.d("Setting new Thread: $id")
        if (postMap[id] == null) {
            postMap[id] = postDao.findPostByIdSync(id)
            readingPageMap[id] = getReadingPageOnId(id)
            browsingHistoryMap[id] = getBrowsingHistoryOnId(id, fid)
            commentsMap[id] = SparseArray()
            fifoPostList.add(id)
        }
    }

    private suspend fun getReadingPageOnId(id: String): ReadingPage {
        return readingPageDao.getReadingPageById(id) ?: ReadingPage(id, 1)
    }

    private suspend fun getBrowsingHistoryOnId(id: String, fid: String): BrowsingHistory {
        return browsingHistoryDao.getBrowsingHistoryByTodayAndIdSync(todayDateLong, id)
            ?: BrowsingHistory(
                todayDateLong,
                getTimeElapsedToday(),
                id,
                fid,
                mutableSetOf()
            )
    }

    // get default page
    fun getLandingPage(id: String): Int {
        return if (DawnApp.applicationDataStore.readingProgressStatus) {
            readingPageMap[id]?.page ?: 1
        } else 1
    }

    /**
     * By default, a post is only stored in the post table, but not stored in the comment table.
     * However when requesting references, all references are stored as comment in comment table.
     * Therefore, the first page can have or not have the header post
     */
    fun getHeaderPost(id: String): Comment? = postMap[id]?.toComment()

    suspend fun saveReadingProgress(id: String, progress: Int) {
        readingPageMap[id]!!.page = progress
        readingPageDao.insertReadingPageWithTimeStamp(readingPageMap[id]!!)
    }

    private fun clearCachedPages() {
        for (i in 0 until (commentsMap.size - cacheCap)) {
            fifoPostList.first().run {
                Timber.d("Reached cache Cap. Clearing ${this}...")
                postMap.remove(this)
                readingPageMap.remove(this)
                browsingHistoryMap.remove(this)
                commentsMap.remove(this)
            }
            fifoPostList.removeAt(0)
        }
    }

    /**
     *  w. cookie, responses have 20 reply w. ad, or 19 reply w/o ad
     *  w/o cookie, always have 20 reply w. ad
     *  *** here DB only store nonAd data
     */
    fun checkFullPage(id: String, page: Int): Boolean =
        (commentsMap[id]?.get(page)?.value?.data?.size ?: 0) >= 19

    fun getCommentsOnPage(
        id: String,
        page: Int,
        remoteDataOnly: Boolean
    ): LiveData<DataResource<List<Comment>>> {
        commentsMap[id]!!.let {
            it.put(page, getLivePage(id, page, remoteDataOnly))
            return it[page]
        }
    }

    private fun getLivePage(
        id: String,
        page: Int,
        remoteDataOnly: Boolean
    ): LiveData<DataResource<List<Comment>>> = liveData(Dispatchers.IO) {
        emit(DataResource.create())
        if (!remoteDataOnly) emitSource(getLocalData(id, page))
        emitSource(getServerData(id, page))
    }

    private fun getLocalData(id: String, page: Int): LiveData<DataResource<List<Comment>>> {
        Timber.d("Querying local data for Thread $id on $page")
        return Transformations.map(commentDao.findDistinctPageByParentId(id, page)) {
            Timber.d("Got ${it.size} rows from database")
            val status: LoadingStatus =
                if (it.isNullOrEmpty()) LoadingStatus.NO_DATA else LoadingStatus.SUCCESS
            DataResource.create(status, it)
        }
    }

    private suspend fun getServerData(
        id: String,
        page: Int
    ): LiveData<DataResource<List<Comment>>> {
        return liveData {
            Timber.d("Querying remote data for Thread $id on $page")
            val response = DataResource.create(webService.getComments(id, page))
            if (response.status == LoadingStatus.SUCCESS) {
                emit(convertServerData(id, response.data!!, page))
            } else {
                emit(DataResource.create(response.status, emptyList(), response.message!!))
            }
        }
    }

    private suspend fun convertServerData(
        id: String,
        data: Post,
        page: Int
    ): DataResource<List<Comment>> {
        // update current thread with latest info
        if (data.fid.isBlank() && postMap[id]?.fid?.isNotBlank() == true) {
            data.fid = postMap[id]?.fid!!
        }
        // update postFid for browse history(search jump does not have fid)
        browsingHistoryMap[id]?.run {
            if (postFid != data.fid) {
                postFid = data.fid
            }
            browsedTime = getTimeElapsedToday()
            pages.add(page)
            browsingHistoryDao.insertBrowsingHistory(this)
        }

        if (data != postMap[id]) {
            postMap[id] = data.stripCopy()
            savePost(id, data)
        }

        data.comments.map {
            it.page = page
            it.parentId = id
        }
        /**
         *  On the first or last page, show NO DATA to indicate footer load end;
         *  When entering to a thread with cached data, refresh header is showing,
         *  set LoadingStatus to Success to hide the header
         */

        saveComments(id, page, data.comments)
        return DataResource.create(LoadingStatus.SUCCESS, data.comments)
    }

    // DO NOT SAVE ADS
    private suspend fun saveComments(id: String, page: Int, serverComments: List<Comment>) =
        coroutineScope {
            launch {
                val cacheNoAd =
                    commentsMap[id]!![page]?.value?.data?.filter { it.isNotAd() } ?: emptyList()
                val serverNoAd = serverComments.filter { it.isNotAd() }
                Timber.d("Got ${serverComments.size} comments and ${serverComments.size - serverNoAd.size} ad from server")
                if (!cacheNoAd.equalsWithServerComments(serverNoAd)) {
                    Timber.d("Updating ${serverNoAd.size} rows for $id on $page")
                    commentDao.insertAllWithTimeStamp(serverNoAd)
                }
            }
        }

    private suspend fun savePost(id: String, post: Post) {
        postMap[id] = post.stripCopy()
        postDao.insertWithTimeStamp(post)
    }

    suspend fun addFeed(uuid: String, id: String) {
        Timber.d("Adding Feed $id")
        val cachedFeed = feedDao.findFeedByPostId(id)
        if (cachedFeed != null) {
            addFeedResponse.postValue(
                SingleLiveEvent.create(
                    LoadingStatus.SUCCESS,
                    "已经订阅过了哦"
                )
            )
            return
        }

        webService.addFeed(uuid, id).run {
            if (this is APIMessageResponse.APISuccessMessageResponse) {
                if (messageType == APIMessageResponse.MessageType.String) {
                    addFeedResponse.postValue(
                        SingleLiveEvent.create(
                            LoadingStatus.SUCCESS,
                            message
                        )
                    )
                    coroutineScope {
                        launch {
                            val newFeed = Feed(1, id, "", Date().time)
                            feedDao.addFeedToTopAndIncrementFeedIds(newFeed)
                        }
                    }
                } else {
                    Timber.e(message)
                }
            } else {
                Timber.e("Response type: ${this.javaClass.simpleName}\n $message")
                addFeedResponse.postValue(
                    SingleLiveEvent.create(
                        LoadingStatus.ERROR,
                        "订阅失败...是不是已经订阅了呢?"
                    )
                )
            }
        }
    }
}