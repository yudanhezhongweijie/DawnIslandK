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
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.WhichButton
import com.afollestad.materialdialogs.actions.setActionButtonEnabled
import com.afollestad.materialdialogs.input.getInputField
import com.afollestad.materialdialogs.input.input
import com.afollestad.materialdialogs.lifecycle.lifecycleOwner
import com.afollestad.materialdialogs.list.listItemsMultiChoice
import com.afollestad.materialdialogs.list.listItemsSingleChoice
import com.laotoua.dawnislandk.DawnApp.Companion.applicationDataStore
import com.laotoua.dawnislandk.R
import com.laotoua.dawnislandk.data.local.entity.BlockedId
import com.laotoua.dawnislandk.data.local.entity.Forum
import com.laotoua.dawnislandk.databinding.FragmentCustomSettingBinding
import com.laotoua.dawnislandk.screens.SharedViewModel
import com.laotoua.dawnislandk.screens.util.ContentTransformation
import com.laotoua.dawnislandk.screens.util.Layout.toast
import com.laotoua.dawnislandk.screens.util.Layout.updateSwitchSummary
import com.laotoua.dawnislandk.util.DawnConstants
import dagger.android.support.DaggerFragment
import javax.inject.Inject

class CustomSettingFragment : DaggerFragment() {

    private var binding: FragmentCustomSettingBinding? = null

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private val viewModel: CustomSettingViewModel by viewModels { viewModelFactory }

    private val sharedViewModel: SharedViewModel by activityViewModels { viewModelFactory }

    private var blockedForumIds: List<String>? = null

    private var serverForums: List<Forum>? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentCustomSettingBinding.inflate(inflater, container, false)
        binding?.commonForums?.apply {
            key.setText(R.string.common_forum_setting)
            root.setOnClickListener {
                if (activity == null || !isAdded) return@setOnClickListener
                val action =
                    CustomSettingFragmentDirections.actionCustomSettingsFragmentToCommonForumsFragment()
                findNavController().navigate(action)
            }
        }

        binding?.commonPosts?.apply {
            key.setText(R.string.common_posts_setting)
            root.setOnClickListener {
                if (activity == null || !isAdded) return@setOnClickListener
                val action =
                    CustomSettingFragmentDirections.actionCustomSettingsFragmentToCommonPostsFragment()
                findNavController().navigate(action)
            }
        }

        binding?.timelineFilter?.apply {
            key.setText(R.string.timeline_filter_setting)
            root.setOnClickListener {
                if (activity == null || !isAdded) return@setOnClickListener
                if (blockedForumIds == null || serverForums == null) {
                    toast(R.string.please_try_again_later)
                    return@setOnClickListener
                }
                val nonTimeline = serverForums!!.filter { f -> f.id != DawnConstants.TIMELINE_COMMUNITY_ID }
                val blockingFidIndices = mutableListOf<Int>()
                for ((ind, f) in nonTimeline.withIndex()) {
                    if (blockedForumIds!!.contains(f.id)) {
                        blockingFidIndices.add(ind)
                    }
                }

                MaterialDialog(requireContext()).show {
                    lifecycleOwner(this@CustomSettingFragment)
                    title(R.string.timeline_filter_setting)
                    message(R.string.please_select_timeline_filter_forums)
                    listItemsMultiChoice(
                        items = nonTimeline.map { f -> ContentTransformation.htmlToSpanned(f.getDisplayName()) },
                        initialSelection = blockingFidIndices.toIntArray(),
                        allowEmptySelection = true
                    ) { _, indices, _ ->
                        val blockedForumIds = mutableListOf<BlockedId>()
                        for (i in indices) {
                            blockedForumIds.add(BlockedId.makeTimelineBlockedForum(nonTimeline[i].id))
                        }
                        viewModel.blockForums(blockedForumIds)
                        toast(R.string.might_need_to_restart_to_apply_setting)
                    }
                    positiveButton(R.string.submit)
                    negativeButton(R.string.cancel)
                    @Suppress("DEPRECATION")
                    neutralButton(R.string.uncheck_all_items) {
                        viewModel.blockForums(emptyList())
                    }
                }
            }
        }

