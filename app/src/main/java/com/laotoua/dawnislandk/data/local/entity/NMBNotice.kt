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
import java.util.*

@JsonClass(generateAdapter = true)
@Entity
data class NMBNotice(
    @PrimaryKey(autoGenerate = true)
    val id: Int? = null, // only used to keep track of versions
    val content: String,
    val date: Long,
    val enable: Boolean,
    var read: Boolean = false,
    var lastUpdatedAt: Long = 0
){
    override fun equals(other: Any?) =
        if (other is NMBNotice) {
            content == other.content && date == other.date
                    && enable == other.enable && read == other.read
        } else false

    override fun hashCode(): Int {
        var result = id ?: 0
        result = 31 * result + content.hashCode()
        result = 31 * result + date.hashCode()
        result = 31 * result + enable.hashCode()
        result = 31 * result + read.hashCode()
        return result
    }

    fun setUpdatedTimestamp(time: Long? = null) {
        lastUpdatedAt = time ?: Date().time
    }
}