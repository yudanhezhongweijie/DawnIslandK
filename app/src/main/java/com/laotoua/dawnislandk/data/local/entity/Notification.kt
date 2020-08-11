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
    val fid: String,
    var newReplyCount: Int,
    val message: String = "", // feed subscription update do not use this field, it is reserved for other notifications
    var read: Boolean = false,
    var lastUpdatedAt: Long
) {
    // valid targetId should be all digits, here only checking the first digit should be sufficient
    // fake feed notification should start with some non digit char
    fun isValidFeedPushNotification(): Boolean = id.first().isDigit()

    companion object {
        fun makeNotification(
            targetId: String,
            fid: String,
            newReplyCount: Int,
            message: String = ""
        ): Notification {
            return Notification(targetId, fid, newReplyCount, message, false, Date().time)
        }
    }
}