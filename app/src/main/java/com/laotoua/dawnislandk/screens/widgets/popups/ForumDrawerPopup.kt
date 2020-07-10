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
import android.widget.Button
import android.widget.ImageView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.laotoua.dawnislandk.R
import com.laotoua.dawnislandk.data.local.entity.Community
import com.laotoua.dawnislandk.data.local.entity.Forum
import com.laotoua.dawnislandk.screens.MainActivity
import com.laotoua.dawnislandk.screens.SharedViewModel
import com.laotoua.dawnislandk.screens.adapters.CommunityNodeAdapter
import com.laotoua.dawnislandk.util.GlideApp
import com.lxj.xpopup.XPopup
import com.lxj.xpopup.core.DrawerPopupView
import timber.log.Timber

@SuppressLint("ViewConstructor")
class ForumDrawerPopup(
    mainActivity: MainActivity,
    private val sharedVM: SharedViewModel
) : DrawerPopupView(mainActivity) {
    override fun getImplLayoutId(): Int = R.layout.drawer_forum

    private val forumListAdapter =
        CommunityNodeAdapter(object : CommunityNodeAdapter.ForumClickListener {
            override fun onForumClick(forum: Forum) {
                Timber.d("Clicked on Forum ${forum.name}")
                sharedVM.setForumId(forum.id)
                dismiss()
            }
        })

    private var reedImageUrl: String = ""
    private lateinit var reedImageView: ImageView

    fun setData(list: List<Community>) {
        forumListAdapter.setData(list)
    }

    fun setReedPicture(url: String) {
        reedImageUrl = url
        if (isShow) {
            loadReedPicture()
        }
    }

    fun loadReedPicture() {
        GlideApp.with(reedImageView)
            .load(reedImageUrl)
            .placeholder(R.drawable.drawer_placeholder)
            .fitCenter()
            .into(reedImageView)
    }

    override fun onCreate() {
        super.onCreate()

        findViewById<Button>(R.id.forumRefresh).setOnClickListener {
            forumListAdapter.setData(emptyList())
            sharedVM.forumRefresh()
        }
        reedImageView = findViewById(R.id.reedImageView)
        if (reedImageUrl.isNotBlank()) loadReedPicture()
        reedImageView.setOnClickListener {
            if (reedImageUrl.isBlank()) return@setOnClickListener
            val viewerPopup = ImageViewerPopup(reedImageUrl, context)
            viewerPopup.setSingleSrcView(reedImageView, reedImageUrl)
            XPopup.Builder(context)
                .asCustom(viewerPopup)
                .show()
        }

        findViewById<Button>(R.id.ReedPictureRefresh).setOnClickListener {
            reedImageView.setImageResource(R.drawable.drawer_placeholder)
            sharedVM.getRandomReedPicture()
            reedImageUrl = ""
        }

        findViewById<RecyclerView>(R.id.forumContainer).apply {
            layoutManager = LinearLayoutManager(context)
            adapter = forumListAdapter
        }
    }
}