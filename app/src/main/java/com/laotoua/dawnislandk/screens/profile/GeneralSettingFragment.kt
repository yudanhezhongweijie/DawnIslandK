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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.WhichButton
import com.afollestad.materialdialogs.actions.getActionButton
import com.afollestad.materialdialogs.actions.setActionButtonEnabled
import com.afollestad.materialdialogs.checkbox.checkBoxPrompt
import com.afollestad.materialdialogs.input.input
import com.afollestad.materialdialogs.list.listItems
import com.laotoua.dawnislandk.DawnApp.Companion.applicationDataStore
import com.laotoua.dawnislandk.R
import com.laotoua.dawnislandk.databinding.FragmentGeneralSettingBinding
import com.laotoua.dawnislandk.screens.util.Layout.toast
import com.laotoua.dawnislandk.screens.util.Layout.updateSwitchSummary

class GeneralSettingFragment : Fragment() {

    private var binding:FragmentGeneralSettingBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentGeneralSettingBinding.inflate(inflater, container,false)
        binding?.feedId?.apply {
            var feedId = applicationDataStore.getFeedId()
            key.setText(R.string.feedId)
            summary.text = feedId
            root.setOnClickListener {
                if (activity == null || !isAdded) return@setOnClickListener
                MaterialDialog(requireContext()).show {
                    title(R.string.feedId)
                    input(hint = feedId, prefill = feedId) { _, text ->
                        feedId = text.toString()
                        applicationDataStore.setFeedId(feedId)
                        summary.text = feedId
                        toast(R.string.restart_to_apply_setting)
                    }
                    positiveButton(R.string.submit)
                    negativeButton(R.string.cancel)
                }
            }
        }

        binding?.timeFormat?.apply {
            key.setText(R.string.time_display_format)
            val entries = resources.getStringArray(R.array.time_format_entries)
            val values = resources.getStringArray(R.array.time_format_values)
            summary.text =
                if (values.first() == applicationDataStore.displayTimeFormat) {
                    entries.first()
                } else {
                    entries.last()
                }

            root.setOnClickListener {
                if (activity == null || !isAdded) return@setOnClickListener
                MaterialDialog(requireContext()).show {
                    title(R.string.time_display_format)
                    listItems(R.array.time_format_entries) { _, index, text ->
                        applicationDataStore.setDisplayTimeFormat(values[index])
                        summary.text = text
                        toast(R.string.restart_to_apply_setting)
                    }
                }
            }
        }

        binding?.clearCommentCache?.apply {
            key.setText(R.string.clear_comment_cache)
            root.setOnClickListener {
                if (activity == null || !isAdded) return@setOnClickListener
                MaterialDialog(requireContext()).show {
                    title(R.string.clear_comment_cache)
                    message(R.string.clear_comment_cache_confirm_message)
                    setActionButtonEnabled(WhichButton.POSITIVE, false)
                    checkBoxPrompt(R.string.acknowledge) { checked ->
                        setActionButtonEnabled(WhichButton.POSITIVE, checked)
                    }
                    positiveButton(R.string.submit) {
                        applicationDataStore.nukeCommentTable()
                        toast(R.string.cleared_comment_cache_message)
                    }
                    negativeButton(R.string.cancel)
                }
            }
        }

        binding?.restoreBlockedPosts?.apply {
            key.setText(R.string.restore_blocked_post)
            root.setOnClickListener {
                if (activity == null || !isAdded) return@setOnClickListener
                MaterialDialog(requireContext()).show {
                    title(R.string.restore_blocked_post)
                    message(R.string.restore_blocked_post_confirm_message)
                    setActionButtonEnabled(WhichButton.POSITIVE, false)
                    checkBoxPrompt(R.string.acknowledge) { checked ->
                        setActionButtonEnabled(WhichButton.POSITIVE, checked)
                    }
                    positiveButton(R.string.submit) {
                        applicationDataStore.nukeBlockedPostTable()
                        toast(R.string.restored_blocked_post)
                    }
                    negativeButton(R.string.cancel)
                }
            }
        }
        return binding!!.root
    }

    override fun onResume() {
        super.onResume()

        binding?.viewCaching?.apply {
            key.setText(R.string.view_caching)
            preferenceSwitch.visibility = View.VISIBLE
            preferenceSwitch.isClickable = true
            preferenceSwitch.isChecked = applicationDataStore.getViewCaching()
            preferenceSwitch.setOnCheckedChangeListener { _, isChecked ->
                applicationDataStore.setViewCaching(isChecked)
                updateSwitchSummary(R.string.view_caching_on, R.string.view_caching_off)
                toast(R.string.restart_to_apply_setting)
            }
            updateSwitchSummary(R.string.view_caching_on, R.string.view_caching_off)
            root.setOnClickListener {
                if (activity == null || !isAdded) return@setOnClickListener
                if (!preferenceSwitch.isChecked) {
                    MaterialDialog(requireContext()).show {
                        title(R.string.view_caching)
                        message(R.string.view_caching_warning)
                        getActionButton(WhichButton.POSITIVE).isEnabled = false
                        checkBoxPrompt(R.string.acknowledge) {
                            getActionButton(WhichButton.POSITIVE).isEnabled = it
                        }
                        positiveButton(R.string.submit) {
                            preferenceSwitch.toggle()
                        }
                        negativeButton(R.string.cancel)
                    }
                } else {
                    preferenceSwitch.toggle()
                }
            }
        }

        binding?.useReadingProgress?.apply {
            key.setText(R.string.saves_reading_progress)
            preferenceSwitch.visibility = View.VISIBLE
            preferenceSwitch.isClickable = true
            preferenceSwitch.isChecked = applicationDataStore.getReadingProgressStatus()
            preferenceSwitch.setOnCheckedChangeListener { _, isChecked ->
                applicationDataStore.setReadingProgressStatus(isChecked)
                updateSwitchSummary(R.string.reading_progress_on, R.string.reading_progress_off)
                toast(R.string.restart_to_apply_setting)
            }
            updateSwitchSummary(R.string.reading_progress_on, R.string.reading_progress_off)
            root.setOnClickListener {
                preferenceSwitch.toggle()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }
}