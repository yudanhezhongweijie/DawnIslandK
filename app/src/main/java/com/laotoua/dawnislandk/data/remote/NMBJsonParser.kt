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

import com.laotoua.dawnislandk.data.local.entity.*
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import org.json.JSONObject
import java.util.*


abstract class NMBJsonParser<T> {
    abstract fun parse(response: String): T

    companion object {
        private val moshi: Moshi = Moshi.Builder().build()
    }

    class ReleaseParser : NMBJsonParser<Release>() {
        override fun parse(response: String): Release {
            return JSONObject(response).run {
                Release(
                    1,
                    optString("tag_name"),
                    optString("html_url"),
                    optString("body"),
                    Date().time
                )
            }
        }
    }

    class NMBNoticeParser : NMBJsonParser<NMBNotice>() {
        override fun parse(response: String): NMBNotice {
            return moshi.adapter(NMBNotice::class.java).fromJson(response)!!
        }
    }

    class LuweiNoticeParser : NMBJsonParser<LuweiNotice>() {
        override fun parse(response: String): LuweiNotice {
            return moshi.adapter(LuweiNotice::class.java).fromJson(response)!!
        }
    }

    class CommunityParser : NMBJsonParser<List<Community>>() {
        override fun parse(response: String): List<Community> {
            return moshi.adapter<List<Community>>(
                Types.newParameterizedType(
                    List::class.java,
                    Community::class.java
                )
            ).fromJson(response)!!
        }
    }

    class PostParser : NMBJsonParser<List<Post>>() {
        override fun parse(response: String): List<Post> {
            return moshi.adapter<List<Post>>(
                Types.newParameterizedType(
                    List::class.java,
                    Post::class.java
                )
            ).fromJson(response)!!
        }
    }

    class CommentParser : NMBJsonParser<Post>() {
        override fun parse(response: String): Post {
            return moshi.adapter(Post::class.java).fromJson(response)!!
        }
    }

    class QuoteParser : NMBJsonParser<Comment>() {
        override fun parse(response: String): Comment {
            return moshi.adapter(Comment::class.java).fromJson(response)!!
        }
    }

    class SearchResultParser(val query:String, val page: Int) : NMBJsonParser<SearchResult>() {
        override fun parse(response: String): SearchResult {
            return JSONObject(response).run {
                getJSONObject("hits").run {
                    val count = optInt("total")
                    val hitsList = mutableListOf<SearchResult.Hit>()
                    optJSONArray("hits")?.run {
                        for (i in 0 until length()) {
                            val hitObject = getJSONObject(i)
                            val sourceObject = getJSONObject("_source")
                            val hit = SearchResult.Hit(
                                hitObject.optString("_id"),
                                sourceObject.optString("now"),
                                sourceObject.optString("time"),
                                sourceObject.optString("img"),
                                sourceObject.optString("ext"),
                                sourceObject.optString("title"),
                                sourceObject.optString("resto"), // the parent id that the hit replys to
                                sourceObject.optString("userid"),
                                sourceObject.optString("email"),
                                sourceObject.optString("content")
                            )
                            hitsList.add(hit)
                        }
                    }
                    SearchResult(
                        query,
                        count,
                        page,
                        hitsList
                    )
                }
            }
        }
    }

    class ReedRandomPictureParser : NMBJsonParser<String>() {
        override fun parse(response: String): String {
            return response
        }
    }
}