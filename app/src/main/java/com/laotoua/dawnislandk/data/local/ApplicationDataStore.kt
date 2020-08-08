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

package com.laotoua.dawnislandk.data.local

import com.laotoua.dawnislandk.BuildConfig
import com.laotoua.dawnislandk.data.local.dao.*
import com.laotoua.dawnislandk.data.local.entity.Cookie
import com.laotoua.dawnislandk.data.local.entity.LuweiNotice
import com.laotoua.dawnislandk.data.local.entity.NMBNotice
import com.laotoua.dawnislandk.data.local.entity.Release
import com.laotoua.dawnislandk.data.remote.APIDataResponse
import com.laotoua.dawnislandk.data.remote.NMBServiceClient
import com.laotoua.dawnislandk.util.DawnConstants
import com.laotoua.dawnislandk.util.lazyOnMainOnly
import com.tencent.mmkv.MMKV
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApplicationDataStore @Inject constructor(
    private val cookieDao: CookieDao,
    private val commentDao: CommentDao,
    private val feedDao: FeedDao,
    private val NMBNoticeDao: NMBNoticeDao,
    private val luweiNoticeDao: LuweiNoticeDao,
    private val releaseDao: ReleaseDao,
    private val blockedIdDao: BlockedIdDao,
    private val webService: NMBServiceClient
) {

    private var mCookies = mutableListOf<Cookie>()
    val cookies: List<Cookie> get() = mCookies
    val firstCookieHash get() = cookies.firstOrNull()?.getApiHeaderCookieHash()
    var luweiNotice: LuweiNotice? = null
        private set

    var nmbNotice: NMBNotice? = null
        private set

    val mmkv: MMKV by lazyOnMainOnly { MMKV.defaultMMKV() }

    private var feedId: String? = null
    fun getFeedId(): String {
        if (feedId == null) {
            feedId = mmkv.getString(DawnConstants.FEED_ID, "")
        }
        if (feedId.isNullOrBlank()) {
            setFeedId(UUID.randomUUID().toString())
        }
        return feedId!!
    }

    fun setFeedId(value: String) {
        if (feedId != value) {
            GlobalScope.launch { feedDao.nukeTable() }
        }
        feedId = value
        mmkv.putString(DawnConstants.FEED_ID, value)
    }

    private var defaultForumId: String? = null

    fun getDefaultForumId(): String {
        if (defaultForumId == null) {
            defaultForumId = mmkv.getString(DawnConstants.DEFAULT_FORUM_ID, "-1")
        }
        return defaultForumId!!
    }

    fun setDefaultForumId(fid: String) {
        defaultForumId = fid
        mmkv.putString(DawnConstants.DEFAULT_FORUM_ID, fid)
    }

    private var firstTimeUse: Boolean? = null

    fun getFirstTimeUse(): Boolean {
        if (firstTimeUse == null) {
            firstTimeUse = mmkv.getBoolean(DawnConstants.USE_APP_FIRST_TIME, true)
        }
        return firstTimeUse!!
    }

    fun setFirstTimeUse() {
        mmkv.putBoolean(DawnConstants.USE_APP_FIRST_TIME, false)
    }

    // View settings
    val letterSpace by lazyOnMainOnly { mmkv.getFloat(DawnConstants.LETTER_SPACE, 0f) }
    val lineHeight by lazyOnMainOnly { mmkv.getInt(DawnConstants.LINE_HEIGHT, 0) }
    val segGap by lazyOnMainOnly { mmkv.getInt(DawnConstants.SEG_GAP, 0) }
    val textSize by lazyOnMainOnly { mmkv.getFloat(DawnConstants.MAIN_TEXT_SIZE, 15f) }

    private var layoutCustomizationStatus: Boolean? = null

    fun getLayoutCustomizationStatus(): Boolean {
        if (layoutCustomizationStatus == null) {
            layoutCustomizationStatus = mmkv.getBoolean(DawnConstants.LAYOUT_CUSTOMIZATION, true)
        }
        return layoutCustomizationStatus!!
    }

    fun setLayoutCustomizationStatus(value: Boolean) {
        layoutCustomizationStatus = value
        mmkv.putBoolean(DawnConstants.LAYOUT_CUSTOMIZATION, value)
    }

    private var customToolbarImageStatus: Boolean? = null
    fun getCustomToolbarImageStatus(): Boolean {
        if (customToolbarImageStatus == null) {
            customToolbarImageStatus = mmkv.getBoolean(DawnConstants.CUSTOM_TOOLBAR_STATUS, false)
        }
        return customToolbarImageStatus!!
    }

    fun setCustomToolbarImageStatus(value: Boolean) {
        customToolbarImageStatus = value
        mmkv.putBoolean(DawnConstants.CUSTOM_TOOLBAR_STATUS, value)
    }

    private var customToolbarImagePath: String? = null
    fun getCustomToolbarImagePath(): String {
        if (customToolbarImagePath == null) {
            customToolbarImagePath = mmkv.getString(DawnConstants.TOOLBAR_IMAGE_PATH, "")
        }
        return customToolbarImagePath!!
    }

    fun setCustomToolbarImagePath(value: String) {
        customToolbarImagePath = value
        mmkv.putString(DawnConstants.TOOLBAR_IMAGE_PATH, value)
    }

    val displayTimeFormat by lazyOnMainOnly {
        mmkv.getString(
            DawnConstants.TIME_FORMAT,
            DawnConstants.DEFAULT_TIME_FORMAT
        )!!
    }

    fun setDisplayTimeFormat(format: String) {
        mmkv.putString(DawnConstants.TIME_FORMAT, format)
    }

    // adapter settings
    val animationOption by lazyOnMainOnly { mmkv.getInt(DawnConstants.ANIMATION_OPTION, 0) }
    fun setAnimationOption(option: Int) {
        mmkv.putInt(DawnConstants.ANIMATION_OPTION, option)
    }

    val animationFirstOnly by lazyOnMainOnly {
        mmkv.getBoolean(
            DawnConstants.ANIMATION_FIRST_ONLY,
            false
        )
    }

    fun setAnimationFirstOnly(status: Boolean) {
        mmkv.putBoolean(DawnConstants.ANIMATION_FIRST_ONLY, status)
    }

    // Reading settings
    private var readingProgressStatus: Boolean? = null
    fun getReadingProgressStatus(): Boolean {
        if (readingProgressStatus == null) {
            readingProgressStatus = mmkv.getBoolean(DawnConstants.READING_PROGRESS, true)
        }
        return readingProgressStatus!!
    }

    fun setReadingProgressStatus(value: Boolean) {
        readingProgressStatus = value
        mmkv.putBoolean(DawnConstants.READING_PROGRESS, value)
    }

    // view caching
    private var viewCaching: Boolean? = null
    fun getViewCaching(): Boolean {
        if (viewCaching == null) {
            viewCaching = mmkv.getBoolean(DawnConstants.VIEW_CACHING, false)
        }
        return viewCaching!!
    }

    fun setViewCaching(value: Boolean) {
        viewCaching = value
        mmkv.putBoolean(DawnConstants.VIEW_CACHING, value)
    }

    suspend fun loadCookies() {
        mCookies = cookieDao.getAll().toMutableList()
    }

    fun getCookieDisplayName(cookieName: String): String? =
        cookies.firstOrNull { it.cookieName == cookieName }?.cookieDisplayName

    suspend fun addCookie(cookie: Cookie) {
        cookies.firstOrNull { it.cookieHash == cookie.cookieHash }?.let {
            it.cookieDisplayName = cookie.cookieDisplayName
            cookieDao.updateCookie(it)
            return
        }
        mCookies.add(cookie)
        cookieDao.insert(cookie)
    }

    suspend fun deleteCookies(cookie: Cookie) {
        mCookies.remove(cookie)
        cookieDao.delete(cookie)
    }

    fun nukeCommentTable() {
        GlobalScope.launch { commentDao.nukeTable() }
    }

    fun nukeBlockedPostTable() {
        GlobalScope.launch { blockedIdDao.nukeBlockedPostIds() }
    }

    suspend fun getLatestNMBNotice(): NMBNotice? {
        nmbNotice = NMBNoticeDao.getLatestNMBNotice()
        webService.getNMBNotice().run {
            if (this is APIDataResponse.Success) {
                if (nmbNotice == null || data!!.date > nmbNotice!!.date) {
                    coroutineScope { launch { NMBNoticeDao.insertNMBNoticeWithTimestamp(data!!) } }
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
            if (this is APIDataResponse.Success) {
                if (luweiNotice != data) {
                    luweiNotice = data
                    coroutineScope { launch { luweiNoticeDao.insertNoticeWithTimestamp(data!!) } }
                }
            } else {
                Timber.e(message)
            }
        }
        return luweiNotice
    }

    fun checkAcknowledgementPostingRule(): Boolean {
        return mmkv.getBoolean(DawnConstants.ACKNOWLEDGE_POSTING_RULES, false)
    }

    fun acknowledgementPostingRule() {
        mmkv.putBoolean(DawnConstants.ACKNOWLEDGE_POSTING_RULES, true)
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
            if (this is APIDataResponse.Success) data
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
        mmkv.putInt(DawnConstants.TRENDS_FRAG_PAGER_INDEX, trendsIndex)
        mmkv.putInt(DawnConstants.FEEDS_FRAG_PAGER_INDEX, feedsIndex)
    }

    fun getFeedPagerPageIndices(): Pair<Int, Int> {
        return Pair(
            mmkv.getInt(DawnConstants.TRENDS_FRAG_PAGER_INDEX, 0),
            mmkv.getInt(DawnConstants.FEEDS_FRAG_PAGER_INDEX, 1)
        )
    }

    fun setHistoryPagerDefaultPage(browseIndex: Int, postIndex: Int) {
        mmkv.putInt(DawnConstants.BROWSING_HISTORY_FRAG_PAGER_INDEX, browseIndex)
        mmkv.putInt(DawnConstants.POST_HISTORY_FRAG_PAGER_INDEX, postIndex)
    }

    fun getHistoryPagerPageIndices(): Pair<Int, Int> {
        return Pair(
            mmkv.getInt(DawnConstants.BROWSING_HISTORY_FRAG_PAGER_INDEX, 0),
            mmkv.getInt(DawnConstants.POST_HISTORY_FRAG_PAGER_INDEX, 1)
        )
    }
}