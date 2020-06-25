package com.laotoua.dawnislandk.screens.profile

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laotoua.dawnislandk.DawnApp.Companion.applicationDataStore
import com.laotoua.dawnislandk.data.local.entity.Cookie
import com.laotoua.dawnislandk.data.remote.NMBServiceClient
import kotlinx.coroutines.launch
import javax.inject.Inject

class ProfileViewModel @Inject constructor(val webNMBServiceClient: NMBServiceClient) :ViewModel() {
    private val _cookies = MutableLiveData<List<Cookie>>(applicationDataStore.cookies)
    val cookies:LiveData<List<Cookie>> get() = _cookies

    fun updateCookie(cookie: Cookie) {
        viewModelScope.launch {
            applicationDataStore.updateCookie(cookie)
            _cookies.value = applicationDataStore.cookies
        }
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