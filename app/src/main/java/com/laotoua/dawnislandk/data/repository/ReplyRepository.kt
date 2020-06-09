package com.laotoua.dawnislandk.data.repository

import android.util.SparseArray
import androidx.lifecycle.LiveData
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

class ReplyRepository @Inject constructor(
    private val webService: NMBServiceClient,
    private val replyDao: ReplyDao,
    private val threadDao: ThreadDao
) {
    var currentThreadId: String = "0"
    private val currentThreadIdInt get() = currentThreadId.toInt()

    /** remember all pages for last 30 thread, using threadId and page as index
     * using fifoList to pop the first page
     */
    private val cacheCap = 30
    private val threadMap = SparseArray<Thread>(cacheCap)
    private val replysMap = SparseArray<SparseArray<LiveData<List<Reply>>>>(cacheCap)
    private val fifoList = mutableListOf<Int>()
    private val adMap = SparseArray<SparseArray<Reply>>()

    private val maxReply get() = threadMap[currentThreadIdInt]?.replyCount?.toInt() ?: 0
    val po get() = threadMap[currentThreadIdInt]?.userid ?: ""
    val maxPage get() = 1.coerceAtLeast(kotlin.math.ceil(maxReply.toDouble() / 19).toInt())
    val loadingStatus = MutableLiveData<SingleLiveEvent<EventPayload<Nothing>>>()
    val addFeedResponse = MutableLiveData<SingleLiveEvent<EventPayload<Nothing>>>()

    private var downloadJob: Job? = null
    val emptyPage = MutableLiveData<Boolean>()

    fun getAd(page: Int): Reply? = adMap[currentThreadIdInt]?.get(page)

    suspend fun setThreadId(id: String) {
        if (id == currentThreadId) return
        setLoadingStatus(LoadingStatus.LOADING)
        clearCachedPages()
        Timber.d("Setting new Thread: $id")
        currentThreadId = id
        if (threadMap[currentThreadIdInt] == null) {
            threadDao.findThreadByIdSync(id)?.let {
                threadMap.put(currentThreadIdInt, it)
            }
        }
    }

    // set default page
    fun getLandingPage(): Int {
        return if (DawnApp.applicationDataStore.readingProgressStatus) {
            threadMap[currentThreadIdInt]?.readingProgress ?: 1
        } else 1
    }

    fun getHeaderReply(): Reply = threadMap[currentThreadIdInt]!!.toReply()

    suspend fun saveReadingProgress(progress: Int) {
        threadMap[currentThreadIdInt].readingProgress = progress
        threadDao.updateReadingProgressWithTimestampById(currentThreadId, progress)
    }

    private fun clearCachedPages() {
        Timber.d("Clearing cache on ${threadMap[currentThreadIdInt]?.id}...")
        emptyPage.value = false
        for (i in 0 until (replysMap.size() - cacheCap)) {
            replysMap.delete(fifoList.first())
            threadMap.delete(fifoList.first())
            fifoList.removeAt(0)
        }
        downloadJob?.cancel()
        downloadJob = null
    }

    /**
     *  w. cookie, responses have 20 reply w. ad, or 19 reply w/o ad
     *  w/o cookie, always have 20 reply w. ad
     *  *** here DB only store nonAd data
     */
    fun checkFullPage(page: Int): Boolean =
        (replysMap[currentThreadIdInt]?.get(page)?.value?.size == 19)

    fun setLoadingStatus(status: LoadingStatus, message: String? = null) =
        loadingStatus.postValue(SingleLiveEvent.create(status, message))

    fun getLivePage(page: Int): LiveData<List<Reply>> {
        if (replysMap[currentThreadIdInt] == null) {
            replysMap.append(currentThreadIdInt, SparseArray<LiveData<List<Reply>>>())
            fifoList.add(currentThreadIdInt)
        }
        replysMap[currentThreadIdInt]!!.let {
            if (it[page] == null) {
                it.append(page, liveData<List<Reply>>(Dispatchers.IO) {
                    Timber.d("Querying data for Thread $currentThreadId on $page")
                    setLoadingStatus(LoadingStatus.LOADING)
                    emitSource(getLocalData(page))
                    downloadJob = getServerData(page)
                })
            }
            return it[page]
        }
    }

    private fun getLocalData(page: Int): LiveData<List<Reply>> =
        replyDao.findDistinctPageByParentId(currentThreadId, page)

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
        if (!data.equalsWithServerData(threadMap[currentThreadIdInt])) {
            saveThread(data)
        }
        val noAd = mutableListOf<Reply>()
        data.replys.map {
            it.page = page
            it.parentId = currentThreadId
            // handle Ad
            if (it.isAd()) {
                if (adMap[currentThreadIdInt] == null) {
                    adMap.append(currentThreadIdInt, SparseArray<Reply>())
                }
                adMap[currentThreadIdInt]!!.append(page, it)
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

        if (replysMap[currentThreadIdInt]!![page]?.value.equalsExceptTimestamp(noAd)) {
            if (page == maxPage) setLoadingStatus(LoadingStatus.NODATA)
            return
        }
        Timber.d("Updating ${noAd.size} rows for $currentThreadId on $page")
        saveReplys(noAd)
    }

    // DO NOT SAVE ADS
    private suspend fun saveReplys(replys: List<Reply>) = replyDao.insertAllWithTimeStamp(replys)

    private suspend fun saveThread(thread: Thread) {
        threadMap[currentThreadIdInt]?.run {
            thread.readingProgress = readingProgress
        }
        threadMap.put(currentThreadIdInt, thread.stripCopy())
        threadDao.insertWithTimeStamp(thread)
    }

    // TODO: do not send request if subscribe already
    suspend fun addFeed(uuid: String, id: String) {
        Timber.d("Adding Feed $id")
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