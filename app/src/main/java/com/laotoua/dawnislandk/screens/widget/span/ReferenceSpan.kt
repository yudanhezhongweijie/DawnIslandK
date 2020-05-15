package com.laotoua.dawnislandk.screens.widget.span

import android.text.TextPaint
import android.text.style.ClickableSpan
import android.view.View

class ReferenceSpan(val id: String, private val clickListener: ((id: String) -> Unit)? = null) :
    ClickableSpan() {
    override fun onClick(widget: View) {
        clickListener?.invoke(id)
    }

    override fun updateDrawState(ds: TextPaint) {}
}