        binding?.defaultForum?.apply {
            key.setText(R.string.default_forum_setting)
            summary.text = ContentTransformation.htmlToSpanned(
                sharedViewModel.getForumOrTimelineDisplayName(applicationDataStore.getDefaultForumId())
            )
            root.setOnClickListener {
                if (activity == null || !isAdded) return@setOnClickListener
                if (serverForums == null) {
                    toast(R.string.please_try_again_later)
                    return@setOnClickListener
                }
                MaterialDialog(requireContext()).show {
                    lifecycleOwner(this@CustomSettingFragment)
                    title(R.string.default_forum_setting)
                    val items =
                        serverForums!!.map { ContentTransformation.htmlToSpanned(it.getDisplayName()) }
                    listItemsSingleChoice(
                        items = items,
                        waitForPositiveButton = true
                    ) { _, index, _ ->
                        val fid = serverForums!![index].id
                        applicationDataStore.setDefaultForumId(fid)
                        summary.text = ContentTransformation.htmlToSpanned(
                            sharedViewModel.getForumOrTimelineDisplayName(applicationDataStore.getDefaultForumId())
                        )
                    }
                    positiveButton(R.string.submit)
                    negativeButton(R.string.cancel)
                }
            }
        }

        binding?.defaultExpandCommunities?.apply {
            key.setText(R.string.communities_expansion_setting)
            val communities = sharedViewModel.communityList.value?.data ?: listOf()
            val expandedIDs = applicationDataStore.getExpandedCommunityIDs()
            val expandedNames = mutableListOf<String>()
            if (expandedIDs.contains(DawnConstants.TIMELINE_COMMUNITY_ID)) expandedNames.add(requireContext().getString(R.string.timeline))
            expandedIDs.filterNot { it == DawnConstants.TIMELINE_COMMUNITY_ID }.map { id -> communities.find { c -> c.id == id }?.name }
                .forEach { name -> if (!name.isNullOrBlank()) expandedNames.add(name) }

            summary.text = getString(R.string.communities_expansion_setting_summary, expandedNames.joinToString(","))
            root.setOnClickListener {
                if (activity == null || !isAdded) return@setOnClickListener
                if (serverForums == null) {
                    toast(R.string.please_try_again_later)
                    return@setOnClickListener
                }
                MaterialDialog(requireContext()).show {
                    lifecycleOwner(this@CustomSettingFragment)
                    title(R.string.communities_expansion_setting)
                    val items = mutableListOf<String>()
                    items.add(context.getString(R.string.timeline))
                    items.addAll(communities.map { it.name })
                    val expanded = applicationDataStore.getExpandedCommunityIDs()
                    // find items indices
                    val expandedIndices = mutableListOf<Int>()
                    // timeline
                    if (expanded.contains(DawnConstants.TIMELINE_COMMUNITY_ID)) expandedIndices.add(0)
                    // communities
                    for ((ind, c) in communities.withIndex()) {
                        if (expanded.contains(c.id)) {
                            expandedIndices.add(ind + 1)
                        }
                    }
                    listItemsMultiChoice(
                        items = items,
                        initialSelection = expandedIndices.toIntArray(),
                        allowEmptySelection = true
                    ) { _, indices, _ ->
                        val newIDs = mutableListOf<String>()
                        if (indices.contains(0)) newIDs.add(DawnConstants.TIMELINE_COMMUNITY_ID)
                        newIDs.addAll(indices.filterNot { it == 0 }.map { communities[it - 1].id })
                        applicationDataStore.setExpandedCommunityIDs(newIDs.toSet())
                        val names = mutableListOf<String>()
                        if (newIDs.contains(DawnConstants.TIMELINE_COMMUNITY_ID)) names.add(requireContext().getString(R.string.timeline))
                        newIDs.filterNot { it == DawnConstants.TIMELINE_COMMUNITY_ID }.map { id -> communities.find { c -> c.id == id }?.name }
                            .forEach { name -> if (!name.isNullOrBlank()) names.add(name) }
                        summary.text = getString(R.string.communities_expansion_setting_summary, names.joinToString(","))
                    }
                    positiveButton(R.string.submit)
                    negativeButton(R.string.cancel)
                }
            }
        }

