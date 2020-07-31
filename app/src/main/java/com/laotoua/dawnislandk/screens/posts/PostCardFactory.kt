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

package com.laotoua.dawnislandk.screens.posts

import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.android.material.card.MaterialCardView
import com.laotoua.dawnislandk.DawnApp
import com.laotoua.dawnislandk.R
import com.laotoua.dawnislandk.util.DawnConstants
import com.laotoua.dawnislandk.util.lazyOnMainOnly

object PostCardFactory {

    private var defaultCardViewPadding = 15
    private var defaultCardViewMarginStart = 10
    private var defaultCardViewMarginEnd = 10
    private var defaultCardViewMarginTop = 16
    private var defaultCardViewMarginBottom = 10

    private val mmkv = DawnApp.applicationDataStore.mmkv
    val mainTextSize = DawnApp.applicationDataStore.textSize
    val lineHeight = DawnApp.applicationDataStore.lineHeight
    val letterSpace = DawnApp.applicationDataStore.letterSpace
    val segGap = DawnApp.applicationDataStore.segGap
    val layoutCustomization = DawnApp.applicationDataStore.getLayoutCustomizationStatus()

    val cardRadius by lazyOnMainOnly {
        mmkv.getFloat(
            DawnConstants.CARD_RADIUS,
            15f
        )
    }
    val cardElevation by lazyOnMainOnly {
        mmkv.getFloat(
            DawnConstants.CARD_ELEVATION,
            15f
        )
    }
    val cardMarginTop by lazyOnMainOnly {
        mmkv.getInt(
            DawnConstants.CARD_MARGIN_TOP,
            defaultCardViewMarginTop
        )
    }
    val cardMarginLeft by lazyOnMainOnly {
        mmkv.getInt(
            DawnConstants.CARD_MARGIN_LEFT,
            defaultCardViewMarginStart
        )
    }
    val cardMarginRight by lazyOnMainOnly {
        mmkv.getInt(
            DawnConstants.CARD_MARGIN_RIGHT,
            defaultCardViewMarginEnd
        )
    }
    private val cardMarginBottom by lazyOnMainOnly {
        mmkv.getInt(
            DawnConstants.CARD_MARGIN_BOTTOM,
            defaultCardViewMarginBottom
        )
    }
    val headBarMarginTop by lazyOnMainOnly {
        mmkv.getInt(
            DawnConstants.HEAD_BAR_MARGIN_TOP,
            defaultCardViewPadding
        )
    }
    val contentMarginTop by lazyOnMainOnly {
        mmkv.getInt(
            DawnConstants.CONTENT_MARGIN_TOP,
            15
        )
    }
    val contentMarginLeft by lazyOnMainOnly {
        mmkv.getInt(
            DawnConstants.CONTENT_MARGIN_LEFT,
            defaultCardViewPadding
        )
    }
    val contentMarginRight by lazyOnMainOnly {
        mmkv.getInt(
            DawnConstants.CONTENT_MARGIN_RIGHT,
            defaultCardViewPadding
        )
    }
    val contentMarginBottom by lazyOnMainOnly {
        mmkv.getInt(
            DawnConstants.CONTENT_MARGIN_BOTTOM,
            defaultCardViewPadding
        )
    }

    fun applySettings(cardView: MaterialCardView) {
        if (!layoutCustomization) return
        val marginLayoutParams = (ViewGroup.MarginLayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )).apply {
            marginStart = cardMarginLeft
            marginEnd = cardMarginRight
            topMargin = cardMarginTop
            bottomMargin = cardMarginBottom
        }
        cardView.layoutParams = marginLayoutParams

        cardView.radius = cardRadius
        cardView.elevation = cardElevation
        val threadContainer = cardView.findViewById<ConstraintLayout>(R.id.cardContainer)
        threadContainer.setPadding(
            contentMarginLeft,
            headBarMarginTop,
            contentMarginRight,
            contentMarginBottom
        )
        val threadContent = cardView.findViewById<TextView>(R.id.content)
        val contentLayoutParam = threadContent.layoutParams as ConstraintLayout.LayoutParams
        contentLayoutParam.topMargin = contentMarginTop
        threadContent.layoutParams = contentLayoutParam
        threadContent.textSize = mainTextSize
        threadContent.letterSpacing = letterSpace
    }
}
