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
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.afollestad.materialdialogs.MaterialDialog
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
    ): View? {
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
                }
                val nonTimeline = serverForums!!.filter { f -> f.id != "-1" }
                val blockingFidIndex = mutableListOf<Int>()
                for ((ind, f) in nonTimeline.withIndex()) {
                    if (blockedForumIds!!.contains(f.id)) {
                        blockingFidIndex.add(ind)
                    }
                }

                MaterialDialog(requireContext()).show {
                    title(R.string.timeline_filter_setting)
                    message(R.string.please_select_timeline_filter_forums)
                    listItemsMultiChoice(
                        items = nonTimeline.map { f -> ContentTransformation.htmlToSpanned(f.getDisplayName()) },
                        initialSelection = blockingFidIndex.toIntArray(),
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
                sharedViewModel.getForumDisplayName(applicationDataStore.getDefaultForumId())
            )
            root.setOnClickListener {
                if (activity == null || !isAdded) return@setOnClickListener
                if (serverForums == null) {
                    toast(R.string.please_try_again_later)
                }
                MaterialDialog(requireContext()).show {
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
                            sharedViewModel.getForumDisplayName(applicationDataStore.getDefaultForumId())
                        )
                    }
                    positiveButton(R.string.submit)
                    negativeButton(R.string.cancel)
                }
            }
        }

        binding?.defaultSubscriptionPage?.apply {
            key.setText(R.string.edit_subscription_default_page)
            val items = listOf(getString(R.string.trend), getString(R.string.my_feed))
            val trendIndex = applicationDataStore.getFeedPagerPageIndices().first
            if (trendIndex == 0){
                summary.text = getString(R.string.trend)
            } else {
                summary.text = getString(R.string.my_feed)
            }
            root.setOnClickListener {
                if (activity == null || !isAdded) return@setOnClickListener
                MaterialDialog(requireContext()).show {
                    title(R.string.edit_subscription_default_page)
                    listItemsSingleChoice(items = items) { _, index, _ ->
                        applicationDataStore.setFeedPagerDefaultPage(index, 1 - index)
                        toast(R.string.restart_to_apply_setting)
                        if (index == 0){
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
            val items = listOf(getString(R.string.browsing_history), getString(R.string.post_history))
            val browsingHistoryIndex = applicationDataStore.getHistoryPagerPageIndices().first
            if (browsingHistoryIndex == 0){
                summary.text = getString(R.string.browsing_history)
            } else {
                summary.text = getString(R.string.post_history)
            }
            root.setOnClickListener {
                if (activity == null || !isAdded) return@setOnClickListener
                MaterialDialog(requireContext()).show {
                    title(R.string.edit_history_default_page)
                    listItemsSingleChoice(items = items) { _, index, _ ->
                        applicationDataStore.setHistoryPagerDefaultPage(index, 1 - index)
                        toast(R.string.restart_to_apply_setting)
                        if (index == 0){
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

        viewModel.timelineBlockedForumIds.observe(viewLifecycleOwner, Observer {
            blockedForumIds = it
            binding?.timelineFilter?.summary?.text = resources.getString(
                R.string.timeline_filtered_count,
                it.size
            )
        })

        sharedViewModel.communityList.observe(viewLifecycleOwner, Observer {
            serverForums = it.data?.filterNot { c -> c.isCommonForums() || c.isCommonPosts() }
                ?.flatMap { c -> c.forums }

            binding?.commonForums?.summary?.text = resources.getString(
                R.string.common_forum_count,
                it.data?.firstOrNull { c -> c.isCommonForums() }?.forums?.size ?: 0
            )

            binding?.commonPosts?.summary?.text = resources.getString(
                R.string.common_posts_count,
                it.data?.firstOrNull { c -> c.isCommonPosts() }?.forums?.size ?: 0
            )
        })

        return binding!!.root
    }


    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }
}