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

package com.laotoua.dawnislandk.data.remote

data class SearchResult(
    val query: String = "", // to be set in VM
    val queryHits: Int, // # of return result matching the query
    val page: Int = 1, // current page of the result, to be set in VM
    val hits: List<Hit> = emptyList()
) {
    data class Hit(
        val id: String,
        val now: String,
        val time: String,
        val sage:String = "0",
        val img: String = "",
        val ext: String = "",
        val title: String = "",
        val resto: String, // the parent id that the hit replys to
        val userid: String,
        val email: String,
        val content: String
    ) {
        var page:Int = 0 // to be set when data comes back
        fun getImgUrl(): String = img + ext
        fun getPostId(): String = if (resto == "0") id else resto
    }
}