package com.laotoua.dawnislandk.screens.widgets.spans

import android.text.TextPaint
import android.text.style.ClickableSpan
import android.view.View

class ReferenceSpan(val id: String, private val clickListener: ReferenceClickHandler? = null) :
    ClickableSpan() {
    override fun onClick(widget: View) {
        clickListener?.handleReference(id)
    }

    override fun updateDrawState(ds: TextPaint) {}

    interface ReferenceClickHandler{
        fun handleReference(id:String)
    }
}