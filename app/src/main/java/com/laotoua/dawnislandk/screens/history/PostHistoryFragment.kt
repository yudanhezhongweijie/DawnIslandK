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

package com.laotoua.dawnislandk.screens.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.datetime.datePicker
import com.afollestad.materialdialogs.lifecycle.lifecycleOwner
import com.chad.library.adapter.base.binder.QuickItemBinder
import com.chad.library.adapter.base.util.getItemView
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.google.android.material.card.MaterialCardView
import com.laotoua.dawnislandk.DawnApp
import com.laotoua.dawnislandk.MainNavDirections
import com.laotoua.dawnislandk.R
import com.laotoua.dawnislandk.data.local.entity.PostHistory
import com.laotoua.dawnislandk.databinding.FragmentHistoryPostBinding
import com.laotoua.dawnislandk.screens.SharedViewModel
import com.laotoua.dawnislandk.screens.adapters.*
import com.laotoua.dawnislandk.screens.posts.PostCardFactory
import com.laotoua.dawnislandk.screens.util.Layout.toast
import com.laotoua.dawnislandk.screens.widgets.BaseNavFragment
import com.laotoua.dawnislandk.screens.widgets.SectionHeader
import com.laotoua.dawnislandk.screens.widgets.popups.ImageViewerPopup
import com.laotoua.dawnislandk.util.ReadableTime
import com.lxj.xpopup.XPopup
import timber.log.Timber
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*

class PostHistoryFragment : BaseNavFragment() {

    companion object {
        fun newInstance() = PostHistoryFragment()
    }

    private var binding: FragmentHistoryPostBinding? = null
    private var mAdapter: QuickMultiBinder? = null
    private var viewCaching = false

    private val viewModel: PostHistoryViewModel by viewModels { viewModelFactory }

    private var showNewPosts = true
    private var showReplies = true

    private var postHeader = SectionHeader("发布") { togglePosts() }
    private var replyHeader = SectionHeader("回复") { toggleReplies() }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        if (mAdapter == null) {
            mAdapter = QuickMultiBinder(sharedVM).apply {
                addItemBinder(PostHistoryBinder(sharedVM).apply {
                    addChildClickViewIds(R.id.attachedImage)
                }, PostHistoryDiffer())
                addItemBinder(DateStringBinder(), DateStringDiffer())
                addItemBinder(SectionHeaderBinder(), SectionHeaderDiffer())
                loadMoreModule.isEnableLoadMore = false
                loadMoreModule.enableLoadMoreEndClick = false
            }
        }
        if (binding != null) {
            Timber.d("Fragment View Reusing!")
        } else {
            Timber.d("Fragment View Created")
            binding = FragmentHistoryPostBinding.inflate(inflater, container, false)

            binding!!.recyclerView.apply {
                setHasFixedSize(true)
                layoutManager = LinearLayoutManager(context)
                adapter = mAdapter
            }

            binding!!.startDate.text = ReadableTime.getDateString(viewModel.startDate)
            binding!!.endDate.text = ReadableTime.getDateString(viewModel.endDate)
            binding!!.startDate.setOnClickListener {
                if (activity == null || !isAdded) return@setOnClickListener
                MaterialDialog(requireContext()).show {
                    lifecycleOwner(this@PostHistoryFragment)
                    datePicker(currentDate = ReadableTime.localDateTimeToCalendarDate(viewModel.startDate)) { _, date ->
                        setStartDate(date)
                    }
                }
            }

            binding!!.endDate.setOnClickListener {
                if (activity == null || !isAdded) return@setOnClickListener
                MaterialDialog(requireContext()).show {
                    lifecycleOwner(this@PostHistoryFragment)
                    datePicker(currentDate = ReadableTime.localDateTimeToCalendarDate(viewModel.endDate)) { _, date ->
                        setEndDate(date)
                    }
                }
            }

            binding!!.confirmDate.setOnClickListener {
                if (activity == null || !isAdded) return@setOnClickListener
                if (viewModel.startDate.toLocalDate().isBefore(viewModel.endDate.toLocalDate())) {
                    viewModel.searchByDate()
                } else {
                    toast(R.string.data_range_selection_error)
                }
            }
        }

        sharedVM.currentDomain.observe(viewLifecycleOwner) {
            viewModel.changeDomain(it)
        }

        viewModel.postHistoryList.observe(viewLifecycleOwner) { list ->
            if (mAdapter == null || binding == null || activity == null || !isAdded) return@observe
            if (list.isEmpty()) {
                mAdapter?.showNoData()
                return@observe
            }
            displayList(list)
        }

