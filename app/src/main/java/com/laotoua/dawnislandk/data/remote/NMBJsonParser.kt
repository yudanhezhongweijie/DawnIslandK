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
import org.jsoup.Jsoup
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

    class FeedParser : NMBJsonParser<List<Feed.ServerFeed>>() {
        override fun parse(response: String): List<Feed.ServerFeed> {
            return moshi.adapter<List<Feed.ServerFeed>>(
                Types.newParameterizedType(
                    List::class.java,
                    Feed.ServerFeed::class.java
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
            val thread = Jsoup.parse(response).getElementsByClass("h-threads-item").first()
            val id = thread.getElementsByClass("h-threads-item-reply h-threads-item-ref").first()
                .attr("data-threads-id")
            if (id.isNullOrBlank()) throw Exception("无法获取引用")
            val title = thread.getElementsByClass("h-threads-info-title").first().text()
            // key is email but actual content is name???
            val name = thread.getElementsByClass("h-threads-info-email").first().text()
            val now = thread.getElementsByClass("h-threads-info-createdat").first().text()
            val imageFullPath =
                thread.getElementsByClass("h-threads-img-a").firstOrNull()?.attr("href")
                    ?.substringAfter("image/") ?: ""
            val splitter = imageFullPath.indexOf(".")
            val img = if (splitter > 0) imageFullPath.substring(0, splitter) else ""
            val ext = if (splitter > 0) imageFullPath.substring(splitter) else ""
            val uid = thread.getElementsByClass("h-threads-info-uid").first()
            val uidText = uid.text()
            val userid = if (uidText.startsWith("ID:")) uidText.substring(3) else uidText
            val admin = if (uid.childNodeSize() > 1) "1" else "0"
            val href = thread.getElementsByClass("h-threads-info-id").first().attr("href")
            val endIndex = href.indexOf("?")
            val parentId = if (endIndex < 0) href.substring(3) else href.substring(3, endIndex)
            val content = thread.getElementsByClass("h-threads-content").first().html()
            return Comment(
                id,
                userid,
                name,
                "",
                admin,
                "",
                title,
                "",
                now,
                content,
                img,
                ext,
                1,
                parentId
            )
        }
    }

    class SearchResultParser(val query: String, val page: Int) : NMBJsonParser<SearchResult>() {
        override fun parse(response: String): SearchResult {
            return JSONObject(response).run {
                getJSONObject("hits").run {
                    val count = optInt("total")
                    val hitsList = mutableListOf<SearchResult.Hit>()
                    optJSONArray("hits")?.run {
                        for (i in 0 until length()) {
                            val hitObject = getJSONObject(i)
                            val sourceObject = hitObject.getJSONObject("_source")
                            val hit = SearchResult.Hit(
                                hitObject.optString("_id"),
                                sourceObject.optString("now"),
                                sourceObject.optString("time"),
                                sourceObject.optString("sage", "0"),
                                sourceObject.optString("img"),
                                sourceObject.optString("ext"),
                                sourceObject.optString("title"),
                                sourceObject.optString("resto"),
                                sourceObject.optString("userid"),
                                sourceObject.optString("email"),
                                sourceObject.optString("content")
                            )
                            hit.page = page
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