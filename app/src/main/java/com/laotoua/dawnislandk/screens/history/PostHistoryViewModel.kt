package com.laotoua.dawnislandk.screens.history

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.ViewModel
import com.laotoua.dawnislandk.data.local.dao.PostHistoryDao
import com.laotoua.dawnislandk.data.local.entity.PostHistory
import com.laotoua.dawnislandk.util.ReadableTime
import java.util.*
import javax.inject.Inject

class PostHistoryViewModel @Inject constructor(private val postHistoryDao: PostHistoryDao) : ViewModel() {
    private var endDate = Date().time
    private var startDate = endDate - ReadableTime.LAST_30_DAYS_MILLIS
    private var currentList: LiveData<List<PostHistory>>? = null
    val postHistoryList = MediatorLiveData<List<PostHistory>>()

    init {
        searchByDate()
    }

    fun setStartDate(date: Date) {
        startDate = date.time
    }

    fun setEndDate(date: Date) {
        endDate = date.time
    }

    fun searchByDate() {
        if (currentList != null) postHistoryList.removeSource(currentList!!)
        currentList = getLiveHistoryInRange(startDate, endDate)
        postHistoryList.addSource(currentList!!) {
            postHistoryList.value = it
        }
    }

    private fun getLiveHistoryInRange(startDate: Long, endDate: Long) =
        postHistoryDao.getAllPostHistoryInDateRange(startDate, endDate)
}