package com.laotoua.dawnislandk.screens.profile

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laotoua.dawnislandk.DawnApp.Companion.applicationDataStore
import com.laotoua.dawnislandk.data.local.dao.PostDao
import com.laotoua.dawnislandk.data.local.entity.Cookie
import com.laotoua.dawnislandk.data.local.entity.Post
import com.laotoua.dawnislandk.data.remote.APIDataResponse
import com.laotoua.dawnislandk.data.remote.APIMessageResponse
import com.laotoua.dawnislandk.data.remote.NMBServiceClient
import com.laotoua.dawnislandk.util.EventPayload
import com.laotoua.dawnislandk.util.LoadingStatus
import com.laotoua.dawnislandk.util.SingleLiveEvent
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import kotlin.math.ceil
import kotlin.random.Random

class ProfileViewModel @Inject constructor(
    private val webNMBServiceClient: NMBServiceClient,
    private val postDao: PostDao
) :
    ViewModel() {
    private val _cookies = MutableLiveData<List<Cookie>>(applicationDataStore.cookies)
    val cookies: LiveData<List<Cookie>> get() = _cookies

    private val _loadingStatus = MutableLiveData<SingleLiveEvent<EventPayload<Nothing>>>()
    val loadingStatus: LiveData<SingleLiveEvent<EventPayload<Nothing>>> get() = _loadingStatus

    private val cookieNameTestPostId = "26165309"
    private val charPool = "AaBbCcDdEeFfGgHhIiJjKkLlMmNnOoPpQqRrSsTtUuVvWwXxYyZz1234567890"
    private val randomTestLength = 40

    fun getDefaultCookieName(cookieHash: String) {
        val randomString = (1..randomTestLength)
            .map { Random.nextInt(0, charPool.length) }
            .map(charPool::get)
            .joinToString("")
        viewModelScope.launch {
            var targetPage = 1
            postDao.findPostByIdSync(cookieNameTestPostId)?.let {
                targetPage = getMaxPage(it.replyCount)
            }

            _loadingStatus.postValue(SingleLiveEvent.create(LoadingStatus.LOADING))
            val message = sendNameTestComment(cookieHash, randomString)
            if (message.substring(0, 2) != ":)") {
                _loadingStatus.postValue(SingleLiveEvent.create(LoadingStatus.FAILED, message))
                return@launch
            }
            findNameInComments(cookieHash, randomString, targetPage, false)
        }
    }

    private fun getMaxPage(replyCount: String): Int =
        if (replyCount.isBlank()) 1
        else ceil(replyCount.toDouble() / 19).toInt()


    private suspend fun sendNameTestComment(cookieHash: String, randomString: String): String {
        return webNMBServiceClient.sendPost(
            false,
            cookieNameTestPostId,
            null,
            null,
            null,
            randomString,
            null,
            null,
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

    private suspend fun findNameInComments(
        cookieHash: String,
        randomString: String,
        targetPage: Int,
        targetPageUpperBound: Boolean
    ) {
        if (targetPage < 1) {
            _loadingStatus.postValue(
                SingleLiveEvent.create(
                    LoadingStatus.FAILED,
                    "无法获取默认饼干名...请联系作者\n"
                )
            )
            return
        }
        webNMBServiceClient.getComments(cookieHash, cookieNameTestPostId, targetPage).run {
            if (this is APIDataResponse.APISuccessDataResponse) {
                val maxPage = getMaxPage(data.replyCount)
                if (targetPage != maxPage && !targetPageUpperBound) {
                    findNameInComments(cookieHash, randomString, maxPage, true)
                } else {
                    postDao.insert(data)
                    extractCookieName(data, cookieHash, randomString, targetPage, true)
                }
            } else {
                Timber.e(message)
                _loadingStatus.postValue(
                    SingleLiveEvent.create(
                        LoadingStatus.FAILED,
                        "无法获取默认饼干名...\n$message"
                    )
                )
            }
        }
    }

    private suspend fun extractCookieName(
        data: Post,
        cookieHash: String,
        randomString: String,
        targetPage: Int,
        targetPageUpperBound: Boolean
    ) {
        for (reply in data.comments.reversed()) {
            if (reply.content == randomString) {
                addCookie(Cookie(cookieHash, reply.userid))
                _loadingStatus.postValue(SingleLiveEvent.create(LoadingStatus.SUCCESS))
                return
            }
        }
        findNameInComments(cookieHash, randomString, targetPage - 1, targetPageUpperBound)
    }

    fun addCookie(cookie: Cookie) {
        viewModelScope.launch {
            applicationDataStore.addCookie(cookie)
            _cookies.value = applicationDataStore.cookies
        }
    }

    fun deleteCookie(cookie: Cookie) {
        viewModelScope.launch {
            applicationDataStore.deleteCookies(cookie)
            _cookies.value = applicationDataStore.cookies
        }
    }
}