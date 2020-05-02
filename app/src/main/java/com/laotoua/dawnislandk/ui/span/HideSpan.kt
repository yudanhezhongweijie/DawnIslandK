package com.laotoua.dawnislandk.ui.span

import android.graphics.Color
import android.text.Spannable
import android.text.Spanned
import android.text.TextPaint
import android.text.style.BackgroundColorSpan
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.view.View
import android.widget.TextView

class HideSpan(val start: Int, val end: Int) : ClickableSpan() {

    private var hidden: Boolean = true

    private val foregroundColorSpan = ForegroundColorSpan(Color.TRANSPARENT)
    private val backgroundColorSpan = BackgroundColorSpan(Color.parseColor("#555555"))

    override fun onClick(widget: View) {
        if (widget is TextView) toggle(widget)

    }

    // overrides, DO NOT CREATE PAINT
    override fun updateDrawState(ds: TextPaint) {
    }

    fun toggle(widget: TextView) {
        val charSequence = widget.text
        if (charSequence is Spannable) {
            hidden = !hidden
            if (!hidden) {
                showSecret(charSequence)
            } else {
                hideSecret(charSequence, start, end)
            }
        }
    }

    private fun showSecret(charSequence: Spannable) {
        charSequence.removeSpan(backgroundColorSpan)
        charSequence.removeSpan(foregroundColorSpan)
        // remove highlight color
//        widget.highlightColor = Color.TRANSPARENT
    }

    fun hideSecret(charSequence: Spannable, start: Int, end: Int) {
        charSequence.setSpan(
            backgroundColorSpan,
            start,
            end,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        charSequence.setSpan(
            foregroundColorSpan,
            start,
            end,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
    }

}