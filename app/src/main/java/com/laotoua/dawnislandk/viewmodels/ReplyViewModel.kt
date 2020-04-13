package com.laotoua.dawnislandk.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laotoua.dawnislandk.entities.Reply
import com.laotoua.dawnislandk.entities.Thread
import com.laotoua.dawnislandk.network.NMBServiceClient
import kotlinx.coroutines.launch
import timber.log.Timber

class ReplyViewModel : ViewModel() {
//    private var dao: ReplyDao? = null


    private var _currentThread: Thread? = null
    val currentThread: Thread? get() = _currentThread
    private val replyList = mutableListOf<Reply>()
    private val replyIds = mutableSetOf<String>()
    private var _reply = MutableLiveData<List<Reply>>()
    val reply: LiveData<List<Reply>>
        get() = _reply
    private var _nextPage = 1
    val nextPage get() = _nextPage

    private var _maxPage = 1
    val maxPage get() = _maxPage

    private var _maxReply = 0
    val maxReply get() = _maxReply

    // flags to indicate status of loading reply
    private var _loadFail = MutableLiveData(false)
    val loadFail: LiveData<Boolean>
        get() = _loadFail
    private var _loadEnd = MutableLiveData(false)
    val loadEnd: LiveData<Boolean>
        get() = _loadEnd

    fun setThread(f: Thread) {
        if (f.id == currentThread?.id ?: "") return
        Timber.i("$f vs $currentThread Thread has changed... Clearing old data")
        replyList.clear()
        replyIds.clear()
        Timber.i("Setting new Thread: ${f.id}")
        _currentThread = f
        _nextPage = 1
        getReplys()
    }

    fun getReplys() {
        if (_currentThread == null) {
            Timber.e("Trying to read replys without selected forum")
            return
        }
        viewModelScope.launch {
            val list = mutableListOf<Reply>()
            // add thread to as first reply for page 1
            if (_nextPage == 1) {
                list.add(_currentThread!!.toReply())
            }
            // TODO: handle case where thread is deleted
            try {
                val thread = NMBServiceClient.getReplys(_currentThread!!.id, _nextPage)
                _maxReply = thread.replyCount.toInt()
                _maxPage = (maxReply / 20)

                list.addAll(thread.replys!!)
            } catch (e: Exception) {
                Timber.e(e, "reply api error")
                _loadFail.postValue(true)
                return@launch
            }

            val noDuplicates = list.filterNot { replyIds.contains(it.id) && it.id != "9999999" }

            if (noDuplicates.isNotEmpty() &&
                (noDuplicates.size > 1 || noDuplicates.first().id != "9999999")
            ) {
                replyIds.addAll(noDuplicates.map { it.id })
                Timber.i(
                    "no duplicate reply size ${noDuplicates.size}, replyIds size ${replyIds.size}"
                )

                // add page to Reply
                replyList.addAll(noDuplicates.apply { map { it.page = _nextPage } })
                _reply.postValue(replyList)
                // TODO: updates differently with cookie
                if (replyList.size % 20 == 1) _nextPage += 1
                // replyids = replysList - page(ad per page) - 1(head)
                Timber.i("NextPage: $nextPage Downloaded Replys(exclu. ad, head): ${replyIds.size - 2} replyList: ${replyList.size} replyCount(exclu. ad): $maxReply")
            } else {
                Timber.i("Thread ${_currentThread!!.id} has no new replys.")
                _loadEnd.postValue(true)
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
