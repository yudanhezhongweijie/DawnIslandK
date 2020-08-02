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

package com.laotoua.dawnislandk.screens.profile

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
import com.google.android.material.card.MaterialCardView
import com.laotoua.dawnislandk.DawnApp
import com.laotoua.dawnislandk.R
import com.laotoua.dawnislandk.screens.posts.PostCardFactory
import com.laotoua.dawnislandk.screens.util.Layout.toast
import com.laotoua.dawnislandk.screens.widgets.spans.RoundBackgroundColorSpan
import com.laotoua.dawnislandk.screens.widgets.spans.SegmentSpacingSpan
import com.laotoua.dawnislandk.util.DawnConstants
import kotlinx.android.synthetic.main.list_item_post.view.*

class SizesCustomizationFragment : Fragment() {

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

    private var settingsChanged = false
    private val mmkv = DawnApp.applicationDataStore.mmkv

    private val rootView by lazy { LinearLayout(context) }
    private val demoCard by lazy {
        layoutInflater.inflate(
            R.layout.list_item_post,
            rootView,
            false
        ) as MaterialCardView
    }
    private val demoCardContainer: ConstraintLayout by lazy { demoCard.cardContainer }
    private var charSequence: CharSequence? = null

    private val progressContainer by lazy { LinearLayout(requireContext()) }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        rootView.setPaddingRelative(10, 10, 10, 10)
        rootView.orientation = LinearLayout.VERTICAL

        PostCardFactory.applySettings(demoCard)

        demoCard.findViewById<TextView>(R.id.title).apply {
            visibility = View.VISIBLE
            text = "标题： 无标题"
        }
        demoCard.findViewById<TextView>(R.id.name).apply {
            visibility = View.VISIBLE
            text = "名称： 无名氏"
        }

        demoCard.findViewById<TextView>(R.id.refId).setText(R.string.sample_ref_id)
        demoCard.findViewById<TextView>(R.id.userId).setText(R.string.sample_user_id)
        demoCard.findViewById<TextView>(R.id.timestamp).setText(R.string.sample_timestamp_simplified)
        demoCard.findViewById<ImageView>(R.id.attachedImage).setImageResource(R.mipmap.ic_launcher)

        val threadForumAndReplyCount = SpannableString(requireContext().getString(R.string.sample_forum_and_reply_count))
        threadForumAndReplyCount.setSpan(
            RoundBackgroundColorSpan(
                Color.parseColor("#12DBD1"),
                Color.parseColor("#FFFFFF")
            ), 0, threadForumAndReplyCount.length, Spanned.SPAN_INCLUSIVE_EXCLUSIVE
        )

        demoCard.findViewById<TextView>(R.id.forumAndReplyCount)
            .setText(threadForumAndReplyCount, TextView.BufferType.SPANNABLE)

        val threadContent = SpannableString(requireContext().getString(R.string.sample_post_content))
        threadContent.setSpan(
            SegmentSpacingSpan(
                PostCardFactory.lineHeight,
                PostCardFactory.segGap
            ), 0, threadContent.length, Spanned.SPAN_INCLUSIVE_EXCLUSIVE
        )
        demoCard.content.apply {
            setText(threadContent, TextView.BufferType.SPANNABLE)
            letterSpacing = PostCardFactory.letterSpace
            textSize = PostCardFactory.mainTextSize
        }
        rootView.addView(demoCard)

        progressContainer.orientation = LinearLayout.VERTICAL

        progressContainer.layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        generateSeekBar(radius, requireContext().getString(R.string.radius)).let {
            it?.findViewWithTag<SeekBar>("SeekBar")?.progress = PostCardFactory.cardRadius.toInt()
            progressContainer.addView(it)
        }

        generateSeekBar(elevation, requireContext().getString(R.string.elevation)).let {
            it?.findViewWithTag<SeekBar>("SeekBar")?.progress =
                PostCardFactory.cardElevation.toInt()
            progressContainer.addView(it)
        }

        generateSeekBar(
            mainTextSize,
            requireContext().getString(R.string.main_text_size),
            20
        ).let {
            it?.findViewWithTag<SeekBar>("SeekBar")?.progress =
                PostCardFactory.mainTextSize.toInt() - 10
            progressContainer.addView(it)
        }

        generateSeekBar(lineHeight, requireContext().getString(R.string.line_height), 40).let {
            it?.findViewWithTag<SeekBar>("SeekBar")?.progress = PostCardFactory.lineHeight
            progressContainer.addView(it)
        }

        generateSeekBar(segGap, requireContext().getString(R.string.seg_gap), 40).let {
            it?.findViewWithTag<SeekBar>("SeekBar")?.progress = PostCardFactory.segGap
            progressContainer.addView(it)
        }

        generateSeekBar(letterSpace, requireContext().getString(R.string.letter_space), 40).let {
            it?.findViewWithTag<SeekBar>("SeekBar")?.progress =
                (PostCardFactory.letterSpace * 50f).toInt()
            progressContainer.addView(it)
        }

        generateSeekBar(cardMarginTop, requireContext().getString(R.string.card_margin_top)).let {
            it?.findViewWithTag<SeekBar>("SeekBar")?.progress = PostCardFactory.cardMarginTop
            progressContainer.addView(it)
        }

        generateSeekBar(
            cardMarginLeft,
            requireContext().getString(R.string.card_margin_left)
        ).let {
            it?.findViewWithTag<SeekBar>("SeekBar")?.progress = PostCardFactory.cardMarginLeft
            progressContainer.addView(it)
        }

