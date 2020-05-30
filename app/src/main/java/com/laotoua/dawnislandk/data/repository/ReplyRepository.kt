package com.laotoua.dawnislandk.data.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.liveData
import com.laotoua.dawnislandk.DawnApp
import com.laotoua.dawnislandk.data.local.Reply
import com.laotoua.dawnislandk.data.local.Thread
import com.laotoua.dawnislandk.data.local.dao.ReplyDao
import com.laotoua.dawnislandk.data.local.dao.ThreadDao
import com.laotoua.dawnislandk.data.remote.*
import com.laotoua.dawnislandk.util.EventPayload
import com.laotoua.dawnislandk.util.LoadingStatus
import com.laotoua.dawnislandk.util.SingleLiveEvent
import kotlinx.coroutines.Dispatchers
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReplyRepository @Inject constructor(
    private val webService: NMBServiceClient,
    private val replyDao: ReplyDao,
    private val threadDao: ThreadDao
) {
    var currentThreadId: String = ""
    private var currentThread: Thread? = null
    private val replyMap = sortedMapOf<Int, LiveData<List<Reply>>>()
    private val adMap = sortedMapOf<Int, Reply>()
    private var maxReply = 0
    var po: String = ""
    val maxPage get() = 1.coerceAtLeast(kotlin.math.ceil(maxReply.toDouble() / 19).toInt())
    val loadingStatus = MutableLiveData<SingleLiveEvent<EventPayload<Nothing>>>()
    val addFeedResponse = MutableLiveData<SingleLiveEvent<EventPayload<Nothing>>>()

    /** When thread has 0 reply, this triggers VM to show an empty page
     *
     */
    val emptyPage = MutableLiveData<Boolean>()

    fun attachAdAndHead(list: List<Reply>, page: Int) = mutableListOf<Reply>().apply {
        /**
         *  w. cookie, responses have 20 reply w. ad, or 19 reply w/o ad
         *  w/o cookie, always have 20 reply w. ad
         *  MARK full page for next page load
         *  *** FIRST PAGE also has thread head
         */
        if (page == 1) add(currentThread!!.toReply())
        adMap[page]?.let { add(it) }
        addAll(list)
    }

    fun setThread(f: Thread) {
        if (f.id == currentThreadId) return
        Timber.i("Thread has changed... Clearing old data")
        clearCache()
        Timber.i("Setting new Thread: ${f.id}")
        currentThreadId = f.id
        currentThread = f
        po = f.userid
        emptyPage.value = false
    }

    private fun clearCache() {
        emptyPage.value = false
        replyMap.clear()
    }

    fun checkFullPage(page: Int): Boolean = (replyMap[page]?.value?.size == 19)

    private fun setLoadingStatus(status: LoadingStatus, message: String? = null) {
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
        getServerData(page)
    }

    private fun getLocalData(page: Int): LiveData<List<Reply>> =
        replyDao.findDistinctPageByParentId(currentThreadId, page)

    suspend fun getServerData(page: Int) {
        Timber.d("Querying remote data for Thread $currentThreadId on $page")
        webService.getReplys(
            DawnApp.applicationDataStore.firstCookieHash,
            currentThreadId,
            page
        ).run {
            when (this) {
                is APISuccessDataResponse -> convertServerData(data, page)
                is APIErrorDataResponse -> {
                    Timber.e(message)
                    setLoadingStatus(LoadingStatus.FAILED, "无法读取串回复...\n$message")
                }
                else -> {
                    Timber.e("Unhandled response $this")
                }
            }
        }
    }

    private suspend fun convertServerData(data: Thread, page: Int) {
        // update current thread with latest info
        currentThread = data

        maxReply = data.replyCount.toInt()
        data.replys.map {
            it.page = page
            it.parentId = currentThreadId
        }

        // handle Ad
        data.replys.firstOrNull { !it.isNotAd() }?.let {
            adMap[page] = it
        }
        val noAd = data.replys.filter { it.isNotAd() }
        if (page == 1 && noAd.isEmpty()) {
            emptyPage.postValue(true)
            return
        }
        if (noAd.isEmpty() || (replyMap[page]?.value == noAd && page == maxPage)) {
            setLoadingStatus(LoadingStatus.NODATA)
            return
        }
        Timber.d("Updating ${noAd.size} rows for $currentThreadId on $page")
        saveReplys(noAd)
    }

    // DO NOT SAVE ADS
    private suspend fun saveReplys(replys: List<Reply>) = replyDao.insertAll(replys)

    // TODO: do not send request if subscribe already
    suspend fun addFeed(uuid: String, id: String) {
        Timber.i("Adding Feed $id")
        webService.addFeed(uuid, id).run {
            when (this) {
                is APISuccessMessageResponse -> {
                    if (messageType == MessageType.String) {
                        addFeedResponse.postValue(
                            SingleLiveEvent.create(
                                LoadingStatus.SUCCESS,
                                message
                            )
                        )
                    } else {
                        Timber.e(message)
                    }
                }
                else -> {
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
}