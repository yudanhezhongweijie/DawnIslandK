package com.laotoua.dawnislandk.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laotoua.dawnislandk.data.entity.Reply
import com.laotoua.dawnislandk.data.entity.Thread
import com.laotoua.dawnislandk.data.network.APISuccessMessageResponse
import com.laotoua.dawnislandk.data.network.MessageType
import com.laotoua.dawnislandk.data.network.NMBServiceClient
import kotlinx.coroutines.launch
import timber.log.Timber

class ReplyViewModel : ViewModel() {
//    private var dao: ReplyDao? = null

    private var _currentThread: Thread? = null
    val currentThread: Thread? get() = _currentThread
    private val replyList = mutableListOf<Reply>()
    private val replyIds = mutableSetOf<String>()
    private var _reply = MutableLiveData<List<Reply>>()
    val reply: LiveData<List<Reply>> get() = _reply

    var fullPage = false

    private var maxReply = 0

    // TODO: previous page & next page should be handled the same
    var previousPage = mutableListOf<Reply>()

    val maxPage get() = 1.coerceAtLeast(kotlin.math.ceil(maxReply.toDouble() / 19).toInt())

    private var _loadingStatus = MutableLiveData<SingleLiveEvent<EventPayload<Nothing>>>()
    val loadingStatus: LiveData<SingleLiveEvent<EventPayload<Nothing>>>
        get() = _loadingStatus

    private val _addFeedResponse = MutableLiveData<SingleLiveEvent<EventPayload<Nothing>>>()
    val addFeedResponse: LiveData<SingleLiveEvent<EventPayload<Nothing>>> get() = _addFeedResponse

    enum class DIRECTION {
        NEXT,
        PREVIOUS
    }

    fun setThread(f: Thread) {
        if (f.id == currentThread?.id ?: "") return
        Timber.i("Thread has changed... Clearing old data")
        replyList.clear()
        replyIds.clear()
        Timber.i("Setting new Thread: ${f.id}")
        _currentThread = f
        getNextPage()
    }

    fun getNextPage() {
        var page = 1
        if (replyList.isNotEmpty()) page = replyList.last().page!!
        if (fullPage) page += 1
        getReplys(page, DIRECTION.NEXT)
    }

    fun getPreviousPage() {
        if (replyList.isEmpty()) {
            Timber.e("Refreshing without data?")
            _loadingStatus.postValue(
                SingleLiveEvent.create(
                    LoadingStatus.FAILED,
                    "Refreshing without data?"
                )
            )

            return
        }
        val page = replyList.first().page!!.toInt()
        if (page == 1) {
            _loadingStatus.postValue(
                SingleLiveEvent.create(
                    LoadingStatus.NODATA,
                    "没有上一页了..."

                )
            )
            return
        }

        previousPage.clear()

        getReplys(page - 1, DIRECTION.PREVIOUS)
    }

    fun jumpTo(page: Int) {
        Timber.i("Jumping to page $page... Clearing old data")
        replyList.clear()
        replyIds.clear()
        getReplys(page, DIRECTION.NEXT)
    }

    private fun getReplys(page: Int, direction: DIRECTION) {
        if (_currentThread == null) {
            Timber.e("Trying to read replys without selected forum")
            return
        }

        viewModelScope.launch {
            DataResource.create(NMBServiceClient.getReplys(_currentThread!!.id, page)).run {
                when (this) {
                    is DataResource.Success -> {
                        convertServerData(data!!, page, direction)
                    }
                    is DataResource.Error -> {
                        Timber.e(message)
                        _loadingStatus.postValue(
                            SingleLiveEvent.create(
                                LoadingStatus.FAILED, "无法读取串回复...\n$message"
                            )
                        )
                    }
                }
            }
        }
    }

    private fun convertServerData(data: Thread, page: Int, direction: DIRECTION) {
        val list = mutableListOf<Reply>()
        maxReply = data.replyCount?.toInt() ?: 0
        list.addAll(data.replys!!)
        /**
         *  w. cookie, responses have 20 reply w. ad, or 19 reply w/o ad
         *  w/o cookie, always have 20 reply w. ad
         *  MARK full page for next page load
         */
        fullPage =
            (list.size == 20 || (list.size == 19 && list.first().id != "9999999"))

        // add thread to as first reply for page 1
        if (page == 1 && !replyIds.contains(_currentThread!!.id)) {
            replyList.add(0, _currentThread!!.toReply())
            replyIds.add(_currentThread!!.id)

            _reply.postValue(replyList)
            // TODO: previous page & next page should be handled the same
            if (direction == DIRECTION.PREVIOUS) previousPage.add(_currentThread!!.toReply())
        }

        val noDuplicates =
            list.filterNot { replyIds.contains(it.id) && it.id != "9999999" }

        if ((noDuplicates.isNotEmpty() &&
                    (noDuplicates.size > 1 || noDuplicates.first().id != "9999999"))
        ) {
            replyIds.addAll(noDuplicates.map { it.id })
            Timber.i(
                "No duplicate reply size ${noDuplicates.size}, replyIds size ${replyIds.size}"
            )

            // add page to Reply
            noDuplicates.apply { map { it.page = page } }
                .let {
                    if (direction == DIRECTION.NEXT) {
                        replyList.addAll(it)
                    } else {
                        val insertInd = if (page == 1) 1 else 0
                        replyList.addAll(insertInd, it)
                        // TODO: previous page & next page should be handled the same
                        previousPage.addAll(it)
                    }
                }

            _reply.postValue(replyList)

            Timber.i("CurrentPage: $page ReplyIds(w. ad, head): ${replyIds.size} replyList: ${replyList.size} replyCount: $maxReply")
        } else {
            Timber.i("Thread ${_currentThread!!.id} has no new replys.")
            _loadingStatus.postValue(
                SingleLiveEvent.create(
                    LoadingStatus.NODATA,
                    "No New Replys"
                )

            )

        }
    }

    // TODO: do not send request if subscribe already
    fun addFeed(uuid: String, id: String) {
        Timber.i("Adding Feed $id")
        viewModelScope.launch {
            NMBServiceClient.addFeed(uuid, id).run {
                when (this) {
                    is APISuccessMessageResponse -> {
                        if (messageType == MessageType.String) {
                            _addFeedResponse.postValue(
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
                        _addFeedResponse.postValue(
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
    // TODO
//    fun loadFromDB() {
//        viewModelScope.launch {
//            withContext(Dispatchers.IO) {
//                dao?.getAll().let {
//                    if (it?.size ?: 0 > 0) {
//                        Timber.i("Loaded ${it?.size} replys from db")
//                        _replyList.postValue(it)
//                    } else {
//                        Timber.i("Db has no data about replys")
//                    }
//                }
//            }
//        }
//    }

    // TODO
//    fun setDb(dao: ForumDao) {
//        this.dao = dao
//        Timber.i("Forum DAO set!!!")
//    }


}
