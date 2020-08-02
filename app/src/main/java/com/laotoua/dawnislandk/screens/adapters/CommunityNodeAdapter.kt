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

import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import com.chad.library.adapter.base.BaseNodeAdapter
import com.chad.library.adapter.base.entity.node.BaseExpandNode
import com.chad.library.adapter.base.entity.node.BaseNode
import com.chad.library.adapter.base.provider.BaseNodeProvider
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.laotoua.dawnislandk.R
import com.laotoua.dawnislandk.data.local.entity.Community
import com.laotoua.dawnislandk.data.local.entity.Forum
import com.laotoua.dawnislandk.screens.util.ContentTransformation.transformForumName


class CommunityNodeAdapter(val clickListener: ForumClickListener) : BaseNodeAdapter() {

    companion object {
        const val EXPAND_COLLAPSE_PAYLOAD = 110
    }

    init {
        addFullSpanNodeProvider(CommunityProvider())
        addFullSpanNodeProvider(ForumProvider())
    }

    override fun getItemType(data: List<BaseNode>, position: Int): Int {
        val node = data[position]
        if (node is CommunityNode) {
            return 1
        } else if (node is ForumNode) {
            return 2
        }
        return -1
    }

    fun setData(list: List<Community>) {
        val commonForumIds = list.firstOrNull { it.isCommonForums() }?.forums?.map { it.id } ?: emptyList()

        val nodes = mutableListOf<CommunityNode>()
        for (c in list){
            if (c.isCommonForums() || c.isCommonPosts()){
                nodes.add(CommunityNode(c))
            } else {
                val noDuplicateCommunity = Community(
                    c.id,
                    c.sort,
                    c.name,
                    c.status,
                    c.forums.filterNot { f -> commonForumIds.contains(f.id) })
                nodes.add(CommunityNode(noDuplicateCommunity))
            }
        }

        setList(nodes)
    }


    inner class CommunityProvider : BaseNodeProvider() {
        override val itemViewType: Int = 1

        override val layoutId: Int = R.layout.list_item_community

        override fun convert(helper: BaseViewHolder, item: BaseNode) {
            val community = (item as CommunityNode).community
            helper.setText(R.id.communityName, community.name)
            setArrowSpin(helper, item, false)
        }

        override fun convert(
            helper: BaseViewHolder,
            item: BaseNode,
            payloads: List<Any>
        ) {
            for (payload in payloads) {
                if (payload is Int && payload == EXPAND_COLLAPSE_PAYLOAD) {
                    // 增量刷新，使用动画变化箭头
                    setArrowSpin(helper, item, true)
                }
            }
        }

        private fun setArrowSpin(
            helper: BaseViewHolder,
            data: BaseNode,
            isAnimate: Boolean
        ) {
            val icon: ImageView = helper.getView(R.id.icon)
            if ((data as CommunityNode).isExpanded) {
                if (isAnimate) {
                    icon.animate().setDuration(200)
                        .setInterpolator(DecelerateInterpolator())
                        .rotation(0f)
                        .start()
                } else {
                    icon.rotation = 0f
                }
            } else {
                if (isAnimate) {
                    icon.animate().setDuration(200)
                        .setInterpolator(DecelerateInterpolator())
                        .rotation(180f)
                        .start()
                } else {
                    icon.rotation = 180f
                }
            }
        }

        override fun onClick(
            helper: BaseViewHolder,
            view: View,
            data: BaseNode,
            position: Int
        ) {
            // 这里使用payload进行增量刷新（避免整个item刷新导致的闪烁，不自然）
            getAdapter()!!.expandOrCollapse(
                position,
                animate = true,
                notify = true,
                parentPayload = EXPAND_COLLAPSE_PAYLOAD
            )
        }
    }


    inner class ForumProvider : BaseNodeProvider() {
        override val itemViewType: Int = 2
        override val layoutId: Int = R.layout.list_item_forum

        override fun convert(helper: BaseViewHolder, item: BaseNode) {
            val forum = (item as ForumNode).forum
            if (forum.isValidForum()) {
                val biId = if (forum.id.toInt() > 0) forum.id.toInt() else 1
                val resourceId: Int = context.resources.getIdentifier(
                    "bi_$biId", "drawable",
                    context.packageName
                )
                helper.setImageResource(R.id.forumIcon, resourceId)
            } else {
                val resourceId: Int = context.resources.getIdentifier(
                    "ic_label_24px", "drawable",
                    context.packageName
                )
                helper.setImageResource(R.id.forumIcon, resourceId)
            }
            helper.setText(
                R.id.forumName,
                transformForumName(forum.getDisplayName())
            )
        }

        override fun onClick(helper: BaseViewHolder, view: View, data: BaseNode, position: Int) {
            clickListener.onForumClick((data as ForumNode).forum)
        }
    }

    inner class CommunityNode(val community: Community) :
        BaseExpandNode() {

        init {
            isExpanded = false
        }

        override val childNode: MutableList<BaseNode> =
            community.forums.map { ForumNode(it) }.toMutableList()
    }

    inner class ForumNode(val forum: Forum) : BaseNode() {
        override val childNode: MutableList<BaseNode>? = null
    }

    interface ForumClickListener {
        fun onForumClick(forum: Forum)
    }
}


