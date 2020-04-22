package com.laotoua.dawnislandk.ui.fragment

import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.preference.PreferenceManager
import com.laotoua.dawnislandk.R
import com.laotoua.dawnislandk.ui.span.RoundBackgroundColorSpan
import com.laotoua.dawnislandk.ui.span.SegmentSpacingSpan
import com.laotoua.dawnislandk.ui.viewfactory.ThreadCardFactory
import com.laotoua.dawnislandk.util.AppState
import com.laotoua.dawnislandk.util.CONST
import com.laotoua.dawnislandk.viewmodel.SharedViewModel

class SizeCustomizationFragment : Fragment() {

    private val sharedVM: SharedViewModel by activityViewModels()

    private val MAIN_TEXT_SIZE = 0
    private val RADIUS = 1
    private val ELEVATION = 2
    private val CARD_MARGIN_TOP = 3
    private val CARD_MARGIN_LEFT = 4
    private val CARD_MARGIN_RIGHT = 5
    private val CONTENT_MARGIN_TOP = 6
    private val CONTENT_MARGIN_LEFT = 7
    private val CONTENT_MARGIN_RIGHT = 8
    private val CONTENT_MARGIN_BOTTOM = 9
    private val HEAD_BAR_MARGIN_TOP = 10
    private val TEXT_SCALEX = 11
    private val LINE_SPACE_EXTRA = 12
    private val SEGMENT_GAP = 13

