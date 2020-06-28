package com.laotoua.dawnislandk.screens.widgets.spans

import android.graphics.Paint.FontMetricsInt
import android.text.style.LineHeightSpan

class SegmentSpacingSpan(private var mHeight: Int, private var segmentGap: Int) :
    LineHeightSpan {
    fun setHeight(mHeight: Int) {
        this.mHeight = mHeight
        seg = false
        line = false
    }

    fun setSegmentGap(segmentGap: Int) {
        this.segmentGap = segmentGap
        seg = false
        line = false
    }

    private var lineH = 0
    private var segH = 0
    private var seg = false
    private var line = false
    override fun chooseHeight(
        text: CharSequence,
        start: Int,
        end: Int,
        spanstartv: Int,
        lineHeight: Int,
        fm: FontMetricsInt
    ) {
        val originHeight = fm.descent - fm.ascent
        // If original height is not positive, do nothing.
        if (originHeight <= 0) {
            return
        }
        if (text.subSequence(start, end).toString().contains("\n")) {
            if (seg) {
                fm.descent = segH
            } else {
                fm.descent += segmentGap
                segH = fm.descent
                seg = true
            }
        }
        if (!text.subSequence(start, end).toString().contains("\n")) {
            if (line) {
                fm.descent = lineH
            } else {
                fm.descent += mHeight
                lineH = fm.descent
                line = true
            }
        }
    }


}