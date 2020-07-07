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
    private var _currentPostId: String = "0"
    val currentPostId get() = _currentPostId
    private var _currentPostFid: String = "-1"
    val currentPostFid get() = _currentPostFid
    private val currentPostIdInt get() = currentPostId.toInt()

    /** remember all pages for last 15 thread, using threadId and page as index
     * using fifoList to pop the first thread
     */
    private val cacheCap = 15
    private val postMap = SparseArray<Post>(cacheCap)
    private val commentsMap = SparseArray<SparseArray<LiveData<List<Comment>>>>(cacheCap)
    private val readingPageMap = SparseArray<ReadingPage>(cacheCap)
    private val browsingHistoryMap = SparseArray<BrowsingHistory>(cacheCap)
    private val fifoPostList = mutableListOf<Int>()
    private val adMap = SparseArray<SparseArray<Comment>>()

    val po get() = postMap[currentPostIdInt]?.userid ?: ""
    val maxPage get() = postMap[currentPostIdInt]?.getMaxPage() ?: 1
    val loadingStatus = MutableLiveData<SingleLiveEvent<EventPayload<Nothing>>>()
    val addFeedResponse = MutableLiveData<SingleLiveEvent<EventPayload<Nothing>>>()
    private val todayDateLong = ReadableTime.getTodayDateLong()

    private var pageDownloadJob: Job? = null
    val emptyPage = MutableLiveData<Boolean>()

    fun getAd(page: Int): Comment? = adMap[currentPostIdInt]?.get(page)

    private fun getTimeElapsedToday():Long = Date().time - todayDateLong

    suspend fun setPost(id: String, fid: String) {
        if (id == currentPostId) return
        setLoadingStatus(LoadingStatus.LOADING)
        clearCachedPages()
        Timber.d("Setting new Thread: $id")
        _currentPostId = id
        _currentPostFid = fid
        if (postMap[currentPostIdInt] == null) {
            postDao.findPostByIdSync(id)?.let {
                postMap.put(currentPostIdInt, it)
                _currentPostFid = it.fid
            }
            readingPageDao.getReadingPageById(id).let {
                readingPageMap.put(currentPostIdInt, it ?: ReadingPage(currentPostId, 1))
            }
            browsingHistoryDao.getBrowsingHistoryByTodayAndIdSync(todayDateLong, currentPostId)
                .let {
                    it?.browsedTime = getTimeElapsedToday()
                    browsingHistoryMap.put(
                        currentPostIdInt,
                        it ?: BrowsingHistory(
                            todayDateLong,
                            getTimeElapsedToday(),
                            currentPostId,
                            currentPostFid,
                            mutableSetOf()
                        )
                    )
                }
        }
        // catch for jumps without fid or without server updates
        if (currentPostFid.isBlank()) {
            postMap[currentPostIdInt]?.fid?.let {
                _currentPostFid = it
            }
        }
    }

    // set default page
    fun getLandingPage(): Int {
        return if (DawnApp.applicationDataStore.readingProgressStatus) {
            readingPageMap[currentPostIdInt]?.page ?: 1
        } else 1
    }

    fun getHeaderPost(): Comment = postMap[currentPostIdInt]!!.toComment()

    suspend fun saveReadingProgress(progress: Int) {
        readingPageMap[currentPostIdInt].page = progress
        readingPageDao.insertReadingPageWithTimeStamp(readingPageMap[currentPostIdInt])
    }

    private fun clearCachedPages() {
        emptyPage.value = false
        for (i in 0 until (commentsMap.size() - cacheCap)) {
            fifoPostList.first().run {
                Timber.d("Reached cache Cap. Clearing ${this}...")
                commentsMap.delete(this)
                postMap.delete(this)
                readingPageMap.delete(this)
                browsingHistoryMap.delete(this)
            }
            fifoPostList.removeAt(0)
        }
        pageDownloadJob?.cancel()
        pageDownloadJob = null
    }

    /**
     *  w. cookie, responses have 20 reply w. ad, or 19 reply w/o ad
     *  w/o cookie, always have 20 reply w. ad
     *  *** here DB only store nonAd data
     */
    fun checkFullPage(page: Int): Boolean =
        (commentsMap[currentPostIdInt]?.get(page)?.value?.size == 19)

    fun setLoadingStatus(status: LoadingStatus, message: String? = null) =
        loadingStatus.postValue(SingleLiveEvent.create(status, message))

    // also saved page browsing history
    fun getLivePage(page: Int): LiveData<List<Comment>> {
        if (commentsMap[currentPostIdInt] == null) {
            commentsMap.append(currentPostIdInt, SparseArray())
            fifoPostList.add(currentPostIdInt)
        }
        commentsMap[currentPostIdInt]!!.let {
            if (it[page] == null) {
                it.append(page, liveData(Dispatchers.IO) {
                    Timber.d("Querying data for Thread $currentPostId on $page")
                    setLoadingStatus(LoadingStatus.LOADING)
                    emitSource(getLocalData(page))
                    pageDownloadJob = getServerData(page)
                })
            }
            return it[page]
        }
    }

    private fun getLocalData(page: Int): LiveData<List<Comment>> =
        commentDao.findDistinctPageByParentId(currentPostId, page)

    suspend fun getServerData(page: Int): Job = coroutineScope {
        launch {
            Timber.d("Querying remote data for Thread $currentPostId on $page")
            webService.getComments(currentPostId, page).run {
                if (this is APIDataResponse.APISuccessDataResponse) convertServerData(data, page)
                else {
                    if (pageDownloadJob?.isCancelled != true) {
                        Timber.e(message)
                        commentsMap[currentPostIdInt]?.run {
                            if (get(page) != null) {
                                delete(page)
                            }
                        }
                        if (commentsMap[currentPostIdInt]?.size() == 0) {
                            commentsMap.delete(currentPostIdInt)
                        }
                        setLoadingStatus(LoadingStatus.FAILED, "无法读取串回复...\n$message")
                    }
                }
            }
        }
    }

    private suspend fun convertServerData(data: Post, page: Int) {
        // update current thread with latest info
        _currentPostFid = data.fid
        // update postFid for browse history(search jump does not have fid)
        browsingHistoryMap[currentPostIdInt].run {
            if (postFid != currentPostFid) {
                postFid = currentPostFid
            }
            browsedTime = getTimeElapsedToday()
            pages.add(page)
            browsingHistoryDao.insertBrowsingHistory(this)
        }

        if (data != postMap[currentPostIdInt]) {
            savePost(data)
        }
        val noAd = mutableListOf<Comment>()
        data.comments.map {
            it.page = page
            it.parentId = currentPostId
            // handle Ad
            if (it.isAd()) {
                if (adMap[currentPostIdInt] == null) {
                    adMap.append(currentPostIdInt, SparseArray())
                }
                adMap[currentPostIdInt]!!.append(page, it)
            } else noAd.add(it)
        }
        /**
         *  On the first or last page, show NO DATA to indicate footer load end;
         *  When entering to a thread with cached data, refresh header is showing,
         *  set LoadingStatus to Success to hide the header
         */
        if (noAd.isEmpty()) {
            emptyPage.postValue(true)
            setLoadingStatus(LoadingStatus.NODATA)
            return
        }

        if (commentsMap[currentPostIdInt]!![page]?.value.equalsWithServerComments(noAd)) {
            if (page == maxPage) setLoadingStatus(LoadingStatus.NODATA)
            return
        }
        Timber.d("Updating ${noAd.size} rows for $currentPostId on $page")
        saveComments(noAd)
    }

    // DO NOT SAVE ADS
    private suspend fun saveComments(comments: List<Comment>) =
        commentDao.insertAllWithTimeStamp(comments)

    private suspend fun savePost(post: Post) {
        postMap.put(currentPostIdInt, post.stripCopy())
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