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

package com.laotoua.dawnislandk.screens

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laotoua.dawnislandk.data.local.dao.CommentDao
import com.laotoua.dawnislandk.data.local.dao.PostDao
import com.laotoua.dawnislandk.data.local.dao.PostHistoryDao
import com.laotoua.dawnislandk.data.local.entity.Community
import com.laotoua.dawnislandk.data.local.entity.Post
import com.laotoua.dawnislandk.data.local.entity.PostHistory
import com.laotoua.dawnislandk.data.remote.APIDataResponse
import com.laotoua.dawnislandk.data.remote.APIMessageResponse
import com.laotoua.dawnislandk.data.remote.NMBServiceClient
import com.laotoua.dawnislandk.data.repository.CommunityRepository
import com.laotoua.dawnislandk.screens.util.ContentTransformation
import com.laotoua.dawnislandk.util.SingleLiveEvent
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import java.util.*
import javax.inject.Inject

class SharedViewModel @Inject constructor(
    private val webNMBServiceClient: NMBServiceClient,
    private val postDao: PostDao,
    private val commentDao: CommentDao,
    private val postHistoryDao: PostHistoryDao,
    private val communityRepository: CommunityRepository
) : ViewModel() {

    val communityList = communityRepository.communityList

    val reedPictureUrl = MutableLiveData<String>()
    private var _selectedForumId = MutableLiveData<String>()
    val selectedForumId: LiveData<String> get() = _selectedForumId

    private val _savePostStatus = MutableLiveData<SingleLiveEvent<Boolean>>()
    val savePostStatus: LiveData<SingleLiveEvent<Boolean>> get() = _savePostStatus

    private lateinit var loadingBible: List<String>

    var forumNameMapping = mapOf<String, String>()
        private set

    private var forumMsgMapping = mapOf<String, String>()


    init {
        getRandomReedPicture()
    }

    fun forumRefresh() {
        viewModelScope.launch {
            communityRepository.refresh()
        }
    }

    fun setForumMappings(list: List<Community>) {
        val flatten = list.flatMap { it.forums }
        forumNameMapping =
            flatten.associateBy(keySelector = { it.id }, valueTransform = { it.name })
        forumMsgMapping = flatten.associateBy(keySelector = { it.id }, valueTransform = { it.msg })
    }

    fun getRandomReedPicture() {
        viewModelScope.launch {
            webNMBServiceClient.getRandomReedPicture().run {
                if (this is APIDataResponse.Success) {
                    reedPictureUrl.postValue(data!!)
                }
            }
        }
    }

    fun setForumId(fid: String) {
        Timber.d("Setting forum to id: $fid")
        _selectedForumId.value = fid
    }

    fun setLuweiLoadingBible(bible: List<String>) {
        loadingBible = bible
    }

    fun getRandomLoadingBible(): String =
        if (this::loadingBible.isInitialized) loadingBible.random()
        else "正在加载中..."

    fun getForumMsg(id: String): String = if (id.isBlank()) "" else forumMsgMapping[id] ?: ""

    fun getForumDisplayName(fid: String): String =
        if (fid.isBlank()) "" else forumNameMapping[fid] ?: "A岛"

    fun getSelectedPostForumName(fid: String): String = getForumDisplayName(fid)

    fun getForumIdByName(name: String): String {
        return forumNameMapping.filterValues { it == name }.keys.firstOrNull() ?: ""
    }

    suspend fun sendPost(
        newPost: Boolean,
        targetId: String, name: String?,
        email: String?, title: String?,
        content: String?, waterMark: String?,
        imageFile: File?, cookieHash: String
    ): String {
        return webNMBServiceClient.sendPost(
            newPost,
            targetId,
            name,
            email,
            title,
            content,
            waterMark,
            imageFile,
            cookieHash
        ).run {
            if (this is APIMessageResponse.Success) {
                if (messageType == APIMessageResponse.MessageType.String) {
                    message
                } else {
                    dom!!.getElementsByClass("system-message")
                        .first().children().not(".jump").text()
                }
            } else {
                Timber.e(message)
                message
            }
        }
    }

    fun searchAndSavePost(
        newPost: Boolean,// false if replying
        postTargetId: String, // equals postTargetFid when sending a new Post
        postTargetFid: String,
        postTargetPage: Int,
        cookieName: String,
        content: String
    ) {
        if (cookieName.isBlank()) {
            _savePostStatus.postValue(SingleLiveEvent.create(false))
            Timber.e("Trying to save a Post without cookieName")
            return
        }
        viewModelScope.launch {
            delay(3000L) // give some time the server to refresh
            val draft = PostHistory.Draft(
                newPost,
                postTargetId,
                postTargetFid,
                cookieName,
                content,
                Date().time
            )
            if (!newPost) searchCommentInPost(draft, postTargetPage, false)
            else searchPostInForum(draft, postTargetFid)
        }
    }

    private suspend fun searchPostInForum(draft: PostHistory.Draft, targetFid: String) {
        Timber.d("Searching new Post in the first page of forum $targetFid")
        var saved = false
        webNMBServiceClient.getPosts(targetFid, 1).run {
            if (this is APIDataResponse.Success) {
                for (post in data!!) {
                    // content may be formatted to html by server hence compared by unformatted string
                    val striped = ContentTransformation.htmlToSpanned(post.content).toString()
                    if (post.userid == draft.cookieName && striped == draft.content) {
                        // store server's copy
                        draft.content = post.content
                        postHistoryDao.insertPostHistory(
                            PostHistory(
                                post.id,
                                1,
                                post.img,
                                post.ext,
                                draft
                            )
                        )
                        saved = true
                        _savePostStatus.postValue(SingleLiveEvent.create(true))
                        Timber.d("Saved new post with id ${post.id}")
                        break
                    }
                }
                postDao.insertAll(data)
            }
            if (!saved) {
                _savePostStatus.postValue(SingleLiveEvent.create(false))
                Timber.e("Failed to save new post")
            }
        }
    }

    private suspend fun searchCommentInPost(
        draft: PostHistory.Draft,
        targetPage: Int,
        targetPageUpperBound: Boolean
    ) {
        if (targetPage < 1) {
            _savePostStatus.postValue(SingleLiveEvent.create(false))
            Timber.e("Did not find comment in all pages")
            return
        }
        Timber.d("Searching posted comment in ${draft.postTargetId} on page $targetPage")

        webNMBServiceClient.getComments(draft.postTargetId, targetPage).run {
            if (this is APIDataResponse.Success) {
                val maxPage = data!!.getMaxPage()
                if (targetPage != maxPage && !targetPageUpperBound) {
                    searchCommentInPost(draft, maxPage, true)
                } else {
                    postDao.insert(data)
                    extractCommentInPost(data, draft, targetPage, true)
                }
                commentDao.insertAllWithTimeStamp(data.comments)
            } else {
                Timber.e(message)
                _savePostStatus.postValue(SingleLiveEvent.create(false))
            }
        }
    }

    private suspend fun extractCommentInPost(
        data: Post,
        draft: PostHistory.Draft,
        targetPage: Int,
        targetPageUpperBound: Boolean
    ) {
        for (reply in data.comments.reversed()) {
            // content may be formatted to html by server hence compared by unformatted string
            val striped = ContentTransformation.htmlToSpanned(reply.content).toString()
            if (reply.userid == draft.cookieName && striped == draft.content) {
                // store server's copy
                draft.content = reply.content
                postHistoryDao.insertPostHistory(
                    PostHistory(
                        reply.id,
                        targetPage,
                        reply.img,
                        reply.ext,
                        draft
                    )
                )
                _savePostStatus.postValue(SingleLiveEvent.create(true))
                Timber.d("Saved posted comment with id ${reply.id}")
                return
            }
        }
        searchCommentInPost(draft, targetPage - 1, targetPageUpperBound)
    }
}