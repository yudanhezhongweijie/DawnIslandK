package com.laotoua.dawnislandk.data.local

import com.laotoua.dawnislandk.data.local.dao.CookieDao
import com.laotoua.dawnislandk.data.local.dao.NoticeDao
import com.laotoua.dawnislandk.data.local.dao.ReplyDao
import com.laotoua.dawnislandk.data.remote.APIDataResponse
import com.laotoua.dawnislandk.data.remote.NMBServiceClient
import com.laotoua.dawnislandk.util.Constants
import com.laotoua.dawnislandk.util.lazyOnMainOnly
import com.tencent.mmkv.MMKV
import timber.log.Timber
import java.util.*
import javax.inject.Inject

class ApplicationDataStore @Inject constructor(
    private val cookieDao: CookieDao, private val replyDao: ReplyDao,
    private val noticeDao: NoticeDao,
    private val webService: NMBServiceClient
) {

    private var mCookies = mutableListOf<Cookie>()
    val cookies get() = mCookies
    val firstCookieHash get() = cookies.firstOrNull()?.cookieHash
    private var _luweiNotice: LuweiNotice? = null
    val luweiNotice: LuweiNotice? get() = _luweiNotice

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

    suspend fun nukeReplyTable() {
        replyDao.nukeTable()
    }

    suspend fun getNotice(): NMBNotice? {
        var cacheNotice = noticeDao.getLatestNotice()
        webService.getNotice().run {
            if (this is APIDataResponse.APISuccessDataResponse) {
                if (cacheNotice == null || data.date > cacheNotice!!.date) {
                    noticeDao.insertNotice(data)
                    cacheNotice = data
                }
            } else {
                Timber.e(message)
            }
        }
        if (cacheNotice?.read != true) {
            return cacheNotice
        }
        return null
    }

    suspend fun readNotice(NMBNotice: NMBNotice) {
        noticeDao.updateNotice(NMBNotice.content, NMBNotice.enable, NMBNotice.read, NMBNotice.date)
    }

    suspend fun getLuweiNotice(): LuweiNotice? {
        webService.getLuweiNotice().run {
            if (this is APIDataResponse.APISuccessDataResponse) {
                _luweiNotice = data
                return data
            } else {
                Timber.e(message)
            }
        }
        return null
    }
}