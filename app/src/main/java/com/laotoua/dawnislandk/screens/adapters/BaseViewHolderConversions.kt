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

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.laotoua.dawnislandk.DawnApp
import com.laotoua.dawnislandk.R
import com.laotoua.dawnislandk.screens.util.ContentTransformation
import com.laotoua.dawnislandk.screens.widgets.ClickableMovementMethod
import com.laotoua.dawnislandk.screens.widgets.spans.ReferenceSpan
import com.laotoua.dawnislandk.screens.widgets.spans.RoundBackgroundColorSpan
import com.laotoua.dawnislandk.util.GlideApp
import java.time.LocalDateTime

fun BaseViewHolder.convertUserId(userId: String, admin: String, po: String = ""): BaseViewHolder {
    setText(R.id.userId, ContentTransformation.transformCookie(userId, admin, po))
    return this
}

fun BaseViewHolder.convertTimeStamp(now: String, isAd: Boolean = false): BaseViewHolder {
    setText(R.id.timestamp, ContentTransformation.transformTime(now))
    setGone(R.id.timestamp, isAd)
    return this
}

fun BaseViewHolder.convertTimeStamp(now: LocalDateTime): BaseViewHolder {
    setText(R.id.timestamp, ContentTransformation.transformTime(now))
    return this
}

fun BaseViewHolder.convertForumAndReplyCount(
    replyCount: String,
    forumDisplayName: String
): BaseViewHolder {
    val suffix = if (replyCount.isNotBlank()) " • $replyCount" else ""
    val spannableString = SpannableString(forumDisplayName + suffix)
    spannableString.setSpan(
        RoundBackgroundColorSpan(), 0, spannableString.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
    )
    getView<TextView>(R.id.forumAndReplyCount).setText(
        spannableString,
        TextView.BufferType.SPANNABLE
    )
    return this
}

fun BaseViewHolder.convertRefId(
    context: Context,
    id: String,
    isAd: Boolean = false
): BaseViewHolder {
    setText(R.id.refId, context.resources.getString(R.string.ref_id_formatted, id))
    setGone(R.id.refId, isAd)
    return this
}

fun BaseViewHolder.convertTitleAndName(
    title: String,
    name: String,
    visible: Boolean = true,
    isAd: Boolean = false
): BaseViewHolder {
    if (title.isNotBlank() && visible) {
        val span = SpannableString(title)
        if (isAd) {
            val adminColor = ForegroundColorSpan(Color.parseColor("#FF0F0F"))
            span.setSpan(adminColor, 0, span.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            span.setSpan(
                StyleSpan(Typeface.BOLD),
                0,
                span.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        getView<TextView>(R.id.title).setText(span, TextView.BufferType.SPANNABLE)
    }
    setGone(R.id.title, title.isBlank() || !visible)

    if (name.isNotBlank() && visible) {
        setText(R.id.name, name)
    }
    setGone(R.id.name, name.isBlank() || !visible)
    return this
}

fun BaseViewHolder.convertSage(sage: String?, skipConversion: Boolean = false): BaseViewHolder {
    if (sage == "1" && !skipConversion) setVisible(R.id.sage, true)
    else setGone(R.id.sage, true)
    return this
}

fun BaseViewHolder.convertImage(imgUrl: String, visible: Boolean = true): BaseViewHolder {
    if (imgUrl.isNotBlank() && visible) {
        val imageView = getView<ImageView>(R.id.attachedImage)
        GlideApp.with(imageView)
            .load(DawnApp.currentThumbCDN + imgUrl)
//                .override(400, 400)
            .fitCenter()
            .into(imageView)
        setVisible(R.id.attachedImage, true)
    } else {
        setGone(R.id.attachedImage, true)
    }
    return this
}

fun BaseViewHolder.convertContent(
    context: Context,
    content: String,
    referenceClickListener: ReferenceSpan.ReferenceClickHandler? = null,
    visible: Boolean = true
): BaseViewHolder {
    val res = ContentTransformation.transformContent(
        context = context,
        content = content,
        referenceClickListener = referenceClickListener
    )
    if (res.isNotBlank()) setText(R.id.content, res)
    setGone(R.id.content, res.isBlank() || !visible)
    return this
}

fun BaseViewHolder.convertExpandSummary(context: Context, visible: Boolean): BaseViewHolder {
    if (!visible) {
        setText(
            R.id.expandSummary,
            context.resources.getString(
                R.string.checked_filtered_comment,
                getView<TextView>(R.id.content).text.count()
            )
        )
    }
    setGone(R.id.expandSummary, visible)
    return this
}

fun View.applyTextSizeAndLetterSpacing(clickable: Boolean = false) = apply {
    findViewById<TextView>(R.id.content).apply {
        if (clickable) movementMethod = ClickableMovementMethod.getInstance()
        if (DawnApp.applicationDataStore.getLayoutCustomizationStatus()) {
            textSize = DawnApp.applicationDataStore.textSize
            letterSpacing = DawnApp.applicationDataStore.letterSpace
        }
    }
}