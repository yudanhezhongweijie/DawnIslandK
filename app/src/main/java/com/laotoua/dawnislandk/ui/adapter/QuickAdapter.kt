package com.laotoua.dawnislandk.ui.adapter

import android.graphics.Color
import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.loadmore.BaseLoadMoreView
import com.chad.library.adapter.base.module.LoadMoreModule
import com.chad.library.adapter.base.util.getItemView
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.google.android.material.card.MaterialCardView
import com.laotoua.dawnislandk.R
import com.laotoua.dawnislandk.data.entity.Reply
import com.laotoua.dawnislandk.data.entity.Thread
import com.laotoua.dawnislandk.data.entity.Trend
import com.laotoua.dawnislandk.ui.span.RoundBackgroundColorSpan
import com.laotoua.dawnislandk.ui.util.ContentTransformationUtil
import com.laotoua.dawnislandk.ui.util.GlideApp
import com.laotoua.dawnislandk.ui.viewfactory.ThreadCardFactory
import com.laotoua.dawnislandk.util.Constants
import com.laotoua.dawnislandk.viewmodel.SharedViewModel
import com.tencent.mmkv.MMKV
import timber.log.Timber


// TODO: handle no new data exception
class QuickAdapter(private val layoutResId: Int) :
    BaseQuickAdapter<Any, BaseViewHolder>(layoutResId, ArrayList()),
    LoadMoreModule {

    private val thumbCDN = Constants.thumbCDN
    private lateinit var sharedViewModel: SharedViewModel
    private lateinit var referenceClickListener: (String) -> Unit

    private val mLetterSpace by lazy { MMKV.defaultMMKV().getFloat(Constants.LETTER_SPACE, 0f) }
    private val mLineHeight by lazy { MMKV.defaultMMKV().getInt(Constants.LINE_HEIGHT, 0) }
    private val mSegGap by lazy { MMKV.defaultMMKV().getInt(Constants.SEG_GAP, 0) }
    private val mTextSize by lazy { MMKV.defaultMMKV().getFloat(Constants.MAIN_TEXT_SIZE, 15f) }

    // TODO: support multiple Po
    private var po: String = ""

    init {
        // 所有数据加载完成后，是否允许点击（默认为false）
        loadMoreModule.enableLoadMoreEndClick = true

        // 当数据不满一页时，是否继续自动加载（默认为true）
        loadMoreModule.isEnableLoadMoreIfNotFullPage = false

        setAnimationWithDefault(AnimationType.ScaleIn)
        isAnimationFirstOnly = false
        setDiffCallback(DiffItemCallback())
    }

    fun setSharedVM(vm: SharedViewModel) {
        this.sharedViewModel = vm
    }

    fun setPo(po: String) {
        this.po = po
    }

    fun setReferenceClickListener(referenceClickListener: (String) -> Unit) {
        this.referenceClickListener = referenceClickListener
    }

    /** default handler for recyclerview item
     *
     */
    override fun convert(holder: BaseViewHolder, item: Any) {
        if (layoutResId == R.layout.list_item_thread && item is Thread) {
            holder.convertThread(item, sharedViewModel.getForumDisplayName(item.fid!!))
        } else if (layoutResId == R.layout.list_item_reply && item is Reply) {
            holder.convertReply(item, po)
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

    override fun convert(holder: BaseViewHolder, item: Any, payloads: List<Any>) {
        if (layoutResId == R.layout.list_item_thread && item is Thread) {
            holder.convertThreadWithPayload(
                payloads.first() as Payload.ThreadPayload,
                sharedViewModel.getForumDisplayName(item.fid!!)
            )
        } else if (layoutResId == R.layout.list_item_reply && item is Reply) {
            holder.convertReplyWithPayload(payloads.first() as Payload.ReplyPayload)
        } else {
            Timber.e("unhandled payload conversion")
            throw Exception("unhandled payload conversion")
        }
    }

    override fun onCreateDefViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
        return if (layoutResId == R.layout.list_item_thread) {
            val view = parent.getItemView(layoutResId)
            ThreadCardFactory.applySettings(view as MaterialCardView)
            createBaseViewHolder(view)
        } else {
            super.onCreateDefViewHolder(parent, layoutResId)
        }
    }


    private fun BaseViewHolder.convertThread(item: Thread, forumDisplayName: String) {
        convertUserId(item.userid, item.admin)
        convertTimeStamp(item.now)
        convertForumAndReply(item.replyCount, forumDisplayName)
        convertSage(item.sage)
        convertImage(item.img, item.ext)
        convertContent(item.content, false)
    }

    private fun BaseViewHolder.convertThreadWithPayload(
        payload: Payload.ThreadPayload, forumDisplayName: String
    ) {
        convertTimeStamp(payload.now)
        convertForumAndReply(payload.replyCount, forumDisplayName)
        convertSage(payload.sage)
        convertContent(payload.content, false)
    }

    private fun BaseViewHolder.convertReply(item: Reply, po: String) {
        convertUserId(item.userid, item.admin!!, po)
        convertTimeStamp(item.now)
        convertSage(item.sage)
        convertImage(item.img, item.ext)
        convertContent(item.content, true, referenceClickListener)
        convertRefId(item.id)
        convertTitleAndName(item.title, item.name)
    }

    private fun BaseViewHolder.convertReplyWithPayload(
        payload: Payload.ReplyPayload
    ) {
        convertTimeStamp(payload.now)
        convertSage(payload.sage)
        convertContent(payload.content, true, referenceClickListener)
    }

    private fun BaseViewHolder.convertTrend(item: Trend) {
        setText(R.id.trendRank, item.rank)
        convertRefId(item.id)
        setText(R.id.trendForum, item.forum)
        setText(R.id.hits, item.hits)
        convertContent(item.content, false)
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


    private fun BaseViewHolder.convertUserId(
        userId: String,
        admin: String,
        po: String = ""
    ) {
        setText(R.id.userId, ContentTransformationUtil.transformCookie(userId, admin, po))
    }

    private fun BaseViewHolder.convertTimeStamp(now: String) {
        setText(R.id.timestamp, ContentTransformationUtil.transformTime(now))
    }

    private fun BaseViewHolder.convertForumAndReply(replyCount: String?, forumDisplayName: String) {
        val suffix = if (replyCount != null) " • $replyCount" else ""
        val spannableString = SpannableString(forumDisplayName + suffix)
        spannableString.setSpan(
            RoundBackgroundColorSpan(
                Color.parseColor("#12DBD1"),
                Color.parseColor("#FFFFFF")
            ), 0, spannableString.length, Spanned.SPAN_INCLUSIVE_EXCLUSIVE
        )

        getView<TextView>(R.id.forumAndReplyCount)
            .setText(spannableString, TextView.BufferType.SPANNABLE)
    }

    private fun BaseViewHolder.convertRefId(id: String) {
        // TODO: handle ads
        setText(R.id.refId, id)
    }

    private fun BaseViewHolder.convertTitleAndName(title: String?, name: String?) {
        val titleAndName =
            ContentTransformationUtil.transformTitleAndName(
                title,
                name
            )
        if (titleAndName != "") {
            setText(R.id.titleAndName, titleAndName)
            setVisible(R.id.titleAndName, true)
        } else {
            setGone(R.id.titleAndName, true)
        }
    }

    private fun BaseViewHolder.convertSage(sage: String?) {
        if (sage == "1") {
            setVisible(R.id.sage, true)
        } else {
            setGone(R.id.sage, true)
        }
    }

    private fun BaseViewHolder.convertImage(img: String, ext: String) {
        if (img != "") {
            GlideApp.with(context)
                .load(thumbCDN + img + ext)
                .override(100, 100)
                .fitCenter()
                .into(getView(R.id.attachedImage))
            setVisible(R.id.attachedImage, true)
        } else {
            setGone(R.id.attachedImage, true)
        }
    }

    private fun BaseViewHolder.convertContent(
        content: String,
        clickable: Boolean,
        referenceClickListener: ((String) -> Unit)? = null
    ) {
        val res = ContentTransformationUtil.transformContent(
            content,
            mLineHeight,
            mSegGap,
            referenceClickListener
        )

        if (res.isEmpty()) setGone(R.id.content, true)
        else {
            setText(R.id.content, res)
            setVisible(R.id.content, true)
            getView<TextView>(R.id.content).apply {
                /**
                 *  special handler for clickable spans
                 */
                if (clickable) movementMethod = LinkMovementMethod.getInstance()
                textSize = mTextSize
                letterSpacing = mLetterSpace
            }
        }
    }

    private class DiffItemCallback : DiffUtil.ItemCallback<Any>() {

        /**
         * 判断是否是同一个item
         *
         * @param oldItem New data
         * @param newItem old Data
         * @return
         */
        override fun areItemsTheSame(
            oldItem: Any,
            newItem: Any
        ): Boolean {
            return when {
                (oldItem is Thread && newItem is Thread) -> oldItem.id == newItem.id && oldItem.fid == newItem.fid

                (oldItem is Reply && newItem is Reply) -> oldItem.id == newItem.id

                (oldItem is Trend && newItem is Trend) -> oldItem.id == newItem.id

                else -> {
                    Timber.e("Unhandled type comparison $oldItem vs $newItem")
                    throw Exception("Unhandled type comparison")
                }
            }
        }

        /**
         * 当是同一个item时，再判断内容是否发生改变
         *
         * @param oldItem New data
         * @param newItem old Data
         * @return
         */
        override fun areContentsTheSame(
            oldItem: Any,
            newItem: Any
        ): Boolean {
            return when {
                (oldItem is Thread && newItem is Thread) -> {
                    oldItem.now == newItem.now
                            && oldItem.sage == newItem.sage
                            && oldItem.replyCount == newItem.replyCount
                            && oldItem.content == newItem.content
                }
                (oldItem is Reply && newItem is Reply) -> {
                    oldItem.now == newItem.now
                            && oldItem.sage == newItem.sage
                            && oldItem.content == newItem.content
                }

                (oldItem is Trend && newItem is Trend) -> {
                    true
                }
                else -> {
                    Timber.e("Unhandled type comparison")
                    throw Exception("Unhandled type comparison")
                }
            }
        }

        /**
         * 可选实现
         * 如果需要精确修改某一个view中的内容，请实现此方法。
         * 如果不实现此方法，或者返回null，将会直接刷新整个item。
         *
         * @param oldItem Old data
         * @param newItem New data
         * @return Payload info. if return null, the entire item will be refreshed.
         */
        override fun getChangePayload(
            oldItem: Any,
            newItem: Any
        ): Any? {
            return when {
                (oldItem is Thread && newItem is Thread) -> {
                    Payload.ThreadPayload(
                        newItem.now,
                        newItem.content,
                        newItem.sage,
                        newItem.replyCount
                    )
                }
                (oldItem is Reply && newItem is Reply) -> {
                    Payload.ReplyPayload(newItem.now, newItem.content, newItem.sage)
                }
                (oldItem is Trend && newItem is Trend) -> {
                    null
                }
                else -> {
                    Timber.e("Unhandled type comparison")
                    null
                }
            }
        }
    }

    internal sealed class Payload {
        class ThreadPayload(
            val now: String,
            val content: String,
            val sage: String?,
            val replyCount: String?
        )

        class ReplyPayload(
            val now: String,
            val content: String,
            val sage: String?
        )
    }

    private class DiffCallback(private val oldList: List<Any>, private val newList: List<Any>) :
        DiffUtil.Callback() {
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val oldItem = oldList[oldItemPosition]
            val newItem = newList[newItemPosition]
            return when {
                (oldItem is Thread && newItem is Thread) -> oldItem.id == newItem.id && oldItem.fid == newItem.fid

                (oldItem is Reply && newItem is Reply) -> oldItem.id == newItem.id

                else -> {
                    Timber.e("Unhandled type comparison")
                    false
                }
            }
        }

        override fun getOldListSize(): Int {
            return oldList.size
        }

        override fun getNewListSize(): Int {
            return newList.size
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val oldItem = oldList[oldItemPosition]
            val newItem = newList[newItemPosition]
            return when {
                (oldItem is Thread && newItem is Thread) -> {
                    oldItem.sage == newItem.sage
                            && oldItem.replyCount == newItem.replyCount
                            && oldItem.content == newItem.content
                }
                (oldItem is Reply && newItem is Reply) -> {
                    oldItem.sage == newItem.sage
                            && oldItem.content == newItem.content
                }
                else -> {
                    Timber.e("Unhandled type comparison")
                    false
                }
            }
        }


    }


    // TODO
    private class DawnLoadMoreView : BaseLoadMoreView() {
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
}