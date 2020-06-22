package com.laotoua.dawnislandk.screens.history

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.ViewModel
import com.laotoua.dawnislandk.data.local.dao.BrowsingHistoryDao
import com.laotoua.dawnislandk.data.local.entity.BrowsingHistoryAndPost
import com.laotoua.dawnislandk.util.ReadableTime
import java.util.*
import javax.inject.Inject

class BrowsingHistoryViewModel @Inject constructor(private val browsingHistoryDao: BrowsingHistoryDao) :
    ViewModel() {

    // get a week's history by default
    private var endDate = ReadableTime.getTodayDateLong()
    private var startDate = endDate - ReadableTime.WEEK_MILLIS
    private var currentList:LiveData<List<BrowsingHistoryAndPost>>?=null
    val browsingHistoryList = MediatorLiveData<List<BrowsingHistoryAndPost>>()

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
        if (currentList != null) browsingHistoryList.removeSource(currentList!!)
        currentList = getLiveHistoryInRange(startDate, endDate)
        browsingHistoryList.addSource(currentList!!) {
            browsingHistoryList.value = it
        }
    }

    private fun getLiveHistoryInRange(startDate: Long, endDate: Long) =
        browsingHistoryDao.getAllBrowsingHistoryAndPostInDateRange(startDate, endDate)
}