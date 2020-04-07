package com.laotoua.dawnislandk.components

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.Typeface
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.Dimension
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.preference.PreferenceManager
import com.google.android.material.card.MaterialCardView
import com.laotoua.dawnislandk.R

class ThreadCardFactory(context: Context) {
    private var DEFAULT_CARDVIEW_PADDING = 15
    private var DEFAULT_CARDVIEW_MARGINSTART = 10
    private var DEFAULT_CARDVIEW_MARGINEND = 10
    private var DEFAULT_CARDVIEW_MARGINTOP = 16
    private var DEFAULT_CARDVIEW_MARGINBOTTOM = 6
    private val displayMetrics: DisplayMetrics = context.resources.displayMetrics
    private val sharedPreferences: SharedPreferences
    private var mainTextSize = 0
    private var cardRadius = 0
    private var cardElevaion = 0
    private var cardMarginTop = 0
    private var cardMarginLeft = 0
    private var cardMarginRight = 0
    private var headBarMarginTop = 0
    private var contentMarginTop = 0
    private var contentMarginLeft = 0
    private var contentMarginRight = 0
    private var contentMarginBottom = 0
    private var lineHeight = 0
    private var letterSpace = 0
    private var segGap = 0

    private fun readSetting() {
        mainTextSize = sharedPreferences.getInt(MAIN_TEXT_SIZE, 15)
        cardRadius = sharedPreferences.getInt(CARD_RADIUS, dip2px(5f))
        cardElevaion =
            sharedPreferences.getInt(CARD_ELEVATION, dip2px(2f))
        cardMarginTop = sharedPreferences.getInt(
            CARD_MARGIN_TOP,
            DEFAULT_CARDVIEW_MARGINTOP
        )
        cardMarginLeft = sharedPreferences.getInt(
            CARD_MARGIN_LEFT,
            DEFAULT_CARDVIEW_MARGINSTART
        )
        cardMarginRight = sharedPreferences.getInt(
            CARD_MARGIN_RIGHT,
            DEFAULT_CARDVIEW_MARGINEND
        )
        headBarMarginTop = sharedPreferences.getInt(
            HEAD_BAR_MARGIN_TOP,
            DEFAULT_CARDVIEW_PADDING
        )
        contentMarginTop =
            sharedPreferences.getInt(CONTENT_MARGIN_TOP, dip2px(8f))
        contentMarginLeft = sharedPreferences.getInt(
            CONTENT_MARGIN_LEFT,
            DEFAULT_CARDVIEW_PADDING
        )
        contentMarginRight = sharedPreferences.getInt(
            CONTENT_MARGIN_RIGHT,
            DEFAULT_CARDVIEW_PADDING
        )
        contentMarginBottom = sharedPreferences.getInt(
            CONTENT_MARGIN_BOTTOM,
            DEFAULT_CARDVIEW_PADDING
        )
        letterSpace = sharedPreferences.getInt(LETTER_SPACE, 0)
        lineHeight = sharedPreferences.getInt(LINE_HEIGHT, 0)
        segGap = sharedPreferences.getInt(SEG_GAP, 0)
    }

