package com.laotoua.dawnislandk.screens

import androidx.lifecycle.*
import com.laotoua.dawnislandk.data.local.dao.PostHistoryDao
import com.laotoua.dawnislandk.data.local.entity.Forum
import com.laotoua.dawnislandk.data.local.entity.PostHistory
import com.laotoua.dawnislandk.data.remote.APIMessageResponse
import com.laotoua.dawnislandk.data.remote.NMBServiceClient
import com.laotoua.dawnislandk.data.repository.CommunityRepository
import com.laotoua.dawnislandk.util.EventPayload
import com.laotoua.dawnislandk.util.SingleLiveEvent
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import java.util.*
import javax.inject.Inject

class SharedViewModel @Inject constructor(
    private val webNMBServiceClient: NMBServiceClient,
    private val postHistoryDao: PostHistoryDao,
    private val communityRepository: CommunityRepository

) :
    ViewModel() {
    val communityList get() = communityRepository.communityList
    val reedPictureUrl: LiveData<String> get() = communityRepository.reedPictureUrl
    val communityListLoadingStatus: LiveData<SingleLiveEvent<EventPayload<Nothing>>> =
        communityRepository.loadingStatus

    private var _selectedForumId = MutableLiveData<String>()
    val selectedForumId: LiveData<String> get() = _selectedForumId
    private var _selectedPostId = MutableLiveData<String>()
    val selectedPostId: LiveData<String> get() = _selectedPostId
    private var _selectedPostFid: String = "-1"
    val selectedPostFid get() = _selectedPostFid

    private val forumNameMapping = Transformations.map(communityList) { list ->
        val flattenForums = list.flatMap { it.forums }
        // TODO: set default forum
        _selectedForumId.value = flattenForums.first().id
        toolbarTitle = flattenForums.first().name

        flattenForums.associateBy(
            keySelector = { it.id },
            valueTransform = { it.name })

    }

    private val forumMsgMapping = Transformations.map(communityList) { list ->
        val flattenForums = list.flatMap { it.forums }
        flattenForums.associateBy(keySelector = { it.id },
            valueTransform = { it.msg })
    }

    private lateinit var loadingBible: List<String>

    private var toolbarTitle = "A岛"


    init {
        getRandomReedPicture()
    }

    fun forumRefresh() {
        viewModelScope.launch { communityRepository.refresh() }
    }

    fun getRandomReedPicture() {
        viewModelScope.launch { communityRepository.getRandomReedPicture() }
    }

    fun setForum(f: Forum) {
        Timber.d("Setting forum to id: ${f.id}")
        toolbarTitle = forumNameMapping.value?.get(f.id) ?: ""
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

    fun getForumNameMapping(): Map<String, String> = forumNameMapping.value ?: emptyMap()

    fun getForumMsg(id: String): String = forumMsgMapping.value?.get(id) ?: ""

    fun getForumDisplayName(id: String): String = forumNameMapping.value?.get(id) ?: ""

    fun getSelectedPostForumName(): String = getForumDisplayName(_selectedPostFid)

    fun getToolbarTitle(): String = toolbarTitle

    fun getForumIdByName(name: String): String {
        return forumNameMapping.value!!.filterValues { it == name }.keys.first()
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

    fun savePost(
        postCookieName: String,
        postTargetId: String, // do not have this when sending a newPost
        postTargetPage: Int,
        postTargetFid: String,
        newPost: Boolean,// false if replying
        imgPath: String,
        content: String //content
    ) {
        viewModelScope.launch {
            postHistoryDao.insertPostHistory(
                PostHistory(
                    null,
                    postCookieName,
                    postTargetId,
                    postTargetPage,
                    postTargetFid,
                    newPost,
                    imgPath,
                    content,
                    Date().time
                )
            )
        }
    }

}