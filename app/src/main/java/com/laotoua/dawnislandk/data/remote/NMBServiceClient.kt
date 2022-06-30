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
import com.laotoua.dawnislandk.util.DawnConstants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import timber.log.Timber
import java.io.File
import javax.inject.Inject

class NMBServiceClient @Inject constructor(private val service: NMBService) {

    suspend fun getNMBSearch(
        query: String,
        page: Int = 1,
        userHash: String
    ): APIDataResponse<SearchResult> {
        Timber.d("Getting search result for $query on Page $page...")
        return APIDataResponse.create(
            service.getNMBSearch(query, page, userHash, DawnConstants.NMBXDHost),
            NMBJsonParser.SearchResultParser(query, page)
        )
    }

    suspend fun getPrivacyAgreement(): APIMessageResponse {
        return APIMessageResponse.create(service.getPrivacyAgreement())
    }

    suspend fun getChangeLog(): APIMessageResponse {
        return APIMessageResponse.create(service.getChangeLog())
    }

    suspend fun getRandomReedPicture(): APIDataResponse<String> {
        Timber.d("Getting Random Reed Picture...")
        return APIDataResponse.create(service.getRandomReedPicture(), NMBJsonParser.ReedRandomPictureParser())
    }

    suspend fun getLatestRelease(): APIDataResponse<Release> {
        Timber.d("Checking Latest Version...")
        return APIDataResponse.create(service.getLatestRelease(), NMBJsonParser.ReleaseParser())
    }

    suspend fun getNMBNotice(): APIDataResponse<NMBNotice> {
        Timber.i("Downloading Notice...")
        return APIDataResponse.create(service.getNMBNotice(), NMBJsonParser.NMBNoticeParser())
    }

    suspend fun getLuweiNotice(): APIDataResponse<LuweiNotice> {
        Timber.i("Downloading LuWeiNotice...")
        return APIDataResponse.create(service.getLuweiNotice(), NMBJsonParser.LuweiNoticeParser())
    }


    suspend fun getCommunities(): APIDataResponse<List<Community>> {
        Timber.i("Downloading Communities and Forums...")
        return APIDataResponse.create(service.getNMBForumList(), NMBJsonParser.CommunityParser())
    }

    suspend fun getTimeLines(): APIDataResponse<List<Timeline>> {
        Timber.i("Downloading Timeline Forums...")
        return APIDataResponse.create(service.getNMBTimelineList(), NMBJsonParser.TimelinesParser())
    }

    suspend fun getPosts(fid: String, page: Int, userHash: String? = DawnApp.applicationDataStore.firstCookieHash): APIDataResponse<List<Post>> {
        Timber.i("Downloading Posts on Forum $fid...")
        val call = if (fid.startsWith("-")) service.getNMBTimeLine(fid.substringAfter("-"), page, userHash)
        else service.getNMBPosts(fid, page, userHash)
        return APIDataResponse.create(call, NMBJsonParser.PostParser())
    }

    suspend fun getComments(
        id: String,
        page: Int,
        userHash: String? = DawnApp.applicationDataStore.firstCookieHash
    ): APIDataResponse<Post> {
        Timber.i("Downloading Comments on Post $id on Page $page...")
        return APIDataResponse.create(
            service.getNMBComments(id, page, userHash),
            NMBJsonParser.CommentParser()
        )
    }

    suspend fun getFeeds(uuid: String, page: Int): APIDataResponse<List<Feed.ServerFeed>> {
        Timber.i("Downloading Feeds on Page $page...")
        return APIDataResponse.create(service.getNMBFeeds(uuid, page), NMBJsonParser.FeedParser())
    }


    // Returns BlankDataResponse(not Error) when comment is deleted
    suspend fun getQuote(id: String): APIDataResponse<Comment> {
        Timber.i("Downloading Quote $id...")
        return APIDataResponse.create(service.getNMBQuote(id), NMBJsonParser.QuoteParser())
    }

    suspend fun addFeed(uuid: String, tid: String): APIMessageResponse {
        Timber.i("Adding Feed $tid...")
        return APIMessageResponse.create(service.addNMBFeed(uuid, tid))
    }

    suspend fun delFeed(uuid: String, tid: String): APIMessageResponse {
        Timber.i("Deleting Feed $tid...")
        return APIMessageResponse.create(service.delNMBFeed(uuid, tid))
    }

    // Note: userHash should be already converted to header style beforehand
    // i.e. "userHash=v%C6%CB...."
    suspend fun sendPost(
        newPost: Boolean,
        targetId: String, name: String?,
        email: String?, title: String?,
        content: String?, water: String?,
        image: File?, userHash: String
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
                    userHash
                )
            } else {
                service.postComment(
                    targetId.toRequestBody(), name?.toRequestBody(),
                    email?.toRequestBody(), title?.toRequestBody(),
                    content?.toRequestBody(), water?.toRequestBody(),
                    imagePart,
                    userHash
                )
            }
            APIMessageResponse.create(call)
        }
    }

    suspend fun getBackupDomains(): APIDataResponse<List<String>> {
        Timber.d("Checking backup domain...")
        return APIDataResponse.create(service.getNMBBackupDomains(), NMBJsonParser.BackupDomainParser())
    }
}