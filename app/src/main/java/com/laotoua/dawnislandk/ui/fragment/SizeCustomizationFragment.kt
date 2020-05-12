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
import androidx.appcompat.widget.Toolbar
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.card.MaterialCardView
import com.laotoua.dawnislandk.R
import com.laotoua.dawnislandk.ui.span.RoundBackgroundColorSpan
import com.laotoua.dawnislandk.ui.span.SegmentSpacingSpan
import com.laotoua.dawnislandk.ui.util.ToolBarUtil.immersiveToolbar
import com.laotoua.dawnislandk.ui.viewfactory.ThreadCardFactory
import com.laotoua.dawnislandk.util.Constants
import com.tencent.mmkv.MMKV
import kotlinx.android.synthetic.main.list_item_thread.view.*

class SizeCustomizationFragment : Fragment() {

    private val mainTextSize = 0
    private val radius = 1
    private val elevation = 2
    private val cardMarginTop = 3
    private val cardMarginLeft = 4
    private val cardMarginRight = 5
    private val contentMarginTop = 6
    private val contentMarginLeft = 7
    private val contentMarginRight = 8
    private val contentMarginBottom = 9
    private val headBarMarginTop = 10
    private val letterSpace = 11
    private val lineHeight = 12
    private val segGap = 13

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

        generateSeekBar(radius, requireContext().getString(R.string.radius)).let {
            it?.findViewWithTag<SeekBar>("SeekBar")?.progress = ThreadCardFactory.cardRadius.toInt()
            progressContainer.addView(it)
        }

        generateSeekBar(elevation, requireContext().getString(R.string.elevation)).let {
            it?.findViewWithTag<SeekBar>("SeekBar")?.progress =
                ThreadCardFactory.cardElevation.toInt()
            progressContainer.addView(it)
        }

        generateSeekBar(
            mainTextSize,
            requireContext().getString(R.string.main_text_size),
            10
        ).let {
            it?.findViewWithTag<SeekBar>("SeekBar")?.progress =
                ThreadCardFactory.mainTextSize.toInt() - 10
            progressContainer.addView(it)
        }

        generateSeekBar(lineHeight, requireContext().getString(R.string.line_height), 20).let {
            it?.findViewWithTag<SeekBar>("SeekBar")?.progress = ThreadCardFactory.lineHeight
            progressContainer.addView(it)
        }

        generateSeekBar(segGap, requireContext().getString(R.string.seg_gap), 25).let {
            it?.findViewWithTag<SeekBar>("SeekBar")?.progress = ThreadCardFactory.segGap
            progressContainer.addView(it)
        }

        generateSeekBar(letterSpace, requireContext().getString(R.string.letter_space), 17).let {
            it?.findViewWithTag<SeekBar>("SeekBar")?.progress =
                (ThreadCardFactory.letterSpace * 50f).toInt()
            progressContainer.addView(it)
        }

        generateSeekBar(cardMarginTop, requireContext().getString(R.string.card_margin_top)).let {
            it?.findViewWithTag<SeekBar>("SeekBar")?.progress = ThreadCardFactory.cardMarginTop
            progressContainer.addView(it)
        }

        generateSeekBar(
            cardMarginLeft,
            requireContext().getString(R.string.card_margin_left),
            50
        ).let {
            it?.findViewWithTag<SeekBar>("SeekBar")?.progress = ThreadCardFactory.cardMarginLeft
            progressContainer.addView(it)
        }

        generateSeekBar(
            cardMarginRight,
            requireContext().getString(R.string.card_margin_right),
            50
        ).let {
            it?.findViewWithTag<SeekBar>("SeekBar")?.progress = ThreadCardFactory.cardMarginRight
            progressContainer.addView(it)
        }

        generateSeekBar(
            headBarMarginTop,
            requireContext().getString(R.string.head_bar_margin_top),
            60
        ).let {
            it?.findViewWithTag<SeekBar>("SeekBar")?.progress = ThreadCardFactory.headBarMarginTop
            progressContainer.addView(it)
        }

        generateSeekBar(
            contentMarginTop,
            requireContext().getString(R.string.content_margin_top),
            50
        ).let {
            it?.findViewWithTag<SeekBar>("SeekBar")?.progress = ThreadCardFactory.contentMarginTop
            progressContainer.addView(it)
        }

        generateSeekBar(
            contentMarginLeft,
            requireContext().getString(R.string.content_margin_left),
            60
        ).let {
            it?.findViewWithTag<SeekBar>("SeekBar")?.progress = ThreadCardFactory.contentMarginLeft
            progressContainer.addView(it)
        }

        generateSeekBar(
            contentMarginRight,
            requireContext().getString(R.string.content_margin_right),
            60
        ).let {
            it?.findViewWithTag<SeekBar>("SeekBar")?.progress = ThreadCardFactory.contentMarginRight
            progressContainer.addView(it)
        }