        generateSeekBar(
            cardMarginRight,
            requireContext().getString(R.string.card_margin_right)
        ).let {
            it?.findViewWithTag<SeekBar>("SeekBar")?.progress = PostCardFactory.cardMarginRight
            progressContainer.addView(it)
        }

        generateSeekBar(
            headBarMarginTop,
            requireContext().getString(R.string.head_bar_margin_top)
        ).let {
            it?.findViewWithTag<SeekBar>("SeekBar")?.progress = PostCardFactory.headBarMarginTop
            progressContainer.addView(it)
        }

        generateSeekBar(
            contentMarginTop,
            requireContext().getString(R.string.content_margin_top)
        ).let {
            it?.findViewWithTag<SeekBar>("SeekBar")?.progress = PostCardFactory.contentMarginTop
            progressContainer.addView(it)
        }

        generateSeekBar(
            contentMarginLeft,
            requireContext().getString(R.string.content_margin_left)
        ).let {
            it?.findViewWithTag<SeekBar>("SeekBar")?.progress = PostCardFactory.contentMarginLeft
            progressContainer.addView(it)
        }

        generateSeekBar(
            contentMarginRight,
            requireContext().getString(R.string.content_margin_right)
        ).let {
            it?.findViewWithTag<SeekBar>("SeekBar")?.progress = PostCardFactory.contentMarginRight
            progressContainer.addView(it)
        }

        generateSeekBar(
            contentMarginBottom,
            requireContext().getString(R.string.content_margin_bottom)
        ).let {
            it?.findViewWithTag<SeekBar>("SeekBar")?.progress =
                PostCardFactory.contentMarginBottom
            progressContainer.addView(it)
        }


        val scrollView = ScrollView(context)
        scrollView.overScrollMode = View.OVER_SCROLL_NEVER
        progressContainer.setPadding(0, 50, 0, 50)
        scrollView.addView(progressContainer)

        rootView.addView(scrollView)

        val wrapper =
            inflater.inflate(R.layout.fragment_empty_linear, container, false) as LinearLayout
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
            if (fromUser) settingsChanged = true
            var res = progress
            val cardLayoutParams = demoCard.layoutParams as MarginLayoutParams
            val contentView: View = demoCard.findViewById(R.id.content)
            when (seekBar.id) {
                mainTextSize -> {
                    res += DawnConstants.MAIN_TEXT_MIN_SIZE
                    (contentView as TextView).textSize = res.toFloat()
                    mmkv.putFloat(DawnConstants.MAIN_TEXT_SIZE, res.toFloat())
                }
                radius -> {
                    demoCard.radius = res.toFloat()
                    mmkv.putFloat(DawnConstants.CARD_RADIUS, res.toFloat())
                }
                elevation -> {
                    demoCard.elevation = res.toFloat()
                    mmkv.putFloat(DawnConstants.CARD_ELEVATION, res.toFloat())
                }
                cardMarginTop -> {
                    cardLayoutParams.topMargin = res
                    demoCard.layoutParams = cardLayoutParams
                    mmkv.putInt(DawnConstants.CARD_MARGIN_TOP, res)
                }
                cardMarginLeft -> {
                    cardLayoutParams.marginStart = res
                    demoCard.layoutParams = cardLayoutParams
                    mmkv.putInt(DawnConstants.CARD_MARGIN_LEFT, res)

                }
                cardMarginRight -> {
                    cardLayoutParams.marginEnd = res
                    demoCard.layoutParams = cardLayoutParams
                    mmkv.putInt(DawnConstants.CARD_MARGIN_RIGHT, res)
                }
                headBarMarginTop -> {
                    demoCardContainer.setPadding(
                        demoCardContainer.paddingLeft,
                        res,
                        demoCardContainer.paddingRight,
                        demoCardContainer.paddingBottom
                    )

                    mmkv.putInt(DawnConstants.HEAD_BAR_MARGIN_TOP, res)
                }
                contentMarginTop -> {
                    val contentLayoutParams =
                        contentView.layoutParams as ConstraintLayout.LayoutParams
                    contentLayoutParams.topMargin = res
                    contentView.layoutParams = contentLayoutParams
                    mmkv.putInt(DawnConstants.CONTENT_MARGIN_TOP, res)
                }
                contentMarginLeft -> {
                    demoCardContainer.setPadding(
                        res,
                        demoCardContainer.paddingTop,
                        demoCardContainer.paddingRight,
                        demoCardContainer.paddingBottom
                    )
                    mmkv.putInt(DawnConstants.CONTENT_MARGIN_LEFT, res)
                }
                contentMarginRight -> {
                    demoCardContainer.setPadding(
                        demoCardContainer.paddingLeft,
                        demoCardContainer.paddingTop,
                        res,
                        demoCardContainer.paddingBottom
                    )
                    mmkv.putInt(DawnConstants.CONTENT_MARGIN_RIGHT, res)
                }
                contentMarginBottom -> {
                    demoCardContainer.setPadding(
                        demoCardContainer.paddingLeft,
                        demoCardContainer.paddingTop,
                        demoCardContainer.paddingRight,
                        res
                    )
                    mmkv.putInt(DawnConstants.CONTENT_MARGIN_BOTTOM, res)
                }
                letterSpace -> {
                    var i = res * 1.0f
                    i /= 50f
                    (contentView as TextView).letterSpacing = i
                    mmkv.putFloat(DawnConstants.LETTER_SPACE, i)
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
                    mmkv.putInt(DawnConstants.LINE_HEIGHT, res)
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
                    mmkv.putInt(DawnConstants.SEG_GAP, res)
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
        if (settingsChanged) {
            toast(R.string.restart_to_apply_setting)
        }
    }
}