        binding?.defaultSubscriptionPage?.apply {
            key.setText(R.string.edit_subscription_default_page)
            val items = listOf(getString(R.string.trend), getString(R.string.my_feed))
            summary.text = if (applicationDataStore.getSubscriptionPagerFeedIndex() == 1) {
                getString(R.string.trend)
            } else {
                getString(R.string.my_feed)
            }
            root.setOnClickListener {
                if (activity == null || !isAdded) return@setOnClickListener
                MaterialDialog(requireContext()).show {
                    lifecycleOwner(this@CustomSettingFragment)
                    title(R.string.edit_subscription_default_page)
                    listItemsSingleChoice(items = items) { _, index, _ ->
                        applicationDataStore.setSubscriptionPagerFeedIndex(1 - index)
                        toast(R.string.restart_to_apply_setting)
                        if (index == 1) {
                            summary.text = getString(R.string.trend)
                        } else {
                            summary.text = getString(R.string.my_feed)
                        }
                    }
                    positiveButton(R.string.submit)
                    negativeButton(R.string.cancel)
                }
            }
        }

        binding?.defaultHistoryPage?.apply {
            key.setText(R.string.edit_history_default_page)
            val items =
                listOf(getString(R.string.browsing_history), getString(R.string.post_history))
            summary.text = if (applicationDataStore.getHistoryPagerBrowsingIndex() == 0) {
                getString(R.string.browsing_history)
            } else {
                getString(R.string.post_history)
            }
            root.setOnClickListener {
                if (activity == null || !isAdded) return@setOnClickListener
                MaterialDialog(requireContext()).show {
                    lifecycleOwner(this@CustomSettingFragment)
                    title(R.string.edit_history_default_page)
                    listItemsSingleChoice(items = items) { _, index, _ ->
                        applicationDataStore.setHistoryPagerBrowsingIndex(index)
                        toast(R.string.restart_to_apply_setting)
                        if (index == 0) {
                            summary.text = getString(R.string.browsing_history)
                        } else {
                            summary.text = getString(R.string.post_history)
                        }
                    }
                    positiveButton(R.string.submit)
                    negativeButton(R.string.cancel)
                }
            }
        }

        binding?.emojiSetting?.apply {
            key.setText(R.string.emoji_setting)
            preferenceSwitch.visibility = View.VISIBLE
            preferenceSwitch.isClickable = true
            preferenceSwitch.isChecked = applicationDataStore.getSortEmojiByLastUsedStatus()
            updateSwitchSummary(
                R.string.emoji_sort_by_last_used_at_on,
                R.string.emoji_sort_by_last_used_at_off
            )
            preferenceSwitch.setOnCheckedChangeListener { _, isChecked ->
                applicationDataStore.setSortEmojiByLastUsedStatus(isChecked)
                updateSwitchSummary(
                    R.string.emoji_sort_by_last_used_at_on,
                    R.string.emoji_sort_by_last_used_at_off
                )
            }
            root.setOnClickListener {
                if (activity == null || !isAdded) return@setOnClickListener
                val action =
                    CustomSettingFragmentDirections.actionCustomSettingFragmentToEmojiSettingFragment()
                findNavController().navigate(action)
            }
        }