        generateSeekBar(
            contentMarginBottom,
            requireContext().getString(R.string.content_margin_bottom),
            70
        ).let {
            it?.findViewWithTag<SeekBar>("SeekBar")?.progress =
                ThreadCardFactory.contentMarginBottom
            progressContainer.addView(it)
        }


        val scrollView = ScrollView(context)
        scrollView.overScrollMode = View.OVER_SCROLL_NEVER
        progressContainer.setPadding(0, 50, 0, 50)
        scrollView.addView(progressContainer)

        rootView.addView(scrollView)

        val wrapper = inflater.inflate(R.layout.fragment_empty_linear, container, false).apply {
            findViewById<Toolbar>(R.id.toolbar).apply {
                immersiveToolbar()
                setTitle(R.string.size_customization_settings)
                subtitle = ""
                val drawerLayout = requireActivity().findViewById<DrawerLayout>(R.id.drawerLayout)
                drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
                setNavigationIcon(R.drawable.ic_arrow_back_white_24px)
                setNavigationOnClickListener {
                    findNavController().popBackStack()
                }
            }
        } as LinearLayout

        wrapper.addView(rootView)
        return wrapper

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
                mainTextSize -> {
                    res += Constants.MAIN_TEXT_MIN_SIZE
                    (contentView as TextView).textSize = res.toFloat()
                    mmkv.putFloat(Constants.MAIN_TEXT_SIZE, res.toFloat())
                }
                radius -> {
                    demoCard.radius = res.toFloat()
                    mmkv.putFloat(Constants.CARD_RADIUS, res.toFloat())
                }
                elevation -> {
                    demoCard.elevation = res.toFloat()
                    mmkv.putFloat(Constants.CARD_ELEVATION, res.toFloat())
                }
                cardMarginTop -> {
                    cardLayoutParams.topMargin = res
                    demoCard.layoutParams = cardLayoutParams
                    mmkv.putInt(Constants.CARD_MARGIN_TOP, res)
                }
                cardMarginLeft -> {
                    cardLayoutParams.marginStart = res
                    demoCard.layoutParams = cardLayoutParams
                    mmkv.putInt(Constants.CARD_MARGIN_LEFT, res)

                }
                cardMarginRight -> {
                    cardLayoutParams.marginEnd = res
                    demoCard.layoutParams = cardLayoutParams
                    mmkv.putInt(Constants.CARD_MARGIN_RIGHT, res)
                }
                headBarMarginTop -> {
                    demoCardContainer.setPadding(
                        demoCardContainer.paddingLeft,
                        res,
                        demoCardContainer.paddingRight,
                        demoCardContainer.paddingBottom
                    )

                    mmkv.putInt(Constants.HEAD_BAR_MARGIN_TOP, res)
                }
                contentMarginTop -> {
                    val contentLayoutParams =
                        contentView.layoutParams as ConstraintLayout.LayoutParams
                    contentLayoutParams.topMargin = res
                    contentView.layoutParams = contentLayoutParams
                    mmkv.putInt(Constants.CONTENT_MARGIN_TOP, res)
                }
                contentMarginLeft -> {
                    demoCardContainer.setPadding(
                        res,
                        demoCardContainer.paddingTop,
                        demoCardContainer.paddingRight,
                        demoCardContainer.paddingBottom
                    )
                    mmkv.putInt(Constants.CONTENT_MARGIN_LEFT, res)
                }
                contentMarginRight -> {
                    demoCardContainer.setPadding(
                        demoCardContainer.paddingLeft,
                        demoCardContainer.paddingTop,
                        res,
                        demoCardContainer.paddingBottom
                    )
                    mmkv.putInt(Constants.CONTENT_MARGIN_RIGHT, res)
                }
                contentMarginBottom -> {
                    demoCardContainer.setPadding(
                        demoCardContainer.paddingLeft,
                        demoCardContainer.paddingTop,
                        demoCardContainer.paddingRight,
                        res
                    )
                    mmkv.putInt(Constants.CONTENT_MARGIN_BOTTOM, res)
                }
                letterSpace -> {
                    var i = res * 1.0f
                    i /= 50f
                    (contentView as TextView).letterSpacing = i
                    mmkv.putFloat(Constants.LETTER_SPACE, i)
                }
                lineHeight -> {
                    charSequence = (contentView as TextView).text
                    if (charSequence is SpannableString) {

                        val segmentSpacingSpans: Array<SegmentSpacingSpan> =
                            (charSequence as SpannableString).getSpans(
                                0, (charSequence as SpannableString).length,
                                SegmentSpacingSpan::class.java
                            )
                        segmentSpacingSpans[0].setHeight(res)
                    }
                    contentView.requestLayout()
                    mmkv.putInt(Constants.LINE_HEIGHT, res)
                }
                segGap -> {
                    charSequence = (contentView as TextView).text
                    if (charSequence is SpannableString) {
                        val segmentSpacingSpans: Array<SegmentSpacingSpan> =
                            (charSequence as SpannableString).getSpans(
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
        Toast.makeText(context, R.string.restart_to_apply_setting, Toast.LENGTH_SHORT).show()
    }
}