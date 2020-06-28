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

@Entity
class PostHistory(
    @PrimaryKey
    val id: String, // actual id of post
    val newPost: Boolean,// false if replying
    val postTargetId: String, // equals postTargetFid when sending a new Post
    val postTargetFid: String,
    val postTargetPage: Int, // if replying, indicates posted page for jumps otherwise page 1
    val cookieName: String,
    val content: String, //content
    val img: String,
    val ext: String,
    val postDate: Long
) {
    constructor(id: String, targetPage: Int, img: String, ext: String, draft: Draft) : this(
        id,
        draft.newPost,
        draft.postTargetId,
        draft.postTargetFid,
        targetPage,
        draft.cookieName,
        draft.content,
        img,
        ext,
        draft.postDate
    )

    class Draft(
        val newPost: Boolean,// false if replying
        val postTargetId: String, // equals postTargetFid when sending a new Post
        val postTargetFid: String,
        val cookieName: String,
        var content: String, //content
        val postDate: Long
    )

    fun getImgUrl() = (img + ext)
}