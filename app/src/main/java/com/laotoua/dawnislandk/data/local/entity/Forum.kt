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

import androidx.annotation.Keep
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

// from api/getForumList, differs from NoticeForum
@JsonClass(generateAdapter = true)
@Keep
data class Forum(
    @PrimaryKey
    val id: String,
    val fgroup: String = "",
    val sort: String = "",
    val name: String,
    val showName: String = "",
    val msg: String,
    val interval: String = "",
    val createdAt: String = "",
    val updateAt: String = "",
    val status: String = ""
) {
    fun getDisplayName(): String =  if (showName.isNotBlank()) showName else name

    fun isFakeForum():Boolean = fgroup == "42" && sort == "42" && name == "42"  && msg == "42"

    fun isValidForum():Boolean = !isFakeForum()

    companion object{
        fun makeFakeForum(id: String, showName: String):Forum{
            return Forum(id, "42", "42", "42", showName, "42")
        }
    }
}