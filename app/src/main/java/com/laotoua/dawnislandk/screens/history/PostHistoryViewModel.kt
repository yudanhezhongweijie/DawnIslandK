/*
 *  Copyright 2020 Fishballzzz
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

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