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

package com.laotoua.dawnislandk.screens.notification

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laotoua.dawnislandk.DawnApp
import com.laotoua.dawnislandk.data.local.dao.NotificationDao
import com.laotoua.dawnislandk.data.local.entity.Notification
import com.laotoua.dawnislandk.data.local.entity.NotificationAndPost
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

class NotificationViewModel @Inject constructor(private val notificationDao: NotificationDao):ViewModel() {
    private var _notificationAndPost:LiveData<List<NotificationAndPost>>? = null
    val notificationAndPost = MediatorLiveData<List<NotificationAndPost>>()

    private var cacheDomain: String = DawnApp.currentDomain
    
    fun changeDomain(domain: String) {
        if (domain != cacheDomain) {
            getLiveNotifications()
            cacheDomain = domain
        }
    }

    init {
        getLiveNotifications()
    }

    private fun getLiveNotifications() {
        Timber.d("Getting live notifications...")
        if (_notificationAndPost != null) notificationAndPost.removeSource(_notificationAndPost!!)
        _notificationAndPost = notificationDao.getLiveAllNotificationsAndPosts()
        notificationAndPost.addSource(_notificationAndPost!!) {
            notificationAndPost.value = it
        }
    }
    
    fun deleteNotification(notification: Notification) {
        viewModelScope.launch {
            notificationDao.deleteNotifications(notification)
        }
    }

    fun readNotification(notification: Notification) {
        viewModelScope.launch {
            notification.read = true
            notification.newReplyCount = 0
            notificationDao.insertNotification(notification)
        }
    }
    
}