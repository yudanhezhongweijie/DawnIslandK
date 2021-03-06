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
import com.squareup.moshi.JsonClass
import java.time.LocalDateTime
import kotlin.math.ceil

@JsonClass(generateAdapter = true)
@Entity
data class DailyTrend(
    val postId: String, // id of the specific reply that generates the content
    val po: String, // userid of the poster
    @PrimaryKey val date: LocalDateTime, // date when the trends posted
    val trends: List<Trend>,
    val lastReplyCount: Int
) {

    val page: Int get() = ceil(lastReplyCount.toDouble() / 19).toInt()

    override fun equals(other: Any?): Boolean =
        if (other is DailyTrend) {
            po == other.po && date.toLocalDate().isEqual(other.date.toLocalDate()) && postId == other.postId
                    && trends == other.trends && lastReplyCount == other.lastReplyCount
        } else false

    override fun hashCode(): Int {
        var result = po.hashCode()
        result = 31 * result + postId.hashCode()
        result = 31 * result + date.hashCode()
        result = 31 * result + trends.hashCode()
        result = 31 * result + lastReplyCount.hashCode()
        return result
    }
}