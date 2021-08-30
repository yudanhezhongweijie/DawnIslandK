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

package com.laotoua.dawnislandk.screens.widgets.popups

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.laotoua.dawnislandk.DawnApp
import com.laotoua.dawnislandk.MainNavDirections
import com.laotoua.dawnislandk.R
import com.laotoua.dawnislandk.data.local.entity.Community
import com.laotoua.dawnislandk.data.local.entity.Forum
import com.laotoua.dawnislandk.data.local.entity.Timeline
import com.laotoua.dawnislandk.screens.MainActivity
import com.laotoua.dawnislandk.screens.SharedViewModel
import com.laotoua.dawnislandk.screens.adapters.CommunityNodeAdapter
import com.laotoua.dawnislandk.util.DawnConstants
import com.laotoua.dawnislandk.util.GlideApp
import com.lxj.xpopup.XPopup
import com.lxj.xpopup.core.DrawerPopupView
import me.dkzwm.widget.srl.MaterialSmoothRefreshLayout
import me.dkzwm.widget.srl.RefreshingListenerAdapter
import timber.log.Timber

@SuppressLint("ViewConstructor")
class ForumDrawerPopup(
    context: Context,
    private val sharedVM: SharedViewModel
) : DrawerPopupView(context) {
    override fun getImplLayoutId(): Int = R.layout.drawer_forum

    private val forumListAdapter = CommunityNodeAdapter(
        object : CommunityNodeAdapter.ForumClickListener {
            override fun onForumClick(forum: Forum) {
                Timber.d("Clicked on Forum ${forum.name}")
                dismissWith {
                    if (forum.isFakeForum()) {
                        val action = MainNavDirections.actionGlobalCommentsFragment(forum.id, "")
                        (context as MainActivity).findNavController(R.id.navHostFragment).navigate(action)
                    } else {
                        sharedVM.setForumId(forum.id)
                    }
                }
            }
        }, object : CommunityNodeAdapter.TimelineClickListener {
            override fun onTimelineClick(timeline: Timeline) {
                Timber.d("Clicked on Timeline ${timeline.name}")
                dismissWith {
                    sharedVM.setForumId("-${timeline.id}")
                }
            }
        })

    private var reedImageUrl: String = ""
    private var reedImageView: ImageView? = null

    fun setCommunities(list: List<Community>) {
        forumListAdapter.setCommunities(list)
        findViewById<MaterialSmoothRefreshLayout>(R.id.refreshLayout).refreshComplete()
    }

    fun setTimelines(list: List<Timeline>) {
        forumListAdapter.setTimelines(list)
        findViewById<MaterialSmoothRefreshLayout>(R.id.refreshLayout).refreshComplete()
    }

    fun setReedPicture(url: String) {
        reedImageUrl = url
        if (isShow) {
            loadReedPicture()
        }
    }

    fun loadReedPicture() {
        reedImageView?.run {
            GlideApp.with(this)
                .load(reedImageUrl)
                .placeholder(R.drawable.placeholder)
                .fitCenter()
                .into(this)
        }
    }

    override fun onCreate() {
        super.onCreate()
        findViewById<MaterialSmoothRefreshLayout>(R.id.refreshLayout).apply {
            setOnRefreshListener(object : RefreshingListenerAdapter() {
                override fun onRefreshing() {
                    sharedVM.refreshCommunitiesAndTimelines()
                    refreshComplete(1000)
                }
            })
        }

        reedImageView = findViewById(R.id.reedImageView)
        if (reedImageUrl.isNotBlank()) loadReedPicture()
        reedImageView!!.setOnClickListener {
            if (!isShow || reedImageUrl.isBlank()) return@setOnClickListener
            val viewerPopup = ImageViewerPopup(context)
            viewerPopup.setSingleSrcView(reedImageView, reedImageUrl)
            XPopup.Builder(context)
                .asCustom(viewerPopup)
                .show()
        }

        findViewById<Button>(R.id.ReedPictureRefresh).setOnClickListener {
            if (!isShow) return@setOnClickListener
            reedImageUrl = ""
            sharedVM.getRandomReedPicture()
        }

        findViewById<RecyclerView>(R.id.forumContainer).apply {
            layoutManager = LinearLayoutManager(context)
            adapter = forumListAdapter
        }

        var nightModeOn = false
        findViewById<MaterialButton>(R.id.themeToggle).apply {
            when (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
                Configuration.UI_MODE_NIGHT_YES -> {
                    text = "光来 (╬ﾟдﾟ)"
                    icon = ContextCompat.getDrawable(context, R.drawable.ic_brightness_5_24px)
                    setIconTintResource(R.color.pure_light)
                    nightModeOn = true
                }
                Configuration.UI_MODE_NIGHT_NO -> {
                    text = "光走 (;´Д`)"
                    icon = ContextCompat.getDrawable(context, R.drawable.ic_brightness_2_24px)
                    setIconTintResource(R.color.pure_dark)
                    nightModeOn = false
                }
            }
            setOnClickListener {
                if (!isShow) return@setOnClickListener
                dismissWith {
                    if (nightModeOn) AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                    else AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                    nightModeOn = !nightModeOn
                }
            }
        }


        findViewById<MaterialButton>(R.id.hostToggle).apply {
            when (DawnApp.currentDomain) {
                DawnConstants.ADNMBDomain -> {
                    text = "B(*ﾟ∇ﾟ)T"
                }
                DawnConstants.TNMBDomain -> {
                    text = "A(*´∀`)A"
                }
            }
            setOnClickListener {
                if (!isShow) return@setOnClickListener
                dismissWith {
                    if (DawnApp.currentDomain == DawnConstants.TNMBDomain) (context as MainActivity).goToADNMB()
                    else (context as MainActivity).goToTNMB()
                }
            }
        }
    }
}