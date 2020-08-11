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
import com.laotoua.dawnislandk.data.local.entity.Notification
import com.laotoua.dawnislandk.data.local.entity.NotificationAndPost

@Dao
interface NotificationDao {

    @Query("SELECT * FROM Notification ORDER BY lastUpdatedAt DESC")
    fun getLiveAllNotifications():LiveData<List<Notification>>

    @Transaction
    @Query("SELECT * From Notification ORDER BY lastUpdatedAt DESC")
    fun getLiveAllNotificationsAndPosts(): LiveData<List<NotificationAndPost>>

    @Query("SELECT * FROM Notification WHERE id=:id LIMIT 1")
    suspend fun getNotificationByIdSync(id:String):Notification?

    @Transaction
    suspend fun insertOrUpdateNotification(notification: Notification){
        val cache = getNotificationByIdSync(notification.id)
        if (cache != null){
            notification.newReplyCount += cache.newReplyCount
        }
        insertNotification(notification)
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: Notification)

    @Delete
    suspend fun deleteNotifications(vararg notifications: Notification)

    @Query("DELETE FROM Notification WHERE id=:id")
    suspend fun deleteNotificationById(id: String)

    @Query("DELETE FROM Notification")
    suspend fun nukeTable()
}