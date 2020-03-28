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
            // add replys
            list.addAll(api.getReplys("id=${currentThread!!.id}&page=$pageCount"))

            val noDuplicates = list.filterNot { replyIds.contains(it.id) }
            if (noDuplicates.isNotEmpty()) {
                replyIds.addAll(noDuplicates.map { it.id })
                Log.i(
                    TAG,
                    "no duplicate reply size ${noDuplicates.size}, replyIds size ${replyIds.size}"
                )

                replyList.addAll(noDuplicates)
                _newPage.postValue(noDuplicates)
                pageCount += 1
            } else {
                Log.i(TAG, "Thread ${currentThread!!.id} has no new replys.")
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
