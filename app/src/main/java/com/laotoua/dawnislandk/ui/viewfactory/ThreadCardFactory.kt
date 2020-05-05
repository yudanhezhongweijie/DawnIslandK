package com.laotoua.dawnislandk.ui.viewfactory

import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.android.material.card.MaterialCardView
import com.laotoua.dawnislandk.R
import com.laotoua.dawnislandk.util.Constants
import com.tencent.mmkv.MMKV

object ThreadCardFactory {

    private var DEFAULT_CARDVIEW_PADDING = 15
    private var DEFAULT_CARDVIEW_MARGINSTART = 10
    private var DEFAULT_CARDVIEW_MARGINEND = 10
    private var DEFAULT_CARDVIEW_MARGINTOP = 16
    private var DEFAULT_CARDVIEW_MARGINBOTTOM = 10

    private val mmkv by lazy { MMKV.defaultMMKV() }
    val mainTextSize by lazy { mmkv.getFloat(Constants.MAIN_TEXT_SIZE, 15f) }
    val cardRadius by lazy {
        mmkv.getFloat(
            Constants.CARD_RADIUS,
            15f
        )
    }
    val cardElevation by lazy {
        mmkv.getFloat(
            Constants.CARD_ELEVATION,
            15f
        )
    }
    val cardMarginTop by lazy {
        mmkv.getInt(
            Constants.CARD_MARGIN_TOP,
            DEFAULT_CARDVIEW_MARGINTOP
        )
    }
    val cardMarginLeft by lazy {
        mmkv.getInt(
            Constants.CARD_MARGIN_LEFT,
            DEFAULT_CARDVIEW_MARGINSTART
        )
    }
    val cardMarginRight by lazy {
        mmkv.getInt(
            Constants.CARD_MARGIN_RIGHT,
            DEFAULT_CARDVIEW_MARGINEND
        )
    }
    val cardMarginBottom by lazy {
        mmkv.getInt(
            Constants.CARD_MARGIN_BOTTOM,
            DEFAULT_CARDVIEW_MARGINBOTTOM
        )
    }
    val headBarMarginTop by lazy {
        mmkv.getInt(
            Constants.HEAD_BAR_MARGIN_TOP,
            DEFAULT_CARDVIEW_PADDING
        )
    }
    val contentMarginTop by lazy {
        mmkv.getInt(
            Constants.CONTENT_MARGIN_TOP,
            15
        )
    }
    val contentMarginLeft by lazy {
        mmkv.getInt(
            Constants.CONTENT_MARGIN_LEFT,
            DEFAULT_CARDVIEW_PADDING
        )
    }
    val contentMarginRight by lazy {
        mmkv.getInt(
            Constants.CONTENT_MARGIN_RIGHT,
            DEFAULT_CARDVIEW_PADDING
        )
    }
    val contentMarginBottom by lazy {
        mmkv.getInt(
            Constants.CONTENT_MARGIN_BOTTOM,
            DEFAULT_CARDVIEW_PADDING
        )
    }
    val lineHeight by lazy { mmkv.getInt(Constants.LINE_HEIGHT, 10) }
    val letterSpace by lazy { mmkv.getFloat(Constants.LETTER_SPACE, 15f) }
    val segGap by lazy { mmkv.getInt(Constants.SEG_GAP, 10) }

    fun applySettings(cardView: MaterialCardView) {
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
        val threadContainer = cardView.findViewById<ConstraintLayout>(R.id.threadContainer)
        threadContainer.setPadding(
            contentMarginLeft,
            headBarMarginTop,
            contentMarginRight,
            contentMarginBottom
        )

        val threadContent = cardView.findViewById<TextView>(R.id.threadContent)
        val contentLayoutParam = threadContent.layoutParams as ConstraintLayout.LayoutParams

        contentLayoutParam.topMargin = contentMarginTop
        threadContent.layoutParams = contentLayoutParam
        threadContent.textSize = mainTextSize

        threadContent.letterSpacing = letterSpace
    }
}
