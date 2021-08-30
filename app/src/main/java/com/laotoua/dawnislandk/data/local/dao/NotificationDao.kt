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

package com.laotoua.dawnislandk.data.local.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.laotoua.dawnislandk.DawnApp
import com.laotoua.dawnislandk.data.local.entity.Notification
import com.laotoua.dawnislandk.data.local.entity.NotificationAndPost

@Dao
interface NotificationDao {

    @Query("SELECT * FROM Notification WHERE domain=:domain ORDER BY lastUpdatedAt DESC")
    fun getLiveAllNotifications(domain: String = DawnApp.currentDomain): LiveData<List<Notification>>

    @Query("SELECT COUNT(*) FROM Notification WHERE read=0 AND domain=:domain ORDER BY lastUpdatedAt DESC")
    fun getLiveUnreadNotificationsCount(domain: String = DawnApp.currentDomain): LiveData<Int>

    @Transaction
    @Query("SELECT * From Notification WHERE domain=:domain ORDER BY lastUpdatedAt DESC")
    fun getLiveAllNotificationsAndPosts(domain: String = DawnApp.currentDomain): LiveData<List<NotificationAndPost>>

    @Query("SELECT * FROM Notification WHERE id=:id AND domain=:domain LIMIT 1")
    suspend fun getNotificationByIdSync(id: String, domain: String = DawnApp.currentDomain): Notification?

    @Query("UPDATE Notification SET read=1, newReplyCount=0 WHERE id=:id AND domain=:domain")
    suspend fun readNotificationByIdSync(id: String, domain: String = DawnApp.currentDomain)

    @Transaction
    suspend fun insertOrUpdateNotification(notification: Notification) {
        val cache = getNotificationByIdSync(notification.id)
        if (cache != null && !cache.read) {
            notification.newReplyCount += cache.newReplyCount
        }
        insertNotification(notification)
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: Notification)

    @Delete
    suspend fun deleteNotifications(vararg notifications: Notification)

    @Query("DELETE FROM Notification WHERE id=:id AND domain=:domain")
    suspend fun deleteNotificationById(id: String, domain: String = DawnApp.currentDomain)

    @Query("DELETE FROM Notification")
    suspend fun nukeTable()
}