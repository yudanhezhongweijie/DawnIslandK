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
import com.laotoua.dawnislandk.data.local.entity.Cookie
import com.laotoua.dawnislandk.util.EventPayload
import com.laotoua.dawnislandk.util.SingleLiveEvent
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

class ProfileViewModel @Inject constructor() :
    ViewModel() {
    private val _cookies = MutableLiveData(applicationDataStore.cookies)
    val cookies: LiveData<List<Cookie>> get() = _cookies

    private val _loadingStatus = MutableLiveData<SingleLiveEvent<EventPayload<Nothing>>>()
    val loadingStatus: LiveData<SingleLiveEvent<EventPayload<Nothing>>> get() = _loadingStatus


    fun updateCookie(cookie: Cookie) {
        viewModelScope.launch {
            applicationDataStore.addCookie(cookie)
            _cookies.value = applicationDataStore.cookies
        }
    }

    fun addNewCookie(cookieHash: String, cookieName: String, cookieDisplayName: String? = null) {
        if (cookieHash.isBlank()) {
            Timber.e("Trying to add empty cookie")
            return
        }
        viewModelScope.launch {
            if (cookieName.isNotBlank()) {
                applicationDataStore.addCookie(
                    Cookie(
                        cookieHash,
                        cookieName,
                        cookieDisplayName ?: cookieName
                    )
                )
                _cookies.value = applicationDataStore.cookies
            }
        }
    }

    fun deleteCookie(cookie: Cookie) {
        viewModelScope.launch {
            applicationDataStore.deleteCookies(cookie)
            _cookies.value = applicationDataStore.cookies
        }
    }
}