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

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Trend(
    val rank: String,
    val hits: String,
    val forum: String,
    val id: String,
    val content: String
) {
    fun toPost(fid: String): Post {
        return Post(
            id = id,
            fid = fid,
            img = "",
            ext = "",
            now = "",
            userHash = "",
            name = "",
            email = "",
            title = "",
            content = "",
            admin = ""
        )

    }
}