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

@Entity
data class Feed(
    val id: Int = 1, // table id for sorting
    val page: Int, // each page has at most 10 feeds, page also helps sorting
    @PrimaryKey val postId: String, //	该串的id
    val category: String,
    var lastUpdatedAt: Long = 0 // timestamp which the row is updated
) {
    override fun equals(other: Any?) =
        if (other is Feed) {
            id == other.id && page == other.page && postId == other.postId && category == other.category
        } else false

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + page.hashCode()
        result = 31 * result + postId.hashCode()
        result = 31 * result + category.hashCode()
        return result
    }

    @JsonClass(generateAdapter = true)
    data class ServerFeed(
        @PrimaryKey val id: String,
        val fid: String = "",
        val category: String,
        val img: String,
        val ext: String,
        val now: String,
        val userid: String,
        val name: String,
        val email: String,
        val title: String,
        val content: String,
        val admin: String = "0",
        val status: String = ""
    ) {
        fun toPost() = Post(
            id,
            fid,
            img,
            ext,
            now,
            userid,
            name,
            email,
            title,
            content,
            "",
            "0",
            status,
            emptyList(),
            "",
            0
        )
    }
}