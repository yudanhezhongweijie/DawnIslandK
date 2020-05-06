package com.laotoua.dawnislandk.ui.util

import android.content.Context
import android.util.DisplayMetrics
import android.util.TypedValue

object LayoutUtil {
    fun dip2px(context: Context, dipValue: Float): Int {
        val displayMetrics: DisplayMetrics = context.resources.displayMetrics
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dipValue,
            displayMetrics
        ).toInt()
    }

    fun pix2dp(context: Context, pix: Int): Float {
        return pix / context.resources.displayMetrics.density
    }
}