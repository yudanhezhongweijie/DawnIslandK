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
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.time.LocalDateTime

@JsonClass(generateAdapter = true)
@Entity
data class LuweiNotice(
    @PrimaryKey(autoGenerate = true)
    val id: Int? = null, // only used to keep track of versions
    @Json(name = "appstore")
    val appVersion: Map<String, Boolean>,
    @Json(name = "beitaiForum")
    val beitaiForums: List<NoticeForum>,
    @Json(name = "forum")
    val nmbForums: List<NoticeForum>,
    @Json(name = "loading")
    val loadingMsgs: List<String>,
    @Json(name = "update")
    val clientsInfo: Map<String, ClientInfo>,
    @Json(name = "whitelist")
    val whitelist: WhiteList,
    var lastUpdatedAt: LocalDateTime = LocalDateTime.now()
) {

    override fun equals(other: Any?) =
        if (other is LuweiNotice) {
            appVersion == other.appVersion && beitaiForums == other.beitaiForums
                    && nmbForums == other.nmbForums && loadingMsgs == other.loadingMsgs
                    && clientsInfo == other.clientsInfo && whitelist == other.whitelist
        } else false

    override fun hashCode(): Int {
        var result = appVersion.hashCode()
        result = 31 * result + beitaiForums.hashCode()
        result = 31 * result + nmbForums.hashCode()
        result = 31 * result + loadingMsgs.hashCode()
        result = 31 * result + clientsInfo.hashCode()
        result = 31 * result + whitelist.hashCode()
        return result
    }

    fun setUpdatedTimestamp(time: LocalDateTime = LocalDateTime.now()) {
        lastUpdatedAt = time
    }

    @JsonClass(generateAdapter = true)
    data class ClientInfo(
        val description: String = "",
        val url: String = "",
        val version: String = "",
        @Json(name = "versionid")
        val versionId: Int = 0
    )

    @JsonClass(generateAdapter = true)
    data class WhiteList(val date: Long, @Json(name = "threads") val posts: List<String>)
}