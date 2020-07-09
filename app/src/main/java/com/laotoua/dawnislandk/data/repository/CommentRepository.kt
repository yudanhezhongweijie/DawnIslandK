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
import androidx.lifecycle.liveData
import com.laotoua.dawnislandk.DawnApp
import com.laotoua.dawnislandk.data.local.dao.*
import com.laotoua.dawnislandk.data.local.entity.*
import com.laotoua.dawnislandk.data.remote.APIDataResponse
import com.laotoua.dawnislandk.data.remote.APIMessageResponse
import com.laotoua.dawnislandk.data.remote.NMBServiceClient
import com.laotoua.dawnislandk.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.*
import javax.inject.Inject

class CommentRepository @Inject constructor(
    private val webService: NMBServiceClient,
    private val commentDao: CommentDao,
    private val postDao: PostDao,
    private val readingPageDao: ReadingPageDao,
    private val browsingHistoryDao: BrowsingHistoryDao,
    private val feedDao: FeedDao
) {

    /** remember all pages for last 15 posts, using threadId and page as index
     * using fifoList to pop the first post
     */
    private val cacheCap = 15
    private val postMap = ArrayMap<String, Post>(cacheCap)
    private val commentsMap = ArrayMap<String,SparseArray<LiveData<List<Comment>>>>(cacheCap)
    private val readingPageMap = ArrayMap<String,ReadingPage>(cacheCap)
    private val browsingHistoryMap = ArrayMap<String,BrowsingHistory>(cacheCap)
    private val fifoPostList = mutableListOf<String>()
    private val adMap = ArrayMap<String,SparseArray<Comment>>()

    val loadingStatus = MutableLiveData<SingleLiveEvent<EventPayload<Nothing>>>()
    val addFeedResponse = MutableLiveData<SingleLiveEvent<EventPayload<Nothing>>>()
    private val todayDateLong = ReadableTime.getTodayDateLong()

    fun getPo(id:String) = postMap[id]?.userid ?: ""
    fun getMaxPage(id:String) = postMap[id]?.getMaxPage() ?: 1
    fun getAd(id:String, page: Int): Comment? = adMap[id]?.get(page)

    private fun getTimeElapsedToday(): Long = Date().time - todayDateLong

    suspend fun setPost(id: String, fid: String) {
        clearCachedPages()
        Timber.d("Setting new Thread: $id")
        if (postMap[id] == null) {
            postMap[id] = postDao.findPostByIdSync(id)
            readingPageDao.getReadingPageById(id).let {
                readingPageMap.put(id, it ?: ReadingPage(id, 1))
            }
            browsingHistoryDao.getBrowsingHistoryByTodayAndIdSync(todayDateLong, id)
                .let {
                    it?.browsedTime = getTimeElapsedToday()
                    browsingHistoryMap.put(
                        id,
                        it ?: BrowsingHistory(
                            todayDateLong,
                            getTimeElapsedToday(),
                            id,
                            fid,
                            mutableSetOf()
                        )
                    )
                }
        }
    }

    // get default page
    fun getLandingPage(id:String): Int {
        return if (DawnApp.applicationDataStore.readingProgressStatus) {
            readingPageMap[id]?.page ?: 1
        } else 1
    }

    /**
     * By default, a post is only stored in the post table, but not stored in the comment table.
     * However when requesting references, all references are stored as comment in comment table.
     * Therefore, the first page can have or not have the header post
     */
    fun getHeaderPost(id:String): Comment? = postMap[id]?.toComment()

    suspend fun saveReadingProgress(id:String, progress: Int) {
        readingPageMap[id]!!.page = progress
        readingPageDao.insertReadingPageWithTimeStamp(readingPageMap[id]!!)
    }

    private fun clearCachedPages() {
        for (i in 0 until (commentsMap.size - cacheCap)) {
            fifoPostList.first().run {
                Timber.d("Reached cache Cap. Clearing ${this}...")
                commentsMap.remove(this)
                postMap.remove(this)
                readingPageMap.remove(this)
                browsingHistoryMap.remove(this)
            }
            fifoPostList.removeAt(0)
        }
    }

    /**
     *  w. cookie, responses have 20 reply w. ad, or 19 reply w/o ad
     *  w/o cookie, always have 20 reply w. ad
     *  *** here DB only store nonAd data
     */
    fun checkFullPage(id:String, page: Int): Boolean =
        (commentsMap[id]?.get(page)?.value?.size ?: 0) >= 19

    fun setLoadingStatus(status: LoadingStatus, message: String? = null) =
        loadingStatus.postValue(SingleLiveEvent.create(status, message))

    // also saved page browsing history
    fun getLivePage(id:String, page: Int): LiveData<List<Comment>> {
        if (commentsMap[id] == null) {
            commentsMap[id] = SparseArray()
            fifoPostList.add(id)
        }
        commentsMap[id]!!.let {
            if (it[page] == null) {
                it.append(page, liveData(Dispatchers.IO) {
                    Timber.d("Querying data for Thread $id on $page")
                    setLoadingStatus(LoadingStatus.LOADING)
                    emitSource(getLocalData(id, page))
                    getServerData(id, page)
                })
            }
            return it[page]
        }
    }

    private fun getLocalData(id: String, page: Int): LiveData<List<Comment>> =
        commentDao.findDistinctPageByParentId(id, page)

    suspend fun getServerData(id:String, page: Int): Job = coroutineScope {
        launch {
            Timber.d("Querying remote data for Thread $id on $page")
            webService.getComments(id, page).run {
                if (this is APIDataResponse.APISuccessDataResponse) convertServerData(id, data, page)
                else {
                        Timber.e(message)
                        commentsMap[id]?.run {
                            if (get(page) != null) {
                                delete(page)
                            }
                        }
                        if (commentsMap[id]?.size() == 0) {
                            commentsMap.remove(id)
                        }
                        setLoadingStatus(LoadingStatus.FAILED, "无法读取串回复...\n$message")
                }
            }
        }
    }

    private suspend fun convertServerData(id:String, data: Post, page: Int) {

        // update postFid for browse history(search jump does not have fid)
        browsingHistoryMap[id]?.run {
            if (postFid != data.fid) {
                postFid = data.fid
            }
            browsedTime = getTimeElapsedToday()
            pages.add(page)
            browsingHistoryDao.insertBrowsingHistory(this)
        }
        // update current thread with latest info
        if (data != postMap[id]) {
            postMap[id] = data.stripCopy()
            savePost(data)
        }
        val noAd = mutableListOf<Comment>()
        data.comments.map {
            it.page = page
            it.parentId = id
            // handle Ad
            if (it.isAd()) {
                if (adMap[id] == null) {
                    adMap.put(id, SparseArray())
                }
                adMap[id]!!.append(page, it)
            } else noAd.add(it)
        }
        /**
         *  On the first or last page, show NO DATA to indicate footer load end;
         *  When entering to a thread with cached data, refresh header is showing,
         *  set LoadingStatus to Success to hide the header
         */
        if (noAd.isEmpty()) {
            setLoadingStatus(LoadingStatus.NODATA)
            return
        }

        if (commentsMap[id]!![page]?.value.equalsWithServerComments(noAd)) {
            if (page == postMap[id]?.getMaxPage()) setLoadingStatus(LoadingStatus.NODATA)
            return
        }
        Timber.d("Updating ${noAd.size} rows for $id on $page")
        saveComments(noAd)
    }

    // DO NOT SAVE ADS
    private suspend fun saveComments(comments: List<Comment>) =
        commentDao.insertAllWithTimeStamp(comments)

    private suspend fun savePost(post: Post) {
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
                        LoadingStatus.FAILED,
                        "订阅失败...是不是已经订阅了呢?"
                    )
                )
            }
        }
    }
}