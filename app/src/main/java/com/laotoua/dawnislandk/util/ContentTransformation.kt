package com.laotoua.dawnislandk.util

import android.graphics.Color
import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan


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