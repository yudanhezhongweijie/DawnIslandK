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

import com.laotoua.dawnislandk.DawnApp
import com.laotoua.dawnislandk.data.local.entity.*
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import timber.log.Timber
import java.io.File
import java.util.*
import javax.inject.Inject

class NMBServiceClient @Inject constructor(private val service: NMBService) {

    private val moshi: Moshi = Moshi.Builder().build()

    private val parseLatestRelease: (String) -> Release = { response ->
        JSONObject(response).run {
            Release(
                1,
                optString("tag_name"),
                optString("html_url"),
                optString("body"),
                Date().time
            )
        }
    }

    private val parseNMBNotice: (String) -> NMBNotice = { response ->
        moshi.adapter(NMBNotice::class.java).fromJson(response)!!
    }

    private val parseLuweiNotice: (String) -> LuweiNotice = { response ->
        moshi.adapter(LuweiNotice::class.java).fromJson(response)!!
    }

    private val parseCommunities: (String) -> List<Community> = { response ->
        moshi.adapter<List<Community>>(
            Types.newParameterizedType(
                List::class.java,
                Community::class.java
            )
        ).fromJson(response)!!
    }

    private val parsePosts: (String) -> List<Post> = { response ->
        moshi.adapter<List<Post>>(
            Types.newParameterizedType(
                List::class.java,
                Post::class.java
            )
        ).fromJson(response)!!
    }

    private val parseComments: (String) -> Post = { response ->
        moshi.adapter(Post::class.java).fromJson(response)!!
    }

    private val parseQuote: (String) -> Comment = { response ->
        moshi.adapter(Comment::class.java).fromJson(response)!!
    }

    private val parseSearchResult: (String) -> SearchResult = { response ->
        JSONObject(response).run {
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
                            sourceObject.optLong("time"),
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
                    "",
                    count,
                    1,
                    hitsList
                )
            }
        }
    }

    suspend fun getNMBSearch(
        query: String,
        page: Int = 1
    ): APIDataResponse<SearchResult> {
        Timber.d("Getting search result for $query on Page $page...")
        return APIDataResponse.create(service.getNMBSearch(query, page), parseSearchResult)
    }

    suspend fun getPrivacyAgreement(): APIMessageResponse {
        return APIMessageResponse.create(service.getPrivacyAgreement())
    }

    suspend fun getRandomReedPicture(): APIDataResponse<String> {
        Timber.d("Getting Random Reed Picture...")
        return APIDataResponse.create(service.getRandomReedPicture()) { it }
    }

    suspend fun getLatestRelease(): APIDataResponse<Release> {
        Timber.d("Checking Latest Version...")
        return APIDataResponse.create(service.getLatestRelease(), parseLatestRelease)
    }

    suspend fun getNMBNotice(): APIDataResponse<NMBNotice> {
        Timber.i("Downloading Notice...")
        return APIDataResponse.create(service.getNMBNotice(), parseNMBNotice)
    }

    suspend fun getLuweiNotice(): APIDataResponse<LuweiNotice> {
        Timber.i("Downloading LuWeiNotice...")
        return APIDataResponse.create(service.getLuweiNotice(), parseLuweiNotice)
    }


    suspend fun getCommunities(): APIDataResponse<List<Community>> {
        Timber.i("Downloading Communities and Forums...")
        return APIDataResponse.create(service.getNMBForumList(), parseCommunities)
    }

    suspend fun getPosts(fid: String, page: Int): APIDataResponse<List<Post>> {
        Timber.i("Downloading Posts on Forum $fid...")
        val call =
            if (fid == "-1") service.getNMBTimeLine(page)
            else service.getNMBPosts(fid, page)
        return APIDataResponse.create(call, parsePosts)
    }

    suspend fun getComments(
        id: String,
        page: Int,
        userhash: String? = DawnApp.applicationDataStore.firstCookieHash
    ): APIDataResponse<Post> {
        Timber.i("Downloading Comments on Post $id on Page $page...")
        val hash = userhash?.let { "userhash=$it" }
        return APIDataResponse.create(service.getNMBComments(hash, id, page), parseComments)
    }

    suspend fun getFeeds(uuid: String, page: Int): APIDataResponse<List<Post>> {
        Timber.i("Downloading Feeds on Page $page...")
        return APIDataResponse.create(service.getNMBFeeds(uuid, page), parsePosts)
    }

    suspend fun getQuote(id: String): APIDataResponse<Comment> {
        Timber.i("Downloading Quote $id...")
        return APIDataResponse.create(service.getNMBQuote(id), parseQuote)
    }

    suspend fun addFeed(uuid: String, tid: String): APIMessageResponse {
        Timber.i("Adding Feed $tid...")
        return APIMessageResponse.create(service.addNMBFeed(uuid, tid))
    }

    suspend fun delFeed(uuid: String, tid: String): APIMessageResponse {
        Timber.i("Deleting Feed $tid...")
        return APIMessageResponse.create(service.delNMBFeed(uuid, tid))
    }

    suspend fun sendPost(
        newPost: Boolean,
        targetId: String, name: String?,
        email: String?, title: String?,
        content: String?, water: String?,
        image: File?, userhash: String
    ): APIMessageResponse {
        return withContext(Dispatchers.IO) {
            Timber.d("Posting to $targetId...")
            var imagePart: MultipartBody.Part? = null
            image?.run {
                asRequestBody(("image/${image.extension}").toMediaTypeOrNull()).run {
                    imagePart = MultipartBody.Part.createFormData("image", image.name, this)
                }
            }
            val call = if (newPost) {
                service.postNewPost(
                    targetId.toRequestBody(), name?.toRequestBody(),
                    email?.toRequestBody(), title?.toRequestBody(),
                    content?.toRequestBody(), water?.toRequestBody(),
                    imagePart,
                    "userhash=$userhash"
                )
            } else {
                service.postComment(
                    targetId.toRequestBody(), name?.toRequestBody(),
                    email?.toRequestBody(), title?.toRequestBody(),
                    content?.toRequestBody(), water?.toRequestBody(),
                    imagePart,
                    "userhash=$userhash"
                )
            }
            APIMessageResponse.create(call)
        }
    }


}