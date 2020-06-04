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

    // add status bar's height to toolbar content margin
    fun Toolbar.immersiveToolbar(): Toolbar = apply {
        setOnApplyWindowInsetsListener { _, insets ->
            setMarginTop(insets.systemWindowInsetTop)
            insets.consumeSystemWindowInsets()
        }
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