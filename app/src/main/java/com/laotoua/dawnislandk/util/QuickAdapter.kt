package com.laotoua.dawnislandk.util

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import com.bumptech.glide.Glide
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.loadmore.BaseLoadMoreView
import com.chad.library.adapter.base.module.LoadMoreModule
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.laotoua.dawnislandk.R
import com.laotoua.dawnislandk.viewmodels.SharedViewModel
import kotlinx.android.synthetic.main.quote_list_item.view.*


// TODO: handle no new data exception
class QuickAdapter(private val layoutResId: Int) :
    BaseQuickAdapter<Any, BaseViewHolder>(layoutResId, ArrayList()),
    LoadMoreModule {
    private val thumbCDN = "https://nmbimg.fastmirror.org/thumb/"
    private lateinit var sharedViewModel: SharedViewModel

    init {
        // 所有数据加载完成后，是否允许点击（默认为false）
        this.loadMoreModule.enableLoadMoreEndClick = true

        // 当数据不满一页时，是否继续自动加载（默认为true）
        this.loadMoreModule.isEnableLoadMoreIfNotFullPage = false

    }

    fun setSharedVM(vm: SharedViewModel) {
        this.sharedViewModel = vm
    }

    /** default handler for recyclerview item
     *
     */
    override fun convert(helper: BaseViewHolder, item: Any) {
        if (layoutResId == R.layout.forum_list_item && item is Forum) {
            convertForum(helper, item)
        } else if (layoutResId == R.layout.thread_list_item && item is ThreadList) {
            convertThread(helper, item, sharedViewModel.getForumDisplayName(item.fid!!))
        } else if (layoutResId == R.layout.reply_list_item && item is Reply) {
            convertReply(helper, item, sharedViewModel.getPo())
        }
    }


    private fun convertForum(card: BaseViewHolder, item: Forum) {
        // special handling for drawable resource ID, which cannot have -
        val biId = if (item.id.toInt() > 0) item.id.toInt() else 1
        val resourceId: Int = context.resources.getIdentifier(
            "bi_$biId", "drawable",
            context.packageName
        )
        card.setImageResource(R.id.forumIcon, resourceId)

        card.setText(R.id.forumName, formatForumName(item.getDisplayName()))

    }


    private fun convertThread(card: BaseViewHolder, item: ThreadList, forumDisplayName: String) {

        card.setText(R.id.threadCookie, formatCookie(item.userid, item.admin))
        card.setText(R.id.threadTime, formatTime(item.now))

        card.setText(
            R.id.threadForumAndReplyCount, forumDisplayName + " • " + item.replyCount
        )

        // TODO: add sage formatting
        // sage
        if (item.sage == "1") {
            card.setVisible(R.id.sage, false)
        } else {
            card.setGone(R.id.sage, true)
        }

        // load image
        if (item.img != "") {
            Glide.with(context)
                .load(thumbCDN + item.img + item.ext)
                .override(100, 100)
                .fitCenter()
                .into(card.getView(R.id.threadImage))
            card.setVisible(R.id.threadImage, true)
        } else {
            card.setGone(R.id.threadImage, true)
        }

        // TODO: handle quotation
        val quotes = extractQuote(item.content)
        val quotesContainer: LinearLayout = card.getView(R.id.threadQuotes)
        quotesContainer.removeAllViews()
        quotes.map {
            val q = LayoutInflater.from(context)
                .inflate(R.layout.quote_list_item, quotesContainer, false)
            q.quoteId.text = "No. $it"
            quotesContainer.addView(q)
        }

        card.setText(R.id.threadContent, formatContent(removeQuote(item.content)))

    }

    private fun convertReply(card: BaseViewHolder, item: Reply, po: String) {

        card.setText(R.id.replyCookie, formatCookie(item.userid, item.admin!!, po))

        card.setText(R.id.replyTime, formatTime(item.now))
        // TODO: handle ads
        card.setText(R.id.replyId, item.id)

        // TODO: add sage formatting
        if (item.sage == "1") {
            card.setVisible(R.id.sage, true)
        } else {
            card.setGone(R.id.sage, true)
        }

        val titleAndName = formatTitleAndName(item.title, item.name)
        if (titleAndName != "") {
            card.setText(R.id.replyTitleAndName, titleAndName)
            card.setVisible(R.id.replyTitleAndName, true)
        } else {
            card.setGone(R.id.replyTitleAndName, true)
        }

        // load image
        if (item.img != "") {
            Glide.with(context)
                .load(thumbCDN + item.img + item.ext)
                .override(250, 250)
                .fitCenter()
                .into(card.getView(R.id.replyImage))
            card.setVisible(R.id.replyImage, true)
        } else {
            card.setGone(R.id.replyImage, true)
        }

        // TODO: need quotation handler, should be done in view however
        val quotesContainer: LinearLayout = card.getView(R.id.replyQuotes)
        val quotes = extractQuote(item.content)
        if (quotes.size > 0) {
            quotesContainer.removeAllViews()
            quotes.map {
                val q = LayoutInflater.from(context)
                    .inflate(R.layout.quote_list_item, quotesContainer, false)
                q.quoteId.text = "No. $it"
                quotesContainer.addView(q)
            }
            card.setVisible(R.id.replyQuotes, true)
        } else {
            card.setGone(R.id.replyQuotes, true)
        }

        card.setText(R.id.replyContent, formatContent(removeQuote(item.content)))
    }
}

// TODO
class DawnLoadMoreView : BaseLoadMoreView() {
    override fun getLoadComplete(holder: BaseViewHolder): View {
        TODO("Not yet implemented")
//        return holder.findView(R.id.load_more_load_complete_view);
    }

    override fun getLoadEndView(holder: BaseViewHolder): View {
        TODO("Not yet implemented")
//        return holder.findView(R.id.load_more_load_end_view);
    }

    override fun getLoadFailView(holder: BaseViewHolder): View {
        TODO("Not yet implemented")
//        return holder.findView(R.id.load_more_load_fail_view);
    }

    override fun getLoadingView(holder: BaseViewHolder): View {
        TODO("Not yet implemented")
//        return holder.findView(R.id.load_more_loading_view);
    }

    override fun getRootView(parent: ViewGroup): View {
        TODO("Not yet implemented")
        // 布局中 “加载失败”的View
//        return LayoutInflater.from(parent.getContext()).inflate(R.layout.view_load_more, parent, false);
    }

}