    fun getSeriesCardView(context: Context): ThreadListCard {
        /**
         * 创建CardView
         */
        val cardView = ThreadListCard(context)
//            cardView.setId(R.id.threadContainer)
        /**
         * 设置CardView layout属性
         */
        val marginLayoutParams = MarginLayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        marginLayoutParams.marginStart = cardMarginLeft
        marginLayoutParams.marginEnd = cardMarginRight
        marginLayoutParams.topMargin = cardMarginTop
        cardView.layoutParams = marginLayoutParams
        /**
         * 获取点击效果资源
         */
        val typedValue = TypedValue()
        context.theme
            .resolveAttribute(R.attr.selectableItemBackground, typedValue, true)
        val attribute = intArrayOf(R.attr.selectableItemBackground)
        val typedArray =
            context.theme.obtainStyledAttributes(typedValue.resourceId, attribute)
        val drawable = typedArray.getDrawable(0)
        typedArray.recycle()
        /**
         * 设置点击效果
         */
        cardView.foreground = drawable
        cardView.isClickable = true
        /**
         * 设置背景颜色
         */
        cardView.setCardBackgroundColor(Color.parseColor("#Ffffff"))
        cardView.radius = cardRadius.toFloat()
        cardView.elevation = cardElevaion.toFloat()
        val constraintLayout = ConstraintLayout(context)
        constraintLayout.id = R.id.threadContainer
        constraintLayout.setPadding(
            contentMarginLeft,
            headBarMarginTop,
            contentMarginRight,
            contentMarginBottom
        )
        /**
         * cookie TextView
         */
        val cookieView = TextView(context)
        cookieView.id = R.id.threadCookie
        cookieView.typeface = Typeface.defaultFromStyle(Typeface.BOLD)
        val cookieLayoutParams = ConstraintLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
        )
        cookieLayoutParams.topToTop = R.id.threadContainer
        cookieLayoutParams.startToStart = R.id.threadContainer
        cookieView.layoutParams = cookieLayoutParams
        /**
         * time TextView
         */
        val timeView = TextView(context)
        timeView.id = R.id.threadTime
        val timeLayoutParams = ConstraintLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
        )
        timeLayoutParams.startToEnd = R.id.threadCookie
        timeLayoutParams.marginStart = dip2px(8f)
        timeLayoutParams.topToTop = R.id.threadContainer
        timeView.layoutParams = timeLayoutParams
        /**
         * forum TextView
         */
        val forumAndRelpycount = TextView(context)
        forumAndRelpycount.id = R.id.threadForumAndReplyCount
        val forumLayoutParams = ConstraintLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
        )
        forumLayoutParams.topToTop = R.id.threadContainer
        forumLayoutParams.endToEnd = R.id.threadContainer
        /***
         * xml中使用padding代替了margin，因为要绘制标签背景，这里暂时空着8，因为考虑使用Span进行绘制,所以size，color之类的属性都暂时不写
         * 省略的属性有padding、textColor、textSize、background
         */
        forumAndRelpycount.layoutParams = forumLayoutParams
        forumAndRelpycount.setTextSize(Dimension.SP, 12f)
        /**
         * content TextView
         */
        val contentView = TextView(context)
        contentView.id = R.id.threadContent
        val contentLayoutParam =
            ConstraintLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT)
        contentLayoutParam.topToBottom = R.id.threadCookie
        contentLayoutParam.topMargin = contentMarginTop
        contentLayoutParam.endToStart = R.id.threadImage
        contentLayoutParam.marginEnd = dip2px(2f)
        contentLayoutParam.startToStart = R.id.threadContainer
        contentView.layoutParams = contentLayoutParam
        contentView.setTextColor(Color.BLACK)
        contentView.textSize = mainTextSize.toFloat()
        contentView.maxLines = 10
        var trueLetterSpace = letterSpace * 1.0f
        trueLetterSpace /= 50f
        contentView.letterSpacing = trueLetterSpace
        val imageContent = ImageView(context)
        imageContent.id = R.id.threadImage
        val imageLayoutParam =
            ConstraintLayout.LayoutParams(250, 250)
        imageLayoutParam.topToTop = R.id.threadContent
        imageLayoutParam.endToEnd = R.id.threadContainer
        imageContent.layoutParams = imageLayoutParam
        cardView.addView(constraintLayout)
        constraintLayout.addView(cookieView)
        constraintLayout.addView(timeView)
        constraintLayout.addView(forumAndRelpycount)
        constraintLayout.addView(contentView)
        constraintLayout.addView(imageContent)
        cardView.constraintLayout = constraintLayout
        cardView.cookieView = cookieView
        cardView.timeView = timeView
        cardView.forumAndRelpycount = forumAndRelpycount
        cardView.contentView = contentView
        cardView.imageContent = imageContent
        return cardView
    }

    private fun dip2px(dipValue: Float): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dipValue,
            displayMetrics
        ).toInt()
    }

    inner class ThreadListCard(context: Context?) :
        MaterialCardView(context) {
        var id: String? = null
        var forum: String? = null
        var constraintLayout: ConstraintLayout? = null
        var cookieView: TextView? = null
        var timeView: TextView? = null
        var forumAndRelpycount: TextView? = null
        var contentView: TextView? = null
        var imageContent: ImageView? = null

    }

    companion object {
        //            private var cardViewFactory: ThreadCardFactory? = null
        const val MAIN_TEXT_SIZE = "main_text_size"
        const val CARD_RADIUS = "card_radius"
        const val CARD_ELEVATION = "card_elevation"
        const val CARD_MARGIN_TOP = "card_margin_top"
        const val CARD_MARGIN_LEFT = "card_margin_left"
        const val CARD_MARGIN_RIGHT = "card_margin_right"
        const val HEAD_BAR_MARGIN_TOP = "head_bar_margin_top"
        const val CONTENT_MARGIN_TOP = "content_margin_top"
        const val CONTENT_MARGIN_LEFT = "content_margin_left"
        const val CONTENT_MARGIN_RIGHT = "content_margin_right"
        const val CONTENT_MARGIN_BOTTOM = "content_margin_bottom"
        const val LETTER_SPACE = "letter_space"
        const val LINE_HEIGHT = "line_height"
        const val SEG_GAP = "seg_gap"
        const val MAIN_TEXT_MIN_SIZE = 10
//            fun getInstance(context: Context): ThreadCardFactory? {
//                if (cardViewFactory != null) {
//                    cardViewFactory!!.readSetting()
//                    return cardViewFactory
//                }
//                cardViewFactory = ThreadCardFactory(context)
//                cardViewFactory!!.readSetting()
//                return cardViewFactory
//            }
    }

    init {
        DEFAULT_CARDVIEW_PADDING = dip2px(DEFAULT_CARDVIEW_PADDING.toFloat())
        DEFAULT_CARDVIEW_MARGINSTART = dip2px(DEFAULT_CARDVIEW_MARGINSTART.toFloat())
        DEFAULT_CARDVIEW_MARGINEND = dip2px(DEFAULT_CARDVIEW_MARGINEND.toFloat())
        DEFAULT_CARDVIEW_MARGINTOP = dip2px(DEFAULT_CARDVIEW_MARGINTOP.toFloat())
        DEFAULT_CARDVIEW_MARGINBOTTOM = dip2px(DEFAULT_CARDVIEW_MARGINBOTTOM.toFloat())
        /**
         * 获取存储
         */
        sharedPreferences =
            PreferenceManager.getDefaultSharedPreferences(context)
        readSetting()
    }
}
