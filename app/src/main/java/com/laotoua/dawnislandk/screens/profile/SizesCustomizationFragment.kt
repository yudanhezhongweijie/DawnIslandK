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

import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import com.laotoua.dawnislandk.DawnApp
import com.laotoua.dawnislandk.R
import com.laotoua.dawnislandk.databinding.FragmentSizeCustomizationBinding
import com.laotoua.dawnislandk.screens.posts.PostCardFactory
import com.laotoua.dawnislandk.screens.util.Layout.toast
import com.laotoua.dawnislandk.screens.widgets.spans.RoundBackgroundColorSpan
import com.laotoua.dawnislandk.screens.widgets.spans.SegmentSpacingSpan
import com.laotoua.dawnislandk.util.DawnConstants

class SizesCustomizationFragment : Fragment() {

    private var settingsChanged = false
    private val mmkv = DawnApp.applicationDataStore.mmkv

    private var binding: FragmentSizeCustomizationBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        binding = FragmentSizeCustomizationBinding.inflate(inflater, container, false)


        binding?.demoCard?.apply {
            title.visibility = View.VISIBLE
            title.text = "标题： 无标题"
            name.visibility = View.VISIBLE
            name.text = "名称： 无名氏"
            refId.setText(R.string.sample_ref_id)
            userId.setText(R.string.sample_user_id)
            timestamp.setText(R.string.sample_timestamp_simplified)
            attachedImage.setImageResource(R.mipmap.ic_launcher)
            val threadForumAndReplyCount =
                SpannableString(requireContext().getString(R.string.sample_forum_and_reply_count))
            threadForumAndReplyCount.setSpan(
                RoundBackgroundColorSpan(),
                0,
                threadForumAndReplyCount.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            forumAndReplyCount.setText(threadForumAndReplyCount, TextView.BufferType.SPANNABLE)

            val threadContent =
                SpannableString(requireContext().getString(R.string.sample_post_content))
            threadContent.setSpan(
                SegmentSpacingSpan(
                    PostCardFactory.lineHeight,
                    PostCardFactory.segGap
                ), 0, threadContent.length, Spanned.SPAN_INCLUSIVE_EXCLUSIVE
            )
            content.apply {
                setText(threadContent, TextView.BufferType.SPANNABLE)
                letterSpacing = PostCardFactory.letterSpace
                textSize = PostCardFactory.mainTextSize
            }
        }

        binding?.radius?.apply {
            title.setText(R.string.radius)
            slider.value = PostCardFactory.cardRadius
            slider.valueFrom = 0f
            slider.valueTo = 100f
            slider.addOnChangeListener { _, value, fromUser ->
                if (fromUser) settingsChanged = true
                binding?.demoCard?.card?.radius = value
                mmkv.putFloat(DawnConstants.CARD_RADIUS, value)
            }
        }

        binding?.elevation?.apply {
            title.setText(R.string.elevation)
            slider.value = PostCardFactory.cardElevation
            slider.valueFrom = 0f
            slider.valueTo = 100f
            slider.addOnChangeListener { _, value, fromUser ->
                if (fromUser) settingsChanged = true
                binding?.demoCard?.card?.elevation = value
                mmkv.putFloat(DawnConstants.CARD_ELEVATION, value)
            }
        }

        binding?.mainTextSize?.apply {
            title.setText(R.string.main_text_size)
            slider.value = 10f.coerceAtLeast(PostCardFactory.mainTextSize)
            slider.valueFrom = 10f
            slider.valueTo = 25f
            slider.addOnChangeListener { _, value, fromUser ->
                if (fromUser) settingsChanged = true
                binding?.demoCard?.content?.textSize = value
                mmkv.putFloat(DawnConstants.MAIN_TEXT_SIZE, value)
            }
        }

        binding?.lineHeight?.apply {
            title.setText(R.string.line_height)
            slider.value = PostCardFactory.lineHeight.toFloat()
            slider.valueFrom = 0f
            slider.valueTo = 40f
            slider.addOnChangeListener { _, value, fromUser ->
                if (fromUser) settingsChanged = true
                val cs = binding?.demoCard?.content?.text
                if (cs is SpannableString) {
                    val segmentSpacingSpans: Array<SegmentSpacingSpan> =
                        cs.getSpans(0, cs.length, SegmentSpacingSpan::class.java)
                    segmentSpacingSpans[0].setHeight(value.toInt())
                }
                binding?.demoCard?.content?.requestLayout()
                mmkv.putInt(DawnConstants.LINE_HEIGHT, value.toInt())
            }
        }

        binding?.segGap?.apply {
            title.setText(R.string.seg_gap)
            slider.value = PostCardFactory.segGap.toFloat()
            slider.valueFrom = 0f
            slider.valueTo = 40f
            slider.addOnChangeListener { _, value, fromUser ->
                if (fromUser) settingsChanged = true
                val cs = binding?.demoCard?.content?.text
                if (cs is SpannableString) {
                    val segmentSpacingSpans: Array<SegmentSpacingSpan> =
                        cs.getSpans(0, cs.length, SegmentSpacingSpan::class.java)
                    segmentSpacingSpans[0].setSegmentGap(value.toInt())
                }
                binding?.demoCard?.content?.requestLayout()
                mmkv.putInt(DawnConstants.SEG_GAP, value.toInt())
            }
        }

        binding?.letterSpace?.apply {
            title.setText(R.string.letter_space)
            slider.value = PostCardFactory.letterSpace * 50f
            slider.valueFrom = 0f
            slider.valueTo = 40f
            slider.addOnChangeListener { _, value, fromUser ->
                if (fromUser) settingsChanged = true
                binding?.demoCard?.content?.letterSpacing = value / 50f
                mmkv.putFloat(DawnConstants.LETTER_SPACE, value / 50f)
            }
        }

        binding?.cardMarginTop?.apply {
            title.setText(R.string.card_margin_top)
            slider.value = PostCardFactory.cardMarginTop.toFloat()
            slider.valueFrom = 0f
            slider.valueTo = 100f
            slider.addOnChangeListener { _, value, fromUser ->
                if (fromUser) settingsChanged = true
                val cardLayoutParams = binding?.demoCard?.root?.layoutParams as MarginLayoutParams?
                cardLayoutParams?.topMargin = value.toInt()
                binding?.demoCard?.root?.layoutParams = cardLayoutParams
                mmkv.putInt(DawnConstants.CARD_MARGIN_TOP, value.toInt())
            }
        }

        binding?.cardMarginLeft?.apply {
            title.setText(R.string.card_margin_left)
            slider.value = PostCardFactory.cardMarginLeft.toFloat()
            slider.valueFrom = 0f
            slider.valueTo = 100f
            slider.addOnChangeListener { _, value, fromUser ->
                if (fromUser) settingsChanged = true
                val cardLayoutParams = binding?.demoCard?.root?.layoutParams as MarginLayoutParams?
                cardLayoutParams?.leftMargin = value.toInt()
                binding?.demoCard?.root?.layoutParams = cardLayoutParams
                mmkv.putInt(DawnConstants.CARD_MARGIN_LEFT, value.toInt())
            }
        }

        binding?.cardMarginRight?.apply {
            title.setText(R.string.card_margin_right)
            slider.value = PostCardFactory.cardMarginRight.toFloat()
            slider.valueFrom = 0f
            slider.valueTo = 100f
            slider.addOnChangeListener { _, value, fromUser ->
                if (fromUser) settingsChanged = true
                val cardLayoutParams = binding?.demoCard?.root?.layoutParams as MarginLayoutParams?
                cardLayoutParams?.rightMargin = value.toInt()
                binding?.demoCard?.root?.layoutParams = cardLayoutParams
                mmkv.putInt(DawnConstants.CARD_MARGIN_RIGHT, value.toInt())
            }
        }

        binding?.headBarMarginTop?.apply {
            title.setText(R.string.head_bar_margin_top)
            slider.value = PostCardFactory.headBarMarginTop.toFloat()
            slider.valueFrom = 0f
            slider.valueTo = 100f
            slider.addOnChangeListener { _, value, fromUser ->
                if (fromUser) settingsChanged = true
                binding?.demoCard?.cardContainer?.setPadding(
                    binding?.demoCard?.cardContainer!!.paddingLeft,
                    value.toInt(),
                    binding?.demoCard?.cardContainer!!.paddingRight,
                    binding?.demoCard?.cardContainer!!.paddingBottom
                )
                mmkv.putInt(DawnConstants.HEAD_BAR_MARGIN_TOP, value.toInt())
            }
        }

        binding?.contentMarginTop?.apply {
            title.setText(R.string.content_margin_top)
            slider.value = PostCardFactory.contentMarginTop.toFloat()
            slider.valueFrom = 0f
            slider.valueTo = 100f
            slider.addOnChangeListener { _, value, fromUser ->
                if (fromUser) settingsChanged = true
                val contentLayoutParams =
                    binding?.demoCard?.content?.layoutParams as ConstraintLayout.LayoutParams?
                contentLayoutParams?.topMargin = value.toInt()
                binding?.demoCard?.content?.layoutParams = contentLayoutParams
                mmkv.putInt(DawnConstants.CONTENT_MARGIN_TOP, value.toInt())
            }
        }

        binding?.contentMarginLeft?.apply {
            title.setText(R.string.content_margin_left)
            slider.value = PostCardFactory.contentMarginLeft.toFloat()
            slider.valueFrom = 0f
            slider.valueTo = 100f
            slider.addOnChangeListener { _, value, fromUser ->
                if (fromUser) settingsChanged = true
                val contentLayoutParams =
                    binding?.demoCard?.content?.layoutParams as ConstraintLayout.LayoutParams?
                contentLayoutParams?.leftMargin = value.toInt()
                binding?.demoCard?.content?.layoutParams = contentLayoutParams
                mmkv.putInt(DawnConstants.CONTENT_MARGIN_LEFT, value.toInt())
            }
        }

        binding?.contentMarginRight?.apply {
            title.setText(R.string.content_margin_right)
            slider.value = PostCardFactory.contentMarginRight.toFloat()
            slider.valueFrom = 0f
            slider.valueTo = 100f
            slider.addOnChangeListener { _, value, fromUser ->
                if (fromUser) settingsChanged = true
                val contentLayoutParams =
                    binding?.demoCard?.content?.layoutParams as ConstraintLayout.LayoutParams?
                contentLayoutParams?.rightMargin = value.toInt()
                binding?.demoCard?.content?.layoutParams = contentLayoutParams
                mmkv.putInt(DawnConstants.CONTENT_MARGIN_RIGHT, value.toInt())
            }
        }

        binding?.contentMarginBottom?.apply {
            title.setText(R.string.content_margin_bottom)
            slider.value = PostCardFactory.contentMarginBottom.toFloat()
            slider.valueFrom = 0f
            slider.valueTo = 100f
            slider.addOnChangeListener { _, value, fromUser ->
                if (fromUser) settingsChanged = true
                val contentLayoutParams =
                    binding?.demoCard?.content?.layoutParams as ConstraintLayout.LayoutParams?
                contentLayoutParams?.bottomMargin = value.toInt()
                binding?.demoCard?.content?.layoutParams = contentLayoutParams
                mmkv.putInt(DawnConstants.CONTENT_MARGIN_BOTTOM, value.toInt())
            }
        }

        return binding!!.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
        if (settingsChanged) {
            toast(R.string.restart_to_apply_setting)
        }
    }

}