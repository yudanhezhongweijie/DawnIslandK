package com.laotoua.dawnislandk.ui.adapter

import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import androidx.core.view.ViewCompat
import com.chad.library.adapter.base.BaseNodeAdapter
import com.chad.library.adapter.base.entity.node.BaseExpandNode
import com.chad.library.adapter.base.entity.node.BaseNode
import com.chad.library.adapter.base.provider.BaseNodeProvider
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.laotoua.dawnislandk.R
import com.laotoua.dawnislandk.data.entity.Community
import com.laotoua.dawnislandk.data.entity.Forum
import com.laotoua.dawnislandk.ui.util.ContentTransformationUtil.transformForumName


class QuickNodeAdapter(val clickListener: ForumClickListener) : BaseNodeAdapter() {

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
        val l = list.map {
            CommunityNode(it)
        }

        setList(l)
    }


    inner class CommunityProvider : BaseNodeProvider() {
        override val itemViewType: Int
            get() = 1

        override val layoutId: Int
            get() = R.layout.list_item_community

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
            val icon: ImageView = helper.getView(R.id.arrow)
            if ((data as CommunityNode).isExpanded) {
                if (isAnimate) {
                    ViewCompat.animate(icon).setDuration(200)
                        .setInterpolator(DecelerateInterpolator())
                        .rotation(0f)
                        .start()
                } else {
                    icon.rotation = 0f
                }
            } else {
                if (isAnimate) {
                    ViewCompat.animate(icon).setDuration(200)
                        .setInterpolator(DecelerateInterpolator())
                        .rotation(90f)
                        .start()
                } else {
                    icon.rotation = 90f
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
        override val itemViewType: Int
            get() = 2
        override val layoutId: Int
            get() = R.layout.list_item_forum

        override fun convert(helper: BaseViewHolder, item: BaseNode) {
            val forum = (item as ForumNode).forum
            val biId = if (forum.id.toInt() > 0) forum.id.toInt() else 1
            val resourceId: Int = context.resources.getIdentifier(
                "bi_$biId", "drawable",
                context.packageName
            )
            helper.setText(
                R.id.forumName,
                transformForumName(forum.getDisplayName())
            )
            helper.setImageResource(R.id.forumIcon, resourceId)
        }

        override fun onClick(helper: BaseViewHolder, view: View, data: BaseNode, position: Int) {
            clickListener.onForumClick((data as ForumNode).forum)
        }
    }

    inner class CommunityNode(val community: Community) :
        BaseExpandNode() {
        private val _childNode = community.forums.map { ForumNode(it) }.toMutableList()

        init {
            isExpanded = false
        }

        override val childNode: MutableList<BaseNode>?
            get() = _childNode as MutableList<BaseNode>
    }

    inner class ForumNode(val forum: Forum) : BaseNode() {
        override val childNode: MutableList<BaseNode>? get() = null
    }

    interface ForumClickListener {
        fun onForumClick(forum: Forum)
    }
}


