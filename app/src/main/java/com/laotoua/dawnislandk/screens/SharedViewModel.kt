package com.laotoua.dawnislandk.screens

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.laotoua.dawnislandk.data.local.Forum
import com.laotoua.dawnislandk.data.remote.APIMessageResponse
import com.laotoua.dawnislandk.data.remote.NMBServiceClient
import timber.log.Timber
import java.io.File
import javax.inject.Inject

class SharedViewModel @Inject constructor(private val webNMBServiceClient: NMBServiceClient) :
    ViewModel() {
    private var _selectedForumId = MutableLiveData<String>()
    val selectedForumId: LiveData<String> get() = _selectedForumId
    private var _selectedThreadId = MutableLiveData<String>()
    val selectedThreadId: LiveData<String> get() = _selectedThreadId
    private var _selectedThreadFid: String = "-1"
    val selectedThreadFid get() = _selectedThreadFid

    private lateinit var forumNameMapping: Map<String, String>
    private lateinit var forumMsgMapping: Map<String, String>
    private lateinit var loadingBible: List<String>

    private var toolbarTitle = "A岛"

    fun setForum(f: Forum) {
        Timber.d("Setting forum to id: ${f.id}")
        toolbarTitle = forumNameMapping[f.id] ?: ""
        _selectedForumId.value = f.id
    }

    fun setThread(id: String, fid: String? = null) {
        Timber.d("Setting thread to $id and fid to $fid")
        fid?.let { _selectedThreadFid = it }
        _selectedThreadId.value = id
    }

    fun setThreadFid(fid: String) {
        Timber.d("Setting missing fid to $fid for thread $selectedThreadId")
        _selectedThreadFid = fid
    }

    fun setForumMappings(list: List<Forum>) {
        forumNameMapping = list.associateBy(
            keySelector = { it.id },
            valueTransform = { it.name })

        forumMsgMapping = list.associateBy(keySelector = { it.id },
            valueTransform = { it.msg })
    }

    fun setLuweiLoadingBible(bible: List<String>) {
        loadingBible = bible
    }

    fun getRandomLoadingBible(): String =
        if (this::loadingBible.isInitialized) loadingBible.random()
        else "正在加载中..."

    fun getForumNameMapping(): Map<String, String> = forumNameMapping

    fun getForumMsg(id: String): String = forumMsgMapping[id] ?: ""

    fun getForumDisplayName(id: String): String = forumNameMapping[id] ?: ""

    fun getSelectedThreadForumName(): String = getForumDisplayName(_selectedThreadFid)

    fun getToolbarTitle(): String = toolbarTitle

    fun getForumIdByName(name: String): String {
        return forumNameMapping.filterValues { it == name }.keys.first()
    }

    suspend fun sendPost(
        newPost: Boolean,
        targetId: String, name: String?,
        email: String?, title: String?,
        content: String?, waterMark: String?,
        imageFile: File?, userHash: String
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
            userHash
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

}