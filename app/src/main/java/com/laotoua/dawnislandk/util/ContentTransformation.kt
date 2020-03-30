package com.laotoua.dawnislandk.util

import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.text.*
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import androidx.core.text.HtmlCompat


fun formatForumName(forumName: String): Spanned {
    return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
        Html.fromHtml(forumName)
    } else {
        Html.fromHtml(forumName, Html.FROM_HTML_MODE_COMPACT)
    }
}

fun formatCookie(userid: String, admin: String, po: String = ""): Spannable {
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

fun formatTime(now: String, style: String = "default"): String {
    // TODO: format time based on style, which could be in preference
    return ReadableTime.getDisplayTime(now)
}

fun formatTitleAndName(title: String? = "", name: String? = ""): String {
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
    val regex = """&gt;&gt;No.\d\d\d\d\d\d\d\d""".toRegex()

    return regex.findAll(content).toList().map {
        it.value.substring(11)
    }

}

fun removeQuote(content: String): String {
    /** api response
    <font color=\"#789922\">&gt;&gt;No.23527403</font>
     */
    val regex = """<font color="#789922">.*</font>""".toRegex()
    return regex.replace(content, "")
}

// TODO: [h][/h]
fun formatContent(content: String): SpannableString {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        SpannableString(Html.fromHtml(content, HtmlCompat.FROM_HTML_MODE_COMPACT))
    } else {
        SpannableString(Html.fromHtml(content))
    }
}