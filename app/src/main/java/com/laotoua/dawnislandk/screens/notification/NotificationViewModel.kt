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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laotoua.dawnislandk.data.local.dao.NotificationDao
import com.laotoua.dawnislandk.data.local.entity.Notification
import kotlinx.coroutines.launch
import javax.inject.Inject

class NotificationViewModel @Inject constructor(private val notificationDao: NotificationDao):ViewModel() {
    val notificationAndPost = notificationDao.getLiveAllNotificationsAndPosts()

    fun deleteNotification(notification: Notification){
        viewModelScope.launch {
            notificationDao.deleteNotifications(notification)
        }
    }

    fun readNotification(notification: Notification){
        viewModelScope.launch {
            notification.read = true
            notificationDao.insertNotification(notification)
        }
    }
}