package com.laotoua.dawnislandk.data.local

import com.laotoua.dawnislandk.data.local.dao.CookieDao
import com.laotoua.dawnislandk.util.Constants
import com.laotoua.dawnislandk.util.lazyOnMainOnly
import com.tencent.mmkv.MMKV
import java.util.*
import javax.inject.Inject

class ApplicationDataStore @Inject constructor(private val cookieDao: CookieDao) {

    private var mCookies = mutableListOf<Cookie>()
    val cookies get() = mCookies
    val firstCookieHash = cookies.firstOrNull()?.cookieHash

    private var mFeedId: String? = null
    val feedId get() = mFeedId!!

    val mmkv: MMKV by lazyOnMainOnly { MMKV.defaultMMKV() }

    // View settings
    val letterSpace by lazyOnMainOnly { mmkv.getFloat(Constants.LETTER_SPACE, 0f) }
    val lineHeight by lazyOnMainOnly { mmkv.getInt(Constants.LINE_HEIGHT, 0) }
    val segGap by lazyOnMainOnly { mmkv.getInt(Constants.SEG_GAP, 0) }
    val textSize by lazyOnMainOnly { mmkv.getFloat(Constants.MAIN_TEXT_SIZE, 15f) }

    // adapter settings
    val animationStatus by lazyOnMainOnly { mmkv.getBoolean(Constants.ANIMATION, false) }

    // Reading settings
    val readingProgressStatus by lazyOnMainOnly {
        mmkv.getBoolean(
            Constants.READING_PROGRESS,
            false
        )
    }


    fun initializeFeedId() {
        mFeedId = mmkv.getString(Constants.FEED_ID, null)
        if (mFeedId == null) setFeedId(UUID.randomUUID().toString())
    }

    fun setFeedId(string: String) {
        mFeedId = string
        mmkv.putString(Constants.FEED_ID, mFeedId)
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