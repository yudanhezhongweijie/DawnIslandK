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

package com.laotoua.dawnislandk.screens.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.loadmore.BaseLoadMoreView
import com.chad.library.adapter.base.module.LoadMoreModule
import com.chad.library.adapter.base.util.getItemView
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.google.android.material.card.MaterialCardView
import com.laotoua.dawnislandk.DawnApp
import com.laotoua.dawnislandk.R
import com.laotoua.dawnislandk.data.local.entity.Comment
import com.laotoua.dawnislandk.data.local.entity.Post
import com.laotoua.dawnislandk.data.local.entity.Trend
import com.laotoua.dawnislandk.screens.SharedViewModel
import com.laotoua.dawnislandk.screens.adapters.animators.CustomAnimation1
import com.laotoua.dawnislandk.screens.adapters.animators.CustomAnimation2
import com.laotoua.dawnislandk.screens.posts.PostCardFactory
import com.laotoua.dawnislandk.screens.widgets.spans.ReferenceSpan


class QuickAdapter<T>(
    private val layoutResId: Int,
    private val sharedViewModel: SharedViewModel? = null
) :
    BaseQuickAdapter<T, BaseViewHolder>(layoutResId),
    LoadMoreModule {

    private lateinit var referenceClickListener: ReferenceSpan.ReferenceClickHandler

    private var po: String = ""

    init {
        // 所有数据加载完成后，是否允许点击（默认为false）
        loadMoreModule.enableLoadMoreEndClick = true

        // 当数据不满一页时，是否继续自动加载（默认为true）
        loadMoreModule.isEnableLoadMoreIfNotFullPage = false

        when (DawnApp.applicationDataStore.animationOption) {
            0 -> {}
            1 -> setAnimationWithDefault(AnimationType.AlphaIn)
            2 -> setAnimationWithDefault(AnimationType.ScaleIn)
            3 -> setAnimationWithDefault(AnimationType.SlideInBottom)
            4 -> setAnimationWithDefault(AnimationType.SlideInLeft)
            5 -> setAnimationWithDefault(AnimationType.SlideInRight)
            6 -> adapterAnimation = CustomAnimation1()
            7 -> adapterAnimation = CustomAnimation2()
            else -> throw Exception("Unhandled Animation Option")
        }
        if (DawnApp.applicationDataStore.animationOption > 0) {
            isAnimationFirstOnly = DawnApp.applicationDataStore.animationFirstOnly
        }


        setDiffCallback(DiffItemCallback())
        sharedViewModel?.let {
            loadMoreModule.loadMoreView = DawnLoadMoreView(it)
        }
    }

    fun setPo(po: String) {
        this.po = po
    }

    // only works after adapter has been bind to recyclerview
    fun setDefaultEmptyView(){
        setEmptyView(R.layout.view_no_data)
    }

    fun setReferenceClickListener(referenceClickListener: ReferenceSpan.ReferenceClickHandler) {
        this.referenceClickListener = referenceClickListener
    }

    /** default handler for recyclerview item
     *
     */
    override fun convert(holder: BaseViewHolder, item: T) {
        if (layoutResId == R.layout.list_item_post && item is Post) {
            holder.convertPost(item, sharedViewModel!!.getForumDisplayName(item.fid))
        } else if (layoutResId == R.layout.list_item_comment && item is Comment) {
            holder.convertComment(item, po)
        } else if (layoutResId == R.layout.list_item_trend && item is Trend) {
            holder.convertTrend(item)
        } else if (layoutResId == R.layout.grid_item_emoji && item is String) {
            holder.convertEmoji(item)
        } else if (layoutResId == R.layout.grid_item_luwei_sticker && item is String) {
            holder.convertLuweiSticker(item)
        } else {
            throw Exception("Unhandled conversion in adapter")
        }
    }

    override fun convert(holder: BaseViewHolder, item: T, payloads: List<Any>) {
        if (layoutResId == R.layout.list_item_post && item is Post) {
            holder.convertPostWithPayload(
                payloads.first() as Payload.PostPayload,
                sharedViewModel!!.getForumDisplayName(item.fid)
            )
        } else if (layoutResId == R.layout.list_item_comment && item is Comment) {
            holder.convertCommentWithPayload(payloads.first() as Payload.CommentPayload)
        } else {
            throw Exception("unhandled payload conversion")
        }
    }

    override fun onCreateDefViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
        return when (layoutResId) {
            R.layout.list_item_post, R.layout.list_item_trend -> {
                val view = parent.getItemView(layoutResId).applyTextSizeAndLetterSpacing()
                PostCardFactory.applySettings(view as MaterialCardView)
                createBaseViewHolder(view)
            }
            R.layout.list_item_comment -> {
                val view = parent.getItemView(layoutResId).applyTextSizeAndLetterSpacing(true)
                createBaseViewHolder(view)
            }
            else -> {
                super.onCreateDefViewHolder(parent, layoutResId)
            }
        }
    }


    private fun BaseViewHolder.convertPost(item: Post, forumDisplayName: String) {
        convertUserId(item.userid, item.admin)
        convertTitleAndName(item.getSimplifiedTitle(), item.getSimplifiedName())
        convertRefId(context, item.id)
        convertTimeStamp(item.now)
        convertForumAndReplyCount(item.replyCount, forumDisplayName)
        convertSage(item.sage, item.isStickyTopBanner())
        convertImage(item.getImgUrl())
        convertContent(context, item.content)
    }

    private fun BaseViewHolder.convertPostWithPayload(
        payload: Payload.PostPayload, forumDisplayName: String
    ) {
        convertTimeStamp(payload.now)
        convertTitleAndName(payload.title, payload.name)
        convertForumAndReplyCount(payload.replyCount, forumDisplayName)
        convertSage(payload.sage)
        convertContent(context, payload.content)
    }

    private fun BaseViewHolder.convertComment(item: Comment, po: String) {
        convertUserId(item.userid, item.admin, po)
        convertTimeStamp(item.now, item.isAd())
        convertSage(item.sage)
        convertRefId(context, item.id, item.isAd())
        convertImage(item.getImgUrl(), item.visible)
        convertContent(context, item.content, referenceClickListener, item.visible)
        convertTitleAndName(
            item.getSimplifiedTitle(),
            item.getSimplifiedName(),
            item.visible,
            item.isAd()
        )
        convertExpandSummary(context, item.visible)
        hideCommentMenu()
    }

    private fun BaseViewHolder.hideCommentMenu() {
        setGone(R.id.commentMenu, true)
    }

    private fun BaseViewHolder.convertCommentWithPayload(payload: Payload.CommentPayload) {
        convertTimeStamp(payload.now)
        convertSage(payload.sage)
        convertContent(context, payload.content, referenceClickListener, payload.visible)
        convertImage(payload.imgUrl, payload.visible)
        convertTitleAndName(payload.title, payload.name, payload.visible)
        convertExpandSummary(context, payload.visible)
    }

    private fun BaseViewHolder.convertTrend(item: Trend) {
        setText(R.id.trendRank, item.rank)
        convertRefId(context, item.id)
        setText(R.id.trendForum, item.forum)
        setText(R.id.hits, item.hits)
        convertContent(context, item.content)
    }

    private fun BaseViewHolder.convertEmoji(item: String) {
        setText(R.id.emoji, item)
    }

    private fun BaseViewHolder.convertLuweiSticker(item: String) {
        val resourceId: Int = context.resources.getIdentifier(
            "le$item", "drawable",
            context.packageName
        )
        setImageResource(R.id.luweiSticker, resourceId)
    }

    private class DiffItemCallback<T> : DiffUtil.ItemCallback<T>() {
        override fun areItemsTheSame(oldItem: T, newItem: T): Boolean {
            return when {
                (oldItem is Post && newItem is Post) -> oldItem.id == newItem.id && oldItem.fid == newItem.fid
                (oldItem is Comment && newItem is Comment) -> {
                    if (oldItem.isAd() && newItem.isAd()) {
                        oldItem.page == newItem.page && oldItem.content == newItem.content
                    }
                    else{
                        oldItem.id == newItem.id && oldItem.content == newItem.content
                                && oldItem.visible == newItem.visible
                    }
                }
                (oldItem is Trend && newItem is Trend) -> oldItem.id == newItem.id
                (oldItem is String && newItem is String) -> oldItem == newItem
                else -> throw Exception("Unhandled type comparison")
            }
        }

        override fun areContentsTheSame(oldItem: T, newItem: T): Boolean {
            return when {
                (oldItem is Post && newItem is Post) -> {
                    oldItem.now == newItem.now
                            && oldItem.sage == newItem.sage
                            && oldItem.replyCount == newItem.replyCount
                            && oldItem.content == newItem.content
                            && oldItem.title == newItem.title
                            && oldItem.name == newItem.name
                }
                (oldItem is Comment && newItem is Comment) -> {
                    if (oldItem.isAd() && newItem.isAd()) true
                    else oldItem.now == newItem.now
                            && oldItem.sage == newItem.sage
                            && oldItem.content == newItem.content
                            && oldItem.visible == newItem.visible
                            && oldItem.title == newItem.title
                            && oldItem.name == newItem.name
                }
                (oldItem is Trend && newItem is Trend) -> {
                    oldItem.rank == newItem.rank && oldItem.hits == newItem.hits
                }
                else -> throw Exception("Unhandled type comparison")
            }
        }

        override fun getChangePayload(oldItem: T, newItem: T): Any? {
            return when {
                (oldItem is Post && newItem is Post) -> {
                    Payload.PostPayload(
                        newItem.now,
                        newItem.content,
                        newItem.sage,
                        newItem.replyCount,
                        newItem.getSimplifiedTitle(),
                        newItem.getSimplifiedName()
                    )
                }
                (oldItem is Comment && newItem is Comment) -> {
                    Payload.CommentPayload(
                        newItem.now,
                        newItem.content,
                        newItem.sage,
                        newItem.visible,
                        newItem.getImgUrl(),
                        newItem.getSimplifiedTitle(),
                        newItem.getSimplifiedName()
                    )
                }
                (oldItem is Trend && newItem is Trend) -> {
                    null
                }
                else -> throw Exception("Unhandled type comparison")
            }
        }
    }

    internal sealed class Payload {
        class PostPayload(
            val now: String,
            val content: String,
            val sage: String,
            val replyCount: String,
            val title: String,
            val name: String
        )

        class CommentPayload(
            val now: String,
            val content: String,
            val sage: String,
            val visible: Boolean,
            val imgUrl: String,
            val title: String,
            val name: String
        )
    }

    internal class DawnLoadMoreView(private val sharedViewModel: SharedViewModel) : BaseLoadMoreView() {
        override fun getLoadComplete(holder: BaseViewHolder): View {
            return holder.getView(R.id.load_more_load_complete_view)
        }

        override fun getLoadEndView(holder: BaseViewHolder): View {
            return holder.getView(R.id.load_more_load_end_view)
        }

        override fun getLoadFailView(holder: BaseViewHolder): View {
            return holder.getView(R.id.load_more_load_fail_view)
        }

        override fun getLoadingView(holder: BaseViewHolder): View {
            return holder.getView<LinearLayout>(R.id.load_more_loading_view).apply {
                findViewById<TextView>(R.id.loading_text).text =
                    sharedViewModel.getRandomLoadingBible()
            }
        }

        override fun getRootView(parent: ViewGroup): View {
            return LayoutInflater.from(parent.context)
                .inflate(R.layout.adapter_load_more, parent, false)
        }
    }
}