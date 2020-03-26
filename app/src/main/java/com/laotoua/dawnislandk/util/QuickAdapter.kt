package com.laotoua.dawnislandk.util

import android.os.Build
import android.text.Html
import android.text.SpannableString
import android.util.Log
import androidx.core.text.HtmlCompat
import com.bumptech.glide.Glide
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.module.LoadMoreModule
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.laotoua.dawnislandk.R

class QuickAdapter(private val layoutResId: Int) :
    BaseQuickAdapter<Any, BaseViewHolder>(layoutResId, ArrayList()),
    LoadMoreModule {
    private val TAG = "ForumQuickAdapter"
    private val thumbCDN = "https://nmbimg.fastmirror.org/thumb/"

    init {
        // 所有数据加载完成后，是否允许点击（默认为false）
        this.loadMoreModule!!.enableLoadMoreEndClick = true
    }

    override fun convert(card: BaseViewHolder, item: Any) {

        if (layoutResId == R.layout.forum_list_item && item is Forum) {
            convertForum(card, item)
        } else if (layoutResId == R.layout.thread_list_item && item is ThreadList) {
            convertThread(card, item)
        }
    }

    private fun convertForum(card: BaseViewHolder, item: Forum) {
        card.setText(R.id.forumName, item.name)
    }

    private fun convertThread(card: BaseViewHolder, item: ThreadList) {
        // add fix to spannable
        card.setText(R.id.threadCookie, item.userid)
        card.setText(R.id.threadTime, item.now)
        card.setText(R.id.threadReplyCount, "Replays: " + item.replyCount)
        // TODO: forumname

        card.setText(R.id.threadForum, "forum")
//        card.setText(R.id.id, "No. "+ item.id)
        // sage
        if (item.sage == "0") {
            card.setVisible(R.id.sage, false)
        }

        // load image
        if (item.img != "") {
            Glide.with(context)
                .load(thumbCDN + item.img + item.ext)
                .override(100, 100)
                .fitCenter()
                .into(card.getView(R.id.threadImage))
            Log.i(TAG, "added Thumbnail from URL: ${thumbCDN + item.img + item.ext}")
        } else {
            card.setGone(R.id.threadImage, true)
        }

        // spannable content
        val s = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            SpannableString(Html.fromHtml(item.content, HtmlCompat.FROM_HTML_MODE_COMPACT))
        } else {
            SpannableString(Html.fromHtml(item.content))
        }
        card.setText(R.id.threadContent, s)
    }
}