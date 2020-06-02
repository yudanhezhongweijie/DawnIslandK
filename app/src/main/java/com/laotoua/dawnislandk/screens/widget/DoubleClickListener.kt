package com.laotoua.dawnislandk.screens.widget

import android.view.View

class DoubleClickListener(
    private val doubleClickTimeLimitMills: Long = 400,
    private val callback: DoubleClickCallBack
) :
    View.OnClickListener {
    private var lastClicked: Long = -1L

    override fun onClick(v: View?) {
        lastClicked = when {
            lastClicked == -1L -> {
                System.currentTimeMillis()
            }
            isDoubleClicked() -> {
                callback.doubleClicked()
                -1L
            }
            else -> {
                System.currentTimeMillis()
            }
        }
    }

    private fun getTimeDiff(from: Long, to: Long): Long = to - from

    private fun isDoubleClicked(): Boolean {
        return getTimeDiff(lastClicked, System.currentTimeMillis()) <= doubleClickTimeLimitMills
    }

    interface DoubleClickCallBack {
        fun doubleClicked()
    }
}