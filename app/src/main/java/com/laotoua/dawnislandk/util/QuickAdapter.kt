package com.laotoua.dawnislandk.util

import android.os.Build
import android.text.Html
import android.text.SpannableString
import android.text.Spanned
import android.util.Log
import androidx.core.text.HtmlCompat
import com.bumptech.glide.Glide
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.module.LoadMoreModule
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.laotoua.dawnislandk.R
import com.laotoua.dawnislandk.viewmodels.SharedViewModel

// TODO: handle no new data exception
class QuickAdapter(private val layoutResId: Int) :
    BaseQuickAdapter<Any, BaseViewHolder>(layoutResId, ArrayList()),
    LoadMoreModule {
    private val TAG = "QuickAdapter"
    private val thumbCDN = "https://nmbimg.fastmirror.org/thumb/"
    private lateinit var sharedViewModel: SharedViewModel

    init {
        // 所有数据加载完成后，是否允许点击（默认为false）
        this.loadMoreModule!!.enableLoadMoreEndClick = true

        // 当数据不满一页时，是否继续自动加载（默认为true）
        this.loadMoreModule!!.isEnableLoadMoreIfNotFullPage = false;

    }

    fun setSharedVM(vm: SharedViewModel) {
        this.sharedViewModel = vm
    }

    override fun convert(helper: BaseViewHolder, item: Any) {

        if (layoutResId == R.layout.forum_list_item && item is Forum) {
            convertForum(helper, item)
        } else if (layoutResId == R.layout.thread_list_item && item is ThreadList) {
            convertThread(helper, item)
        } else if (layoutResId == R.layout.reply_list_item && item is Reply) {
            convertReply(helper, item)
        }
    }

    private fun convertForum(card: BaseViewHolder, item: Forum) {
        // special handling for drawable resource ID, which cannot have -
        val biId = if (item.id.toInt() > 0) item.id.toInt() else 1
        val resourceId: Int = context.getResources().getIdentifier(
            "bi_$biId", "drawable",
            context.packageName
        )
        card.setImageResource(R.id.forumIcon, resourceId)

        val forumName = item.getDisplayName()
        val displayName: Spanned = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            Html.fromHtml(forumName)
        } else {
            Html.fromHtml(forumName, Html.FROM_HTML_MODE_COMPACT)
        }
        card.setText(R.id.forumName, displayName)

    }

    private fun convertThread(card: BaseViewHolder, item: ThreadList) {
        // add fix to spannable
        // TODO: add admin check
        card.setText(R.id.threadCookie, item.userid)
        card.setText(R.id.threadTime, item.now)
        card.setText(R.id.threadReplyCount, "Replays: " + item.replyCount)

        card.setText(R.id.threadForum, sharedViewModel.getForumDisplayName(item.fid))

        // TODO: add sage formatting
        // sage
        if (item.sage == "0") {
            card.setVisible(R.id.sage, false)
        }

        // TODO: check why images are not displaying sometime
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

        // TODO: handle quotation
        // spannable content
        val s = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            SpannableString(Html.fromHtml(item.content, HtmlCompat.FROM_HTML_MODE_COMPACT))
        } else {
            SpannableString(Html.fromHtml(item.content))
        }
        card.setText(R.id.threadContent, s)
    }

    fun convertReply(card: BaseViewHolder, item: Reply) {
        //TODO: add admin check
        card.setText(R.id.replyCookie, item.userid)
        card.setText(R.id.replyTime, item.now)
        // TODO: handle ads
        card.setText(R.id.replyId, item.id)

        // TODO: add sage display
//        if (item.sage == "0") {
//            card.setVisible(R.id.sage, false)
//        }

        // load image
        if (item.img != "") {
            Glide.with(context)
                .load(thumbCDN + item.img + item.ext)
                .override(100, 100)
                .fitCenter()
                .into(card.getView(R.id.replyImage))
            Log.i(TAG, "added Thumbnail from URL: ${thumbCDN + item.img + item.ext}")
        } else {
            card.setGone(R.id.replyImage, true)
        }

        // TODO: handle quotation
        // spannable content
        val s = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            SpannableString(Html.fromHtml(item.content, HtmlCompat.FROM_HTML_MODE_COMPACT))
        } else {
            SpannableString(Html.fromHtml(item.content))
        }
        card.setText(R.id.replyContent, s)
    }
}