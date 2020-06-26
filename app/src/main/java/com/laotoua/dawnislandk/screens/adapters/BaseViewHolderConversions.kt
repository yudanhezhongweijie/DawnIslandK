package com.laotoua.dawnislandk.screens.adapters

import android.content.Context
import android.graphics.Color
import android.text.SpannableString
import android.text.Spanned
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.laotoua.dawnislandk.DawnApp
import com.laotoua.dawnislandk.R
import com.laotoua.dawnislandk.screens.util.ContentTransformation
import com.laotoua.dawnislandk.screens.widget.ClickableMovementMethod
import com.laotoua.dawnislandk.screens.widget.span.ReferenceSpan
import com.laotoua.dawnislandk.screens.widget.span.RoundBackgroundColorSpan
import com.laotoua.dawnislandk.util.DawnConstants
import com.laotoua.dawnislandk.util.GlideApp


fun BaseViewHolder.convertUserId(userId: String, admin: String, po: String = "") {
    setText(R.id.userId, ContentTransformation.transformCookie(userId, admin, po))
}

fun BaseViewHolder.convertTimeStamp(now: String) {
    setText(R.id.timestamp, ContentTransformation.transformTime(now))
}

fun BaseViewHolder.convertTimeStamp(now: Long) {
    setText(R.id.timestamp, ContentTransformation.transformTime(now))
}

fun BaseViewHolder.convertForumAndReplyCount(replyCount: String, forumDisplayName: String) {
    val suffix = if (replyCount.isNotBlank()) " • $replyCount" else ""
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

fun BaseViewHolder.convertRefId(context: Context, id: String) {
    // TODO: handle ads
    setText(R.id.refId, context.resources.getString(R.string.ref_id_formatted, id))
}

fun BaseViewHolder.convertTitleAndName(
    title: String,
    name: String,
    visible: Boolean = true
) {
    if (title.isNotBlank() && visible) {
        setText(R.id.title, title)
        setVisible(R.id.title, true)
    } else {
        setGone(R.id.title, true)
    }
    if (name.isNotBlank() && visible) {
        setText(R.id.name, name)
        setVisible(R.id.name, true)
    } else {
        setGone(R.id.name, true)
    }
}

fun BaseViewHolder.convertSage(sage: String?) {
    if (sage == "1") {
        setVisible(R.id.sage, true)
    } else {
        setGone(R.id.sage, true)
    }
}

fun BaseViewHolder.convertImage(imgUrl: String, visible: Boolean = true) {
    if (imgUrl.isNotBlank() && visible) {
        val imageView = getView<ImageView>(R.id.attachedImage)
        GlideApp.with(imageView)
            .load(DawnConstants.thumbCDN + imgUrl)
//                .override(400, 400)
            .fitCenter()
            .into(imageView)
        setVisible(R.id.attachedImage, true)
    } else {
        setGone(R.id.attachedImage, true)
    }
}

fun BaseViewHolder.convertContent(
    context: Context,
    content: String,
    referenceClickListener: ReferenceSpan.ReferenceClickHandler? = null,
    visible: Boolean = true
) {
    val res = ContentTransformation.transformContent(
        context = context,
        content = content,
        referenceClickListener = referenceClickListener
    )
    if (res.isNotBlank()) setText(R.id.content, res)
    if (res.isBlank() || !visible) setGone(R.id.content, true)
    else setVisible(R.id.content, true)
}

fun BaseViewHolder.convertExpandSummary(context: Context, visible: Boolean) {
    if (!visible) {
        setText(
            R.id.expandSummary,
            context.resources.getString(
                R.string.checked_filtered_comment,
                getView<TextView>(R.id.content).text.count()
            )
        )
        setVisible(R.id.expandSummary, true)
    } else {
        setGone(R.id.expandSummary, true)
    }
}

fun View.applyTextSizeAndLetterSpacing(clickable: Boolean = false) = apply {
    findViewById<TextView>(R.id.content).apply {
        if (clickable) movementMethod = ClickableMovementMethod.getInstance()
        textSize = DawnApp.applicationDataStore.textSize
        letterSpacing = DawnApp.applicationDataStore.letterSpace
    }
}