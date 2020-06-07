package com.laotoua.dawnislandk.data.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.liveData
import com.laotoua.dawnislandk.DawnApp
import com.laotoua.dawnislandk.data.local.Reply
import com.laotoua.dawnislandk.data.local.Thread
import com.laotoua.dawnislandk.data.local.dao.ReplyDao
import com.laotoua.dawnislandk.data.local.dao.ThreadDao
import com.laotoua.dawnislandk.data.remote.APIDataResponse
import com.laotoua.dawnislandk.data.remote.APIMessageResponse
import com.laotoua.dawnislandk.data.remote.NMBServiceClient
import com.laotoua.dawnislandk.util.EventPayload
import com.laotoua.dawnislandk.util.LoadingStatus
import com.laotoua.dawnislandk.util.SingleLiveEvent
import com.laotoua.dawnislandk.util.equalsExceptTimestamp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.collections.set

// TODO: Singleton creates problem when we want multiple reply fragment exist at the same time
@Singleton
class ReplyRepository @Inject constructor(
    private val webService: NMBServiceClient,
    private val replyDao: ReplyDao,
    private val threadDao: ThreadDao
) {
    var currentThreadId: String = ""
    var currentThread: LiveData<Thread>? = null
    private val replyMap = sortedMapOf<Int, LiveData<List<Reply>>>()
    private val adMap = sortedMapOf<Int, Reply>()
    private var maxReply = 0
    var po = ""
    val maxPage get() = 1.coerceAtLeast(kotlin.math.ceil(maxReply.toDouble() / 19).toInt())
    val loadingStatus = MutableLiveData<SingleLiveEvent<EventPayload<Nothing>>>()
    val addFeedResponse = MutableLiveData<SingleLiveEvent<EventPayload<Nothing>>>()

    private var downloadJob: Job? = null

    fun getAd(page: Int): Reply? = adMap[page]

    fun setThreadId(id: String) {
        if (id == currentThreadId) return
        Timber.i("Thread has changed... Clearing old data")
        clearCache()
        Timber.i("Setting new Thread: $id")
        currentThreadId = id
        setThread(id)
    }

    private fun setThread(id: String) {
        currentThread = threadDao.findDistinctThreadById(id)
    }

    suspend fun getReadingProgress(id: String): Int =
        if (DawnApp.applicationDataStore.readingProgressStatus) {
            threadDao.findThreadByIdSync(id)?.let {
                po = it.userid
                it.readingProgress
            } ?: 1
        } else 1

    suspend fun saveReadingProgress(progress: Int) {
        if (currentThread?.value == null || !DawnApp.applicationDataStore.readingProgressStatus) return
        val new = currentThread?.value!!.copy()
        new.readingProgress = progress
        threadDao.updateThreadsWithTimeStamp(new)
    }

    private fun clearCache() {
        replyMap.clear()
        adMap.clear()
        downloadJob?.cancel()
        downloadJob = null
    }

    /**
     *  w. cookie, responses have 20 reply w. ad, or 19 reply w/o ad
     *  w/o cookie, always have 20 reply w. ad
     *  *** here DB only store nonAd data
     *  *** FIRST PAGE also has thread head
     */
    fun checkFullPage(page: Int): Boolean =
        (replyMap[page]?.value?.size == (if (page == 1) 20 else 19))

    fun setLoadingStatus(status: LoadingStatus, message: String? = null) {
        loadingStatus.postValue(SingleLiveEvent.create(status, message))
    }

    fun getLiveDataOnPage(page: Int): LiveData<List<Reply>> {
        if (replyMap[page] == null) replyMap[page] = getLivePage(page)
        return replyMap[page]!!
    }

    private fun getLivePage(page: Int) = liveData(Dispatchers.IO) {
        Timber.d("Querying data for Thread $currentThreadId on $page")
        setLoadingStatus(LoadingStatus.LOADING)
        emitSource(getLocalData(page))
        downloadJob = getServerData(page)
    }

    private fun combineFirstPage(
        liveHead: LiveData<Thread>,
        livePage: LiveData<List<Reply>>
    ): List<Reply> {
        val head = liveHead.value
        val page = livePage.value
        head?.let {
            maxReply = it.replyCount.toInt()
            po = it.userid
        }
        // Don't send a success until we have both results
        if (head == null || page == null) {
            return emptyList()
        }

        return listOf(head.toReply()).plus(page)
    }

    /**
     * insert Header to first page
     */
    private fun getLiveFirstPage(head: LiveData<Thread>, livePage: LiveData<List<Reply>>) =
        MediatorLiveData<List<Reply>>().apply {
            addSource(head) {
                val res = combineFirstPage(head, livePage)
                if (res.isNotEmpty() && !value.equalsExceptTimestamp(res)) value = res
            }
            addSource(livePage) {
                val res = combineFirstPage(head, livePage)
                if (res.isNotEmpty() && !value.equalsExceptTimestamp(res)) value = res
            }
        }

    private fun getLocalData(page: Int): LiveData<List<Reply>> {
        val livePage = replyDao.findDistinctPageByParentId(currentThreadId, page)
        return if (page == 1) getLiveFirstPage(currentThread!!, livePage)
        else livePage
    }

    suspend fun getServerData(page: Int): Job = coroutineScope {
        launch {
            Timber.d("Querying remote data for Thread $currentThreadId on $page")
            webService.getReplys(
                DawnApp.applicationDataStore.firstCookieHash,
                currentThreadId,
                page
            ).run {
                if (this is APIDataResponse.APISuccessDataResponse) convertServerData(data, page)
                else {
                    if (downloadJob != null) {
                        Timber.e(message)
                        setLoadingStatus(LoadingStatus.FAILED, "无法读取串回复...\n$message")
                    }
                }
            }
        }
    }

    private suspend fun convertServerData(data: Thread, page: Int) {
        // update current thread with latest info
        if (!data.equalsExceptTimestampAndReply(currentThread?.value)) {
            saveThread(data)
        }

        maxReply = data.replyCount.toInt()
        data.replys.map {
            it.page = page
            it.parentId = currentThreadId
        }

        // handle Ad
        data.replys.firstOrNull { it.isAd() }?.let {
            adMap[page] = it
        }
        val noAd = data.replys.filter { it.isNotAd() }
        /**
         *  On the first or last page, show NO DATA to indicate footer load end;
         *  When entering to a thread with cached data, refresh header is showing,
         *  set LoadingStatus to Success to hide the header
         */
        if (noAd.isEmpty() || replyMap[page]?.value.equalsExceptTimestamp(noAd) ||
            (page == 1 && replyMap[page]?.value?.drop(1).equalsExceptTimestamp(noAd))
        ) {
            if (page == maxPage || page == 1) setLoadingStatus(LoadingStatus.NODATA)
            else setLoadingStatus(LoadingStatus.SUCCESS)
            return
        }
        Timber.d("Updating ${noAd.size} rows for $currentThreadId on $page")
        saveReplys(noAd)
    }

    // DO NOT SAVE ADS
    private suspend fun saveReplys(replys: List<Reply>) = replyDao.insertAllWithTimeStamp(replys)

    private suspend fun saveThread(thread: Thread) {
        currentThread?.value?.run {
            thread.readingProgress = this.readingProgress
        }
        threadDao.insertWithTimeStamp(thread)
    }

    // TODO: do not send request if subscribe already
    suspend fun addFeed(uuid: String, id: String) {
        Timber.i("Adding Feed $id")
        webService.addFeed(uuid, id).run {
            if (this is APIMessageResponse.APISuccessMessageResponse) {
                if (messageType == APIMessageResponse.MessageType.String) {
                    addFeedResponse.postValue(
                        SingleLiveEvent.create(
                            LoadingStatus.SUCCESS,
                            message
                        )
                    )
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