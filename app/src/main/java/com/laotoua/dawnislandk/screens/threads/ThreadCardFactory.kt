package com.laotoua.dawnislandk.screens.threads

import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.android.material.card.MaterialCardView
import com.laotoua.dawnislandk.R
import com.laotoua.dawnislandk.util.Constants
import com.tencent.mmkv.MMKV

object ThreadCardFactory {

    private var defaultCardViewPadding = 15
    private var defaultCardViewMarginStart = 10
    private var defaultCardViewMarginEnd = 10
    private var defaultCardViewMarginTop = 16
    private var defaultCardViewMarginBottom = 10

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
            defaultCardViewMarginTop
        )
    }
    val cardMarginLeft by lazy {
        mmkv.getInt(
            Constants.CARD_MARGIN_LEFT,
            defaultCardViewMarginStart
        )
    }
    val cardMarginRight by lazy {
        mmkv.getInt(
            Constants.CARD_MARGIN_RIGHT,
            defaultCardViewMarginEnd
        )
    }
    private val cardMarginBottom by lazy {
        mmkv.getInt(
            Constants.CARD_MARGIN_BOTTOM,
            defaultCardViewMarginBottom
        )
    }
    val headBarMarginTop by lazy {
        mmkv.getInt(
            Constants.HEAD_BAR_MARGIN_TOP,
            defaultCardViewPadding
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
            defaultCardViewPadding
        )
    }
    val contentMarginRight by lazy {
        mmkv.getInt(
            Constants.CONTENT_MARGIN_RIGHT,
            defaultCardViewPadding
        )
    }
    val contentMarginBottom by lazy {
        mmkv.getInt(
            Constants.CONTENT_MARGIN_BOTTOM,
            defaultCardViewPadding
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
            marginStart =
                cardMarginLeft
            marginEnd =
                cardMarginRight
            topMargin =
                cardMarginTop
            bottomMargin =
                cardMarginBottom
        }
        cardView.layoutParams = marginLayoutParams

        cardView.radius =
            cardRadius
        cardView.elevation =
            cardElevation
        val threadContainer = cardView.findViewById<ConstraintLayout>(R.id.cardContainer)
        threadContainer.setPadding(
            contentMarginLeft,
            headBarMarginTop,
            contentMarginRight,
            contentMarginBottom
        )

        val threadContent = cardView.findViewById<TextView>(R.id.content)
        val contentLayoutParam = threadContent.layoutParams as ConstraintLayout.LayoutParams

        contentLayoutParam.topMargin =
            contentMarginTop
        threadContent.layoutParams = contentLayoutParam
        threadContent.textSize =
            mainTextSize

        threadContent.letterSpacing =
            letterSpace
    }
}
