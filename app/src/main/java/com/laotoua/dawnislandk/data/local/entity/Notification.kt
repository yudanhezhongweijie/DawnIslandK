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

package com.laotoua.dawnislandk.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.*

@Entity
data class Notification(
    @PrimaryKey val id: String,
    val forumName: String,
    var newReplyCount: Int,
    val contentAbbr: String,
    val message: String = "", // feed subscription update do not use this field, it is reserved for other notifications
    var lastUpdatedAt: Long
) {
    // valid targetId should be all digits, here only checking the first digit should be sufficient
    // fake feed notification should start with some non digit char
    fun isValidFeedPushNotification(): Boolean = id.first().isDigit()

    fun getNotificationMessage(): String {
        return if (message.isBlank()) "$forumName · No.$id 有${newReplyCount}个新回复\n$contentAbbr..."
        else message
    }

    companion object {
        fun makeNotification(
            targetId: String,
            forumName: String,
            newReplyCount: Int,
            content: String,
            message: String? = null
        ): Notification {
            // only store at most 20 words in cache
            val abbrLength = content.length.coerceAtMost(20)
            val contentAbbr = content.substring(0, abbrLength)
            return Notification(targetId, forumName, newReplyCount,contentAbbr, message ?: "", Date().time)
        }
    }
}