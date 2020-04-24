package com.laotoua.dawnislandk.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laotoua.dawnislandk.data.entity.Reply
import com.laotoua.dawnislandk.data.entity.Thread
import com.laotoua.dawnislandk.data.network.APIErrorResponse
import com.laotoua.dawnislandk.data.network.APINoDataResponse
import com.laotoua.dawnislandk.data.network.APISuccessResponse
import com.laotoua.dawnislandk.data.network.NMBServiceClient
import com.laotoua.dawnislandk.data.util.SingleLiveEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.apache.commons.text.StringEscapeUtils
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

    // flags to indicate status of loading reply
    private var _loadFail = MutableLiveData(false)
    val loadFail: LiveData<Boolean>
        get() = _loadFail
    private var _loadEnd = MutableLiveData(false)
    val loadEnd: LiveData<Boolean>
        get() = _loadEnd

    private val _addFeedResponse = MutableLiveData<SingleLiveEvent<String>>()
    val addFeedResponse: LiveData<SingleLiveEvent<String>> get() = _addFeedResponse

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
            Timber.i("Refreshing without data?")
            _loadFail.postValue(true)
            return
        }
        val page = replyList.first().page!!.toInt()
        if (page == 1) {
            Timber.i("Already first page")
            _loadFail.postValue(true)
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
            val list = mutableListOf<Reply>()
            when (val response = NMBServiceClient.getReplys(_currentThread!!.id, page)) {
                // TODO thread deleted
                is APINoDataResponse -> {
                    Timber.e("APINoDataResponse: ${response.errorMessage}")
                }
                // TODO mostly network error
                is APIErrorResponse -> {
                    Timber.e("APIErrorResponse: ${response.errorMessage}")
                }
                is APISuccessResponse -> {
                    val thread = response.data
                    maxReply = thread.replyCount?.toInt() ?: 0
                    list.addAll(thread.replys!!)
                    /**
                     *  w. cookie, responses have 20 reply w. ad, or 19 reply w/o ad
                     *  w/o cookie, always have 20 reply w. ad
                     *  MARK full page for next page load
                     */
                    fullPage =
                        (list.size == 20 || (list.size == 19 && list.first().id != "9999999"))

                    // add thread to as first reply for page 1
                    if (page == 1) {
                        replyList.add(0, _currentThread!!.toReply())
                        // TODO: previous page & next page should be handled the same
                        if (direction == DIRECTION.PREVIOUS) previousPage.add(_currentThread!!.toReply())
                    }

                    val noDuplicates =
                        list.filterNot { replyIds.contains(it.id) && it.id != "9999999" }

                    if ((noDuplicates.isNotEmpty() &&
                                (noDuplicates.size > 1 || noDuplicates.first().id != "9999999"))
                        || page == 1
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
                        _loadEnd.postValue(true)
                    }
                }
                else -> {
                    Timber.e("unhandled API type response $response")
                }
            }


        }
    }

    fun addFeed(uuid: String, id: String) {
        Timber.i("Adding Feed $id")
        viewModelScope.launch(Dispatchers.IO) {
            NMBServiceClient.addFeed(uuid, id).run {
                // TODO: check failure response
                /** res:
                 *  "\u53d6\u6d88\u8ba2\u9605\u6210\u529f!"
                 */
                val msg = StringEscapeUtils.unescapeJava(this.replace("\"", ""))
                SingleLiveEvent(msg).run {
                    _addFeedResponse.postValue(this)
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
