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
import com.laotoua.dawnislandk.data.local.entity.BrowsingHistoryAndPost
import com.laotoua.dawnislandk.data.local.entity.Post
import com.laotoua.dawnislandk.databinding.FragmentHistoryBrowsingBinding
import com.laotoua.dawnislandk.screens.SharedViewModel
import com.laotoua.dawnislandk.screens.adapters.*
import com.laotoua.dawnislandk.screens.posts.PostCardFactory
import com.laotoua.dawnislandk.screens.widgets.BaseNavFragment
import com.laotoua.dawnislandk.screens.widgets.popups.ImageViewerPopup
import com.laotoua.dawnislandk.util.ReadableTime
import com.lxj.xpopup.XPopup
import timber.log.Timber
import java.util.*
import kotlin.collections.ArrayList

class BrowsingHistoryFragment : BaseNavFragment() {

    companion object {
        fun newInstance() = BrowsingHistoryFragment()
    }

    private var binding: FragmentHistoryBrowsingBinding? = null

    private var mAdapter: QuickMultiBinder? = null

    private val viewModel: BrowsingHistoryViewModel by viewModels { viewModelFactory }

    private var endDate = Calendar.getInstance()
    private var startDate = Calendar.getInstance().apply { add(Calendar.WEEK_OF_YEAR, -1) }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        if (mAdapter == null) {
            mAdapter = QuickMultiBinder(sharedVM).apply {
                addItemBinder(DateStringBinder())
                addItemBinder(PostBinder(sharedVM).apply {
                    addChildClickViewIds(R.id.attachedImage)
                })
            }
        }
        if (binding != null) {
            Timber.d("Fragment View Reusing!")
        } else {
            Timber.d("Fragment View Created")
            binding = FragmentHistoryBrowsingBinding.inflate(inflater, container, false)

            binding!!.recyclerView.apply {
                setHasFixedSize(true)
                layoutManager = LinearLayoutManager(context)
                adapter = mAdapter
            }

            binding!!.startDate.text = ReadableTime.getDateString(startDate.time.time)
            binding!!.endDate.text = ReadableTime.getDateString(endDate.time.time)
            binding!!.startDate.setOnClickListener {
                MaterialDialog(requireContext()).show {
                    datePicker(currentDate = startDate) { _, date ->
                        setStartDate(date)
                    }
                }
            }

            binding!!.endDate.setOnClickListener {
                MaterialDialog(requireContext()).show {
                    datePicker(currentDate = endDate) { _, date ->
                        setEndDate(date)
                    }
                }
            }

            binding!!.confirmDate.setOnClickListener {
                if (startDate.before(endDate)) {
                    viewModel.searchByDate()
                } else {
                    Toast.makeText(context, R.string.data_range_selection_error, Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }
        return binding!!.root
    }

    private val listObs = Observer<List<BrowsingHistoryAndPost>> { list ->
        if (mAdapter == null || binding == null) return@Observer
        if (list.isEmpty()) {
            if (!mAdapter!!.hasEmptyView()) mAdapter?.setDefaultEmptyView()
            mAdapter!!.setDiffNewData(null)
            return@Observer
        }
        var lastDate: String? = null
        val data: MutableList<Any> = ArrayList()
        list.map {
            val dateString = ReadableTime.getDateString(
                it.browsingHistory.browsedDate,
                ReadableTime.DATE_ONLY_FORMAT
            )
            if (lastDate == null || dateString != lastDate) {
                data.add(dateString)
            }
            if (it.post != null) {
                data.add(it.post)
                lastDate = dateString
            }
        }
        mAdapter!!.setDiffNewData(data)
        mAdapter!!.setFooterView(
            layoutInflater.inflate(
                R.layout.view_no_more_data,
                binding!!.recyclerView,
                false
            )
        )
        Timber.i("${this.javaClass.simpleName} Adapter will have ${list.size} items")
    }

    override fun onResume() {
        super.onResume()
        viewModel.browsingHistoryList.observe(viewLifecycleOwner, listObs)
    }

    override fun onPause() {
        super.onPause()
        viewModel.browsingHistoryList.removeObserver(listObs)
    }

    private fun setStartDate(date: Calendar) {
        startDate = date
        viewModel.setStartDate(date.time)
        binding?.startDate?.text = ReadableTime.getDateString(date.time)
    }

    private fun setEndDate(date: Calendar) {
        endDate = date
        viewModel.setEndDate(date.time)
        binding?.endDate?.text = ReadableTime.getDateString(date.time)
    }

    private class DateStringBinder : QuickItemBinder<String>() {
        override fun convert(holder: BaseViewHolder, data: String) {
            holder.setText(R.id.text, data)
        }

        override fun getLayoutId(): Int = R.layout.list_item_simple_text
    }

    inner class PostBinder(private val sharedViewModel: SharedViewModel) : QuickItemBinder<Post>() {
        override fun convert(holder: BaseViewHolder, data: Post) {
            holder.convertUserId(data.userid, data.admin)
            holder.convertTitleAndName(data.getSimplifiedTitle(), data.getSimplifiedName())
            holder.convertRefId(context, data.id)
            holder.convertTimeStamp(data.now)
            holder.convertForumAndReplyCount(
                data.replyCount,
                sharedViewModel.getForumDisplayName(data.fid)
            )
            holder.convertSage(data.sage, data.skipSageConversion())
            holder.convertImage(data.getImgUrl())
            holder.convertContent(context, data.content)
        }

        override fun getLayoutId(): Int = R.layout.list_item_post

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
            val view = parent.getItemView(getLayoutId()).applyTextSizeAndLetterSpacing()
            PostCardFactory.applySettings(view as MaterialCardView)
            return BaseViewHolder(view)
        }

        override fun onClick(holder: BaseViewHolder, view: View, data: Post, position: Int) {
            val navAction = MainNavDirections.actionGlobalCommentsFragment(data.id, data.fid)
            findNavController().navigate(navAction)
        }

        override fun onChildClick(holder: BaseViewHolder, view: View, data: Post, position: Int) {
            if (view.id == R.id.attachedImage) {
                val viewerPopup = ImageViewerPopup(context)
                viewerPopup.setSingleSrcView(view as ImageView?, data)
                XPopup.Builder(context)
                    .asCustom(viewerPopup)
                    .show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (!DawnApp.applicationDataStore.getViewCaching()) {
            mAdapter = null
            binding = null
        }
        Timber.d("Fragment View Destroyed ${binding == null}")
    }
}