        binding?.setBaseCdn?.apply {
            key.setText(R.string.set_base_cdn)
            var baseCDN = applicationDataStore.getBaseCDN()
            summary.text = if (baseCDN == "auto") "自动" else baseCDN
            root.setOnClickListener {
                if (activity == null || !isAdded) return@setOnClickListener
                MaterialDialog(requireActivity()).show {
                    lifecycleOwner(this@CustomSettingFragment)
                    title(R.string.set_base_cdn)
                    listItemsSingleChoice(R.array.base_cdn_options, waitForPositiveButton = true) { _, index, text ->
                        when (index) {
                            0, 1, 2, 3 -> {
                                baseCDN = if (index == 0) "auto" else text.toString()
                                applicationDataStore.setBaseCDN(baseCDN)
                                summary.text = if (index == 0) "自动" else baseCDN
                                toast(R.string.restart_to_apply_setting)
                            }
                            4 -> {
                                MaterialDialog(requireActivity()).show {
                                    lifecycleOwner(this@CustomSettingFragment)
                                    title(R.string.set_base_cdn)
                                    message(R.string.set_base_cdn_prompt)
                                    input(
                                        hint = baseCDN,
                                        prefill = if (baseCDN == "auto") "" else baseCDN,
                                        waitForPositiveButton = false
                                    ) { dialog, text ->
                                        val inputField = dialog.getInputField()
                                        val isValid = text.startsWith("https://", true)
                                        inputField.error = if (isValid) null else "必须以https://开始"
                                        dialog.setActionButtonEnabled(WhichButton.POSITIVE, isValid)
                                    }
                                    positiveButton(R.string.submit) {
                                        baseCDN = getInputField().text.toString()
                                        summary.text = baseCDN
                                        applicationDataStore.setBaseCDN(baseCDN)
                                        toast(R.string.restart_to_apply_setting)
                                    }
                                    negativeButton(R.string.cancel)
                                }
                            }
                        }
                    }
                    positiveButton(R.string.submit)
                    negativeButton(R.string.cancel)
                }
            }
        }

        binding?.setRefCdn?.apply {
            key.setText(R.string.set_ref_cdn)
            var refCDN = applicationDataStore.getRefCDN()
            summary.text = if (refCDN == "auto") "自动" else refCDN
            root.setOnClickListener {
                if (activity == null || !isAdded) return@setOnClickListener
                MaterialDialog(requireActivity()).show {
                    title(R.string.set_ref_cdn)
                    lifecycleOwner(this@CustomSettingFragment)
                    listItemsSingleChoice(R.array.ref_cdn_options, waitForPositiveButton = true) { _, index, text ->
                        when (index) {
                            0, 1, 2 -> {
                                refCDN = if (index == 0) "auto" else text.toString()
                                summary.text = if (index == 0) "自动" else refCDN
                                applicationDataStore.setRefCDN(refCDN)
                                toast(R.string.restart_to_apply_setting)
                            }
                            3 -> {
                                MaterialDialog(requireActivity()).show {
                                    lifecycleOwner(this@CustomSettingFragment)
                                    title(R.string.set_ref_cdn)
                                    message(R.string.set_ref_cdn_prompt)
                                    input(
                                        hint = refCDN,
                                        prefill = if (refCDN == "auto") "" else refCDN,
                                        waitForPositiveButton = false
                                    ) { dialog, text ->
                                        val inputField = dialog.getInputField()
                                        val isValid = text.startsWith(
                                            "https://",
                                            true
                                        ) && !text.endsWith(DawnConstants.fastMirrorHost, true)
                                        inputField.error =
                                            if (isValid) null else "必须以https://开始且不能使用https://nmb.fastmirror.org"
                                        dialog.setActionButtonEnabled(WhichButton.POSITIVE, isValid)
                                    }
                                    positiveButton(R.string.submit) {
                                        refCDN = getInputField().text.toString()
                                        summary.text = refCDN
                                        applicationDataStore.setRefCDN(refCDN)
                                        toast(R.string.restart_to_apply_setting)
                                    }
                                    negativeButton(R.string.cancel)
                                }
                            }
                        }
                    }
                    positiveButton(R.string.submit)
                    negativeButton(R.string.cancel)
                }
            }
        }

        viewModel.timelineBlockedForumIds.observe(viewLifecycleOwner) {
            blockedForumIds = it
            binding?.timelineFilter?.summary?.text = resources.getString(
                R.string.timeline_filtered_count,
                it.size
            )
        }

        sharedViewModel.communityList.observe(viewLifecycleOwner) {
            serverForums = it.data?.filterNot { c -> c.isCommonForums() || c.isCommonPosts() }
                ?.map { c -> c.forums }?.flatten()

            binding?.commonForums?.summary?.text = resources.getString(
                R.string.common_forum_count,
                it.data?.firstOrNull { c -> c.isCommonForums() }?.forums?.size ?: 0
            )

            binding?.commonPosts?.summary?.text = resources.getString(
                R.string.common_posts_count,
                it.data?.firstOrNull { c -> c.isCommonPosts() }?.forums?.size ?: 0
            )
        }

        return binding!!.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }
}