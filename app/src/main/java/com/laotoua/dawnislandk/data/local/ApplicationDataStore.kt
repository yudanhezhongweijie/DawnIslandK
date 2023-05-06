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
import com.laotoua.dawnislandk.data.remote.APIMessageResponse
import com.laotoua.dawnislandk.data.remote.NMBServiceClient
import com.laotoua.dawnislandk.util.DawnConstants
import com.laotoua.dawnislandk.util.lazyOnMainOnly
import com.tencent.mmkv.MMKV
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import me.jessyan.retrofiturlmanager.RetrofitUrlManager
import timber.log.Timber
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApplicationDataStore @Inject constructor(
    private val cookieDao: CookieDao,
    private val commentDao: CommentDao,
    private val communityDao: CommunityDao,
    private val trendDao: DailyTrendDao,
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

    fun setLastUsedCookie(cookie: Cookie) {
        if (cookie != cookies.firstOrNull()) {
            mCookies.remove(cookie)
            mCookies.add(0, cookie)
            GlobalScope.launch {
                cookieDao.setLastUsedCookie(cookie)
            }
        }
    }

    var luweiNotice: LuweiNotice? = null
        private set

    var nmbNotice: NMBNotice? = null
        private set

    val mmkv: MMKV by lazyOnMainOnly { MMKV.defaultMMKV() }

    private var backupDomains: Set<String>? = null

    fun getBackupDomains(): Set<String> {
        if (backupDomains == null) {
            backupDomains = mmkv.getStringSet(DawnConstants.BACKUP_DOMAINS, setOf())
        }
        return backupDomains!!
    }

    fun setBackupDomains(domains: Set<String>) {
        backupDomains = domains
        mmkv.putStringSet(DawnConstants.BACKUP_DOMAINS, domains)
    }


    val defaultTheme: Int by lazyOnMainOnly {
        mmkv.getInt(DawnConstants.DEFAULT_THEME, 0)
    }

    fun setDefaultTheme(theme: Int) {
        mmkv.putInt(DawnConstants.DEFAULT_THEME, theme)
    }

    private var baseCDN: String? = null
    fun getBaseCDN(): String {
        if (baseCDN == null) {
            baseCDN = mmkv.getString(DawnConstants.DEFAULT_CDN, "auto")
        }
        return baseCDN!!
    }

    fun setBaseCDN(newHost: String) {
        baseCDN = newHost
        mmkv.putString(DawnConstants.DEFAULT_CDN, newHost)
        if (newHost != "auto") RetrofitUrlManager.getInstance().putDomain("nmb", baseCDN)
    }

    private var refCDN: String? = null
    fun getRefCDN(): String {
        if (refCDN == null) {
            refCDN = mmkv.getString(DawnConstants.REF_CDN, "auto")
        }
        return refCDN!!
    }

    fun setRefCDN(newHost: String) {
        refCDN = newHost
        mmkv.putString(DawnConstants.REF_CDN, newHost)
        if (newHost != "auto") RetrofitUrlManager.getInstance().putDomain("nmb-ref", refCDN)
    }

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

    private var expandedCommunityIDs: Set<String>? = null

    fun getExpandedCommunityIDs(): Set<String> {
        if (expandedCommunityIDs == null) {
            expandedCommunityIDs = mmkv.getStringSet(DawnConstants.EXPANDED_COMMUNITY_IDS, setOf())
        }
        return expandedCommunityIDs!!
    }

    fun setExpandedCommunityIDs(set: Set<String>) {
        expandedCommunityIDs = set
        mmkv.putStringSet(DawnConstants.EXPANDED_COMMUNITY_IDS, set)
    }

    private var defaultForumId: String? = null

    fun getDefaultForumId(): String {
        if (defaultForumId == null) {
            defaultForumId = mmkv.getString(DawnConstants.DEFAULT_FORUM_ID, DawnConstants.TIMELINE_COMMUNITY_ID)
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
        firstTimeUse = false
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

    private var sortEmojiByLastUsedStatus: Boolean? = null
    fun getSortEmojiByLastUsedStatus(): Boolean {
        if (sortEmojiByLastUsedStatus == null) {
            sortEmojiByLastUsedStatus =
                mmkv.getBoolean(DawnConstants.SORT_EMOJI_BY_LAST_USED_AT, false)
        }
        return sortEmojiByLastUsedStatus!!
    }

    fun setSortEmojiByLastUsedStatus(value: Boolean) {
        sortEmojiByLastUsedStatus = value
        mmkv.putBoolean(DawnConstants.SORT_EMOJI_BY_LAST_USED_AT, value)
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
        setCustomToolbarImageStatus(true)
        customToolbarImagePath = value
        mmkv.putString(DawnConstants.TOOLBAR_IMAGE_PATH, value)
    }

    val displayTimeFormat: Int by lazyOnMainOnly {
        mmkv.getInt(
            DawnConstants.DISPLAY_TIME_FORMAT,
            DawnConstants.DEFAULT_TIME_FORMAT
        )
    }

    fun setDisplayTimeFormat(format: Int) {
        mmkv.putInt(DawnConstants.DISPLAY_TIME_FORMAT, format)
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

    private var autoUpdateFeed: Boolean? = null
    fun getAutoUpdateFeed(): Boolean {
        if (autoUpdateFeed == null) {
            autoUpdateFeed = mmkv.getBoolean(DawnConstants.AUTO_UPDATE_FEED, false)
        }
        return autoUpdateFeed!!
    }

    fun setAutoUpdateFeed(value: Boolean) {
        autoUpdateFeed = value
        mmkv.putBoolean(DawnConstants.AUTO_UPDATE_FEED, value)
    }

    private var autoUpdateFeedDot: Boolean? = null
    fun getAutoUpdateFeedDot(): Boolean {
        if (autoUpdateFeedDot == null) {
            autoUpdateFeedDot = mmkv.getBoolean(DawnConstants.AUTO_UPDATE_FEED_DOT, true)
        }
        return autoUpdateFeedDot!!
    }

    fun setAutoUpdateFeedDot(value: Boolean) {
        autoUpdateFeedDot = value
        mmkv.putBoolean(DawnConstants.AUTO_UPDATE_FEED_DOT, value)
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

    fun nukeTrendTable() {
        GlobalScope.launch { trendDao.nukeTable() }
    }

    fun nukeBlockedPostTable() {
        GlobalScope.launch { blockedIdDao.nukeBlockedPostIds() }
    }

    fun nukeCommunitiesAndTimelinesTables() {
        GlobalScope.launch {
            communityDao.nukeTable()
        }
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

    suspend fun readNMBXDNotice(notice: NMBNotice) {
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

    private fun checkAcknowledgementAppTerms(): Boolean {
        return mmkv.getBoolean(DawnConstants.ACKNOWLEDGE_APP_PRIVACY_TERMS, false)
    }

    fun acknowledgementAppTerms() {
        mmkv.putBoolean(DawnConstants.ACKNOWLEDGE_APP_PRIVACY_TERMS, true)
    }

    suspend fun getAppPrivacyTerms(): String? {
        return if (checkAcknowledgementAppTerms()) null
        else webService.getPrivacyAgreement().run {
            if (this is APIMessageResponse.Success) {
                dom!!.toString()
            } else {
                Timber.d(message)
                ""
            }
        }
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

    private var subscriptionPagerFeedIndex: Int? = null
    fun getSubscriptionPagerFeedIndex(): Int {
        if (subscriptionPagerFeedIndex == null) {
            subscriptionPagerFeedIndex = mmkv.getInt(DawnConstants.SUBSCRIPTION_PAGER_FEED_INDEX, 1)
        }
        return subscriptionPagerFeedIndex!!
    }

    fun setSubscriptionPagerFeedIndex(feedPageIndex: Int) {
        subscriptionPagerFeedIndex = feedPageIndex
        mmkv.putInt(DawnConstants.SUBSCRIPTION_PAGER_FEED_INDEX, feedPageIndex)
    }

    private var historyPagerBrowsingIndex: Int? = null
    fun getHistoryPagerBrowsingIndex(): Int {
        if (historyPagerBrowsingIndex == null) {
            historyPagerBrowsingIndex = mmkv.getInt(DawnConstants.HISTORY_PAGER_BROWSING_INDEX, 0)
        }
        return historyPagerBrowsingIndex!!
    }

    fun setHistoryPagerBrowsingIndex(browseIndex: Int) {
        historyPagerBrowsingIndex = browseIndex
        mmkv.putInt(DawnConstants.HISTORY_PAGER_BROWSING_INDEX, browseIndex)
    }

    suspend fun checkBackupDomains(): List<String>? = webService.getBackupDomains().data
}