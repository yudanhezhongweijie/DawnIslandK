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
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.datetime.datePicker
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
import com.laotoua.dawnislandk.screens.widgets.BaseNavFragment
import com.laotoua.dawnislandk.screens.widgets.SectionHeader
import com.laotoua.dawnislandk.screens.widgets.popups.ImageViewerPopup
import com.laotoua.dawnislandk.util.ReadableTime
import com.lxj.xpopup.XPopup
import timber.log.Timber
import java.util.*

class PostHistoryFragment : BaseNavFragment() {

    companion object {
        fun newInstance() = PostHistoryFragment()
    }

    private var _binding: FragmentHistoryPostBinding? = null
    private val binding: FragmentHistoryPostBinding get() = _binding!!
    private var _mAdapter: QuickMultiBinder? = null
    private val mAdapter: QuickMultiBinder get() = _mAdapter!!

    private val viewModel: PostHistoryViewModel by viewModels { viewModelFactory }

    private var endDate = Calendar.getInstance()
    private var startDate = Calendar.getInstance().apply { add(Calendar.DATE, -30) }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        if (_mAdapter == null) {
            _mAdapter = QuickMultiBinder(sharedVM).apply {
                addItemBinder(PostHistoryBinder(sharedVM).apply {
                    addChildClickViewIds(R.id.attachedImage)
                })
                addItemBinder(DateStringBinder())
                addItemBinder(SectionHeaderBinder().apply {
                    addChildClickViewIds(R.id.button)
                })
            }
        }
        if (_binding != null) {
            Timber.d("Fragment View Reusing!")
        } else {
            Timber.d("Fragment View Created")
            _binding = FragmentHistoryPostBinding.inflate(inflater, container, false)

            binding.recyclerView.apply {
                setHasFixedSize(true)
                layoutManager = LinearLayoutManager(context)
                adapter = mAdapter
            }

            binding.startDate.text = ReadableTime.getDateString(startDate.time)
            binding.endDate.text = ReadableTime.getDateString(endDate.time)
            binding.startDate.setOnClickListener {
                MaterialDialog(requireContext()).show {
                    datePicker(currentDate = startDate) { _, date ->
                        setStartDate(date)
                    }
                }
            }

            binding.endDate.setOnClickListener {
                MaterialDialog(requireContext()).show {
                    datePicker(currentDate = endDate) { _, date ->
                        setEndDate(date)
                    }
                }
            }

            binding.confirmDate.setOnClickListener {
                if (startDate.before(endDate)) {
                    viewModel.searchByDate()
                } else {
                    Toast.makeText(context, R.string.data_range_selection_error, Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }
        return binding.root
    }

    private val listObs = Observer<List<PostHistory>> { list ->
        if (_mAdapter == null || _binding == null) return@Observer
        if (list.isEmpty()) {
            if (!mAdapter.hasEmptyView()) mAdapter.setDefaultEmptyView()
            mAdapter.setDiffNewData(null)
            return@Observer
        }
        var lastDate: String? = null
        val data: MutableList<Any> = ArrayList()
        list.filter { it.newPost }.run {
            data.add(SectionHeader("发布"))
            map {
                val dateString = ReadableTime.getDateString(
                    it.postDate,
                    ReadableTime.DATE_ONLY_FORMAT
                )
                if (lastDate == null || dateString != lastDate) {
                    data.add(dateString)
                }
                data.add(it)
                lastDate = dateString
            }
        }
        list.filterNot { it.newPost }.run {
            data.add(SectionHeader("回复"))
            lastDate = null
            map {
                val dateString = ReadableTime.getDateString(
                    it.postDate,
                    ReadableTime.DATE_ONLY_FORMAT
                )
                if (lastDate == null || dateString != lastDate) {
                    data.add(dateString)
                }
                data.add(it)
                lastDate = dateString
            }
        }
        mAdapter.setDiffNewData(data)
        mAdapter.setFooterView(
            layoutInflater.inflate(
                R.layout.view_no_more_data,
                binding.recyclerView,
                false
            )
        )
        Timber.i("${this.javaClass.simpleName} Adapter will have ${list.size} items")
    }

    override fun onResume() {
        super.onResume()
        viewModel.postHistoryList.observe(viewLifecycleOwner, listObs)
    }

    override fun onPause() {
        super.onPause()
        viewModel.postHistoryList.removeObserver(listObs)
    }

    private fun setStartDate(date: Calendar) {
        startDate = date
        viewModel.setStartDate(date.time)
        _binding?.startDate?.text = ReadableTime.getDateString(date.time)
    }

    private fun setEndDate(date: Calendar) {
        endDate = date
        viewModel.setEndDate(date.time)
        _binding?.endDate?.text = ReadableTime.getDateString(date.time)
    }

    inner class PostHistoryBinder(private val sharedViewModel: SharedViewModel) :
        QuickItemBinder<PostHistory>() {
        override fun convert(holder: BaseViewHolder, data: PostHistory) {
            val cookieDisplayName =
                DawnApp.applicationDataStore.getCookieDisplayName(data.cookieName)
                    ?: data.cookieName
            holder.convertUserId(cookieDisplayName, "", cookieDisplayName)
            holder.convertRefId(context, data.id)
            holder.convertTitleAndName("", "")
            holder.convertTimeStamp(data.postDate)
            holder.convertForumAndReplyCount(
                "",
                sharedViewModel.getForumDisplayName(data.postTargetFid)
            )
            holder.convertContent(context, data.content)
            holder.convertImage(data.getImgUrl())
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
            if (view.id == R.id.attachedImage) {
                val viewerPopup = ImageViewerPopup(context)
                viewerPopup.setSingleSrcView(view as ImageView?, data)
                XPopup.Builder(context)
                    .asCustom(viewerPopup)
                    .show()
            }
        }

        override fun onClick(holder: BaseViewHolder, view: View, data: PostHistory, position: Int) {
            if (data.newPost) {
                val navAction =
                    MainNavDirections.actionGlobalCommentsFragment(data.id, data.postTargetFid)
                findNavController().navigate(navAction)
            } else {
                val navAction =
                    MainNavDirections.actionGlobalCommentsFragment(data.postTargetId, data.postTargetFid)
                navAction.targetPage = data.postTargetPage
                findNavController().navigate(navAction)
            }
        }
    }

    private class SectionHeaderBinder :
        QuickItemBinder<SectionHeader>() {
        override fun convert(holder: BaseViewHolder, data: SectionHeader) {
            holder.setText(R.id.text, data.text)
            if (data.clickListener == null) {
                holder.setGone(R.id.button, true)
            } else {
                holder.setVisible(R.id.button, true)
            }
        }

        override fun onChildClick(
            holder: BaseViewHolder,
            view: View,
            data: SectionHeader,
            position: Int
        ) {
            if (view.id == R.id.button) data.clickListener?.onClick(view)
        }

        override fun getLayoutId(): Int = R.layout.list_item_section_header
    }

    private class DateStringBinder : QuickItemBinder<String>() {
        override fun convert(holder: BaseViewHolder, data: String) {
            holder.setText(R.id.text, data)
        }

        override fun getLayoutId(): Int = R.layout.list_item_simple_text
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (!DawnApp.applicationDataStore.viewCaching) {
            _mAdapter = null
            _binding = null
        }
        Timber.d("Fragment View Destroyed ${_binding == null}")
    }

}