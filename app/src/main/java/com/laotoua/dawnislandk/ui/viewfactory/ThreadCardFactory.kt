package com.laotoua.dawnislandk.ui.viewfactory

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.Typeface
import android.text.TextUtils
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.Dimension
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.preference.PreferenceManager
import com.google.android.material.card.MaterialCardView
import com.laotoua.dawnislandk.R
import com.laotoua.dawnislandk.ui.util.dip2px
import com.laotoua.dawnislandk.util.Constants

class ThreadCardFactory(val context: Context) {

    private var DEFAULT_CARDVIEW_PADDING = 15
    private var DEFAULT_CARDVIEW_MARGINSTART = 10
    private var DEFAULT_CARDVIEW_MARGINEND = 10
    private var DEFAULT_CARDVIEW_MARGINTOP = 16
    private var DEFAULT_CARDVIEW_MARGINBOTTOM = 6
    private val sharedPreferences: SharedPreferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(context)
    }
    var mainTextSize = 0
    var cardRadius = 0
    var cardElevaion = 0
    var cardMarginTop = 0
    var cardMarginLeft = 0
    var cardMarginRight = 0
    var headBarMarginTop = 0
    var contentMarginTop = 0
    var contentMarginLeft = 0
    var contentMarginRight = 0
    var contentMarginBottom = 0
    var lineHeight = 0
    var letterSpace = 0
    var segGap = 0

    fun loadSettings() {
        mainTextSize = sharedPreferences.getInt(Constants.MAIN_TEXT_SIZE, 15)
        cardRadius = sharedPreferences.getInt(
            Constants.CARD_RADIUS,
            dip2px(context, 5f)
        )
        cardElevaion =
            sharedPreferences.getInt(
                Constants.CARD_ELEVATION,
                dip2px(context, 2f)
            )
        cardMarginTop = sharedPreferences.getInt(
            Constants.CARD_MARGIN_TOP,
            DEFAULT_CARDVIEW_MARGINTOP
        )
        cardMarginLeft = sharedPreferences.getInt(
            Constants.CARD_MARGIN_LEFT,
            DEFAULT_CARDVIEW_MARGINSTART
        )
        cardMarginRight = sharedPreferences.getInt(
            Constants.CARD_MARGIN_RIGHT,
            DEFAULT_CARDVIEW_MARGINEND
        )
        headBarMarginTop = sharedPreferences.getInt(
            Constants.HEAD_BAR_MARGIN_TOP,
            DEFAULT_CARDVIEW_PADDING
        )
        contentMarginTop =
            sharedPreferences.getInt(
                Constants.CONTENT_MARGIN_TOP,
                dip2px(context, 8f)
            )
        contentMarginLeft = sharedPreferences.getInt(
            Constants.CONTENT_MARGIN_LEFT,
            DEFAULT_CARDVIEW_PADDING
        )
        contentMarginRight = sharedPreferences.getInt(
            Constants.CONTENT_MARGIN_RIGHT,
            DEFAULT_CARDVIEW_PADDING
        )
        contentMarginBottom = sharedPreferences.getInt(
            Constants.CONTENT_MARGIN_BOTTOM,
            DEFAULT_CARDVIEW_PADDING
        )
        letterSpace = sharedPreferences.getInt(Constants.LETTER_SPACE, 0)
        lineHeight = sharedPreferences.getInt(Constants.LINE_HEIGHT, 0)
        segGap = sharedPreferences.getInt(Constants.SEG_GAP, 0)
    }

    fun getCardView(context: Context): ThreadListCard {
        /**
         * 创建CardView
         */
        val cardView = ThreadListCard(context)

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
        val threadContainer = ConstraintLayout(context)
        threadContainer.id = R.id.threadContainer
        threadContainer.setPadding(
            contentMarginLeft,
            headBarMarginTop,
            contentMarginRight,
            contentMarginBottom
        )
        /**
         * cookie TextView
         */
        val threadCookie = TextView(context)
        threadCookie.id = R.id.threadCookie
        threadCookie.typeface = Typeface.defaultFromStyle(Typeface.BOLD)
        val cookieLayoutParams = ConstraintLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
        )
        cookieLayoutParams.topToTop = R.id.threadContainer
        cookieLayoutParams.startToStart = R.id.threadContainer
        threadCookie.layoutParams = cookieLayoutParams

        /**
         * time TextView
         */
        val threadTime = TextView(context)
        threadTime.id = R.id.threadTime
        val timeLayoutParams = ConstraintLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
        )
        timeLayoutParams.startToEnd = R.id.threadCookie
        timeLayoutParams.marginStart =
            dip2px(context, 8f)
        timeLayoutParams.topToTop = R.id.threadContainer
        threadTime.layoutParams = timeLayoutParams
        /**
         * forum TextView
         */
        val threadForumAndReplyCount = TextView(context)
        threadForumAndReplyCount.id = R.id.threadForumAndReplyCount
        val forumLayoutParams = ConstraintLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
        )
        forumLayoutParams.topToTop = R.id.threadContainer
        forumLayoutParams.endToEnd = R.id.threadContainer

        /***
         * xml中使用padding代替了margin，因为要绘制标签背景，这里暂时空着8，因为考虑使用Span进行绘制,所以size，color之类的属性都暂时不写
         * 省略的属性有padding、textColor、textSize、background
         */
        threadForumAndReplyCount.layoutParams = forumLayoutParams
        threadForumAndReplyCount.setTextSize(Dimension.SP, 12f)

        /**
         * content TextView
         */
        val threadContent = TextView(context)
        threadContent.id = R.id.threadContent
        val contentLayoutParam =
            ConstraintLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT)
        contentLayoutParam.topToBottom = R.id.threadQuotes
        contentLayoutParam.topMargin = contentMarginTop
        contentLayoutParam.endToStart = R.id.threadImage
        contentLayoutParam.marginEnd =
            dip2px(context, 2f)
        contentLayoutParam.startToStart = R.id.threadContainer
        threadContent.layoutParams = contentLayoutParam
        threadContent.setTextColor(Color.BLACK)
        threadContent.textSize = mainTextSize.toFloat()
        threadContent.maxLines = 10
        threadContent.ellipsize = TextUtils.TruncateAt.END
        var trueLetterSpace = letterSpace * 1.0f
        trueLetterSpace /= 50f
        threadContent.letterSpacing = trueLetterSpace
        val threadImage = ImageView(context)
        threadImage.id = R.id.threadImage
        val imageLayoutParam =
            ConstraintLayout.LayoutParams(250, 250)
        imageLayoutParam.topToTop = R.id.threadContent
        imageLayoutParam.endToEnd = R.id.threadContainer
        threadImage.layoutParams = imageLayoutParam

        /**
         *  sage
         */
        val sage = TextView(context)
        sage.id = R.id.sage
        val sageLayoutParam = ConstraintLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
        )
        sageLayoutParam.verticalWeight = 1F
        sage.layoutParams = sageLayoutParam
        sage.background = context.getDrawable(R.drawable.sage_background)
        sage.setPaddingRelative(15, 15, 15, 15)
        sage.text = "本串已被sage"
        sage.typeface = Typeface.DEFAULT_BOLD
        sage.visibility = View.INVISIBLE


        /**
         * quotes
         */
        val threadQuotes = LinearLayout(context)
        threadQuotes.id = R.id.threadQuotes
        val threadQuotesLayoutParam = ConstraintLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
        )
        threadQuotesLayoutParam.topMargin = 6
        threadQuotesLayoutParam.orientation = LinearLayout.VERTICAL
        threadQuotesLayoutParam.startToStart = R.id.threadContainer
        threadQuotesLayoutParam.topToBottom = R.id.threadCookie
        threadQuotes.layoutParams = threadQuotesLayoutParam



        cardView.addView(threadContainer)
        threadContainer.addView(threadCookie)
        threadContainer.addView(threadTime)
        threadContainer.addView(threadForumAndReplyCount)
        threadContainer.addView(threadContent)
        threadContainer.addView(threadImage)
        threadContainer.addView(sage)
        threadContainer.addView(threadQuotes)
        cardView.threadContainer = threadContainer
        cardView.threadCookie = threadCookie
        cardView.threadTime = threadTime
        cardView.threadForumAndReplyCount = threadForumAndReplyCount
        cardView.threadContent = threadContent
        cardView.threadImage = threadImage
        cardView.sage = sage
        cardView.threadQuotes = threadQuotes
        return cardView
    }



    inner class ThreadListCard(context: Context?) :
        MaterialCardView(context) {
        var threadContainer: ConstraintLayout? = null
        var threadCookie: TextView? = null
        var threadTime: TextView? = null
        var threadForumAndReplyCount: TextView? = null
        var threadContent: TextView? = null
        var threadImage: ImageView? = null
        var sage: TextView? = null
        var threadQuotes: LinearLayout? = null
    }


    init {
        DEFAULT_CARDVIEW_PADDING = dip2px(
            context,
            DEFAULT_CARDVIEW_PADDING.toFloat()
        )
        DEFAULT_CARDVIEW_MARGINSTART = dip2px(
            context,
            DEFAULT_CARDVIEW_MARGINSTART.toFloat()
        )
        DEFAULT_CARDVIEW_MARGINEND = dip2px(
            context,
            DEFAULT_CARDVIEW_MARGINEND.toFloat()
        )
        DEFAULT_CARDVIEW_MARGINTOP = dip2px(
            context,
            DEFAULT_CARDVIEW_MARGINTOP.toFloat()
        )
        DEFAULT_CARDVIEW_MARGINBOTTOM =
            dip2px(
                context,
                DEFAULT_CARDVIEW_MARGINBOTTOM.toFloat()
            )

        /**
         * 获取存储
         */
        loadSettings()
    }
}