    private val sharedPreferences: SharedPreferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(context)
    }

    private val cardFactory: ThreadCardFactory by lazy {
        AppState.getThreadCardFactory(
            requireContext()
        )
    }
    private val demoCard by lazy { cardFactory.getCardView(requireContext()) }
    private val demoCardContainer: ConstraintLayout? by lazy { demoCard.threadContainer }
    private var charSequence: CharSequence? = null

    private val progressContainer by lazy { LinearLayout(requireContext()) }
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        sharedVM.setFragment(this)

        val rootView = LinearLayout(context)
        rootView.setPaddingRelative(10, 10, 10, 10)
        rootView.orientation = LinearLayout.VERTICAL


        demoCard.threadCookie!!.text = "cookie"
        demoCard.threadTime!!.text = "2小时前"
        val spannableString = SpannableString("欢乐恶搞" + " · " + 12)
        spannableString.setSpan(
            RoundBackgroundColorSpan(
                Color.parseColor("#12DBD1"),
                Color.parseColor("#FFFFFF")
            ), 0, spannableString.length, Spanned.SPAN_INCLUSIVE_EXCLUSIVE
        )
        demoCard.threadForumAndReplyCount!!.setText(spannableString, TextView.BufferType.SPANNABLE)
        val exampleText =
            SpannableString("北分则易红在保，干品政两报米术，料询容保美。\n该府术没也例空解，法露作长心录。 六深事会部青目传向市始，西法医很呀体近数片。\n活林变须阶候业精六只团起已市，下头却广局正支。")
        exampleText.setSpan(
            SegmentSpacingSpan(
                sharedPreferences.getInt(CONST.LINE_HEIGHT, 0),
                sharedPreferences.getInt(CONST.SEG_GAP, 0)
            ), 0, exampleText.length, Spanned.SPAN_INCLUSIVE_EXCLUSIVE
        )
        demoCard.threadContent!!.setText(exampleText, TextView.BufferType.SPANNABLE)
        demoCard.threadImage!!
            .setImageResource(R.mipmap.ic_launcher)

        rootView.addView(demoCard)

        progressContainer.orientation = LinearLayout.VERTICAL

        progressContainer.layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        generateSeekBar(RADIUS, "圆角").let {
            it?.findViewWithTag<SeekBar>("SeekBar")?.progress = cardFactory.cardRadius
            progressContainer.addView(it)
        }

        generateSeekBar(ELEVATION, "阴影").let {
            it?.findViewWithTag<SeekBar>("SeekBar")?.progress = cardFactory.cardElevaion
            progressContainer.addView(it)
        }

        generateSeekBar(MAIN_TEXT_SIZE, "主字号", 10).let {
            it?.findViewWithTag<SeekBar>("SeekBar")?.progress = cardFactory.mainTextSize - 10
            progressContainer.addView(it)
        }

        generateSeekBar(LINE_SPACE_EXTRA, "行间距", 20).let {
            it?.findViewWithTag<SeekBar>("SeekBar")?.progress = cardFactory.lineHeight
            progressContainer.addView(it)
        }

        generateSeekBar(SEGMENT_GAP, "段间距", 25).let {
            it?.findViewWithTag<SeekBar>("SeekBar")?.progress = cardFactory.segGap
            progressContainer.addView(it)
        }

        generateSeekBar(TEXT_SCALEX, "字间距", 17).let {
            it?.findViewWithTag<SeekBar>("SeekBar")?.progress = cardFactory.letterSpace
            progressContainer.addView(it)
        }

        generateSeekBar(CARD_MARGIN_TOP, "卡片间距").let {
            it?.findViewWithTag<SeekBar>("SeekBar")?.progress = cardFactory.cardMarginTop
            progressContainer.addView(it)
        }

        generateSeekBar(CARD_MARGIN_LEFT, "卡片左边距", 50).let {
            it?.findViewWithTag<SeekBar>("SeekBar")?.progress = cardFactory.cardMarginLeft
            progressContainer.addView(it)
        }

        generateSeekBar(CARD_MARGIN_RIGHT, "卡片右边距", 50).let {
            it?.findViewWithTag<SeekBar>("SeekBar")?.progress = cardFactory.cardMarginRight
            progressContainer.addView(it)
        }

        generateSeekBar(HEAD_BAR_MARGIN_TOP, "头部上边距", 60).let {
            it?.findViewWithTag<SeekBar>("SeekBar")?.progress = cardFactory.headBarMarginTop
            progressContainer.addView(it)
        }

        generateSeekBar(CONTENT_MARGIN_TOP, "内容上边距", 50).let {
            it?.findViewWithTag<SeekBar>("SeekBar")?.progress = cardFactory.contentMarginTop
            progressContainer.addView(it)
        }

        generateSeekBar(CONTENT_MARGIN_LEFT, "内容左边距", 60).let {
            it?.findViewWithTag<SeekBar>("SeekBar")?.progress = cardFactory.contentMarginLeft
            progressContainer.addView(it)
        }

        generateSeekBar(CONTENT_MARGIN_RIGHT, "内容右边距", 60).let {
            it?.findViewWithTag<SeekBar>("SeekBar")?.progress = cardFactory.contentMarginRight
            progressContainer.addView(it)
        }

        generateSeekBar(CONTENT_MARGIN_BOTTOM, "内容下边距", 70).let {
            it?.findViewWithTag<SeekBar>("SeekBar")?.progress = cardFactory.contentMarginBottom
            progressContainer.addView(it)
        }


        val scrollView = ScrollView(context)
        progressContainer.setPadding(0, 50, 0, 0)
        scrollView.addView(progressContainer)

        rootView.addView(scrollView)

        return rootView
    }


    private fun generateSeekBar(id: Int, itemName: String): LinearLayout? {
        return generateSeekBar(id, itemName, 100)
    }

    private fun generateSeekBar(id: Int, itemName: String, max: Int): LinearLayout? {
        val layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        )

        val linearLayout = LinearLayout(context)
        linearLayout.layoutParams = layoutParams
        linearLayout.setPadding(8, 8, 8, 0)
        val textView = TextView(context)
        textView.text = itemName
        val number = TextView(context)
        number.tag = "ProgressText"
        val seekBar = SeekBar(context)
        seekBar.tag = "SeekBar"
        seekBar.id = id
        seekBar.max = max
        linearLayout.addView(textView)
        linearLayout.addView(number)
        linearLayout.addView(seekBar, layoutParams)
        seekBar.setOnSeekBarChangeListener(CardSettingSeekBar())
        return linearLayout
    }

    inner class CardSettingSeekBar :
        SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
            var res = progress
            val cardLayoutParams = demoCard.layoutParams as MarginLayoutParams

            val contentView: View = demoCard.findViewById(R.id.threadContent)
            when (seekBar.id) {
                MAIN_TEXT_SIZE -> {
                    res += CONST.MAIN_TEXT_MIN_SIZE
                    (contentView as TextView).textSize = res.toFloat()
                    sharedPreferences.edit().putInt(CONST.MAIN_TEXT_SIZE, res).apply()
                }
                RADIUS -> {
                    demoCard.radius = res.toFloat()
                    sharedPreferences.edit().putInt(CONST.CARD_RADIUS, res).apply()
                }
                ELEVATION -> {
                    demoCard.elevation = res.toFloat()
                    sharedPreferences.edit().putInt(CONST.CARD_ELEVATION, res).apply()
                }
                CARD_MARGIN_TOP -> {
                    cardLayoutParams.topMargin = res
                    demoCard.layoutParams = cardLayoutParams
                    sharedPreferences.edit().putInt(CONST.CARD_MARGIN_TOP, res).apply()
                }
                CARD_MARGIN_LEFT -> {
                    cardLayoutParams.marginStart = res
                    demoCard.layoutParams = cardLayoutParams
                    sharedPreferences.edit().putInt(CONST.CARD_MARGIN_LEFT, res)
                        .apply()
                }
                CARD_MARGIN_RIGHT -> {
                    cardLayoutParams.marginEnd = res
                    demoCard.layoutParams = cardLayoutParams
                    sharedPreferences.edit().putInt(CONST.CARD_MARGIN_RIGHT, res)
                        .apply()
                }
                HEAD_BAR_MARGIN_TOP -> {
                    demoCardContainer!!.setPadding(
                        demoCardContainer!!.paddingLeft,
                        res,
                        demoCardContainer!!.paddingRight,
                        demoCardContainer!!.paddingBottom
                    )
                    sharedPreferences.edit().putInt(CONST.HEAD_BAR_MARGIN_TOP, res)
                        .apply()
                }
                CONTENT_MARGIN_TOP -> {
                    val contentLayoutParams =
                        contentView.layoutParams as ConstraintLayout.LayoutParams
                    contentLayoutParams.topMargin = res
                    contentView.layoutParams = contentLayoutParams
                    sharedPreferences.edit().putInt(CONST.CONTENT_MARGIN_TOP, res)
                        .apply()
                }
                CONTENT_MARGIN_LEFT -> {
                    demoCardContainer!!.setPadding(
                        res,
                        demoCardContainer!!.paddingTop,
                        demoCardContainer!!.paddingRight,
                        demoCardContainer!!.paddingBottom
                    )
                    sharedPreferences.edit().putInt(CONST.CONTENT_MARGIN_LEFT, res)
                        .apply()
                }
                CONTENT_MARGIN_RIGHT -> {
                    demoCardContainer!!.setPadding(
                        demoCardContainer!!.paddingLeft,
                        demoCardContainer!!.paddingTop,
                        res,
                        demoCardContainer!!.paddingBottom
                    )
                    sharedPreferences.edit().putInt(CONST.CONTENT_MARGIN_RIGHT, res)
                        .apply()
                }
                CONTENT_MARGIN_BOTTOM -> {
                    demoCardContainer!!.setPadding(
                        demoCardContainer!!.paddingLeft,
                        demoCardContainer!!.paddingTop,
                        demoCardContainer!!.paddingRight,
                        res
                    )
                    sharedPreferences.edit().putInt(CONST.CONTENT_MARGIN_BOTTOM, res)
                        .apply()
                }
                TEXT_SCALEX -> {
                    var i = res * 1.0f
                    i /= 50f
                    (contentView as TextView).letterSpacing = i
                    sharedPreferences.edit().putInt(CONST.LETTER_SPACE, res).apply()
                }
                LINE_SPACE_EXTRA -> {
                    charSequence = (contentView as TextView).text
                    if (charSequence is SpannableString) {

                        val segmentSpacingSpans: Array<SegmentSpacingSpan> =
                            (charSequence as SpannableString).getSpans<SegmentSpacingSpan>(
                                0, (charSequence as SpannableString).length,
                                SegmentSpacingSpan::class.java
                            )
                        segmentSpacingSpans[0].setmHeight(res)
                    }
                    contentView.requestLayout()
                    sharedPreferences.edit().putInt(CONST.LINE_HEIGHT, res).apply()
                }
                SEGMENT_GAP -> {
                    charSequence = (contentView as TextView).text
                    if (charSequence is SpannableString) {
                        val segmentSpacingSpans: Array<SegmentSpacingSpan> =
                            (charSequence as SpannableString).getSpans<SegmentSpacingSpan>(
                                0, (charSequence as SpannableString).length,
                                SegmentSpacingSpan::class.java
                            )
                        segmentSpacingSpans[0].setSegmentGap(res)
                    }
                    contentView.requestLayout()
                    sharedPreferences.edit().putInt(CONST.SEG_GAP, res).apply()
                }
                else -> {
                }
            }

            (seekBar.parent as LinearLayout)
                .findViewWithTag<TextView>("ProgressText").text = res.toString()

        }

        override fun onStartTrackingTouch(seekBar: SeekBar?) {

        }

        override fun onStopTrackingTouch(seekBar: SeekBar?) {

        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cardFactory.loadSettings()
    }
}