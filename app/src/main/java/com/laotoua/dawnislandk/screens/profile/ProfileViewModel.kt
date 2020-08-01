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


    fun updateCookie(cookie: Cookie) {
        viewModelScope.launch {
            applicationDataStore.addCookie(cookie)
            _cookies.value = applicationDataStore.cookies
        }
    }

    fun addNewCookie(cookieHash: String, cookieDisplayName: String? = null) {
        if (cookieHash.isBlank()) {
            Timber.e("Trying to add empty cookie")
            return
        }
        viewModelScope.launch {
            _loadingStatus.postValue(SingleLiveEvent.create(LoadingStatus.LOADING))
            val cookieName = getDefaultCookieName(cookieHash)
            if (cookieName.isNotBlank()) {
                applicationDataStore.addCookie(
                    Cookie(
                        cookieHash,
                        cookieName,
                        cookieDisplayName ?: cookieName
                    )
                )
                _cookies.value = applicationDataStore.cookies
                _loadingStatus.postValue(SingleLiveEvent.create(LoadingStatus.SUCCESS))
            }
        }
    }

    fun deleteCookie(cookie: Cookie) {
        viewModelScope.launch {
            applicationDataStore.deleteCookies(cookie)
            _cookies.value = applicationDataStore.cookies
        }
    }

    private suspend fun getDefaultCookieName(cookieHash: String): String {
        val randomString = (1..randomTestLength)
            .map { Random.nextInt(0, charPool.length) }
            .map(charPool::get)
            .joinToString("")
        var targetPage = 1
        postDao.findPostByIdSync(cookieNameTestPostId)?.let {
            targetPage = it.getMaxPage()
        }
        val message = sendNameTestComment(cookieHash, randomString)
        if (message.substring(0, 2) != ":)") {
            _loadingStatus.postValue(SingleLiveEvent.create(LoadingStatus.ERROR, message))
            return ""
        }
        return findNameInComments(cookieHash, randomString, targetPage, false)
    }

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
            "userhash=$cookieHash"
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

    private suspend fun findNameInComments(
        cookieHash: String,
        randomString: String,
        targetPage: Int,
        targetPageUpperBound: Boolean
    ): String {
        if (targetPage < 1) {
            _loadingStatus.postValue(
                SingleLiveEvent.create(
                    LoadingStatus.ERROR,
                    "无法获取默认饼干名...请联系作者\n"
                )
            )
            return ""
        }
        return webNMBServiceClient.getComments(cookieNameTestPostId, targetPage).run {
            if (this is APIDataResponse.Success) {
                val maxPage = data!!.getMaxPage()
                if (targetPage != maxPage && !targetPageUpperBound) {
                    findNameInComments(cookieHash, randomString, maxPage, true)
                } else {
                    postDao.insert(data)
                    extractCookieName(data, cookieHash, randomString, targetPage, true)
                }
            } else {
                Timber.e(message)
                ""
            }
        }
    }

    private suspend fun extractCookieName(
        data: Post,
        cookieHash: String,
        randomString: String,
        targetPage: Int,
        targetPageUpperBound: Boolean
    ): String {
        for (reply in data.comments.reversed()) {
            if (reply.content == randomString) {
                return reply.userid
            }
        }
        return findNameInComments(cookieHash, randomString, targetPage - 1, targetPageUpperBound)
    }
}