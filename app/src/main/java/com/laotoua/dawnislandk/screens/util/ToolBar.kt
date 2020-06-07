package com.laotoua.dawnislandk.screens.util

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import androidx.core.view.updateLayoutParams
import com.laotoua.dawnislandk.R

object ToolBar {

    fun Activity.themeStatusBar() = apply {
        window.statusBarColor = resources.getColor(R.color.colorPrimary, null)
    }

    private var topMargin = 0

    // add status bar's height to toolbar content margin
    fun Toolbar.immersiveToolbar(): Toolbar = apply {
        if (topMargin == 0) {
            setOnApplyWindowInsetsListener { _, insets ->
                topMargin = insets.systemWindowInsetTop
                setMarginTop(topMargin)
                insets.consumeSystemWindowInsets()
            }
        } else setMarginTop(topMargin)
    }

    // expand toolbar to status bar
    fun Activity.immersiveToolbarInitialization() = apply {
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
    }

    private fun View.setMarginTop(value: Int) = updateLayoutParams<ViewGroup.MarginLayoutParams> {
        topMargin = value
    }
}