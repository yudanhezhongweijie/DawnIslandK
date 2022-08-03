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
import com.laotoua.dawnislandk.DawnApp
import com.laotoua.dawnislandk.data.local.dao.*
import com.laotoua.dawnislandk.data.local.entity.*
import com.laotoua.dawnislandk.data.remote.APIDataResponse
import com.laotoua.dawnislandk.data.remote.APIMessageResponse
import com.laotoua.dawnislandk.data.remote.NMBServiceClient
import com.laotoua.dawnislandk.data.repository.CommunityRepository
import com.laotoua.dawnislandk.util.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import java.time.LocalDateTime
import javax.inject.Inject

class SharedViewModel @Inject constructor(
    private val webNMBServiceClient: NMBServiceClient,
    private val postDao: PostDao,
    private val commentDao: CommentDao,
    private val postHistoryDao: PostHistoryDao,
    private val feedDao: FeedDao,
    private val notificationDao: NotificationDao,
    private val emojiDao: EmojiDao,
    private val communityRepository: CommunityRepository
) : ViewModel() {


    val communityList: LiveData<DataResource<List<Community>>> = communityRepository.communityList
    val timelineList: LiveData<DataResource<List<Timeline>>> = communityRepository.timelineList

    val notifications: LiveData<Int> = notificationDao.getLiveUnreadNotificationsCount()

    val reedPictureUrl = MutableLiveData<String>()
    private var _selectedForumId = MutableLiveData<String>()
    val selectedForumId: LiveData<String> get() = _selectedForumId

    private val _savePostStatus = MutableLiveData<SingleLiveEvent<Boolean>>()
    val savePostStatus: LiveData<SingleLiveEvent<Boolean>> get() = _savePostStatus

    private lateinit var loadingBible: List<String>

    var forumNameMapping = mapOf<String, String>()
        private set

    private var forumMsgMapping = mapOf<String, String>()

    private var timelineNameMapping = mapOf<String, String>()
    private var timelineMsgMapping = mapOf<String, String>()

    var forceRefresh = false
    val hostChange: MutableLiveData<SingleLiveEvent<Boolean>> = MutableLiveData()

    val currentDomain: MutableLiveData<String> = MutableLiveData()

    fun refreshCommunitiesAndTimelines() {
        viewModelScope.launch {
            communityRepository.refreshCommunitiesAndTimelines()
        }
    }

    fun onNMBXD() {
        DawnApp.onDomain(DawnConstants.NMBXDDomain)
        currentDomain.postValue(DawnConstants.NMBXDDomain)
    }

    fun onTNMB() {
        DawnApp.onDomain(DawnConstants.TNMBDomain)
        currentDomain.postValue(DawnConstants.TNMBDomain)
    }

    init {
        getRandomReedPicture()
        if (DawnApp.applicationDataStore.getAutoUpdateFeed()) autoUpdateFeeds()
    }

    suspend fun getAllEmoji(): List<Emoji> {
        var res = emojiDao.getAllEmoji(DawnApp.applicationDataStore.getSortEmojiByLastUsedStatus())
        if (res.isEmpty()) {
            emojiDao.resetEmoji()
            res = emojiDao.getAllEmoji(DawnApp.applicationDataStore.getSortEmojiByLastUsedStatus())
        }
        return res
    }

    fun setLastUsedEmoji(emoji: Emoji) {
        if (DawnApp.applicationDataStore.getSortEmojiByLastUsedStatus()) {
            viewModelScope.launch {
                emojiDao.setLastUsedEmoji(emoji)
            }
        }
    }

    /** scan cache feed daily, update the most outdated feed
     *  updates 1 feed per 5 minute
     */
    private fun autoUpdateFeeds() {
        viewModelScope.launch {
            while (true) {
                Timber.d("Auto Update Feed is on. Looping...")
                val outDatedFeedAndPost = feedDao.findMostOutdatedFeedAndPost(LocalDateTime.now()) ?: break
                Timber.d("Found outdated Feed ${outDatedFeedAndPost.feed.postId}. Updating...")
                updateOutdatedFeedAndPost(outDatedFeedAndPost)
                delay(300000L)
            }
        }
    }

    // update Post, Comment, Notification, Feed
    private suspend fun updateOutdatedFeedAndPost(outDatedFeedAndPost: FeedAndPost) {
        val id: String = outDatedFeedAndPost.post?.id ?: outDatedFeedAndPost.feed.postId
        val page: Int = outDatedFeedAndPost.post?.getMaxPage() ?: 1
        webNMBServiceClient.getComments(id, page).run {
            if (this.status == LoadingStatus.SUCCESS) {
                if (data == null) {
                    Timber.e("Server returns no data but status is success")
                    return@run
                }
                // save Post & Comment
                postDao.insertWithTimeStamp(data)
                val noAd = data.comments.filter { it.isNotAd() }
                noAd.map { it.page = page; it.parentId = id }
                commentDao.insertAllWithTimeStamp(noAd)

                // update notification, only if there are new replies
                val replyCount: Int = try {
                    data.replyCount.toInt() - (outDatedFeedAndPost.post?.replyCount?.toInt()
                        ?: data.replyCount.toInt())
                } catch (e: Exception) {
                    Timber.e("error in replyCount conversion $e")
                    0
                }
                if (replyCount > 0) {
                    Timber.d("Found feed ${data.id} with new reply. Updating...")
                    val notification = Notification.makeNotification(data.id, data.fid, replyCount)
                    notificationDao.insertOrUpdateNotification(notification)
                }

                // update feed
                outDatedFeedAndPost.feed.let {
                    it.lastUpdatedAt = LocalDateTime.now()
                    feedDao.insertFeed(it)
                }
            }
        }
    }

    fun setForumMappings(list: List<Community>) {
        val flatten = list.filterNot { it.isCommonForums() || it.isCommonPosts() }.map { it.forums }.flatten()
        forumNameMapping = flatten.associateBy(keySelector = { it.id }, valueTransform = { it.name })
        forumMsgMapping = flatten.associateBy(keySelector = { it.id }, valueTransform = { it.msg })
    }

    fun setTimelineMappings(list: List<Timeline>) {
        timelineNameMapping = list.associateBy({ it.id }, { it.name })
        timelineMsgMapping = list.associateBy({ it.id }, { it.notice })
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

    // timeline has `-` prefix, otherwise is just regular forum
    fun setForumId(fid: String, refresh: Boolean = false) {
        Timber.d("Setting forum to id: $fid")
        forceRefresh = refresh || _selectedForumId.value == fid
        _selectedForumId.value = fid
    }

    fun setLuweiLoadingBible(bible: List<String>) {
        loadingBible = bible
    }

    fun getRandomLoadingBible(): String = if (this::loadingBible.isInitialized) loadingBible.random() else "正在加载中..."

    fun getForumOrTimelineMsg(fid: String): String {
        var msg = forumMsgMapping[fid]
        if (msg.isNullOrBlank() && fid.startsWith("-")) {
            val id = fid.substringAfter("-")
            if (id.isNotBlank()) msg = timelineMsgMapping[id]
        }
        if (msg.isNullOrBlank()) msg = ""
        return msg
    }

    fun getForumOrTimelineDisplayName(fid: String): String {
        var name = forumNameMapping[fid]
        if (name.isNullOrBlank() && fid.startsWith("-")) {
            val id = fid.substringAfter("-")
            if (id.isNotBlank()) name = timelineNameMapping[id]
        }
        if (name.isNullOrBlank()) name = "A岛"
        return name
    }

    fun getSelectedPostForumName(fid: String): String = getForumOrTimelineDisplayName(fid)

    fun getForumIdByName(name: String): String = forumNameMapping.filterValues { it == name }.keys.firstOrNull() ?: ""

    suspend fun sendPost(
        newPost: Boolean,
        targetId: String,
        name: String?,
        email: String?,
        title: String?,
        content: String?,
        waterMark: String?,
        imageFile: File?,
        cookieHash: String
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
                    dom!!.getElementsByClass("system-message").first().children().not(".jump").text()
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
            val draft = PostHistory.Draft(newPost, postTargetId, postTargetFid, cookieName, content, LocalDateTime.now())
            if (!newPost) searchCommentInPost(draft, postTargetPage, false)
            else searchPostInForum(draft, postTargetFid, 1)
        }
    }

    // Search up to last 3 pages
    private suspend fun searchPostInForum(draft: PostHistory.Draft, targetFid: String, page: Int) {
        if (page > 3) {
            Timber.e("Failed to save new post after searching more than $page pages in forum $targetFid")
            return
        }
        Timber.d("Searching new Post in the $page page of forum $targetFid")
        var saved = false
        webNMBServiceClient.getPosts(targetFid, page).run {
            if (this is APIDataResponse.Success) {
                for (post in data!!) {
                    if (post.userHash == draft.cookieName) {
                        // store server's copy
                        draft.content = post.content
                        postHistoryDao.insertPostHistory(PostHistory(post.id, 1, post.img, post.ext, DawnApp.currentDomain, draft))
                        saved = true
                        Timber.d("Saved new post with id ${post.id}")
                    }
                }
                postDao.insertAll(data)
            }
            _savePostStatus.postValue(SingleLiveEvent.create(saved))
            if (!saved) searchPostInForum(draft, targetFid, page + 1)

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
        var saved = false
        for (reply in data.comments) {
            // store every server message as long as name match
            if (reply.userHash == draft.cookieName) {
                draft.content = reply.content
                saved = true
                postHistoryDao.insertPostHistory(PostHistory(reply.id, targetPage, reply.img, reply.ext, DawnApp.currentDomain, draft))
                _savePostStatus.postValue(SingleLiveEvent.create(true))
                Timber.d("Saved posted comment with id ${reply.id}")
            }
        }
        if (!saved) searchCommentInPost(draft, targetPage - 1, targetPageUpperBound)
    }

    fun saveCommonCommunity(commonCommunity: Community) {
        viewModelScope.launch {
            communityRepository.saveCommonCommunity(commonCommunity)
        }
    }

    suspend fun getLatestPostId(): Pair<String, LocalDateTime> {
        var id = "0"
        var time = ""
        webNMBServiceClient.getPosts(DawnConstants.TIMELINE_COMMUNITY_ID, 1).run {
            if (this is APIDataResponse.Success) {
                data?.map { post ->
                    if (post.id > id) {
                        id = post.id
                        time = post.now
                    }
                    post.comments.map { comment ->
                        if (comment.id > id) {
                            id = comment.id
                            time = comment.now
                        }
                    }
                }
            } else {
                Timber.e(message)
            }
        }
        return Pair(
            if (id == "0") {
                "没有读取到串号。。"
            } else id, ReadableTime.serverTimeStringToLocalJavaTime(time)
        )

    }
}