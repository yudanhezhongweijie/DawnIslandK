package com.laotoua.dawnislandk.ui.fragment

import android.graphics.Color
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import android.widget.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.card.MaterialCardView
import com.laotoua.dawnislandk.R
import com.laotoua.dawnislandk.ui.span.RoundBackgroundColorSpan
import com.laotoua.dawnislandk.ui.span.SegmentSpacingSpan
import com.laotoua.dawnislandk.ui.viewfactory.ThreadCardFactory
import com.laotoua.dawnislandk.util.Constants
import com.laotoua.dawnislandk.viewmodel.SharedViewModel
import com.tencent.mmkv.MMKV
import kotlinx.android.synthetic.main.list_item_thread.view.*

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
    private val LETTER_SPACE = 11
    private val LINE_HEIGHT = 12
    private val SEG_GAP = 13

    private val mmkv by lazy { MMKV.defaultMMKV() }

    private val rootView by lazy { LinearLayout(context) }
    private val demoCard by lazy {
        layoutInflater.inflate(
            R.layout.list_item_thread,
            rootView,
            false
        ) as MaterialCardView
    }
    private val demoCardContainer: ConstraintLayout by lazy { demoCard.threadContainer }
    private var charSequence: CharSequence? = null

    private val progressContainer by lazy { LinearLayout(requireContext()) }
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        sharedVM.setFragment(this)

        rootView.setPaddingRelative(10, 10, 10, 10)
        rootView.orientation = LinearLayout.VERTICAL

        ThreadCardFactory.applySettings(demoCard)

        val threadForumAndReplyCount = SpannableString(demoCard.threadForumAndReplyCount.text)
        threadForumAndReplyCount.setSpan(
            RoundBackgroundColorSpan(
                Color.parseColor("#12DBD1"),
                Color.parseColor("#FFFFFF")
            ), 0, threadForumAndReplyCount.length, Spanned.SPAN_INCLUSIVE_EXCLUSIVE
        )

        val threadContent = SpannableString(demoCard.threadContent.text)
        threadContent.setSpan(
            SegmentSpacingSpan(
                ThreadCardFactory.lineHeight,
                ThreadCardFactory.segGap
            ), 0, threadContent.length, Spanned.SPAN_INCLUSIVE_EXCLUSIVE
        )
        demoCard.threadContent.apply {
            setText(threadContent, TextView.BufferType.SPANNABLE)
            letterSpacing = ThreadCardFactory.letterSpace
            textSize = ThreadCardFactory.mainTextSize
        }

        rootView.addView(demoCard)

        progressContainer.orientation = LinearLayout.VERTICAL

        progressContainer.layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        generateSeekBar(RADIUS, "圆角").let {
            it?.findViewWithTag<SeekBar>("SeekBar")?.progress = ThreadCardFactory.cardRadius.toInt()
            progressContainer.addView(it)
        }

        generateSeekBar(ELEVATION, "阴影").let {
            it?.findViewWithTag<SeekBar>("SeekBar")?.progress =
                ThreadCardFactory.cardElevation.toInt()
            progressContainer.addView(it)
        }

        generateSeekBar(MAIN_TEXT_SIZE, "主字号", 10).let {
            it?.findViewWithTag<SeekBar>("SeekBar")?.progress =
                ThreadCardFactory.mainTextSize.toInt() - 10
            progressContainer.addView(it)
        }

        generateSeekBar(LINE_HEIGHT, "行间距", 20).let {
            it?.findViewWithTag<SeekBar>("SeekBar")?.progress = ThreadCardFactory.lineHeight
            progressContainer.addView(it)
        }

        generateSeekBar(SEG_GAP, "段间距", 25).let {
            it?.findViewWithTag<SeekBar>("SeekBar")?.progress = ThreadCardFactory.segGap
            progressContainer.addView(it)
        }

        generateSeekBar(LETTER_SPACE, "字间距", 17).let {
            it?.findViewWithTag<SeekBar>("SeekBar")?.progress =
                (ThreadCardFactory.letterSpace * 50f).toInt()
            progressContainer.addView(it)
        }

        generateSeekBar(CARD_MARGIN_TOP, "卡片间距").let {
            it?.findViewWithTag<SeekBar>("SeekBar")?.progress = ThreadCardFactory.cardMarginTop
            progressContainer.addView(it)
        }

        generateSeekBar(CARD_MARGIN_LEFT, "卡片左边距", 50).let {
            it?.findViewWithTag<SeekBar>("SeekBar")?.progress = ThreadCardFactory.cardMarginLeft
            progressContainer.addView(it)
        }

        generateSeekBar(CARD_MARGIN_RIGHT, "卡片右边距", 50).let {
            it?.findViewWithTag<SeekBar>("SeekBar")?.progress = ThreadCardFactory.cardMarginRight
            progressContainer.addView(it)
        }

        generateSeekBar(HEAD_BAR_MARGIN_TOP, "头部上边距", 60).let {
            it?.findViewWithTag<SeekBar>("SeekBar")?.progress = ThreadCardFactory.headBarMarginTop
            progressContainer.addView(it)
        }

        generateSeekBar(CONTENT_MARGIN_TOP, "内容上边距", 50).let {
            it?.findViewWithTag<SeekBar>("SeekBar")?.progress = ThreadCardFactory.contentMarginTop
            progressContainer.addView(it)
        }

        generateSeekBar(CONTENT_MARGIN_LEFT, "内容左边距", 60).let {
            it?.findViewWithTag<SeekBar>("SeekBar")?.progress = ThreadCardFactory.contentMarginLeft
            progressContainer.addView(it)
        }

        generateSeekBar(CONTENT_MARGIN_RIGHT, "内容右边距", 60).let {
            it?.findViewWithTag<SeekBar>("SeekBar")?.progress = ThreadCardFactory.contentMarginRight
            progressContainer.addView(it)
        }

        generateSeekBar(CONTENT_MARGIN_BOTTOM, "内容下边距", 70).let {
            it?.findViewWithTag<SeekBar>("SeekBar")?.progress =
                ThreadCardFactory.contentMarginBottom
            progressContainer.addView(it)
        }


        val scrollView = ScrollView(context)
        scrollView.overScrollMode = View.OVER_SCROLL_NEVER
        progressContainer.setPadding(0, 50, 0, 50)
        scrollView.addView(progressContainer)

        rootView.addView(scrollView)

        return rootView
    }


    private fun generateSeekBar(id: Int, itemName: String, max: Int = 100): LinearLayout? {
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
                    res += Constants.MAIN_TEXT_MIN_SIZE
                    (contentView as TextView).textSize = res.toFloat()
                    mmkv.putFloat(Constants.MAIN_TEXT_SIZE, res.toFloat())
                }
                RADIUS -> {
                    demoCard.radius = res.toFloat()
                    mmkv.putFloat(Constants.CARD_RADIUS, res.toFloat())
                }
                ELEVATION -> {
                    demoCard.elevation = res.toFloat()
                    mmkv.putFloat(Constants.CARD_ELEVATION, res.toFloat())
                }
                CARD_MARGIN_TOP -> {
                    cardLayoutParams.topMargin = res
                    demoCard.layoutParams = cardLayoutParams
                    mmkv.putInt(Constants.CARD_MARGIN_TOP, res)
                }
                CARD_MARGIN_LEFT -> {
                    cardLayoutParams.marginStart = res
                    demoCard.layoutParams = cardLayoutParams
                    mmkv.putInt(Constants.CARD_MARGIN_LEFT, res)

                }
                CARD_MARGIN_RIGHT -> {
                    cardLayoutParams.marginEnd = res
                    demoCard.layoutParams = cardLayoutParams
                    mmkv.putInt(Constants.CARD_MARGIN_RIGHT, res)
                }
                HEAD_BAR_MARGIN_TOP -> {
                    demoCardContainer.setPadding(
                        demoCardContainer.paddingLeft,
                        res,
                        demoCardContainer.paddingRight,
                        demoCardContainer.paddingBottom
                    )

                    mmkv.putInt(Constants.HEAD_BAR_MARGIN_TOP, res)
                }
                CONTENT_MARGIN_TOP -> {
                    val contentLayoutParams =
                        contentView.layoutParams as ConstraintLayout.LayoutParams
                    contentLayoutParams.topMargin = res
                    contentView.layoutParams = contentLayoutParams
                    mmkv.putInt(Constants.CONTENT_MARGIN_TOP, res)
                }
                CONTENT_MARGIN_LEFT -> {
                    demoCardContainer.setPadding(
                        res,
                        demoCardContainer.paddingTop,
                        demoCardContainer.paddingRight,
                        demoCardContainer.paddingBottom
                    )
                    mmkv.putInt(Constants.CONTENT_MARGIN_LEFT, res)
                }
                CONTENT_MARGIN_RIGHT -> {
                    demoCardContainer.setPadding(
                        demoCardContainer.paddingLeft,
                        demoCardContainer.paddingTop,
                        res,
                        demoCardContainer.paddingBottom
                    )
                    mmkv.putInt(Constants.CONTENT_MARGIN_RIGHT, res)
                }
                CONTENT_MARGIN_BOTTOM -> {
                    demoCardContainer.setPadding(
                        demoCardContainer.paddingLeft,
                        demoCardContainer.paddingTop,
                        demoCardContainer.paddingRight,
                        res
                    )
                    mmkv.putInt(Constants.CONTENT_MARGIN_BOTTOM, res)
                }
                LETTER_SPACE -> {
                    var i = res * 1.0f
                    i /= 50f
                    (contentView as TextView).letterSpacing = i
                    mmkv.putFloat(Constants.LETTER_SPACE, i)
                }
                LINE_HEIGHT -> {
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
                    mmkv.putInt(Constants.LINE_HEIGHT, res)
                }
                SEG_GAP -> {
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
                    mmkv.putInt(Constants.SEG_GAP, res)
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
        Toast.makeText(context, "设置将在重启后生效", Toast.LENGTH_SHORT).show()
    }
}