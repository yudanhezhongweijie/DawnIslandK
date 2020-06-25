package com.laotoua.dawnislandk.screens

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laotoua.dawnislandk.data.local.dao.CommentDao
import com.laotoua.dawnislandk.data.local.dao.PostDao
import com.laotoua.dawnislandk.data.local.dao.PostHistoryDao
import com.laotoua.dawnislandk.data.local.entity.Community
import com.laotoua.dawnislandk.data.local.entity.Forum
import com.laotoua.dawnislandk.data.local.entity.Post
import com.laotoua.dawnislandk.data.local.entity.PostHistory
import com.laotoua.dawnislandk.data.remote.APIDataResponse
import com.laotoua.dawnislandk.data.remote.APIMessageResponse
import com.laotoua.dawnislandk.data.remote.NMBServiceClient
import com.laotoua.dawnislandk.data.repository.CommunityRepository
import com.laotoua.dawnislandk.util.EventPayload
import com.laotoua.dawnislandk.util.LoadingStatus
import com.laotoua.dawnislandk.util.SingleLiveEvent
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
) :
    ViewModel() {
    val communityList get() = communityRepository.communityList
    val reedPictureUrl = MutableLiveData<String>()
    val communityListLoadingStatus: LiveData<SingleLiveEvent<EventPayload<Nothing>>> =
        communityRepository.loadingStatus

    private var _selectedForumId = MutableLiveData<String>()
    val selectedForumId: LiveData<String> get() = _selectedForumId
    private var _selectedPostId = MutableLiveData<String>()
    val selectedPostId: LiveData<String> get() = _selectedPostId
    private var _selectedPostFid: String = "-1"
    val selectedPostFid get() = _selectedPostFid

    private val _savePostStatus = MutableLiveData<SingleLiveEvent<EventPayload<Nothing>>>()
    val savePostStatus: LiveData<SingleLiveEvent<EventPayload<Nothing>>> get() = _savePostStatus

    private lateinit var loadingBible: List<String>

    private var toolbarTitle = "A岛"

    var forumNameMapping = mapOf<String, String>()
        private set

    var forumMsgMapping = mapOf<String, String>()
        private set


    init {
        getRandomReedPicture()
    }

    fun forumRefresh() {
        viewModelScope.launch { communityRepository.refresh() }
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
                if (this is APIDataResponse.APISuccessDataResponse) {
                    reedPictureUrl.postValue(data)
                }
            }
        }
    }

    fun setForum(f: Forum) {
        Timber.d("Setting forum to id: ${f.id}")
        toolbarTitle = forumNameMapping[f.id] ?: ""
        _selectedForumId.value = f.id
    }

    fun setPost(id: String, fid: String? = null) {
        Timber.d("Setting thread to $id and fid to $fid")
        fid?.let { _selectedPostFid = it }
        _selectedPostId.value = id
    }

    fun setPostFid(fid: String) {
        Timber.d("Setting missing fid to $fid for thread $selectedPostId")
        _selectedPostFid = fid
    }

    fun setLuweiLoadingBible(bible: List<String>) {
        loadingBible = bible
    }

    fun getRandomLoadingBible(): String =
        if (this::loadingBible.isInitialized) loadingBible.random()
        else "正在加载中..."

    fun getForumMsg(id: String): String = forumMsgMapping[id] ?: ""

    fun getForumDisplayName(id: String): String = forumNameMapping[id] ?: ""

    fun getSelectedPostForumName(): String = getForumDisplayName(_selectedPostFid)

    fun getToolbarTitle(): String = toolbarTitle

    fun getForumIdByName(name: String): String {
        return forumNameMapping.filterValues { it == name }.keys.first()
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
            if (this is APIMessageResponse.APISuccessMessageResponse) {
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
        cookieName: String,
        postTargetId: String, // do not have this when sending a newPost
        postTargetPage: Int,
        postTargetFid: String,
        newPost: Boolean,// false if replying
        content: String //content
    ) {
        viewModelScope.launch {
            val draft = PostHistory.Draft(
                cookieName,
                postTargetId,
                postTargetFid,
                newPost,
                content,
                Date().time
            )
            if (!newPost) searchCommentInPost(draft, postTargetPage, false)
            else TODO()
        }
    }

    private suspend fun searchCommentInPost(
        draft: PostHistory.Draft,
        targetPage: Int,
        targetPageUpperBound: Boolean
    ) {
        if (targetPage < 1) {
            _savePostStatus.postValue(
                SingleLiveEvent.create(
                    LoadingStatus.FAILED,
                    "无法保存发言历史...请联系作者\n"
                )
            )
            return
        }
        Timber.d("Searching posted comment in ${draft.postTargetId} on page $targetPage")

        webNMBServiceClient.getComments(draft.postTargetId, targetPage).run {
            if (this is APIDataResponse.APISuccessDataResponse) {
                val maxPage = data.getMaxPage()
                if (targetPage != maxPage && !targetPageUpperBound) {
                    searchCommentInPost(draft, maxPage, true)
                } else {
                    postDao.insert(data)
                    extractCommentInPost(data, draft, maxPage, true)
                }
                commentDao.insertAllWithTimeStamp(data.comments)
            } else {
                Timber.e(message)
                _savePostStatus.postValue(
                    SingleLiveEvent.create(
                        LoadingStatus.FAILED,
                        "无法保存发言历史...\n$message"
                    )
                )
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
            if (reply.userid == draft.cookieName && reply.content == draft.content) {
                postHistoryDao.insertPostHistory(
                    PostHistory(
                        reply.id,
                        targetPage,
                        reply.img,
                        reply.ext,
                        draft
                    )
                )
                _savePostStatus.postValue(SingleLiveEvent.create(LoadingStatus.SUCCESS))
                Timber.d("Saved posted comment with id ${reply.id}")
                return
            }
        }
        searchCommentInPost(draft, targetPage - 1, targetPageUpperBound)
    }
}