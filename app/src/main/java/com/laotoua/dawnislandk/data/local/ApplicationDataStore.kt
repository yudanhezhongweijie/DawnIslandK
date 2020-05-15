package com.laotoua.dawnislandk.data.local

import com.laotoua.dawnislandk.data.local.dao.CookieDao
import com.tencent.mmkv.MMKV
import java.util.*
import javax.inject.Inject

class ApplicationDataStore @Inject constructor(private val cookieDao: CookieDao) {

    private var mCookies = mutableListOf<Cookie>()
    val cookies get() = mCookies

    private var mFeedId: String? = null
    val feedId get() = mFeedId!!

    fun initializeFeedId() {
        mFeedId = MMKV.defaultMMKV().getString("feedId", null)
        if (mFeedId == null) setFeedId(UUID.randomUUID().toString())
    }

    fun setFeedId(string: String) {
        mFeedId = string
        MMKV.defaultMMKV().putString("feedId", mFeedId)
    }

    suspend fun loadCookies() {
        mCookies = cookieDao.getAll().toMutableList()
    }

    suspend fun addCookie(cookie: Cookie) {
        mCookies.add(cookie)
        cookieDao.insert(cookie)
    }

    suspend fun deleteCookies(cookie: Cookie) {
        mCookies.remove(cookie)
        cookieDao.delete(cookie)
    }

    suspend fun updateCookie(cookie: Cookie) {
        cookies.first { it.cookieHash == cookie.cookieHash }.cookieName =
            cookie.cookieName
        cookieDao.updateCookie(cookie)
    }
}