/*
 *  Copyright 2020 Fishballzzz
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package com.laotoua.dawnislandk.screens.util

import android.app.Activity
import android.content.res.Resources
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import androidx.core.view.updateLayoutParams
import com.laotoua.dawnislandk.R
import timber.log.Timber

object ToolBar {

    fun Activity.themeStatusBar() = apply {
        window.statusBarColor = resources.getColor(R.color.colorPrimary, null)
    }

    private var topMargin = 0

    fun getStatusBarHeight(): Int {
        val resources = Resources.getSystem()
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        return resources.getDimensionPixelSize(resourceId)
    }

    // add status bar's height to toolbar content margin
    fun Toolbar.immersiveToolbar(): Toolbar = apply {
        try {
            topMargin = getStatusBarHeight()
            setMarginTop(topMargin)
        } catch (e:Exception){
            Timber.e("Cannot get status bar height from system. use insets")
            if (topMargin == 0) {
                setOnApplyWindowInsetsListener { _, insets ->
                    topMargin = insets.systemWindowInsetTop
                    setMarginTop(topMargin)
                    insets.consumeSystemWindowInsets()
                }
            } else setMarginTop(topMargin)
        }
    }

    // expand toolbar to status bar
    fun Activity.immersiveToolbarInitialization() = apply {
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
    }

   private fun View.setMarginTop(value: Int = topMargin) = updateLayoutParams<ViewGroup.MarginLayoutParams> {
       // shift by magic value 5, because bang screens does not have the accurate status bar height
       // which may results in weird gap
       topMargin = value - 5
    }
}