        viewCaching = false
        return binding!!.root
    }

    private fun setStartDate(date: Calendar) {
        viewModel.startDate = LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault())
        binding?.startDate?.text = ReadableTime.getDateString(viewModel.startDate)
    }

    private fun setEndDate(date: Calendar) {
        viewModel.endDate = LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault())
        binding?.endDate?.text = ReadableTime.getDateString(viewModel.endDate)
    }

    private fun togglePosts() {
        showNewPosts = !showNewPosts
        displayList(viewModel.postHistoryList.value ?: emptyList())
    }

    private fun toggleReplies() {
        showReplies = !showReplies
        displayList(viewModel.postHistoryList.value ?: emptyList())
    }

    private fun displayList(list: List<PostHistory>) {
        var lastDate: String? = null
        val data: MutableList<Any> = ArrayList()
        data.add(postHeader)
        if (showNewPosts) {
            list.filter { it.newPost }.run {
                map {
                    val dateString = ReadableTime.getDateString(it.postDateTime)
                    if (lastDate == null || dateString != lastDate) {
                        data.add(dateString)
                    }
                    data.add(it)
                    lastDate = dateString
                }
            }
        }
        data.add(replyHeader)
        if (showReplies) {
            list.filterNot { it.newPost }.run {
                lastDate = null
                map {
                    val dateString = ReadableTime.getDateString(it.postDateTime)
                    if (lastDate == null || dateString != lastDate) {
                        data.add(dateString)
                    }
                    data.add(it)
                    lastDate = dateString
                }
            }
        }
        mAdapter?.setDiffNewData(data)
        mAdapter?.setFooterView(
            layoutInflater.inflate(
                R.layout.view_no_more_data,
                binding!!.recyclerView,
                false
            )
        )

        Timber.i("${this.javaClass.simpleName} Adapter will have ${list.size} items")
    }

    inner class PostHistoryBinder(private val sharedViewModel: SharedViewModel) :
        QuickItemBinder<PostHistory>() {
        override fun convert(holder: BaseViewHolder, data: PostHistory) {
            val cookieDisplayName =
                DawnApp.applicationDataStore.getCookieDisplayName(data.cookieName)
                    ?: data.cookieName
            holder.convertUserHash(cookieDisplayName, "", cookieDisplayName)
                .convertRefId(context, data.id)
                .convertTitleAndName("", "")
                .convertTimeStamp(data.postDateTime)
                .convertForumAndReplyCount(
                    "",
                    sharedViewModel.getForumOrTimelineDisplayName(data.postTargetFid)
                )
                .convertContent(context, data.content)
                .convertImage(data.getImgUrl())
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
            val view = parent.getItemView(getLayoutId()).applyTextSizeAndLetterSpacing()
            PostCardFactory.applySettings(view as MaterialCardView)
            return BaseViewHolder(view)
        }

        override fun getLayoutId(): Int = R.layout.list_item_post

        override fun onChildClick(
            holder: BaseViewHolder,
            view: View,
            data: PostHistory,
            position: Int
        ) {
            if (activity == null || !isAdded) return
            if (view.id == R.id.attachedImage) {
                val viewerPopup = ImageViewerPopup(context)
                viewerPopup.setSingleSrcView(view as ImageView?, data)
                XPopup.Builder(context)
                    .isDestroyOnDismiss(true)
                    .asCustom(viewerPopup)
                    .show()
            }
        }

        override fun onClick(holder: BaseViewHolder, view: View, data: PostHistory, position: Int) {
            if (activity == null || !isAdded) return
            viewCaching = DawnApp.applicationDataStore.getViewCaching()
            if (data.newPost) {
                val navAction = MainNavDirections.actionGlobalCommentsFragment(data.id, data.postTargetFid)
                findNavController().navigate(navAction)
            } else {
                val navAction =
                    MainNavDirections.actionGlobalCommentsFragment(
                        data.postTargetId,
                        data.postTargetFid
                    )
                navAction.targetPage = data.postTargetPage
                findNavController().navigate(navAction)
            }
        }
    }

    inner class SectionHeaderBinder : QuickItemBinder<SectionHeader>() {
        override fun convert(holder: BaseViewHolder, data: SectionHeader) {
            holder.setText(R.id.text, data.text)
            if (data.clickListener == null) {
                holder.setGone(R.id.icon, true)
            } else {
                holder.setVisible(R.id.icon, true)
            }
        }

        override fun onClick(holder: BaseViewHolder, view: View, data: SectionHeader, position: Int) {
            if (activity == null || !isAdded) return
            if (data.clickListener == null) return
            data.clickListener.onClick(view)
            data.isExpanded = !data.isExpanded
            val icon: ImageView = holder.getView(R.id.icon)
            if (data.isExpanded) {
                icon.animate().setDuration(200)
                    .setInterpolator(DecelerateInterpolator())
                    .rotation(0f)
                    .start()
            } else {
                icon.animate().setDuration(200)
                    .setInterpolator(DecelerateInterpolator())
                    .rotation(180f)
                    .start()
            }
        }

        override fun getLayoutId(): Int = R.layout.list_item_section_header
    }

    private class DateStringBinder : QuickItemBinder<String>() {
        override fun convert(holder: BaseViewHolder, data: String) {
            holder.setText(R.id.text, data)
        }

        override fun getLayoutId(): Int = R.layout.list_item_simple_text
    }


    private class PostHistoryDiffer : DiffUtil.ItemCallback<PostHistory>() {
        override fun areItemsTheSame(oldItem: PostHistory, newItem: PostHistory): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: PostHistory, newItem: PostHistory): Boolean {
            return true
        }
    }

    private class SectionHeaderDiffer : DiffUtil.ItemCallback<SectionHeader>() {
        override fun areItemsTheSame(oldItem: SectionHeader, newItem: SectionHeader): Boolean {
            return oldItem.text == newItem.text
        }

        override fun areContentsTheSame(oldItem: SectionHeader, newItem: SectionHeader): Boolean {
            return true
        }
    }

    private class DateStringDiffer : DiffUtil.ItemCallback<String>() {
        override fun areItemsTheSame(oldItem: String, newItem: String): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(oldItem: String, newItem: String): Boolean {
            return true
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (!viewCaching) {
            mAdapter = null
            binding = null
        }
        Timber.d("Fragment View Destroyed ${binding == null}")
    }

}