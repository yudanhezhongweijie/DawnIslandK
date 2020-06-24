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
    var luweiNotice: LuweiNotice? = null
        private set

    var nmbNotice: NMBNotice? = null
        private set

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

    suspend fun getLatestNMBNotice(): NMBNotice? {
        nmbNotice = NMBNoticeDao.getLatestNMBNotice()
        webService.getNMBNotice().run {
            if (this is APIDataResponse.APISuccessDataResponse) {
                if (nmbNotice == null || data.date > nmbNotice!!.date) {
                    coroutineScope { launch { NMBNoticeDao.insertNMBNoticeWithTimestamp(data) } }
                    nmbNotice = data
                }
            } else {
                Timber.e(message)
            }
        }
        if (nmbNotice?.read != true) {
            return nmbNotice
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

    suspend fun getLatestLuweiNotice(): LuweiNotice? {
        luweiNotice = luweiNoticeDao.getLatestLuweiNotice()
        webService.getLuweiNotice().run {
            if (this is APIDataResponse.APISuccessDataResponse) {
                if (luweiNotice != data) {
                    luweiNotice = data
                    coroutineScope { launch { luweiNoticeDao.insertNoticeWithTimestamp(data) } }
                }
            } else {
                Timber.e(message)
            }
        }
        return luweiNotice
    }

    fun checkAcknowledgementPostingRule(): Boolean {
        return mmkv.getBoolean(Constants.ACKNOWLEDGE_POSTING_RULES, false)
    }

    fun acknowledgementPostingRule() {
        mmkv.putBoolean(Constants.ACKNOWLEDGE_POSTING_RULES, true)
    }

    suspend fun getLatestRelease(): Release? {
        // TODO: add update check frequency
//        val currentVersion = releaseDao.getLatestRelease()
//        if (currentVersion == null) {
//            val currentRelease = Release(1, BuildConfig.VERSION_NAME, "", "default entry",Date().time)
//            coroutineScope { launch { releaseDao.insertRelease(currentRelease) } }
//        }
        val currentVersionCode = BuildConfig.VERSION_NAME.filter { it.isDigit() }.toInt()
        val latest = webService.getLatestRelease().run {
            if (this is APIDataResponse.APISuccessDataResponse) data
            else {
                Timber.e(message)
                null
            }
        }
        if (latest != null && latest.versionCode > currentVersionCode) {
            coroutineScope { launch { releaseDao.insertRelease(latest) } }
            return latest
        }
        return null
    }

    fun setFeedPagerDefaultPage(trendsIndex: Int, feedsIndex: Int) {
        mmkv.putInt(Constants.TRENDS_FRAG_PAGER_INDEX, trendsIndex)
        mmkv.putInt(Constants.FEEDS_FRAG_PAGER_INDEX, feedsIndex)
    }

    fun getFeedPagerPageIndices(): Pair<Int, Int> {
        return Pair(
            mmkv.getInt(Constants.TRENDS_FRAG_PAGER_INDEX, 0),
            mmkv.getInt(Constants.FEEDS_FRAG_PAGER_INDEX, 1)
        )
    }
}