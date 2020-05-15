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

    private var topInset: Int = 0

    fun Toolbar.immersiveToolbar(): Toolbar = apply {
        setMarginTop(topInset)
    }

    fun Activity.immersiveToolbarInitialization() = apply {
        window.decorView.apply {
            systemUiVisibility =
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            /** *ONLY* compute insets on the based activity, children should only be reusing the attribute
             *
             */
            setOnApplyWindowInsetsListener { _, insets ->
                topInset = insets.systemWindowInsetTop
                insets.consumeSystemWindowInsets()
            }
        }
    }

    private fun View.setMarginTop(value: Int) = updateLayoutParams<ViewGroup.MarginLayoutParams> {
        topMargin = value
    }
}