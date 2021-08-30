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

// from http://cover.acfunwiki.org/luwei.json, differs from Forum
@JsonClass(generateAdapter = true)
data class NoticeForum(
    val id: String,
    val sort: String = "",
    val name: String,
    val showName: String = "",
    val fgroup: String,
    val rule: String = "" // default rule
){
    fun getDisplayName(): String = if (showName.isNotBlank()) showName else name

    fun getPostRule(): String = if (rule.isNotBlank()) rule else "请遵守总版规"

    fun toForum(): Forum = Forum(id = id, sort = sort, name = name, showName = showName, fgroup = fgroup, msg = rule)
}
