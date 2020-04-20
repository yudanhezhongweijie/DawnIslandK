package com.laotoua.dawnislandk.ui.adapter

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.text.*
import android.text.style.BackgroundColorSpan
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.View
import android.widget.TextView
import androidx.core.text.HtmlCompat
import com.laotoua.dawnislandk.util.ReadableTime


fun transformForumName(forumName: String): Spanned {
    return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
        Html.fromHtml(forumName)
    } else {
        Html.fromHtml(forumName, Html.FROM_HTML_MODE_COMPACT)
    }
}

fun transformCookie(userid: String, admin: String, po: String = ""): Spannable {
    /*
      处理饼干
      PO需要加粗
      普通饼干是灰色，po是黑色，红名是红色
     */
    val cookie = SpannableStringBuilder(userid)
    if (admin == "1") {
        val adminColor = ForegroundColorSpan(Color.parseColor("#FF0F0F"))
        cookie.setSpan(adminColor, 0, cookie.length, Spannable.SPAN_INCLUSIVE_EXCLUSIVE)
        // TODO: support multiple po
    } else if (userid == po) {
        val poColor = ForegroundColorSpan(Color.parseColor("#000000"))
        cookie.setSpan(poColor, 0, cookie.length, Spannable.SPAN_INCLUSIVE_EXCLUSIVE)
    }
    if (userid == po) {
        val styleSpanBold = StyleSpan(Typeface.BOLD)
        cookie.setSpan(styleSpanBold, 0, cookie.length, Spannable.SPAN_INCLUSIVE_EXCLUSIVE)
    }
    return cookie
}

fun transformTime(now: String): String {
    return ReadableTime.getDisplayTime(now)
}

fun transformTitleAndName(title: String? = "", name: String? = ""): String {
    var titleAndName = ""
    if (title != null && title != "" && title != "无标题") {
        titleAndName += "标题：$title"
    }
    if (name != null && name != "" && name != "无名氏") {
        if (titleAndName.isNotEmpty()) {
            titleAndName += "\n"
        }
        titleAndName += "作者：$name"
    }
    return titleAndName
}

fun extractQuote(content: String): List<String> {
    /** api response
    <font color=\"#789922\">&gt;&gt;No.23527403</font>
     */
    val regex = """&gt;&gt;No.\d+""".toRegex()

    return regex.findAll(content).toList().map {
        it.value.substring(11)
    }

}

fun removeQuote(content: String): String {
    /** api response
    <font color=\"#789922\">&gt;&gt;No.23527403</font>
     */
    val regex = """<font color="#789922">.*</font>""".toRegex()
    val regex2 = """<font color="#789922">.*</font><br ?/?>""".toRegex()
    return regex.replace(regex2.replace(content, ""), "")
}


fun transformContent(content: String): SpannableStringBuilder {

    val nonHide = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        SpannableStringBuilder(Html.fromHtml(content, HtmlCompat.FROM_HTML_MODE_COMPACT))
    } else {
        SpannableStringBuilder(Html.fromHtml(content))
    }

    return transformHideContent(nonHide)
}

fun transformHideContent(content: SpannableStringBuilder): SpannableStringBuilder {
    var index: Int
    var hideStart: Int
    var hideEnd: Int
    hideStart = content.indexOf("[h]")
    hideEnd = content.indexOf("[/h]")
    while (hideStart != -1 && hideEnd != -1 && hideStart < hideEnd) {
        content.delete(hideStart, hideStart + 3)
        content.delete(hideEnd - 3, hideEnd + 1)
        val foregroundColorSpan = ForegroundColorSpan(Color.TRANSPARENT)
        val backgroundColorSpan = BackgroundColorSpan(Color.parseColor("#555555"))
        content.setSpan(
            backgroundColorSpan,
            hideStart,
            hideEnd - 3,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        content.setSpan(
            foregroundColorSpan,
            hideStart,
            hideEnd - 3,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        val clickableSpan: ClickableSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                if (widget is TextView) {
                    val charSequence = widget.text
                    if (charSequence is Spannable) {
                        charSequence.removeSpan(backgroundColorSpan)
                        charSequence.removeSpan(foregroundColorSpan)
                        widget.highlightColor = Color.TRANSPARENT
                    }
                }
            }

            // overrides, DO NOT CREATE PAINT
            override fun updateDrawState(ds: TextPaint) {
            }
        }
        content.setSpan(clickableSpan, hideStart, hideEnd - 3, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        index = hideEnd - 3
        hideStart = content.indexOf("[h]", index)
        hideEnd = content.indexOf("[/h]", index)
    }
    return content
}

fun dip2px(context: Context, dipValue: Float): Int {
    val displayMetrics: DisplayMetrics = context.resources.displayMetrics
    return TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        dipValue,
        displayMetrics
    ).toInt()
}

fun extractQuoteId(string: String): String {
    val regex = """\D""".toRegex()
    return string.replace(regex, "")
}