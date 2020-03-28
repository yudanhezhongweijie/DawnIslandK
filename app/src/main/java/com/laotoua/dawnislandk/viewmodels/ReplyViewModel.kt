package com.laotoua.dawnislandk.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laotoua.dawnislandk.util.API
import com.laotoua.dawnislandk.util.Reply
import com.laotoua.dawnislandk.util.ThreadList
import kotlinx.coroutines.launch

class ReplyViewModel : ViewModel() {
    private val TAG = "ForumVM"
    private val api = API()
//    private var dao: ReplyDao? = null


    private var currentThread: ThreadList? = null
    private val replyList = mutableListOf<Reply>()
    private val replyIds = mutableSetOf<String>()
    private var _newPage = MutableLiveData<List<Reply>>()
    val newPage: LiveData<List<Reply>>
        get() = _newPage
    private var pageCount = 1

    // flags to indicate status of loading reply
    private var _loadFail = MutableLiveData(false)
    val loadFail: LiveData<Boolean>
        get() = _loadFail
    private var _loadEnd = MutableLiveData(false)
    val loadEnd: LiveData<Boolean>
        get() = _loadEnd

    fun setThread(f: ThreadList) {
        currentThread = f
        pageCount = 1
        getReplys()
    }

    fun getReplys() {
        if (currentThread == null) {
            Log.e(TAG, "Trying to read replys without selected forum")
            return
        }
        viewModelScope.launch {
            val list = mutableListOf<Reply>()
            // add thread to as first reply for page 1
            if (pageCount == 1) {
                list.add(currentThread!!.toReply())
            }
            // TODO: handle case where thread is deleted
            try {
                list.addAll(api.getReplys("id=${currentThread!!.id}&page=$pageCount"))
            } catch (e: Exception) {
                Log.e(TAG, "reply api error", e)
                _loadFail.postValue(true)
                return@launch
            }

            val noDuplicates = list.filterNot { replyIds.contains(it.id) && it.id != "9999999" }

            if (noDuplicates.isNotEmpty()) {
                replyIds.addAll(noDuplicates.map { it.id })
                Log.i(
                    TAG,
                    "no duplicate reply size ${noDuplicates.size}, replyIds size ${replyIds.size}"
                )

                replyList.addAll(noDuplicates)
                _newPage.postValue(noDuplicates)
                // TODO: updates differently with cookie
                if (replyList.size % 20 == 1) pageCount += 1
            } else {
                Log.i(TAG, "Thread ${currentThread!!.id} has no new replys.")
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
//                        Log.i(TAG, "Loaded ${it?.size} replys from db")
//                        _replyList.postValue(it)
//                    } else {
//                        Log.i(TAG, "Db has no data about replys")
//                    }
//                }
//            }
//        }
//    }

    // TODO
//    fun setDb(dao: ForumDao) {
//        this.dao = dao
//        Log.i(TAG, "Forum DAO set!!!")
//    }
}
