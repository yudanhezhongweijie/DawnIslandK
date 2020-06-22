package com.laotoua.dawnislandk.data.local

import com.laotoua.dawnislandk.BuildConfig
import com.laotoua.dawnislandk.data.local.dao.*
import com.laotoua.dawnislandk.data.local.entity.Cookie
import com.laotoua.dawnislandk.data.local.entity.LuweiNotice
import com.laotoua.dawnislandk.data.local.entity.NMBNotice
import com.laotoua.dawnislandk.data.local.entity.Release
import com.laotoua.dawnislandk.data.remote.APIDataResponse
import com.laotoua.dawnislandk.data.remote.NMBServiceClient
import com.laotoua.dawnislandk.util.Constants
import com.laotoua.dawnislandk.util.lazyOnMainOnly
import com.tencent.mmkv.MMKV
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.*
import javax.inject.Inject

class ApplicationDataStore @Inject constructor(
    private val cookieDao: CookieDao, private val commentDao: CommentDao,
    private val NMBNoticeDao: NMBNoticeDao,
    private val luweiNoticeDao: LuweiNoticeDao,
    private val releaseDao: ReleaseDao,
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
            true
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

    suspend fun nukeCommentTable() {
        commentDao.nukeTable()
    }

    suspend fun getNMBNotice(): NMBNotice? {
        var cache = NMBNoticeDao.getLatestNMBNotice()
        webService.getNMBNotice().run {
            if (this is APIDataResponse.APISuccessDataResponse) {
                if (cache == null || data.date > cache!!.date) {
                    coroutineScope { launch { NMBNoticeDao.insertNMBNoticeWithTimestamp(data) } }
                    cache = data
                }
            } else {
                Timber.e(message)
            }
        }
        if (cache?.read != true) {
            return cache
        }
        return null
    }

    suspend fun readNMBNotice(notice: NMBNotice) {
        NMBNoticeDao.updateNMBNoticeWithTimestamp(
            notice.content,
            notice.enable,
            notice.read,
            notice.date
        )
    }

    suspend fun getLuweiNotice(): LuweiNotice? {
        _luweiNotice = luweiNoticeDao.getLatestLuweiNotice()
        webService.getLuweiNotice().run {
            if (this is APIDataResponse.APISuccessDataResponse) {
                if (_luweiNotice != data) {
                    _luweiNotice = data
                    coroutineScope { launch { luweiNoticeDao.insertNoticeWithTimestamp(data) } }
                }
            } else {
                Timber.e(message)
            }
        }
        return _luweiNotice
    }

    fun checkAcknowledgementPostingRule(): Boolean {
        return mmkv.getBoolean(Constants.ACKNOWLEDGE_POSTING_RULES, false)
    }

    fun acknowledgementPostingRule() {
        mmkv.putBoolean(Constants.ACKNOWLEDGE_POSTING_RULES, true)
    }

    suspend fun getLatestRelease(): Release? {
        val currentVersion = releaseDao.getLatestRelease()
        val currentVersionCode = currentVersion?.versionCode
            ?: BuildConfig.VERSION_NAME.filter { it.isDigit() }.toInt()
        if (currentVersion == null) {
            val currentRelease = Release(1, BuildConfig.VERSION_NAME, "", "default entry")
            coroutineScope { launch { releaseDao.insertRelease(currentRelease) } }
        }
        val latest = webService.getLatestRelease().run {
            if (this is APIDataResponse.APISuccessDataResponse) data
            else {
                Timber.e(message)
                null
            }
        }
        if (latest != null && latest.versionCode > currentVersionCode) {
            return latest
        }
        return null
    }
}